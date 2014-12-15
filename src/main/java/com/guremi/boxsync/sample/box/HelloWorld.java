package com.guremi.boxsync.sample.box;

import com.box.boxjavalibv2.*;
import com.box.boxjavalibv2.dao.*;
import com.box.boxjavalibv2.exceptions.*;
import com.box.restclientv2.exceptions.*;
import com.guremi.boxsync.oauth2.LocalServer;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.awt.Desktop;
import java.net.URLEncoder;
import java.util.ArrayList;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HelloWorld {
    private static final Logger LOG = LoggerFactory.getLogger(HelloWorld.class);

    public static final int PORT = 4000;

    public static void main(String[] args) throws AuthFatalFailureException, BoxServerException, BoxRestException {

        Config config = ConfigFactory.load("application.json");
        String key = config.getString("box.key");
        String secret = config.getString("box.secret");

        if (key.isEmpty()) {
            System.out.println("Before this sample app will work, you will need to change the");
            System.out.println("'key' and 'secret' values in the source code.");
            return;
        }

        LocalServer localServer = new LocalServer("localhost", 4000);
        String code = "";
        try {
            String url = "https://www.box.com/api/oauth2/authorize?response_type=code&client_id=" + key + "&redirect_uri=" + URLEncoder.encode(localServer.getRegirectUri(), "iso-8859-1");
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

        BoxClient client = getAuthenticatedClient(code, key, secret);
        showFolder(client, "0", "/", 0);
    }

    private static void showFolder(BoxClient client, String parent, String parentName, int depth) throws BoxRestException, BoxServerException, AuthFatalFailureException {
        BoxFolder boxFolder = client.getFoldersManager().getFolder(parent, null);
        ArrayList<BoxTypedObject> folderEntries = boxFolder.getItemCollection().getEntries();
        int folderSize = folderEntries.size();
        for (int i = 0; i <= folderSize - 1; i++) {
            BoxTypedObject folderEntry = folderEntries.get(i);
            String name = (folderEntry instanceof BoxItem) ? ((BoxItem)folderEntry).getName() : "(unknown)";
            LOG.info("q: {}, i:{}, type: {}, id: {}, name: {}{}", depth, i, folderEntry.getType(), folderEntry.getId(), parentName, name);
            if (folderEntry.getType().equals("folder")) {
                showFolder(client, folderEntry.getId(), parentName + name + "/" , depth+1);
            }
        }
    }

    private static BoxClient getAuthenticatedClient(String code, String key, String secret) throws BoxRestException, BoxServerException, AuthFatalFailureException {
        BoxClient client = new BoxClient(key, secret, null, null, null);
        BoxOAuthToken bt = client.getOAuthManager().createOAuth(code, key, secret, "http://localhost:" + PORT);
        client.authenticate(bt);
        return client;
    }
}
