package gate;

import static gate.Utils.stringFor;

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
	private final static boolean DEBUG = false;
	private String ds_dir;
	private SerialDataStore ds;
	
	public GateDataStore() {}
	
    public GateDataStore(String dir) throws PersistenceException, UnsupportedOperationException, URISyntaxException {
    	this.ds_dir = dir;
    	createDataStore(this.ds_dir);
    }
    
    private String getFullFilePath(String dir) {
    	return "file://" + dir;
    }
    
    public boolean openDataStore(String dir) {
    	try {
    		dir = getFullFilePath(dir);
    		ds = new SerialDataStore(dir);
    		ds.open();
    		return true;
    	} catch (PersistenceException e) {
    		e.printStackTrace();
    		Out.println(ds.getStorageUrl());
    		System.out.println("failed to open datastore!");
    		System.out.println("please check if the folder is empty or exist.");
    		return false;
    	}
    }
    
	public boolean createDataStore(String dir) {
		try {
			// create&open a new Serial Data Store
			// pass the datastore class and path as parameteres
			dir = getFullFilePath(dir);
			ds  = (SerialDataStore) Factory.createDataStore("gate.persist.SerialDataStore", dir);
			ds.open();
			return true;
			
		} catch (PersistenceException e) {
			System.out.println("failed to create datastore");
			return false;
		}
	}
	
	public int getClusterSize() throws PersistenceException {
		return this.getCorpusIDList().size();
	}
	
	public List getCorpusIDList() throws PersistenceException {
		return ds.getLrIds("gate.corpora.SerialCorpusImpl");
	}
	
	public void delteDataStore() throws PersistenceException {
		ds.delete();
	}
	
	public void closeDataStore() throws PersistenceException {
		ds.close();
		ds = null;
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
		if (DEBUG) Out.prln("corpus saved in datastore...");
		return setCorpusName(corpus_name, persistCorp);
	}
	
	public Object setCorpusName(String corpus_name, Corpus persistCorp) throws PersistenceException, SecurityException {
		// change corpus and sync it with the datastore
		persistCorp.setName(corpus_name);
		persistCorp.sync();
		return persistCorp.getLRPersistenceId();
	}
	
	public Corpus getCorpus(int index) throws PersistenceException, ResourceInstantiationException {
		return getCorpus(getCorpusIDList().get(index));
	}
	
	public Corpus getCorpus(Object corpusID) {
		// load corpus from datastore using its persistent ID
		FeatureMap corpFeatures = Factory.newFeatureMap();
		corpFeatures.put(DataStore.LR_ID_FEATURE_NAME, corpusID);
		corpFeatures.put(DataStore.DATASTORE_FEATURE_NAME, ds);

		// tell the factory to load the Serial Corpus with the specified ID
		// from the specified datastore
		try {
			Corpus persistCorp = (Corpus) Factory.createResource("gate.corpora.SerialCorpusImpl", corpFeatures);

			if (DEBUG) Out.println("corpus loaded from datastore...");
			return persistCorp;
		} catch (ResourceInstantiationException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public void deleteCorpus(Object corpusID) throws PersistenceException {
		// remove corpus from datastore
		ds.delete("gate.corpora.SerialCorpusImpl", corpusID);
		if (DEBUG) Out.prln("corpus deleted from datastore...");
	}
	
	public Object saveDocument(Document doc) throws PersistenceException, SecurityException {
		return saveDocument(doc.getName(), doc);
	}
	
	public Object saveDocument(String doc_name, Document doc) throws PersistenceException, SecurityException {
		// save document in datastore
		// SecurityInfo is ingored for SerialDataStore - just pass null
		Document persistDoc = (Document) ds.adopt(doc, null);
		ds.sync(persistDoc);
		if (DEBUG) Out.prln("document saved in datastore...");
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
		if (DEBUG) Out.prln("document deleted from datastore...");
	}
	
	public ArrayList<String> getStringFromAnnotation(Object docID, String type) throws ResourceInstantiationException {
		ArrayList<String> strings = new ArrayList<String>();
		Document doc = getDocument(docID);
		for (Annotation annt: doc.getAnnotations().get(type)) strings.add(stringFor(doc, annt));
		return strings;
	}
	
}