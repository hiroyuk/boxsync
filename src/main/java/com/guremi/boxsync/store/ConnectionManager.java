package com.guremi.boxsync.store;

import com.guremi.boxsync.FileWatcher;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

/**
 *
 * @author htaka
 */
public class ConnectionManager {
	
	public static Connection getConnection() throws SQLException {
		String datafile = FileWatcher.config.getString("box.storepath") + "/sqlite.db";
		Connection connection = DriverManager.getConnection("jdbc:sqlite:" + datafile);

		try (Statement stmt = connection.createStatement()) {
			stmt.execute("create table if not exists digests (id integer primary key autoincrement, path text, digest text, timestamp text)");
		}
		connection.setAutoCommit(false);
		return connection;
	}
}
