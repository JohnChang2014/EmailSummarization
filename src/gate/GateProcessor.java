package gate;

import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.gui.MainFrame;
import gate.persist.PersistenceException;
import gate.util.GateException;

import java.io.File;
import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.SwingUtilities;

public class GateProcessor {
	private static final String plugins_home = "./plugins";
	private static final String gapp_folder = "./gapp";
	
	private GateAnalyserController controller;
	
	public GateProcessor() throws GateException, InterruptedException, InvocationTargetException {
		Gate.init();
		
		// show the main window
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
				MainFrame.getInstance().setVisible(true);
			}
		});
		
		controller = new GateAnalyserController();
	}
	
	public Document loadDocumentFromSource(String source_type, String content) throws Exception {
		Document doc = null;
		if (source_type.equals("text")) {
			doc = Factory.newDocument(content);
		
		} else if (source_type.equals("url")) {
			doc = Factory.newDocument(new URL(content), "UTF-8");
		}
		
		return doc;
	}
	
	public Document loadDocumentShortcuts(String source_type, String docName, String content) throws Exception {
		Document doc = loadDocumentFromSource(source_type, content);
		FeatureMap feats = Factory.newFeatureMap();
		feats.put("CreatedBy", "Johnson!");
		doc.setFeatures(feats);
		doc.setName(docName);
		return doc;
	}
	
	public void getAnnotationSet(Document doc) throws MalformedURLException {
		System.out.println(doc.getNamedAnnotationSets().size());
		for (String key : doc.getNamedAnnotationSets().keySet()) {
			System.out.println("ann set: " + key);
			System.out.println("length: " + doc.getNamedAnnotationSets().get(key).size());
			for (Annotation ann : doc.getNamedAnnotationSets().get(key)) {
				if (ann.getType().equals("a")) {
					System.out.println("-->" + ann.toString());
					System.out.println("----->" + ann.getType());
					System.out.println("-------->" + new URL(doc.getSourceUrl(), ann.getFeatures().get("href").toString()).toString());
				}
			}
		}
		System.out.println(doc.getAnnotations("Original markups").toString());
	}
	
	public void loadDocument() throws Exception {
		FeatureMap params = Factory.newFeatureMap();
		params.put(Document.DOCUMENT_STRING_CONTENT_PARAMETER_NAME, "This is a document!");
		FeatureMap feats = Factory.newFeatureMap();
		feats.put("CreatedBy", "Johnson!");
		Factory.createResource("gate.corpora.DocumentImpl", params, feats, "My first document.");
	}
	
	public Document createDocument() throws Exception {
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
				MainFrame.getInstance().setVisible(true);
			}
		});
		
		//MainFrame.getInstance().setVisible(true);
		//Factory.newDocument("This is a document.");
		//loadDocument();
		//Document doc = loadDocumentShortcuts("url", "This is home", "http://gate.ac.uk");
		Document doc = loadDocumentShortcuts("url", "This is home", "http://money.cnn.com/2013/11/27/pf/black-friday-tricks/index.html?iid=lead2");
		//getAnnotationSet(doc);
		return doc;
	}
	
	public void loadGatePlugins(String plugin_name) throws MalformedURLException, GateException {
		// get the root plugins dir
		// File pluginsDir = Gate.getPluginsHome();
		String plugins_folder = plugins_home;
		File pluginsDir       = new File(plugins_folder);

		// Let’s load the Tools plugin
		File aPluginDir = new File(pluginsDir, plugin_name);
		
		// load the plugin.
		Gate.getCreoleRegister().registerDirectories(aPluginDir.toURI().toURL());
	}
	
	public void execute(ArrayList<LanguageAnalyser> pr_set, Document doc) throws ExecutionException, ResourceInstantiationException {
		controller.addProcessResource(pr_set);
		controller.execute(doc);
	}
	
	public void execute(ArrayList<LanguageAnalyser> pr_set, Corpus corpus) throws ExecutionException {
		controller.addProcessResource(pr_set);
		controller.execute(corpus);
	}
	
	public void save(String file_name) throws PersistenceException, IOException {
		String file_path = gapp_folder + "/" + file_name;
		controller.saveGateApplication(file_path);
	}
	
	public static void main(String[] args) throws Exception {
		
		GateProcessor gp = new GateProcessor();
		
		// create GATE document
		Document doc     = gp.createDocument();

		// load ANNIE plugin
		gp.loadGatePlugins("ANNIE");
		
		// create language analysers tokenizer
		ArrayList<LanguageAnalyser> pr_set = new ArrayList<LanguageAnalyser>();
		LanguageAnalyser docReset    = (LanguageAnalyser) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR");
		LanguageAnalyser tokenizer   = (LanguageAnalyser) Factory.createResource("gate.creole.tokeniser.SimpleTokeniser");
		LanguageAnalyser gazetteer   = (LanguageAnalyser) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer"); 
		LanguageAnalyser senSpliter  = (LanguageAnalyser) Factory.createResource("gate.creole.splitter.SentenceSplitter"); 
		LanguageAnalyser posTagger   = (LanguageAnalyser) Factory.createResource("gate.creole.POSTagger"); 
		LanguageAnalyser neExtracter = (LanguageAnalyser) Factory.createResource("gate.creole.ANNIETransducer");
		pr_set.add(docReset);
		pr_set.add(tokenizer);
		pr_set.add(gazetteer);
		pr_set.add(senSpliter);
		pr_set.add(posTagger);
		pr_set.add(neExtracter);
		
		// execute GATE application
		gp.execute(pr_set, doc);
		gp.save("test.xml");
	}

}
