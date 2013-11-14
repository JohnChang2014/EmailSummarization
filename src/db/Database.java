package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.logging.Level;
import java.util.logging.Logger;

abstract class Database {
	String dbURL, username, password;

	public abstract boolean connect(String dbURL, String username, String password);
	public abstract void close();
}
