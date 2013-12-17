import file.Directory;
import gate.GateDataStore;
import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import gate.util.Out;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;

import db.Transaction;

public class MainApp {
	private final static boolean DEBUG = true;
	
	// reset all data in the database and datastore
	// so that the program can start over
	public static void reset() {
		Directory    dir = new Directory();
		Transaction db   = new Transaction();
		GateDataStore ds = new GateDataStore();
		String dbName    = new String();
		String dsDir     = new String();
		if (mode == 0) {
			dbName = Config.db_development;
			dsDir  = Config.ds_dir_development;
		} else if (mode == 1) {
			dbName = Config.db;
			dsDir  = Config.ds_dir;
		}
			
		if (db.connect(Config.ip, Config.port, dbName, Config.username, Config.password)) {
			db.emptyData("words");
			db.emptyData("email_groups");
			db.emptyData("sentences");
			db.emptyData("summaries");
			db.close();
			db = null;
		} else {
			System.out.println("failed to connect database!!");
			System.exit(0);
		}
		
		if (dir.exists(dsDir) && !dir.isEmpty(dsDir)) {
			dir.delete(dsDir);
			dir.createDir(dsDir);
		} 
	}
	
	// mode == 1 means running on test set
	// mode == 0 means running on training set
	public static int mode = 0; 
	
	// batch process a group of emails
	// make sure giving appropriate record range
	public static void runFirstTask(int record_range1, int record_range2) {
		Transaction db = new Transaction();
		if (mode == 0) db.connect(Config.ip, Config.port, Config.db_development, Config.username, Config.password);
		else if (mode == 1) db.connect(Config.ip, Config.port, Config.db, Config.username, Config.password);

		try {

			HashMap<String, String> params = new HashMap<String, String>();
			params.put("cols", "content, e_id, subject");
			params.put("order", "e_id ASC, sending_time ASC");
			params.put("cond", "e_id >= " + record_range1 + " and e_id <=" + record_range2);
			ResultSet rs;

			rs = db.query("emails", params);

			while (rs.next()) {
				Summary sumApp = new Summary(mode);
				int e_id = rs.getInt("e_id");
				sumApp.runIEAndClustering(e_id);
				sumApp.close();
			}

			db.close();
			db = null;
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (PersistenceException e) {
			e.printStackTrace();
		}
	}
	
	// generate a summary for an email group
	public static void runSecondTask(int g_id) {
		try {
			Summary sumApp = new Summary(mode);
			sumApp.runInference(g_id);
			sumApp.close();
		} catch (PersistenceException e) {
			e.printStackTrace();
		} catch (ResourceInstantiationException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	// batch process a group of emails
	// make sure giving appropriate record range
	public static void runAll(int record_range1, int record_range2) {
		Transaction db = new Transaction();
		if (mode == 0) db.connect(Config.ip, Config.port, Config.db_development, Config.username, Config.password);
		else if (mode == 1) db.connect(Config.ip, Config.port, Config.db, Config.username, Config.password);

		try {

			HashMap<String, String> params = new HashMap<String, String>();
			params.put("cols", "content, e_id, subject");
			params.put("order", "e_id ASC, sending_time ASC");
			params.put("cond", "e_id >= " + record_range1 + " and e_id <=" + record_range2);
			ResultSet rs;

			rs = db.query("emails", params);

			while (rs.next()) {
				Summary sumApp = new Summary(mode);
				int e_id = rs.getInt("e_id");
				sumApp.run(e_id);
				sumApp.close();
			}

			db.close();
			db = null;
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (PersistenceException e) {
			e.printStackTrace();
		}
	}
	
	// the method is only useful to test training data and test data
	public static int[] findDataRange(int file_number) {
		int[] range = { 0, 0 };
		if (mode == 0) {
			if (file_number == 1) { range[0] = 1; range[1] = 18; }
			if (file_number == 2) { range[0] = 19; range[1] = 35; }
			if (file_number == 3) { range[0] = 36; range[1] = 43; }
			if (file_number == 4) { range[0] = 44; range[1] = 52; }
			if (file_number == 5) { range[0] = 53; range[1] = 62; }
			if (file_number == 6) { range[0] = 63; range[1] = 83; }
			if (file_number == 7) { range[0] = 84; range[1] = 105; }
			if (file_number == 8) { range[0] = 106; range[1] = 128; }
			if (file_number == 9) { range[0] = 129; range[1] = 159; }
			if (file_number == 10) { range[0] = 160; range[1] = 175; }
			if (file_number == 11) { range[0] = 176; range[1] = 186; }
			if (file_number == 12) { range[0] = 187; range[1] = 234; }
			if (file_number == 13) { range[0] = 235; range[1] = 246; }
			if (file_number == 14) { range[0] = 247; range[1] = 255; }
			if (file_number == 15) { range[0] = 256; range[1] = 275; }
			if (file_number == 16) { range[0] = 276; range[1] = 290; }
			if (file_number == 17) { range[0] = 291; range[1] = 328; }
			if (file_number == 18) { range[0] = 329; range[1] = 337; }
			if (file_number == 19) { range[0] = 338; range[1] = 375; }
			
		} else if (mode == 1) {
			if (file_number == 1) { range[0] = 1; range[1] = 9; }
			if (file_number == 2) { range[0] = 10; range[1] = 16; }
			if (file_number == 3) { range[0] = 17; range[1] = 28; }
			if (file_number == 4) { range[0] = 29; range[1] = 59; }
			if (file_number == 5) { range[0] = 60; range[1] = 96; }
			if (file_number == 6) { range[0] = 97; range[1] = 104; }
			if (file_number == 7) { range[0] = 105; range[1] = 121; }
			if (file_number == 8) { range[0] = 122; range[1] = 128; }
			if (file_number == 9) { range[0] = 129; range[1] = 131; }
			if (file_number == 10) { range[0] = 132; range[1] = 154; }
			if (file_number == 11) { range[0] = 155; range[1] = 161; }
			if (file_number == 12) { range[0] = 162; range[1] = 192; }
			if (file_number == 13) { range[0] = 193; range[1] = 199; }
			if (file_number == 14) { range[0] = 200; range[1] = 209; }
			if (file_number == 15) { range[0] = 210; range[1] = 224; }
			if (file_number == 16) { range[0] = 225; range[1] = 230; }
			if (file_number == 17) { range[0] = 231; range[1] = 238; }
			if (file_number == 18) { range[0] = 339; range[1] = 244; }
			if (file_number == 19) { range[0] = 245; range[1] = 248; }
		}
		return range;
	}
	
	public static void main(String[] args) throws Exception {
		int file_number = 1;
		int range[] = findDataRange(file_number);
		reset();
		
		runFirstTask(range[0], range[1]);
		
		//int g_id = 2;
		//runSecondTask(1);
		System.out.println("Program done!!");
		//for (int n = 4; n <= 9; n++) runSecondTask(n);
		if (DEBUG) Thread.sleep(400000);
		System.exit(0);
	}
}
