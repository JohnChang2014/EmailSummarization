import static gate.Utils.stringFor;
import file.Directory;
import gate.Annotation;
import gate.AnnotationSet;
import gate.Corpus;
import gate.Document;
import gate.Gate;
import gate.GateDataStore;
import gate.GateDocsHandler;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.gui.MainFrame;
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

import db.*;

public class Summary {
	private final static boolean DEBUG = true;
	private Transaction db;
	private GateDataStore ds;
	private GateDocsHandler dh;
	private IEProcessor ie;
	private ClusterProcessor cluster;
	private DataMaintainProcessor dm;
	private InferenceProcessor inference;
	private String subject = new String();
	private int mode;
	
	public Summary(int mode) {
		this.mode = mode;
		if (!this.init(mode)) {
			System.out.println("failed to initialize program!");
			System.exit(0);
		}
	}
	
	public boolean init(int mode)  {		
		try {
			Gate.init();
			
			this.db = new Transaction();
			this.ds = new GateDataStore();
			this.dh = new GateDocsHandler();
			if (mode == 0) {
				if (!this.openDataStore(Config.ds_dir_development)) return false;
				if (!db.connect(Config.ip, Config.port, Config.db_development, Config.username, Config.password)) return false;
			} else if (mode == 1) {
				if (!this.openDataStore(Config.ds_dir)) return false;
				if (!db.connect(Config.ip, Config.port, Config.db, Config.username, Config.password)) return false;
			}
			return true;
		} catch (GateException e) {
			e.printStackTrace();
				
		} catch (UnsupportedOperationException e) {
			e.printStackTrace();
		}
		return false;
	} 
	
	private boolean openDataStore(String dirname) {
		Directory dir = new Directory();
		if (dir.isEmpty(dirname)) {
			return ds.createDataStore(dirname);
		} else {
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
		    
			//Out.println(term.getTermString() + " = " + termBank.getRawScore(term) + " <> " + termScores.get(term) + " => " + termBank.getRawScore(term) / termFrequency.get(term));
			String tf     = String.valueOf(termFrequency.get(term));
			String df     = String.valueOf(termDocFrequencies.get(term));
			String idf    = String.valueOf(termBank.getRawScore(term) / termFrequency.get(term));
			String tfidf  = String.valueOf(termScores.get(term));
			String[] data = { String.valueOf(e_id), term.getTermString(), term.getType(), tf, df, idf, tfidf };
			wordset.add(data);
		}
		return wordset;
	}
	
	private ArrayList<String[]> getSentencesFromCorpus(String e_id, Corpus corpus) {
		ArrayList<String[]> sentences = new ArrayList<String[]>();
		Document doc = corpus.get(0);
		AnnotationSet annSet = doc.getAnnotations();
		for (Annotation annt : annSet.get("Sentence")) {
			String[] data = { e_id, String.valueOf(annt.getId()), stringFor(doc, annt) };
			sentences.add(data);
		}
		return sentences;
	}
	
	public ArrayList<String[]> runInfoExtraction(int e_id, ArrayList<Document> docs) throws SQLException, ParseException, MalformedURLException, InterruptedException, InvocationTargetException, GateException {
		// start the task of information extraction
		ie                            = new IEProcessor(DEBUG);
		Corpus corpus                 = ie.run(Config.ieScriptController, docs);
		ArrayList<String[]> wordset   = getTermbankFromCorpus(String.valueOf(e_id), corpus);
		ArrayList<String[]> sentences = getSentencesFromCorpus(String.valueOf(e_id), corpus);
		db.insert(wordset, "words");
		db.insert(sentences, "sentences");
		ie.cleanup();
		return wordset;
	}
	
	public int runCluster(String e_id, ArrayList<String[]> wordset) throws SQLException, ParseException {
		ArrayList<String> new_email = new ArrayList<String>();
		cluster                     = new ClusterProcessor(mode);
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
			dm                = new DataMaintainProcessor(DEBUG);
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
	
	public String runInference(int final_group) throws PersistenceException, ResourceInstantiationException, InterruptedException {
		System.out.println("==> " + final_group);
		try {
			inference = new InferenceProcessor(mode);
			ArrayList<Document> docs = new ArrayList<Document>();
			for (Document doc : ds.getCorpus(final_group - 1)) docs.add(doc);
			String summary;
			summary = inference.summarize(final_group, docs);
			inference.cleanup();
			return summary;
			
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	private Document getEmailData(int e_id) throws Exception {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("cols", "content, e_id, subject");
		params.put("cond", "e_id = " + e_id);
		ResultSet rs = db.query("emails", params);
		if (rs.next()) {
			subject        = rs.getString("subject");
			String docName = subject + "_" + rs.getString("e_id");
			String content = subject + ".\n" + rs.getString("content");
			return dh.createDoc(docName, content);
		}
		return null;
	}
	
	public int runIEAndClustering(int e_id) {
		int final_group = 0;
		ArrayList<Document> docs = new ArrayList<Document>();
		Document doc;
		
		try {
			
			doc = getEmailData(e_id);
			docs.add(doc);

			if (DEBUG) Out.println("email ID: " + e_id);

			if (DEBUG) Out.println("====== IE processing ======");
			// information extraction from new email
			ArrayList<String[]> wordset = runInfoExtraction(e_id, docs);
			if (DEBUG) Out.println("=========================== \n\n");

			if (DEBUG) Out.println("====== Cluster processing ======");
			// cluster the incoming email to email group
			final_group = runCluster(String.valueOf(e_id), wordset);
			if (DEBUG) Out.println("=========================== \n\n");

			if (DEBUG) Out.println("====== Data Update processing ====== ");
			// update tfidf for the final group
			updateGroupInfo(final_group);
			if (DEBUG) Out.println("=========================== \n\n");
			
			if (DEBUG) Out.println("Final Group --> " + final_group);
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return final_group;
	}
	
	public void run(int e_id) {
		try {
			int final_group = runIEAndClustering(e_id);
			if (final_group > 0) runInference(final_group);
			else {
				System.out.println("failed to proceed");
			}
		} catch (PersistenceException e) {
			e.printStackTrace();
		} catch (ResourceInstantiationException e) {
			e.printStackTrace();
		} catch (InterruptedException e) {
			e.printStackTrace();
		}
	}
	
	public void close() throws PersistenceException {
		if (db != null) db.close();
		if (ds != null) ds.closeDataStore();
		if (ie != null) ie.cleanup();
		if (cluster != null) cluster.close();
		if (inference != null) inference.cleanup();
		dh = null;
		ie = null;
		cluster = null;
		dm = null;
		inference = null;
	}
}
