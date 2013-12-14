package gate;

import gate.creole.ResourceInstantiationException;
import gate.persist.PersistenceException;
import gate.util.GateException;
import static gate.Utils.*;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;


public class GateAnnotationsHandler {
	private final static boolean DEBUG = true;
	/*
	public void getAnnotationSet(Document doc) throws MalformedURLException {
		System.out.println(doc.getNamedAnnotationSets().size());
		for (String key : doc.getNamedAnnotationSets().keySet()) {
			System.out.println("ann set: " + key);
			System.out.println("length: " + doc.getNamedAnnotationSets()
					.get(key).size());
			for (Annotation ann : doc.getNamedAnnotationSets().get(key)) {
				if (ann.getType().equals("a")) {
					System.out.println("-->" + ann.toString());
					System.out.println("----->" + ann.getType());
					System.out
							.println("-------->" + new URL(doc.getSourceUrl(), ann
									.getFeatures().get("href").toString())
									.toString());
				}
			}
		}
		System.out.println(doc.getAnnotations("Original markups").toString());
	}
	*/

	
	public static void main(String[] args) throws MalformedURLException, GateException {
		Gate.init();
		GateDataStore ds = new GateDataStore();
		GateAnnotationsHandler ann = new GateAnnotationsHandler();
		String ds_dir = System.getProperty("user.dir") + "/data/clusters"; 
		if (ds.openDataStore(ds_dir)) {
			Corpus corpus = ds.getCorpus(0);
			for (Document doc : corpus) {
				System.out.println(doc.getLRPersistenceId());
				doc = ds.getDocument(doc.getLRPersistenceId());
				System.out.println(doc.getAnnotations().get("Sentence"));
				for (Annotation annt: doc.getAnnotations().get("Sentence"))
					System.out.println(stringFor(doc, annt));
			}
		}
	}
	
}
