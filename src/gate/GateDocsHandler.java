package gate;

import gate.creole.ResourceInstantiationException;

import java.net.URL;
import java.util.ArrayList;

public class GateDocsHandler {
	private Document createDocumentFromSource(String content) throws Exception {
		Document doc = null;
		if (!content.matches("^[hH][tT]{2}[pP]s?://[\\w\\d\\-\\?=\\.\\/]+$")) {
			doc = Factory.newDocument(content);
		
		} else {
			doc = Factory.newDocument(new URL(content), "UTF-8");
		}
		
		return doc;
	}
	
	public Document createDoc(String docName, String content) throws Exception {
		Document doc = createDocumentFromSource(content);
		doc.setName(docName);
		return doc;
	}
	
	public Document createDoc(String docName, String content, FeatureMap feats) throws Exception {
		Document doc = createDoc(docName, content);
		doc.setFeatures(feats);
		return doc;
	}
	
	public Corpus createCorpus(String corpusName) throws ResourceInstantiationException {
		return Factory.newCorpus(corpusName);
	}
	
	public Corpus createCorpus(String corpusName, ArrayList<Document> docs) throws ResourceInstantiationException {
		Corpus corpus = createCorpus(corpusName);
		corpus.addAll(docs);
		return corpus;
	}
}
