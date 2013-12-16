package db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

public class Transaction extends MySQL {
	
	public int getEmailCountFromGroup(int g_id) throws SQLException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("cols", "g_id, count(e_id) as n_emails");
		params.put("group", "g_id");
		params.put("having", "g_id = " + g_id);
		params.put("order", "g_id ASC");
		ResultSet rs = this.query("email_groups", params);
		if (rs.next()) return rs.getInt("n_emails");
		return 0;
	}
	
	// remove all words of a group
	public void removeWordsFromGroup(int g_id) throws SQLException {
		this.delete("group_words", "g_id = " + g_id);
	}
	
	// get id number for a new group 
	public int getNewIDGroup() throws SQLException {
		return getGroupCounts() + 1;
	}
	
	public int getGroupCounts() throws SQLException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("cols", "count(distinct g_id)");
		params.put("order", "g_id ASC");
		ResultSet rs = this.query("email_groups", params);
		if (rs.next()) return Integer.valueOf(rs.getInt(1));
		else return 0;
	}
	
	// get number of words in a specific email group
	public int getWordCountForGroup(int g_id) throws SQLException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("cols", "count(*)");
		params.put("cond", "g_id = " + g_id);
		ResultSet rs = this.query("group_words", params);
		if (rs.next()) return Integer.valueOf(rs.getInt(1));
		return 0;
	}
	
	// get all words in a specific email group
	public ResultSet getEmailGroupWords(int g_id) throws SQLException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("cond", "g_id = " + g_id);
		params.put("order", "tfidf DESC");
		return this.query("group_words", params);
	}

	// get id numbers of all group
	public ResultSet getEmailGroups() throws SQLException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("cols", "g_id, count(e_id) as n_emails");
		params.put("group", "g_id");
		params.put("order", "g_id ASC");
		return this.query("email_groups", params);
	}

	// get all of emails in a specific group
	public ResultSet getEmailsFromGroup(int g_id) throws SQLException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("cols", "b.* ");
		params.put("cond", "a.e_id=b.e_id And a.g_id = " + g_id);
		return this.query("email_groups as a, emails as b", params);
	}

	// insert one record
	public void insert(String[] args, String table) throws SQLException, ParseException {
		ArrayList<HashMap<String, String>> paramset = new ArrayList<HashMap<String, String>>();
		if (table == "words") paramset.add(this.getParamsForWords(args));
		if (table == "group_words")
			paramset.add(this.getParamsForGroupWords(args));
		if (table == "emails") paramset.add(this.getParamsForEmail(args));
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
			if (table == "words") params = this.getParamsForWords(args);
			if (table == "group_words") params = this.getParamsForGroupWords(args);
			if (table == "email") params = this.getParamsForEmail(args);
			if (table == "email_groups") params = this.getParamsForGroup(args);
			if (table == "sentences") params = this.getParamsForSentences(args);
			if (table == "summaries") params = this.getParamsForSummaries(args);
			paramset.add(params);
		}
		
		this.insert(table, paramset);
	}

	private HashMap<String, String> getParamsForSentences(String[] args) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("e_id", args[0]);
		params.put("ann_id", args[1]);
		params.put("sentence", args[2]);
		return params;
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
		params.put("g_id", args[0]);
		params.put("n_emails", args[1]);
		params.put("summary", args[2]);
		return params;
	}

	private HashMap<String, String> getParamsForGroup(String[] args) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("g_id", args[0]);
		params.put("e_id", args[1]);
		params.put("head", args[2]);
		return params;
	}

	private HashMap<String, String> getParamsForWords(String[] args) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("e_id", args[0]);
		params.put("word", args[1]);
		params.put("type", args[2]);
		params.put("tf", args[3]);
		params.put("df", args[4]);
		params.put("idf", args[5]);
		params.put("tfidf", args[6]);
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
