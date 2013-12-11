package gate;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import gate.persist.PersistenceException;
import gate.persist.SerialDataStore;
import gate.security.SecurityException;
import gate.util.Err;
import gate.util.Out;
import gate.creole.ResourceInstantiationException;
import gate.util.GateException;

public class GateDataStore {

	// the directory must EXIST and be EMPTY
	// private static final String dsDir = "/var/tmp/gate001";
	private static final String DS_DIR = "./data/clusters";
	
	private String current_path;
	private String ds_dir;
	private SerialDataStore ds;
	
	public GateDataStore() {
		this.current_path = System.getProperty("user.dir");
	}
	
    public GateDataStore(String dir) throws PersistenceException, UnsupportedOperationException, URISyntaxException {
    	this.current_path = System.getProperty("user.dir");
    	this.ds_dir = dir;
    	createDataStore(this.ds_dir);
    }
    
    private String getFilePath(String dir) {
    	return "file://"+ System.getProperty("user.dir") + dir;
    }
    
    public boolean openDataStore(String dir) {
    	try {
    		dir = getFilePath(dir);
    		ds = new SerialDataStore(dir);
    		ds.open();
    		return true;
    	} catch (PersistenceException e) {
    		e.printStackTrace();
    		System.out.println("fail to open datastore!");
    		return false;
    	}
    }
    
	public boolean createDataStore(String dir) {
		try {
			// create&open a new Serial Data Store
			// pass the datastore class and path as parameteres
			dir = getFilePath(dir);
			ds = (SerialDataStore) Factory.createDataStore("gate.persist.SerialDataStore", dir);
			ds.open();
			return true;
			
		} catch (PersistenceException e) {
			System.out.println("fail to create datastore");
			return false;
		}
	}
	
	public List getCorpusIDList() throws PersistenceException {
		return ds.getLrIds("gate.corpora.SerialCorpusImpl");
	}
	
	public void delteDataStore() throws PersistenceException {
		ds.delete();
	}
	
	public void closeDataStore() throws PersistenceException {
		ds.close();
	}
	
	public Object saveCorpus(Corpus corpus) throws PersistenceException, SecurityException {
		return saveCorpus(corpus.getName(), corpus);
	}
	
	public Object saveCorpus(String corpus_name, Corpus corpus) throws PersistenceException, SecurityException {
		// save corpus in datastore
		// SecurityInfo is ingored for SerialDataStore - just pass null
		// a new persisent corpus is returned
		Corpus persistCorp = (Corpus) ds.adopt(corpus, null);
		ds.sync(persistCorp);
		Out.prln("corpus saved in datastore...");
		
		return setCorpusName(corpus_name, persistCorp);
	}
	
	public Object setCorpusName(String corpus_name, Corpus persistCorp) throws PersistenceException, SecurityException {
		// change corpus and sync it with the datastore
		persistCorp.setName(corpus_name);
		persistCorp.sync();
		return persistCorp.getLRPersistenceId();
	}
	
	public Corpus getCorpus(int index) throws PersistenceException, ResourceInstantiationException {
		Object corpusID = getCorpusIDList().get(index);
		return getCorpus(corpusID);
	}
	
	public Corpus getCorpus(Object corpusID) throws ResourceInstantiationException {
		// load corpus from datastore using its persistent ID
		FeatureMap corpFeatures = Factory.newFeatureMap();
		corpFeatures.put(DataStore.LR_ID_FEATURE_NAME, corpusID);
		corpFeatures.put(DataStore.DATASTORE_FEATURE_NAME, ds);

		// tell the factory to load the Serial Corpus with the specified ID
		// from the specified datastore
		Corpus persistCorp = (Corpus) Factory.createResource("gate.corpora.SerialCorpusImpl", corpFeatures);
		Out.prln("corpus loaded from datastore...");
		return persistCorp;
	}
	
	public void deleteCorpus(Object corpusID) throws PersistenceException {
		// remove corpus from datastore
		ds.delete("gate.corpora.SerialCorpusImpl", corpusID);
		Out.prln("corpus deleted from datastore...");
	}
	
	public Object saveDocument(Document doc) throws PersistenceException, SecurityException {
		return saveDocument(doc.getName(), doc);
	}
	
	public Object saveDocument(String doc_name, Document doc) throws PersistenceException, SecurityException {
		// save document in datastore
		// SecurityInfo is ingored for SerialDataStore - just pass null
		Document persistDoc = (Document) ds.adopt(doc, null);
		ds.sync(persistDoc);
		Out.prln("document saved in datastore...");
		return setDocName(doc_name, persistDoc);
		
	}
	
	public Object setDocName(String doc_name, Document persistDoc) throws PersistenceException, SecurityException {
		// change document and sync it with the datastore
		persistDoc.setName(doc_name);
		persistDoc.sync();
		return persistDoc.getLRPersistenceId();
	}
	
	public Document getDocument(Object docID) throws ResourceInstantiationException {
		// load document from datastore
		FeatureMap docFeatures = Factory.newFeatureMap();
		docFeatures.put(DataStore.LR_ID_FEATURE_NAME, docID);
		docFeatures.put(DataStore.DATASTORE_FEATURE_NAME, ds);

		return (Document) Factory.createResource("gate.corpora.DocumentImpl", docFeatures);
	}
	
	public void deleteDocument(Object docID) throws PersistenceException {
		// remove document from datastore
		ds.delete("gate.corpora.DocumentImpl", docID);
		Out.prln("document deleted from datastore...");
	}
	
	/*
	public static void main(String[] args) throws PersistenceException {

		
		// init GATE
		// this is the first thing to be done
		try {
			Gate.init();
		} catch (GateException gex) {
			Err.prln("cannot initialise GATE...");
			gex.printStackTrace();
			return;
		}

		
		
		try {
			
			GateDataStore dsApp = new GateDataStore("file://"+ System.getProperty("user.dir")+"/data/clusters");
			GateDocHandler docHandler = new GateDocHandler();
			
			Corpus corpus1 = Factory.newCorpus("test1");
			Document doc1 = docHandler.createDoc("doc1", "http://www.cnn.com/2013/12/10/world/gapminder-us-ignorance-survey/index.html?hpt=hp_t1");
			Document doc2 = docHandler.createDoc("doc2", "http://www.cnn.com/2013/12/09/world/costa-concordia-trial/index.html?hpt=wo_c2");
			corpus1.add(doc1);
			corpus1.add(doc2);
			
			Corpus corpus2 = Factory.newCorpus("test2");
			Document doc3 = docHandler.createDoc("doc3", "http://www.cnn.com/2013/12/10/world/africa/nelson-mandela-memorial/index.html?hpt=us_c2");
			Document doc4 = docHandler.createDoc("doc4", "http://ireport.cnn.com/docs/DOC-1067344?hpt=us_c2");
			corpus2.add(doc3);
			corpus2.add(doc4);
			
			Object corpusID1 = dsApp.saveCorpus("1", corpus1);
			Object corpusID2 = dsApp.saveCorpus("2", corpus2);
			
			Document doc5 = docHandler.createDoc("doc5", "http://www.cnn.com/2013/12/10/world/africa/nelson-mandela-memorial/index.html?hpt=po_c1");
			Corpus corpus = dsApp.getCorpus(corpusID1);
			corpus.add(doc5);
			//dsApp.saveCorpus("1", corpus);
			
			
			GateDataStore dsApp = new GateDataStore();
			GateDocHandler docHandler = new GateDocHandler();
			dsApp.openDataStore("file://"+ System.getProperty("user.dir")+"/data/clusters");
			
			Document doc5 = docHandler.createDoc("doc5", "http://www.cnn.com/2013/12/10/world/africa/nelson-mandela-memorial/index.html?hpt=po_c1");
			
			//dsApp.saveCorpus("1", corpus);
			
			int i = 0;
			for (Object ob : dsApp.getCorpusList()) {
				Out.println(ob);
				Corpus corpus = dsApp.getCorpus(ob);
				corpus.add(doc5);
				dsApp.saveCorpus("1", corpus);
				Corpus corpus1 = dsApp.getCorpus(ob);
				for (Document doc : corpus1) {
					Out.println(doc.getLRPersistenceId());
				}
			}
			*/
			
/*
			// create&open a new Serial Data Store
			// pass the datastore class and path as parameteres
			SerialDataStore sds = (SerialDataStore) Factory
					.createDataStore("gate.persist.SerialDataStore", DS_DIR);
			sds.open();
			Out.prln("serial datastore created...");

			// create test corpus
			Corpus corp = dsApp.createTestCorpus();
			Corpus persistCorp = null;

			// save corpus in datastore
			// SecurityInfo is ingored for SerialDataStore - just pass null
			// a new persisent corpus is returned
			persistCorp = (Corpus) sds.adopt(corp, null);
			sds.sync(persistCorp);
			Out.prln("corpus saved in datastore...");

			// change corpus and sync it with the datastore
			persistCorp.setName("new name");
			persistCorp.sync();

			// load corpus from datastore using its persistent ID
			Object corpusID = persistCorp.getLRPersistenceId();
			persistCorp = null;

			FeatureMap corpFeatures = Factory.newFeatureMap();
			corpFeatures.put(DataStore.LR_ID_FEATURE_NAME, corpusID);
			corpFeatures.put(DataStore.DATASTORE_FEATURE_NAME, sds);

			// tell the factory to load the Serial Corpus with the specified ID
			// from the specified datastore
			persistCorp = (Corpus) Factory
					.createResource("gate.corpora.SerialCorpusImpl", corpFeatures);
			Out.prln("corpus loaded from datastore...");

			// remove corpus from datastore
			sds.delete("gate.corpora.SerialCorpusImpl", corpusID);
			Out.prln("corpus deleted from datastore...");
			persistCorp = null;

			// close data store
			sds.close();
			sds = null;

			// reopen it
			sds = new SerialDataStore(DS_DIR);
			sds.open();

			// save, load and delete a document
			Document doc = dsApp.createTestDocument();
			Document persistDoc = null;

			// save document in datastore
			// SecurityInfo is ingored for SerialDataStore - just pass null
			persistDoc = (Document) sds.adopt(doc, null);
			sds.sync(persistDoc);
			Out.prln("document saved in datastore...");

			// change document and sync it with the datastore
			persistDoc.setName("new name");
			persistDoc.sync();

			// load document from datastore
			Object docID = persistDoc.getLRPersistenceId();
			persistDoc = null;

			FeatureMap docFeatures = Factory.newFeatureMap();
			docFeatures.put(DataStore.LR_ID_FEATURE_NAME, docID);
			docFeatures.put(DataStore.DATASTORE_FEATURE_NAME, sds);

			persistDoc = (Document) Factory
					.createResource("gate.corpora.DocumentImpl", docFeatures);
			Out.prln("document loaded from datastore...");

			// remove document from datastore
			sds.delete("gate.corpora.DocumentImpl", docID);
			Out.prln("document deleted from datastore...");
			persistDoc = null;

			// close data store
			sds.close();

			// delete datastore
			sds.delete();

		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	*/

}