package com.guremi.boxsync.store;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author htaka
 */
public class DigestService {
    private static final Logger LOG = LoggerFactory.getLogger(DigestService.class);
    private static final ReentrantReadWriteLock LOCK = new ReentrantReadWriteLock();
    private static final DateTimeFormatter DTF = ISODateTimeFormat.dateHourMinuteSecondMillis();

    public String getCachedDigest(Path path) {
        LOCK.readLock().lock();

        try (Connection con = ConnectionManager.getConnection();
             PreparedStatement stmt = con.prepareStatement("select * from digests where path = ?")) {
            stmt.setString(1, path.toAbsolutePath().toString());
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    String digest = rs.getString("digest");
                    DateTime timestamp = DateTime.parse(rs.getString("timestamp"), DTF);

                    if (Files.getLastModifiedTime(path).toMillis() != timestamp.getMillis()) {
                        LOG.debug("dirty cache, delete. path:{}", path.toAbsolutePath().toString());
                        LOCK.readLock().unlock();
                        LOCK.writeLock().lock();
                        try {
                            _clearDigest(path);
                            digest = null;
                            LOCK.readLock().lock();
                        } finally {
                            LOCK.writeLock().unlock();
                        }
                    }

                    return digest;
                } else {
                    return null;
                }
            }
        } catch (SQLException | IOException ex) {
            LOG.error(ex.getMessage(), ex);
        } finally {
            LOCK.readLock().unlock();
        }
        return null;
    }

    public void storeDigest(Path path, String digest) {
        LOCK.writeLock().lock();

        try (Connection con = ConnectionManager.getConnection();
             PreparedStatement stmt = con.prepareStatement("insert into digests(path, digest, timestamp) values (?, ?, ?)")) {
            String date = new DateTime(Files.getLastModifiedTime(path).toMillis()).toString(DTF);
            stmt.setString(1, path.toAbsolutePath().toString());
            stmt.setString(2, digest);
            stmt.setString(3, date);
            stmt.executeUpdate();

            con.commit();
            LOG.info("digest stored. path:{}, digest:{}, timestamp:{}", path.toAbsolutePath().toString(), digest, date);
        } catch (SQLException | IOException ex) {
            LOG.error(ex.getMessage(), ex);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    private void clearDigest(Path path) {
        LOCK.writeLock().lock();
        try {
            _clearDigest(path);
        } finally {
            LOCK.writeLock().unlock();
        }
    }

    private void _clearDigest(Path path) {
        try (Connection con = ConnectionManager.getConnection();
             PreparedStatement stmt = con.prepareStatement("delete from digests where path = ?")) {
            stmt.setString(1, path.toAbsolutePath().toString());
            stmt.executeUpdate();

            con.commit();
        } catch (SQLException ex) {
            LOG.error(ex.getMessage(), ex);
        }
    }
}
