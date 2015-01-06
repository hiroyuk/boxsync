package com.guremi.boxsync.store;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.BoxRESTClient;
import com.box.boxjavalibv2.authorization.IAuthSecureStorage;
import com.box.boxjavalibv2.dao.BoxOAuthToken;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxServerException;
import com.box.restclientv2.exceptions.BoxRestException;
import com.guremi.boxsync.App;
import com.guremi.boxsync.oauth2.LocalServer;
import com.guremi.boxsync.oauth2.SecureAuthStorage;
import com.typesafe.config.Config;
import java.awt.Desktop;
import java.net.URLEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoxClientManager {
    private static final Logger LOG = LoggerFactory.getLogger(BoxClientManager.class);
    private static final IAuthSecureStorage sas = new SecureAuthStorage();
    private static final Object AUTH_SYNC = new Object();

    public BoxClient getAuthenticatedClient() throws AuthFatalFailureException {
        Config config = App.config;
        String key = config.getString("box.key");
        String secret = config.getString("box.secret");

        BoxRESTClient restClient = new BoxRESTClient();

        BoxClient client = new BoxClient(key, secret, null, null, restClient, null);
        client.setAutoRefreshOAuth(true);
        client.addOAuthRefreshListener(newAuthData -> {
            LOG.debug("auth key refreshed.");
            sas.saveAuth(newAuthData);
        });

        synchronized (AUTH_SYNC) {
            if (sas.getAuth() != null) {
                client.authenticateFromSecureStorage(sas);
                if (client.isAuthenticated()) {
                    LOG.debug("get auth key from storage.");
                    return client;
                }
            }

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

            client.saveAuth(sas);
        }
        return client;
    }
}
