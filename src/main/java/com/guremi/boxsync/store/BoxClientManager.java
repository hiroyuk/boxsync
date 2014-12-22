package com.guremi.boxsync.store;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.BoxRESTClient;
import com.box.boxjavalibv2.dao.BoxOAuthToken;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxServerException;
import com.box.restclientv2.IBoxRestVisitor;
import com.box.restclientv2.exceptions.BoxRestException;
import com.guremi.boxsync.App;
import com.guremi.boxsync.oauth2.LocalServer;
import com.guremi.boxsync.oauth2.SecureAuthStorage;
import com.typesafe.config.Config;
import java.awt.Desktop;
import java.net.URLEncoder;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.RequestLine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoxClientManager {
    private static final Logger LOG = LoggerFactory.getLogger(BoxClientManager.class);
    private SecureAuthStorage sas = new SecureAuthStorage();

    private class RestVisitor implements IBoxRestVisitor {

        @Override
        public void visitRequestBeforeSend(HttpRequest request, int sequenceId) {
            RequestLine line = request.getRequestLine();
            LOG.debug("before send. method:{}, uri:{}", line.getMethod(), line.getUri());
        }

        @Override
        public void visitResponseUponReceiving(HttpResponse response, int sequenceId) {
            LOG.debug("{}", response);
        }

        @Override
        public void visitException(Exception e, int sequenceId) {
        }
    }

    public BoxClient getAuthenticatedClient() throws AuthFatalFailureException {
        Config config = App.config;
        String key = config.getString("box.key");
        String secret = config.getString("box.secret");

        BoxRESTClient boxRESTClient = new BoxRESTClient();
        boxRESTClient.acceptRestVisitor(new RestVisitor());

        BoxClient client = new BoxClient(key, secret, null, null, boxRESTClient, null);
        client.setAutoRefreshOAuth(true);
        client.addOAuthRefreshListener(newAuthData -> {
            sas.saveAuth(newAuthData);
        });

        if (sas.getAuth() != null) {
            client.authenticateFromSecureStorage(sas);
            LOG.debug("get auth key from storage.");
        } else {
            LocalServer localServer = new LocalServer("localhost", 4000);
            String code = "";
            try {
                String url = "https://www.box.com/api/oauth2/authorize?response_type=code&client_id=" + key + "&redirect_uri=" + URLEncoder.encode(localServer.getRedirectUri(), "iso-8859-1");
                Desktop.getDesktop().browse(java.net.URI.create(url));
                code = localServer.waitForCode();
            } catch (Exception ex) {
                LOG.error(ex.getMessage(), ex);
            } finally {
                try {
                    localServer.stop();
                } catch (Exception ex) {
                }
            }

            try {
                BoxOAuthToken bt = client.getOAuthManager().createOAuth(code, key, secret, config.getString("box.authUrl"));
                client.authenticate(bt);
            } catch (BoxRestException | BoxServerException ex) {
                LOG.error(ex.getMessage(), ex);
            }
            
        }

        client.saveAuth(sas);
        return client;
    }
}
