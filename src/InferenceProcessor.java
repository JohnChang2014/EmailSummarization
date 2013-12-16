import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import parser.RegexMatches;
import db.Transaction;
import gate.Annotation;
import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.LanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.util.Out;
import static gate.Utils.*;

public class InferenceProcessor extends IEProcessor {
	private final static boolean DEBUG = true;
	private Transaction db;
	private RegexMatches reg = new RegexMatches();
	
	public InferenceProcessor(boolean view) {
		super.init(view);		 
	}
	
	protected ArrayList<LanguageAnalyser> setProcessingResources() throws ResourceInstantiationException, MalformedURLException {
		FeatureMap feats = Factory.newFeatureMap();
		URL japeGrammarURL = new URL("file://" + current_path + "/resources/jape/mainInference.jape");
		feats.put("grammarURL", japeGrammarURL);
		LanguageAnalyser mainInference = (LanguageAnalyser) Factory.createResource("gate.creole.Transducer", feats);

		mainInference.setName("mainInference");
		pr_set.add(mainInference);
		return pr_set;
	}
	
	private Set<String> getPeopleSet(int g_id) throws SQLException {
		Set<String> peopleSet = new HashSet<String>();
		ResultSet rs = db.getEmailsFromGroup(g_id);
		if (DEBUG) Out.println("People Relation: ");
		while(rs.next()) {
			ArrayList<String> sender = reg.parseEmails(rs.getString("sender"));
			ArrayList<String> receivers = reg.parseEmails(rs.getString("receiver") + "," + rs.getString("ccreceiver"));
			for (String receiver : receivers) {
				String combo1 = sender.get(0) + " <-> " + receiver;
				String combo2 = receiver + " <-> " + sender; 
				if (peopleSet.contains(combo1) || peopleSet.contains(combo2)) continue;
				if (DEBUG) Out.println("--> " + combo1);
				peopleSet.add(combo1);
			}
		}
		if (DEBUG)Out.println("\n");
		return peopleSet;
	}
	
	public String summarize(int final_group, ArrayList<Document> docs) throws SQLException, ParseException {
		db                     = new Transaction();
		ArrayList<String> sens = new ArrayList<String>();
		String summary = "";
		
		// assign groovy script file to Scriptable Controller
		Corpus corpus  = run(Config.ifScriptController, docs);
		
		// collect all sentences from corpus
		String sentenceString = "";
		for (Document doc : corpus) {
			for(Annotation senCandidate : doc.getAnnotations().get("SentenceCandidate")) {
				sens.add(senCandidate.getFeatures().get("string").toString());
				sentenceString += ",'" + senCandidate.getFeatures().get("string").toString() + "'";
			}
		}
		sentenceString = sentenceString.substring(1, sentenceString.length());
		
		// 
		if (db.connect(Config.ip, Config.port, Config.db, Config.username, Config.password)) {
			Set<String> peopleSet = getPeopleSet(final_group);
			
			HashMap<String, ArrayList<String>> summaryGroup = new HashMap<String, ArrayList<String>>();
			for (String pair : peopleSet) summaryGroup.put(pair, new ArrayList<String>());
			
			HashMap<String, String> params = new HashMap<String, String>();
			String table = "email_groups as a, emails as b, sentences as c";
			params.put("cols", "a.g_id, b.e_id, b.sending_time, c.ann_id, c.sentence, b.sender, b.receiver, b.ccreceiver");
			params.put("cond", "b.e_id = c.e_id And a.e_id = b.e_id And a.g_id = 1 And c.sentence in (" + sentenceString + ")");
			params.put("order", "b.e_id ASC, b.sending_time ASC, c.ann_id ASC");
			ResultSet rs = db.query(table, params);
			if (DEBUG) Out.println("Sentence Candidates:");
			while(rs.next()) {
				String sentence             = rs.getString("sentence");
				if (DEBUG) Out.println("--> " + rs.getString("sentence"));
				ArrayList<String> sender    = reg.parseEmails(rs.getString("sender"));
				ArrayList<String> receivers = reg.parseEmails(rs.getString("receiver") + "," + rs.getString("ccreceiver"));
				for (String receiver : receivers) {
					String combo1 = sender.get(0) + " <-> " + receiver;
					String combo2 = receiver + " <-> " + sender; 
					if (summaryGroup.containsKey(combo1) && !summaryGroup.get(combo1).contains(sentence)) summaryGroup.get(combo1).add(sentence);
					if (summaryGroup.containsKey(combo2) && !summaryGroup.get(combo2).contains(sentence)) summaryGroup.get(combo1).add(sentence);
				}
			}
			params.clear();
			if (DEBUG) Out.println("\n");
			
			for (String relation : summaryGroup.keySet()) {
				if (DEBUG) Out.println("==> " + relation);
				String tmpSummary = relation + "\n";
				for (String sen : summaryGroup.get(relation)) {
					if (DEBUG) Out.println("-----> " + sen);
					tmpSummary += sen + "\n";
				}
				summary += tmpSummary + "\n";
			}
			String n_emails = String.valueOf(db.getEmailCountFromGroup(final_group));
			String[] data = { String.valueOf(final_group), n_emails, summary };
			if (DEBUG) Out.println("Summary for group " + final_group + " (" + n_emails + " emails): ");
			if (DEBUG) Out.println(summary);
			//db.insert(data, "summaries");
		}
		db.close();
		return summary;
	}
}
