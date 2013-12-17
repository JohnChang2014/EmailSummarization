package db;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

public class Transaction extends MySQL {
	
	public void emptyData(String table) {
		String strSQL = "TRUNCATE `" + table + "`";
		this.dataManipulate(strSQL);
	}
	
	public String getEmailGroupSubject(int g_id) throws SQLException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("cols", "b.subject");
		params.put("cond", "a.g_id = " + g_id + " And a.e_id = b.e_id And a.head = 1");
		ResultSet rs = this.query("email_groups as a, emails as b", params);
		if (rs.next()) return rs.getString(1);
		return "";
	}
	
	public double getGroupAverageScore(int g_id) throws SQLException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("cols", "Avg(tfidf)");
		params.put("cond", "g_id = " + g_id);
		ResultSet rs = this.query("group_words", params);
		if (rs.next()) return rs.getDouble(1);
		return 0.0;
	}
	
	public void updateSummary(int g_id, int n_emails, String summary) throws SQLException {
		summary     = "summary = \"" + summary + "\"";
		String cond = "g_id = " + g_id + " And n_emails = " + n_emails;
		this.update("summaries", summary, cond);
	}
	
	public boolean checkSummaryExist(int g_id, int n_emails) throws SQLException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("cond", "g_id = " + g_id + " And n_emails = " + n_emails);
		ResultSet rs = this.query("summaries", params);
		if (rs.next()) return false;
		return true;
	}
	
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

	// update a first email in a group as the head
	public void updateHeadEmailFromGroup(int g_id) throws SQLException {
		this.update("email_groups", "head = 0", "g_id = " + g_id);
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("cols", "b.e_id");
		params.put("cond", "a.e_id=b.e_id And a.g_id = " + g_id);
		params.put("order", "b.sending_time ASC");
		ResultSet rs = this.query("email_groups as a, emails as b", params);
		int e_id = 0;
		if (rs.first()) {
			e_id = rs.getInt("b.e_id");
			this.update("email_groups", "head = 1", "e_id = " + e_id);
		}
	}
	
	// get email addresses including sender, receiver, ccreceiver of an email
	public ResultSet getEmailAddressData(int e_id) throws SQLException {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("cols", "sender, receiver, ccreceiver");
		params.put("cond", "e_id= " + e_id);
		return this.query("emails", params);
	}
	
	// check if an email pair is a relation in the email group
	public boolean checkEmailPair(int g_id, String sender, String receiver) throws SQLException {
		HashMap<String, String> params = new HashMap<String, String>();
		String condition = "(sender like '%" + sender + "%' And receiver like '%" + receiver + "%') Or (sender like '%" + receiver + "%' And receiver like '%" + sender + "%') Or ";
		condition += "(sender like '%" + sender + "%' And ccreceiver like '%" + receiver + "%') Or (sender like '%" + receiver + "%' And ccreceiver like '%" + sender + "%')";
		params.put("cols", "sender, receiver, ccreceiver");
		params.put("cond", condition );
		ResultSet rs = this.query("emails", params); 
		if (rs.next()) return true;
		return false;
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
		return params;
	}

}
