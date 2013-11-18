package db;

abstract class Database {
	String dbURL, username, password;

	public abstract boolean connect(String dbURL, String username, String password);
	public abstract void close();
}
