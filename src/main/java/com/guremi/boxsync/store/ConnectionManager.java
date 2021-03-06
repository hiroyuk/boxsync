package com.guremi.boxsync.store;

import com.guremi.boxsync.App;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author htaka
 */
public class ConnectionManager {
    private static final Logger LOG = LoggerFactory.getLogger(ConnectionManager.class);

    public static Connection getConnection() throws SQLException {
        Path datafile = Paths.get(App.config.getString("box.storepath"), "sqlite.db");
        try {
            Files.createDirectories(datafile.getParent());
        } catch (IOException ex) {
            LOG.error(ex.getMessage(), ex);
        }
        Connection connection = DriverManager.getConnection("jdbc:sqlite:" + datafile.toString());

        try (Statement stmt = connection.createStatement()) {
            stmt.execute("create table if not exists digests (id integer primary key autoincrement, path text unique, digest text, timestamp text)");
            stmt.execute("create table if not exists config(id integer primary key autoincrement, name text unique, value text)");
        }
        connection.setAutoCommit(false);
        return connection;
    }

    private ConnectionManager() {
    }
}
