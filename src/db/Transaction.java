package db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

public class Transaction extends MySQL {
	// get all words in a specific email group
	public ResultSet getEmailGroupWords(int g_id) throws SQLException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("cond", "g_id = " + g_id);
		return this.query("group_words", params);
	}

	// get id numbers of all group
	public ResultSet getEmailGroups() throws SQLException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("cols", "g_id, count(e_id) as n_emails");
		params.put("group", "g_id");
		return this.query("email_groups", params);
	}

	// get all of emails in a specific group
	public ResultSet getEmailsFromGroup(int g_id) throws SQLException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("cols", "b.* ");
		params.put("cond", "a.g_id=b.g_id And a.gid = " + g_id);
		return this.query("email_groups as a, emails as b", params);
	}

	// insert one record
	public void insert(String[] args, String table) throws SQLException, ParseException {
		ArrayList<HashMap<String, String>> paramset = new ArrayList<HashMap<String, String>>();
		if (table == "words") paramset.add(this.getParamsForWords(args));
		if (table == "group_words")
			paramset.add(this.getParamsForGroupWords(args));
		if (table == "email") paramset.add(this.getParamsForEmail(args));
		if (table == "email_groups")
			paramset.add(this.getParamsForGroup(args));
		if (table == "summaries")
			paramset.add(this.getParamsForSummaries(args));
		this.insert(table, paramset);
	}

	// insert multiple records
	public void insert(ArrayList<String[]> argset, String table) throws SQLException, ParseException {
		ArrayList<HashMap<String, String>> paramset = new ArrayList<HashMap<String, String>>();
		for (String[] args : argset) {
			HashMap<String, String> params = new HashMap<String, String>();
			if (table == "words") this.getParamsForWords(args);
			if (table == "group_words") this.getParamsForGroupWords(args);
			if (table == "email") this.getParamsForEmail(args);
			if (table == "email_groups") this.getParamsForGroup(args);
			if (table == "summaries") this.getParamsForSummaries(args);
			paramset.add(params);
		}
		this.insert(table, paramset);
	}

	private HashMap<String, String> getParamsForGroupWords(String[] args) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("g_id", args[0]);
		params.put("word", args[1]);
		params.put("type", args[2]);
		params.put("tf", args[3]);
		params.put("df", args[4]);
		params.put("idf", args[5]);
		params.put("tfidf", args[6]);
		return params;
	}

	private HashMap<String, String> getParamsForSummaries(String[] args) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("s_id", args[0]);
		params.put("g_id", args[1]);
		params.put("n_emails", args[2]);
		params.put("summary", args[3]);
		return params;
	}

	private HashMap<String, String> getParamsForGroup(String[] args) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("g_id", args[0]);
		params.put("e_id", args[1]);
		return params;
	}

	private HashMap<String, String> getParamsForWords(String[] args) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("e_id", args[0]);
		params.put("word", args[1]);
		params.put("type", args[2]);
		params.put("tf", args[3]);
		return params;
	}

	private HashMap<String, String> getParamsForEmail(String[] args) {
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
		return params;
	}

}
