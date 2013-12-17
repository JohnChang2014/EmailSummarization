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
import file.FWriter;
import gate.Annotation;
import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.LanguageAnalyser;
import gate.creole.ResourceInstantiationException;
import gate.util.Out;

public class InferenceProcessor extends IEProcessor {
	private final static boolean DEBUG = true;
	private Transaction db;
	private RegexMatches reg = new RegexMatches();
	private int mode;
	private String dataset_path;
	
	public InferenceProcessor(int mode) {
		this.mode = mode;
		super.init(DEBUG);		 
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
		if (DEBUG) Out.println("\n");
		return peopleSet;
	}
	
	// collect all sentences from corpus
	private String getSentencesFromCorpus(Corpus corpus, double score) {
		String sentenceString = "";
		for (Document doc : corpus) {
			for(Annotation senCandidate : doc.getAnnotations().get("SentenceCandidate")) {
				// only pickup the sentence with score higher than average group word score
				double s_sen = Double.valueOf(senCandidate.getFeatures().get("score").toString());
				if (s_sen >= score) {
					String tmp = senCandidate.getFeatures().get("string").toString();
					tmp = tmp.replaceAll("\"(.+)\"", "\'$1\'");
					sentenceString += ",\"" + tmp + "\"";
				}
			}
		}
		// if there is no sentence, then use annotation 'BackupSentence' instead
		if (sentenceString.length() == 0) {
			for (Document doc : corpus) {
				for(Annotation senCandidate : doc.getAnnotations().get("BackupSentence")) {
					String tmp = senCandidate.getFeatures().get("string").toString();
					tmp = tmp.replaceAll("\"(.+)\"", "\'$1\'");
					sentenceString += ",\"" + tmp + "\"";
				}
			}
		}
		sentenceString = sentenceString.substring(1, sentenceString.length());
		return sentenceString;
	}
	
	public void outputSummary(int g_id, int n_emails, String summary) {
		if (mode == 0) dataset_path = Config.summary_dir_development;
		else if (mode == 1) dataset_path = Config.summary_dir;
		try {
			String subject = db.getEmailGroupSubject(g_id);
			FWriter fw = new FWriter(dataset_path + subject + ".txt");

			fw.write("Summary for group " + g_id + " (" + n_emails + " emails): \n");
			fw.write(summary);
			fw.close();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		}
	}
	
	public String summarize(int final_group, ArrayList<Document> docs) throws SQLException, ParseException, InterruptedException {
		db             = new Transaction();
		String summary = "";
		
		// 
		String dbname = "";
		if (mode == 0) dbname = Config.db_development;
		else if (mode == 1) dbname = Config.db;
		
		if (db.connect(Config.ip, Config.port, dbname, Config.username, Config.password)) {
			// assign groovy script file to Scriptable Controller
			Corpus corpus  = run(Config.ifScriptController, docs);
			
			// collect all sentences from corpus
			double score = db.getGroupAverageScore(final_group);
			String sentenceString = getSentencesFromCorpus(corpus, score);
			
			Set<String> peopleSet = getPeopleSet(final_group);
			
			HashMap<String, ArrayList<String>> summaryGroup = new HashMap<String, ArrayList<String>>();
			for (String pair : peopleSet) summaryGroup.put(pair, new ArrayList<String>());
			
			HashMap<String, String> params = new HashMap<String, String>();
			String table = "email_groups as a, emails as b, sentences as c";
			params.put("cols", "a.g_id, b.e_id, b.sending_time, c.ann_id, c.sentence, b.sender, b.receiver, b.ccreceiver");
			params.put("cond", "b.e_id = c.e_id And a.e_id = b.e_id And a.g_id = " + final_group + " And c.sentence in (" + sentenceString + ")");
			params.put("order", "b.e_id ASC, b.sending_time ASC, c.ann_id ASC");
			ResultSet rs = db.query(table, params);
			if (DEBUG) Out.println("\n\nSentence Candidates:");
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
			if (DEBUG) Out.println("\n\n");
			
			for (String relation : summaryGroup.keySet()) {
				if (DEBUG) Out.println("==> " + relation);
				String tmpSummary = relation + "\n";
				for (String sen : summaryGroup.get(relation)) {
					if (DEBUG) Out.println("-----> " + sen);
					tmpSummary += sen + "\n";
				}
				summary += tmpSummary + "\n";
			}
			int n_emails = db.getEmailCountFromGroup(final_group);
			String[] data = { String.valueOf(final_group), String.valueOf(n_emails), summary };
			if (db.checkSummaryExist(final_group, n_emails)) db.updateSummary(final_group, n_emails, summary);
			else db.insert(data, "summaries");
			
			outputSummary(final_group, n_emails, summary);
			if (DEBUG) Out.println("Summary for group " + final_group + " (" + n_emails + " emails): ");
			if (DEBUG) Out.println(summary);
		}
		db.close();
		return summary;
	}
}
