package db;

abstract class Database {
	String ip, port, db, username, password;

	public abstract boolean connect(String ip, String port, String db, String username, String password);
	public abstract void close();
}
