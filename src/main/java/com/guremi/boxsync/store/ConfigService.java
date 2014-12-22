package com.guremi.boxsync.store;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Optional;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ConfigService {
    public static enum ConfigKey {
        ACCESS_TOKEN,
        REFRESH_TOKEN,
        EXPIRES_IN,
    }

    private static final Logger LOG = LoggerFactory.getLogger(ConfigService.class);
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();

    public Optional<String> getValue(ConfigKey key) {
        LOCK.readLock().lock();
        try (Connection con = ConnectionManager.getConnection();
             PreparedStatement ps = con.prepareStatement("select value from config where name = ?")) {
            ps.setString(1, key.name());
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return Optional.of(rs.getString("value"));
                }
            }
        } catch (SQLException ex) {
            LOG.error(ex.getMessage(), ex);
        } finally {
            LOCK.readLock().unlock();
        }
        return Optional.empty();
    }

    public void storeValue(ConfigKey key, String value) {
        LOCK.writeLock().lock();
        try (Connection con = ConnectionManager.getConnection();
             PreparedStatement ps = con.prepareStatement("insert or replace into config(name, value) values (?, ?)")) {

            ps.setString(1, key.name());
            ps.setString(2, value);
            ps.executeUpdate();

            con.commit();
            LOG.debug("store config value. key:{}, value:{}", key.name(), value);
        } catch (SQLException ex) {
            LOG.error(ex.getMessage(), ex);
        } finally {
            LOCK.writeLock().unlock();
        }
    }
}
