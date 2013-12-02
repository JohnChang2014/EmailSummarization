package gate;

import java.net.URL;

public class GateDocHandler {
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
		//doc.setParameterValues(feats);
		return doc;
	}
}
