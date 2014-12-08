package com.guremi.boxsync.sample;

import com.box.boxjavalibv2.*;
import com.box.boxjavalibv2.dao.*;
import com.box.boxjavalibv2.exceptions.*;
import com.box.restclientv2.exceptions.*;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.*;
import java.awt.Desktop;
import java.net.ServerSocket;
import java.net.Socket;
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

        String code = "";
        String url = "https://www.box.com/api/oauth2/authorize?response_type=code&client_id=" + key;
        try {
            Desktop.getDesktop().browse(java.net.URI.create(url));
            code = getCode();
        } catch (IOException e) {
            LOG.error(e.getMessage(), e);
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

    private static String getCode() throws IOException {

        ServerSocket serverSocket = new ServerSocket(PORT);
        Socket socket = serverSocket.accept();
        BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
        while (true) {
            String code;
            try {
                try (BufferedWriter out = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()))) {
                    out.write("HTTP/1.1 200 OK\r\n");
                    out.write("Content-Type: text/html\r\n");
                    out.write("\r\n");

                    code = in.readLine();
                    System.out.println(code);
                    String match = "code";
                    int loc = code.indexOf(match);

                    if (loc > 0) {
                        int httpstr = code.indexOf("HTTP") - 1;
                        code = code.substring(code.indexOf(match), httpstr);
                        String parts[] = code.split("=");
                        code = parts[1];
                        out.write("Now return to command line to see the output of the HelloWorld sample app.");
                    } else {
                        // It doesn't have a code
                        out.write("Code not found in the URL!");
                    }
                }

                return code;
            } catch (IOException e) {
                //error ("System: " + "Connection to server lost!");
                System.exit(1);
                break;
            }
        }
        return "";
    }

}
