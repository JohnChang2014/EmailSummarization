package db;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

public interface QueryAction {
	public Object insert(String table, HashMap<String, String> params) throws SQLException, ParseException;
	public Object delete(ArrayList<Object> params);
	public Object update(ArrayList<Object> params);
	public Object query(ArrayList<Object> params);
}
