package db;

import java.sql.SQLException;
import java.text.ParseException;
import java.util.HashMap;

import db.*;

public class Transaction extends MySQL {
	
	public void insert(String[] args, String table) throws SQLException, ParseException {
		if (table == "words") this.insertRecordToWords(args);
		if (table == "email") this.insertRecordToEmail(args);
	}
	
	private void insertRecordToWords(String[] args) throws SQLException, ParseException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("e_id", args[0]);
		params.put("word", args[1]);
		params.put("type", args[2]);
		params.put("tf", args[3]);
		params.put("df", args[4]);
		params.put("idf", args[5]);
		params.put("tfidf", args[6]);
		this.insert("words", params);
	}
	
	private void insertRecordToEmail(String[] args) throws SQLException, ParseException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("sender", args[0]);
		params.put("receiver", args[1]);
		params.put("ccreceiver", args[2]);
		params.put("subject", args[3]);
		params.put("content", args[4]);
		params.put("raw_content", args[5]);
		params.put("sending_time", args[6]);
		params.put("reply_to", args[7]);
		params.put("g_id", args[8]);
		this.insert("emails", params);
	}
}
