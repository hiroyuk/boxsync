package com.guremi.boxsync.store;

import com.box.boxjavalibv2.BoxClient;
import com.box.boxjavalibv2.exceptions.AuthFatalFailureException;
import com.box.boxjavalibv2.exceptions.BoxServerException;
import com.box.restclientv2.exceptions.BoxRestException;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoxClientManager {
    private static final Logger LOG = LoggerFactory.getLogger(BoxClientManager.class);
    private final ConfigService configService = new ConfigService();

    public BoxClient getAuthenticatedClient() {
        Config config = ConfigFactory.load("application.json");
        String key = config.getString("box.key");
        String secret = config.getString("box.secret");

        BoxClient client = new BoxClient(key, secret, null, null, null);
        client.setAutoRefreshOAuth(true);
        client.addOAuthRefreshListener(newAuthData -> {
            configService.storeValue(ConfigService.ConfigKey.REFRESH_TOKEN, newAuthData.getRefreshToken());
        });

        String refreshToken = configService.getValue(ConfigService.ConfigKey.REFRESH_TOKEN);

        if (refreshToken != null) {
            try {
                client.getOAuthManager().refreshOAuth(refreshToken, key, secret);
                return client;
            } catch (BoxRestException | BoxServerException ex) {
                throw new RuntimeException(ex.getMessage(), ex);
            } catch (AuthFatalFailureException ex) {
            }
        }


        ;
        return client;
    }
}
