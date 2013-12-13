import file.Directory;
import gate.Corpus;
import gate.Document;
import gate.Gate;
import gate.GateDataStore;
import gate.GateDocsHandler;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import gate.security.SecurityException;
import gate.termraider.bank.TfIdfTermbank;
import gate.util.GateException;
import gate.util.Out;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.code.jconfig.ConfigurationManager;
import com.google.code.jconfig.listener.IConfigurationChangeListener;

import config.ServerConfigurationContainer;
import db.*;

public class Summary {
	private final static boolean DEBUG = true;
	private Transaction db;
	private GateDataStore ds;
	private GateDocsHandler dh;
	private IEProcessor ie;
	private ClusterProcessor cluster;
	private DataMaintainProcessor dm;
	
	public Summary() {
		if (!this.init()) {
			System.out.println("failed to initialize program!");
			System.exit(0);
		}
	}
	
	public boolean init()  {		
		try {
			Gate.init();
			
			this.db = new Transaction();
			this.ds = new GateDataStore();
			this.dh = new GateDocsHandler();
			if (!this.openDataStore(Config.ds_dir)) return false;
			if (!db.connect(Config.ip, Config.port, Config.db, Config.username, Config.password)) return false;
			return true;
		} catch (GateException e) {
			e.printStackTrace();
				
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
		}
		return false;
	} 
	
	public static void loadConfiguration() {
		String location = System.getProperty("user.dir");
		String configPath = location + "/inc/config.xml";
		System.out.println(configPath);
		Map<String, IConfigurationChangeListener> listeners = new HashMap<String, IConfigurationChangeListener>();
		listeners.put("server_list", new ServerConfigurationContainer());
		ConfigurationManager.configureAndWatch(listeners, configPath, 200L);
	}
	
	private boolean openDataStore(String dirname) {
		Directory dir = new Directory();
		dirname = System.getProperty("user.dir") + dirname;
		if (dir.isEmpty(dirname)) {
			Out.println(">>>>>>>>>>>>>>>> empty");
			return ds.createDataStore(dirname);
		}
		else {
			Out.println(">>>>>>>>>>>>>>>> not empty");
			return ds.openDataStore(dirname);
		}
	}
	
	private ArrayList<String[]> getTermbankFromCorpus(String e_id, Corpus corpus) {
		ArrayList<String[]> wordset = new ArrayList<String[]>();
		TfIdfTermbank termBank      = (TfIdfTermbank) corpus.getFeatures().get("tfidfTermbank");
		
		Map<gate.termraider.util.Term, Integer> termFrequency = termBank.getTermFrequencies();
		Map<gate.termraider.util.Term, Integer> termDocFrequencies = termBank.getDocFrequencies();
		Map<gate.termraider.util.Term, Double> termScores = termBank.getTermScores();
		
		for (gate.termraider.util.Term term : termFrequency.keySet()) {
		    
			Out.println(term.getTermString() + " = " + termBank.getRawScore(term) + " <> " + termScores.get(term) + " => " + termBank.getRawScore(term) / termFrequency.get(term));
			String tf     = String.valueOf(termFrequency.get(term));
			String df     = String.valueOf(termDocFrequencies.get(term));
			String idf    = String.valueOf(termBank.getRawScore(term) / termFrequency.get(term));
			String tfidf  = String.valueOf(termScores.get(term));
			String[] data = { String.valueOf(e_id), term.getTermString(), term.getType(), tf, df, idf, tfidf };
			wordset.add(data);
		}
		return wordset;
	}
	
	public ArrayList<String[]> runInfoExtraction(int e_id, ArrayList<Document> docs) throws SQLException, ParseException, MalformedURLException, InterruptedException, InvocationTargetException, GateException {
		// start the task of information extraction
		ie                          = new IEProcessor(Config.gate_view);
		Corpus corpus               = ie.run(Config.ieScriptController, docs);
		ArrayList<String[]> wordset = getTermbankFromCorpus(String.valueOf(e_id), corpus);
		db.insert(wordset, "words");
		ie.cleanup();
		return wordset;
	}
	
	public int runCluster(String e_id, String subject, ArrayList<String[]> wordset) throws SQLException, ParseException {
		ArrayList<String> new_email = new ArrayList<String>();
		cluster                     = new ClusterProcessor();
		new_email.add(e_id);
		new_email.add(subject);
		ResultSet grs = db.getEmailGroups();
		return cluster.run(grs, new_email, wordset);
	}
	
	public Corpus updateGroupInfo(int final_group) throws ResourceInstantiationException, ExecutionException, IOException, SQLException, ParseException, PersistenceException, SecurityException, InterruptedException {
		// store all annotation data into datastore
		if (db.getGroupCounts() == 0 || final_group > ds.getClusterSize()) {
			Corpus corpus = ie.getCorpus();
			corpus.setName(String.valueOf(final_group));
			ds.saveCorpus(corpus);
			return corpus;
		} else {
			int groupIndex   = final_group - 1;
			// fetch email group from datastore via group index
			// then extract all emails in the group as a new document set
			ArrayList<Document> docs = new ArrayList<Document>();
			Corpus corpus            = ds.getCorpus(groupIndex);
			for (Document d : corpus) docs.add(d);
			
			// fetch email document we processed in IE component
			// add this email to the document set that we just created
			Document doc     = ie.getCorpus().get(0);
			docs.add(doc);
			
			// create a new corpus and add the document set into it for 
			// computing tfidf score for each term in the document set 
			Corpus new_corpus = dh.createCorpus(String.valueOf(final_group));
			new_corpus.addAll(docs);
			dm                = new DataMaintainProcessor(Config.gate_view);
			new_corpus        = dm.run(Config.dataMaintainScriptContoller, new_corpus);
			
			// now new corpus will contain new tfidf score for all terms and annotations
			// then, delete old corpus in the datastore and add the new one into it 
			// (for some reason we have to delete the old one and then we could store the new one)
			ds.deleteCorpus(corpus.getLRPersistenceId());
			ds.saveCorpus(new_corpus);
			
			// fetch all terms with new scores from new corpus and then store all terms into database
			// To avoid lots of comparison between existing terms in database and new terms in corpus,
			// the easiest way to get the job done is remove all existing terms for the group and insert
			// all new terms to the group.
			ArrayList<String[]> wordset = getTermbankFromCorpus(String.valueOf(final_group), new_corpus);
			db.removeWordsFromGroup(final_group);
			db.insert(wordset, "group_words");
			dm.cleanup();
			return corpus;
		}
	}
	
	public ArrayList<Document> createDocumentSet(int g_id) throws Exception {
		ArrayList<Document> docs = new ArrayList<Document>();

		ResultSet rs = db.getEmailsFromGroup(g_id);
		while (rs.next()) {
			String docName = rs.getString("subject") + "_" + rs.getString("e_id");
			String content = rs.getString("subject") + "\n" + rs.getString("content");
			Document doc   = ie.createDocument(docName, content);
			docs.add(doc);
		}
		return docs;
	}
	
	public Document newDoc(String docName, String content) throws Exception {
		return dh.createDoc(docName, content);
	}
	
	public void close() throws PersistenceException {
		db.close();
		ds.closeDataStore();
		dh = null;
		ie = null;
		cluster = null;
		dm = null;
	}
	
	public static void main(String[] args) throws Exception {

		int final_group = 0;
		Transaction db = new Transaction();
		Summary sumApp = new Summary();

		// read and check if there is new email coming to inbox

		// parse email data and store into database

		/*
		 * information extraction and annotation using GATE then compute tf and
		 * store into database
		 */
		// fetch email data from database
		db.connect(Config.ip, Config.port, Config.db, Config.username, Config.password);

		int e_id = 20;
		String subject = new String();

		HashMap<String, String> params = new HashMap<String, String>();

		ArrayList<Document> docs = new ArrayList<Document>();
		params.put("cols", "content, e_id, subject");
		params.put("cond", "e_id = " + e_id);
		ResultSet rs = db.query("emails", params);

		if (rs.first()) {
			subject = rs.getString("subject");
			String docName = subject + "_" + rs.getString("e_id");
			String content = subject + "\n" + rs.getString("content");
			Document doc = sumApp.newDoc(docName, content);
			docs.add(doc);
			
			if (DEBUG) Out.println("email ID: " + e_id);
			
			if (DEBUG) Out.println("====== IE processing ======");
			// information extraction from new email
			ArrayList<String[]> wordset = sumApp.runInfoExtraction(e_id, docs);
			if (DEBUG) Out.println("=========================== \n\n");

			if (DEBUG) Out.println("====== Cluster processing ======");
			// cluster the incoming email to email group
			final_group = sumApp.runCluster(String.valueOf(e_id), subject, wordset);
			if (DEBUG) Out.println("=========================== \n\n");

			if (DEBUG) Out.println("====== Data Update processing ====== ");
			// update tfidf for the final group
			sumApp.updateGroupInfo(final_group);
			if (DEBUG) Out.println("=========================== \n\n");
			if (DEBUG) Out.println("Final Group --> " + final_group);
			params.clear();
			
			/*
			 * summarize the email group read each email sorted by date in the
			 * group apply inferece rules here in GATE
			 */
		}

		db.close();
		sumApp.close();
		db = null;
		sumApp = null;
		Thread.sleep(100000);
		System.exit(0);
	}
}
