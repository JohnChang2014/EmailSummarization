package db;

import java.util.ArrayList;

public interface QueryAction {
	public Object insert(ArrayList<Object> params);
	public Object delete(ArrayList<Object> params);
	public Object update(ArrayList<Object> params);
	public Object query(ArrayList<Object> params);
}
