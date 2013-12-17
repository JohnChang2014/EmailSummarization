import gate.util.Out;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

import parser.RegexMatches;
import db.Transaction;

public class ClusterProcessor {
	private final static boolean DEBUG = false;
	private Transaction dbQuery;
	private RegexMatches reg = new RegexMatches();
	private int mode;
	
	public ClusterProcessor(int mode) {
		this.mode = mode;
		this.dbQuery = new Transaction();
		if (mode == 0) dbQuery.connect(Config.ip, Config.port, Config.db_development, Config.username, Config.password);
		else if (mode == 1) dbQuery.connect(Config.ip, Config.port, Config.db, Config.username, Config.password);
	}

	public int run(ResultSet grs, ArrayList<String> new_email, ArrayList<String[]> wordset) throws SQLException, ParseException {
		int g_id, final_group = 0;
		int e_id              = Integer.valueOf(new_email.get(0));
		String subject        = new_email.get(1);
		
		// create a new group for this email if there is no group.
		// otherwise, compare new email with each group
		if (!grs.next()) {
			if (DEBUG) Out.println("case 1: no group in dababase and then create the first group.");
			final_group = 1;
			assignGroup(final_group, e_id, wordset);
			
		} else {
			do {
				g_id = grs.getInt("g_id");
				// check if the subject of new email and the subject of the
				// group are the same
				// if so, assign to the group. otherwise, check their contents
				if (isSubjectSimilar(g_id, subject)) {
					if (DEBUG) Out.println("case 2: subject is same as the one of a group");
					// assign to this group
					assignGroup(g_id, e_id, wordset);
					return g_id;
				} else {
					ResultSet rs                  = dbQuery.getEmailAddressData(e_id);
					if (rs.next()) {
						String sender = rs.getString("sender");
						ArrayList<String> receivers = reg.parseEmails(rs.getString("receiver") + "," + rs.getString("ccreceiver"));

						for (String receiver : receivers) {
							if (dbQuery.checkEmailPair(g_id, sender, receiver)) {
								if (DEBUG) Out.println("case 3: email ids are a pair in a group");
								assignGroup(g_id, e_id, wordset);
								return g_id;
							}
						}
					}
				}
			} while (grs.next());
			
			if (DEBUG) Out.println("case 4: create a new group");
			final_group = dbQuery.getNewIDGroup();
			// revised each e_id field in the wordset as g_id before inserting
			// data to the table 'group_words'
			int n = 0;
			for (String[] fields : wordset) {
				fields[0] = String.valueOf(final_group);
				wordset.set(n, fields);
				n++;
			}
			assignGroup(final_group, e_id, wordset);
		}
		return final_group;
	}
	
	private void assignGroup(int g_id, int e_id, ArrayList<String[]> wordset) throws SQLException, ParseException {
		String[] newGroup = { String.valueOf(g_id), String.valueOf(e_id), "0" };
		dbQuery.insert(newGroup, "email_groups");
		dbQuery.updateHeadEmailFromGroup(g_id);
	}
	
	public boolean isSubjectSimilar(int g_id, String subject) throws SQLException {
		HashMap<String, String> params = new HashMap<String, String>();
		subject = subject.replaceAll("[rR][eE]:\\s?(.+)$", "$1");
		params.put("cond", "a.e_id = b.e_id And a.g_id = " + g_id + " And a.head = 1 And b.subject like \"%" + subject + "%\"");
		ResultSet rs = dbQuery.query("email_groups as a, emails as b", params);
		if (rs.next()) return true;
		return false;
	}
	
	public void close() {
		this.dbQuery.close();
	}
}
