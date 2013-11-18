package db;

import java.util.ArrayList;
import java.util.HashMap;

public interface QueryAction {
	public Object insert(HashMap<String, ArrayList<String>> params);
	public Object delete(ArrayList<Object> params);
	public Object update(ArrayList<Object> params);
	public Object query(ArrayList<Object> params);
}
