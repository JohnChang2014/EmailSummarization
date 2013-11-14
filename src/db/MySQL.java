package db;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;

public class MySQL extends Database implements QueryAction {
	Connection dbCon = null;
	Statement stmt   = null;
	ResultSet rs     = null;
	
	public MySQL() {
		try {
			// The newInstance() call is a work around for some
			// broken Java implementations
			Class.forName("com.mysql.jdbc.Driver").newInstance();
		} catch (Exception ex) {
			// handle any errors
			
		}
	}
	
	@Override
	public boolean connect(String dbURL, String username, String password) {
		// getting database connection to MySQL server
		try {
			dbCon = DriverManager.getConnection(dbURL, username, password);
			if (dbCon != null) return true;
			else return false;
			
		} catch (SQLException e) {
			// handle any errors
			System.out.println("SQLException: " + e.getMessage());
			System.out.println("SQLState: " + e.getSQLState());
			System.out.println("VendorError: " + e.getErrorCode());
		}
		return false;
	}

	@Override
	public void close() {
		// TODO Auto-generated method stub
		try {
			dbCon.close();
		} catch (SQLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	
	@Override
	public Object insert(ArrayList<Object> params) {
		// TODO Auto-generated method stub
		return null;
	}
	

	@Override
	public Object delete(ArrayList<Object> params) {
		// TODO Auto-generated method stub
		return null;
	}
	

	@Override
	public Object update(ArrayList<Object> params) {
		// TODO Auto-generated method stub
		return null;
	}
	

	@Override
	public Object query(ArrayList<Object> params) {
		// TODO Auto-generated method stub
		return null;
	}
}
