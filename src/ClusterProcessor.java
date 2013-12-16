import gate.util.Out;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

import db.Transaction;

public class ClusterProcessor {
	private final static boolean DEBUG = false;
	private Transaction dbQuery;
	private int mode;
	
	public ClusterProcessor(int mode) {
		this.mode = mode;
		this.dbQuery = new Transaction();
		if (mode == 0) dbQuery.connect(Config.ip, Config.port, Config.db_development, Config.username, Config.password);
		else if (mode == 1) dbQuery.connect(Config.ip, Config.port, Config.db, Config.username, Config.password);
	}

	public int run(ResultSet grs, ArrayList<String> new_email, ArrayList<String[]> wordset) throws SQLException, ParseException {
		int g_id, final_group = 0;
		double max_score = 0.0;
		int e_id       = Integer.valueOf(new_email.get(0));
		String subject = new_email.get(1);
		
		// create a new group for this email if there is no group.
		// otherwise, compare new email with each group
		if (!grs.next()) {
			if (DEBUG) Out.println("case 1: no group in dababase and then create the first group.");
			final_group = 1;
			assignGroup(final_group, e_id, 1, wordset);
		} else {
			do {
				g_id = grs.getInt("g_id");
				// check if the subject of new email and the subject of the
				// group are the same
				// if so, assign to the group. otherwise, check their contents
				if (isSubjectSimilar(g_id, subject)) {
					if (DEBUG) Out.println("case 2: subject is same as the one of a group");
					// assign to this group
					assignGroup(g_id, e_id, 0, wordset);
					return g_id;
				} else {
					double score = computeSimiliarity(g_id, e_id, wordset);
					if (DEBUG) Out.println("group: " + g_id + "==> ( " + score + ", " + max_score + " )");
					if (score > max_score) {
						final_group = g_id;
						max_score   = score;
					}
					
				}
			} while (grs.next());
			
			// assign this email to the group if max_score greater than
			// threshold
			// otherwise, the email become a new group
			if (max_score > Config.threshold && final_group > 0) {
				Out.println("case 3 --> final: " + final_group);
				if (DEBUG) Out.print("case3: similiarity score greater than threshold" + Config.threshold + ", choose group: " + final_group);
				assignGroup(final_group, e_id, 0, wordset);
			} else {
				if (DEBUG)  Out.println("case 4: create a new group");
				final_group = dbQuery.getNewIDGroup();
				// revised each e_id field in the wordset as g_id before inserting data
			    // to the table 'group_words'
				int n = 0;
				for (String[] fields : wordset) {
					fields[0] = String.valueOf(final_group);
					wordset.set(n, fields);
					n++;
				}
				assignGroup(final_group, e_id, 1, wordset);
			}
		}
		return final_group;
	}

	private void assignGroup(int g_id, int e_id, int head, ArrayList<String[]> wordset) throws SQLException, ParseException {
		String[] newGroup = { String.valueOf(g_id), String.valueOf(e_id), String.valueOf(head) };
		dbQuery.insert(newGroup, "email_groups");
		if (head == 1) dbQuery.insert(wordset, "group_words");
	}
	
	public boolean isSubjectSimilar(int g_id, String subject) throws SQLException {
		HashMap<String, String> params = new HashMap<String, String>();
		subject = subject.replaceAll("[rR][eE]:\\s?(.+)$", "$1");
		params.put("cond", "a.e_id = b.e_id And a.g_id = " + g_id + " And a.head = 1 And b.subject like '%" + subject + "%'");
		ResultSet rs = dbQuery.query("email_groups as a, emails as b", params);
		if (rs.next()) return true;
		return false;
	}

	// compute score of similiarity between two email docs based on cosine distance
	public double computeSimiliarity(int g_id, int e_id, ArrayList<String[]> e_wordset) throws SQLException {
		HashMap<String, String> params = new HashMap<String, String>();
		int e_count  = e_wordset.size();
		int g_count  = dbQuery.getWordCountForGroup(g_id);
		String table = new String();
		
		if (e_count >= g_count) table = "words as a Left Join group_words as b On a.word = b.word";
		else table = "words as a Left Join group_words as b On a.word = b.word";
		
		params.put("cols", "sum(a.tfidf * b.tfidf)");
		params.put("cond", "a.e_id =" + e_id + " And b.g_id = " + g_id);
		ResultSet rs = dbQuery.query(table, params);
		if (rs.next()) return Double.valueOf(rs.getDouble(1));
		return 1.0;
	}
	
	public void close() {
		this.dbQuery.close();
	}
}
