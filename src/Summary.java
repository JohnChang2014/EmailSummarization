import file.Directory;
import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.GateDataStore;
import gate.GateDocsHandler;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.annic.Term;
import gate.groovy.ScriptableController;
import gate.gui.MainFrame;
import gate.persist.PersistenceException;
import gate.security.SecurityException;
import gate.termraider.bank.HyponymyTermbank;
import gate.termraider.bank.TfIdfTermbank;
import gate.util.GateException;
import gate.util.Out;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;

import com.google.code.jconfig.ConfigurationManager;
import com.google.code.jconfig.listener.IConfigurationChangeListener;

import config.ServerConfigurationContainer;
import config.ServerConfigurationPlugin;
import date.DateTime;
import db.*;

public class Summary {
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
			// TODO Auto-generated catch block
			e.printStackTrace();
				
		} catch (UnsupportedOperationException e) {
			// TODO Auto-generated catch block
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
		// List corpusIDs = null;
		Directory dir = new Directory();
		dirname = System.getProperty("user.dir") + dirname;
		if (dir.isEmpty(dirname) ) return ds.createDataStore(dirname);
		else return ds.openDataStore(dirname);
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
		ie                          = new IEProcessor(true);
		Corpus corpus               = ie.run(Config.ieScriptController, docs);
		ArrayList<String[]> wordset = getTermbankFromCorpus(String.valueOf(e_id), corpus);
		db.insert(wordset, "words");
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
	
	public Corpus updateGroupInfo(int final_group) throws ResourceInstantiationException, ExecutionException, IOException, SQLException, ParseException, PersistenceException, SecurityException {
		// store all annotation data into datastore
		if (db.getGroupCounts() == 0 || final_group > ds.getClusterSize()) {
			Corpus corpus = ie.getCorpus();
			corpus.setName(String.valueOf(final_group));
			Out.println(corpus.get(0).getContent());
			ds.saveCorpus(corpus);
			return corpus;
		} else {
			int groupIndex   = final_group - 1;
			Corpus corpus    = ds.getCorpus(groupIndex);
			Document doc     = ie.getCorpus().get(0);
			corpus.add(doc);
			
			dm               = new DataMaintainProcessor(true);
			corpus           = dm.run(Config.dataMaintainScriptContoller, corpus);
			ds.saveCorpus(corpus);
			
			ArrayList<String[]> wordset = getTermbankFromCorpus(String.valueOf(final_group), corpus);
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
	
	public static void main(String[] args) throws Exception {
		
		int final_group = 0;
		Transaction db  = new Transaction();
		Summary sumApp  = new Summary();
		
		// read and check if there is new email coming to inbox

		// parse email data and store into database
		
		/*
		 * information extraction and annotation using GATE then compute tf and
		 * store into database
		 */
		// fetch email data from database
		db.connect(Config.ip, Config.port, Config.db, Config.username, Config.password);
		
		int bound = 40;
		String subject = new String();
		
		HashMap<String, String> params = new HashMap<String, String>();
		for (int e_id = 1; e_id <= bound; e_id++) {
			if (e_id != 40) continue;
			ArrayList<Document> docs = new ArrayList<Document>();
			params.put("cols", "content, e_id, subject");
			params.put("cond", "e_id = " + e_id);
			ResultSet rs = db.query("emails", params);

			if (rs.first()) {
				subject = rs.getString("subject");
				String docName = subject + "_" + rs.getString("e_id");
				String content = subject + "\n" + rs.getString("content");
				Document doc   = sumApp.newDoc(docName, content);
				docs.add(doc);
			}

			// extract information from new email
			ArrayList<String[]> wordset = sumApp.runInfoExtraction(e_id, docs);
			Out.println("================== IE done!! ");
			
			// cluster the incoming email to email group
			final_group = sumApp.runCluster(String.valueOf(e_id), subject, wordset);
			Out.println("================== cluster done!! ");

			// update tfidf for the final group
			Out.println("--> " + final_group);
			sumApp.updateGroupInfo(final_group);
			Out.println("================== update cluster done!! ");
			
			params.clear();
		}
		Thread.sleep(120000);
		System.exit(0);
		/*
		 * summarize the email group read each email sorted by date in the group
		 * apply inferece rules here in GATE
		 */
	}
}
