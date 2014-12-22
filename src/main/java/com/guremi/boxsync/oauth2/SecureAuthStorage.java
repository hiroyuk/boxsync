package com.guremi.boxsync.oauth2;

import com.box.boxjavalibv2.authorization.IAuthSecureStorage;
import com.box.boxjavalibv2.dao.BoxOAuthToken;
import com.box.boxjavalibv2.dao.IAuthData;
import com.guremi.boxsync.store.ConfigService;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SecureAuthStorage implements IAuthSecureStorage {
    private static final ConfigService cs = new ConfigService();

    @Override
    public void saveAuth(IAuthData auth) {
        cs.storeValue(ConfigService.ConfigKey.ACCESS_TOKEN, auth.getAccessToken());
        cs.storeValue(ConfigService.ConfigKey.REFRESH_TOKEN, auth.getRefreshToken());
        cs.storeValue(ConfigService.ConfigKey.EXPIRES_IN, auth.getExpiresIn().toString());
    }

    @Override
    public IAuthData getAuth() {
        Map<String, Object> oauthData = new HashMap<>();
        Optional<String> accessToken = cs.getValue(ConfigService.ConfigKey.ACCESS_TOKEN);
        Optional<String> refreshToken = cs.getValue(ConfigService.ConfigKey.REFRESH_TOKEN);
        Optional<String> expiresIn = cs.getValue(ConfigService.ConfigKey.EXPIRES_IN);

        if (accessToken.isPresent() && refreshToken.isPresent() && expiresIn.isPresent()) {
            oauthData.put(BoxOAuthToken.FIELD_ACCESS_TOKEN, accessToken.get());
            oauthData.put(BoxOAuthToken.FIELD_REFRESH_TOKEN, refreshToken.get());
            oauthData.put(BoxOAuthToken.FIELD_EXPIRES_IN, Integer.valueOf(expiresIn.get()));

            return new BoxOAuthToken(oauthData);
        } else {
            return null;
        }
    }

}
