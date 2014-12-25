package com.guremi.boxsync.sample.box;

import com.box.boxjavalibv2.*;
import com.box.boxjavalibv2.dao.*;
import com.box.boxjavalibv2.exceptions.*;
import com.box.restclientv2.exceptions.*;
import com.guremi.boxsync.App;
import com.guremi.boxsync.store.BoxClientManager;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
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

        App.config = config;

        if (key.isEmpty()) {
            System.out.println("Before this sample app will work, you will need to change the");
            System.out.println("'key' and 'secret' values in the source code.");
            return;
        }
        BoxClientManager bcm = new BoxClientManager();;

        BoxClient client = bcm.getAuthenticatedClient();
        showFolder(client, "0", "/", 0);
    }

    private static void showFolder(BoxClient client, String parent, String parentName, int depth) throws BoxRestException, BoxServerException, AuthFatalFailureException {
        BoxFolder boxFolder = client.getFoldersManager().getFolder(parent, null);
        ArrayList<BoxTypedObject> folderEntries = boxFolder.getItemCollection().getEntries();

        int folderSize = folderEntries.size();
        for (int i = 0; i <= folderSize - 1; i++) {
            BoxTypedObject folderEntry = folderEntries.get(i);
            String name = (folderEntry instanceof BoxItem) ? ((BoxItem)folderEntry).getName() : "(unknown)";
            if (folderEntry instanceof BoxFile) {
                BoxFile bf = (BoxFile)folderEntry;
                LOG.info("q: {}, i:{}, type: {}, id: {}, name: {}{}, sha1:{}", depth, i, folderEntry.getType(), folderEntry.getId(), parentName, name, bf.getSha1());
            } else {
                LOG.info("q: {}, i:{}, type: {}, id: {}, name: {}{}", depth, i, folderEntry.getType(), folderEntry.getId(), parentName, name);
            }
            if (folderEntry.getType().equals("folder")) {
                showFolder(client, folderEntry.getId(), parentName + name + "/", depth + 1);
            }
        }
    }
}
