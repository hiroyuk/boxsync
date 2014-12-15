package com.guremi.boxsync.utils;

import com.box.boxjavalibv2.BoxClient;
import com.typesafe.config.Config;
import com.typesafe.config.ConfigFactory;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BoxClientManager {
    private static final Logger LOG = LoggerFactory.getLogger(BoxClientManager.class);

    public BoxClient getAuthenticatedClient() {
        Config config = ConfigFactory.load("application.json");
        String key = config.getString("box.key");
        String secret = config.getString("box.secret");

        String code;
        try (Connection con = getConnection(config);
             PreparedStatement ps = con.prepareStatement("select value from config where name = ?")) {
            ps.setString(1, "code");

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    code = rs.getString("value");
                } else {
                    code = "";
                }
            }
        } catch (SQLException | IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }

        BoxClient client = new BoxClient(key, secret, null, null, null);
        client.addOAuthRefreshListener(newAuthData -> {
            String sql = "insert into config(name, value) values (?, ?)";
            try (Connection con = getConnection(config);
                 PreparedStatement ps = con.prepareStatement(sql)) {
                ps.setString(1, "access_token");
                ps.setString(2, newAuthData.getAccessToken());
                ps.executeQuery();

                ps.setString(1, "refresh_token");
                ps.setString(2, newAuthData.getRefreshToken());
                ps.executeQuery();
            } catch (SQLException | IOException ex) {
                LOG.error(ex.getMessage(), ex);
            }
        });

        client.authenticate(null);
        return client;
    }

    private Connection getConnection(Config config) throws SQLException, IOException {
        Path path = Paths.get(config.getString("box.storepath"));
        if (!Files.exists(path)) {
            Files.createDirectory(path);
        }
        Path dbFilePath = path.resolve("data.db");

        Connection con = DriverManager.getConnection("jdbc:sqlite:" + dbFilePath.toString());
        con.setAutoCommit(false);
        try (Statement stmt = con.createStatement()) {
            stmt.execute("create table if not exists config(id integer primary key autoincrement, name text unique on conflict replace, value text)");
        }
        return con;
    }
}
