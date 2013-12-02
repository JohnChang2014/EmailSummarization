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
import java.util.HashMap;
import java.sql.ResultSet;

import javax.swing.SwingUtilities;

import db.Transaction;

public class GateProcessor {
	private static final String ip = "localhost";
	private static final String port = "3306";
	private static final String db = "nlp";
	private static final String username = "root";
	private static final String password = "";
	private static final String plugins_home = "./plugins";
	private static final String gapp_folder = "./gapp";
	private static Transaction dbGate = new Transaction();
	
	private GateAnalyserController controller;
	private GateDocHandler docHandler;
	
	
    
	public GateProcessor(boolean view) throws InterruptedException, InvocationTargetException, GateException {
    	init(view);
    }
	
	public GateProcessor() throws GateException, InterruptedException, InvocationTargetException {
		init(false);
	}

	private void init(final boolean view) throws InterruptedException, InvocationTargetException, GateException {
		Gate.init();
		
		// show the main window
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
				MainFrame.getInstance().setVisible(view);
			}
		});
		
		controller = new GateAnalyserController();
		docHandler = new GateDocHandler();
	}
	
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

	public void loadGatePlugins(String plugin_name) throws MalformedURLException, GateException {
		// get the root plugins dir
		// File pluginsDir = Gate.getPluginsHome();
		String plugins_folder = plugins_home;
		File pluginsDir = new File(plugins_folder);

		// Let’s load the Tools plugin
		File aPluginDir = new File(pluginsDir, plugin_name);

		// load the plugin.
		Gate.getCreoleRegister()
				.registerDirectories(aPluginDir.toURI().toURL());
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

	public Document createDocument(String docName, String content) throws Exception {
		return docHandler.createDoc(docName, content);
	}

	public Document createDocument(String docName, String content, FeatureMap feats) throws Exception {
		return docHandler.createDoc(docName, content, feats);
	}
	
	public static void main(String[] args) throws Exception {

		GateProcessor gp = new GateProcessor(true);

		// create GATE document
		String docName = "This is home";
		//String content = "http://money.cnn.com/2013/11/27/pf/black-friday-tricks/index.html?iid=lead2";
		//String content = "http://money.cnn.com/2013/11/27/pf/black-friday-tricks/index.html?iid=lead2\n fadfadfadfadfadfafdsfafafdsf";
		//FeatureMap feats = Factory.newFeatureMap();
		//feats.put("author", "John");
		// fetch email data from database
		String content = "";
		if (dbGate.connect(ip, port, db, username, password)) {
		HashMap<String, String> params = new HashMap<String, String>();
		params.put("cols", "raw_content");
		params.put("cond", "e_id=1");
		ResultSet rs = dbGate.query("emails", params);
		
		if (rs.next()) content = rs.getString("raw_content");
		} 
		Document doc = gp.createDocument(docName, content);
		
		// load plugins: ANNIE, BWPGazetteer, KeyphraseAnalyser and TermRaider
		gp.loadGatePlugins("ANNIE");
		gp.loadGatePlugins("BWPGazetteer");
		gp.loadGatePlugins("KeyphraseAnalyser");
		//gp.loadGatePlugins("TermRaider");
		
		// create language analysers tokenizer
		ArrayList<LanguageAnalyser> pr_set = new ArrayList<LanguageAnalyser>();
		LanguageAnalyser docReset = (LanguageAnalyser) Factory
				.createResource("gate.creole.annotdelete.AnnotationDeletePR");
		LanguageAnalyser languageIdentifier = (LanguageAnalyser) Factory
				.createResource("ie.deri.sw.smile.nlp.gate.language.LanguageIdentifier");
		//LanguageAnalyser tokenizer = (LanguageAnalyser) Factory.createResource("gate.creole.tokeniser.SimpleTokeniser");
		LanguageAnalyser tokenizer = (LanguageAnalyser) Factory
				.createResource("gate.creole.tokeniser.DefaultTokeniser");
		LanguageAnalyser gazetteer = (LanguageAnalyser) Factory
				.createResource("gate.creole.gazetteer.DefaultGazetteer");
		LanguageAnalyser bwpGazetteer = (LanguageAnalyser) Factory
				.createResource("bwp.gate.gazetteer.BWPGazetteer");
		LanguageAnalyser senSpliter = (LanguageAnalyser) Factory
				.createResource("gate.creole.splitter.SentenceSplitter");
		LanguageAnalyser posTagger = (LanguageAnalyser) Factory
				.createResource("gate.creole.POSTagger");
		LanguageAnalyser morphAnalyzer = (LanguageAnalyser) Factory
				.createResource("gate.creole.morph.Morph");
		LanguageAnalyser stopWordMarker = (LanguageAnalyser) Factory
				.createResource("ie.deri.sw.smile.nlp.gate.stopword.StopwordMarker");
		LanguageAnalyser simpleNounChunker = (LanguageAnalyser) Factory
				.createResource("ie.deri.sw.smile.nlp.gate.chunking.SimpleNounChunker");
		LanguageAnalyser neExtracter = (LanguageAnalyser) Factory
				.createResource("gate.creole.ANNIETransducer");
		LanguageAnalyser vpChunker = (LanguageAnalyser) Factory
				.createResource("gate.creole.VPChunker");
		//LanguageAnalyser tfidfCopier = (LanguageAnalyser) Factory.createResource("gate.creole.VPChunker");
		
		pr_set.add(docReset);
		pr_set.add(languageIdentifier);
		pr_set.add(tokenizer);
		pr_set.add(gazetteer);
		pr_set.add(bwpGazetteer);
		pr_set.add(senSpliter);
		pr_set.add(posTagger);
		pr_set.add(morphAnalyzer);
		pr_set.add(stopWordMarker);
		pr_set.add(simpleNounChunker);
		pr_set.add(neExtracter);
		pr_set.add(vpChunker);
		//pr_set.add(tsc);
		
		// execute GATE application
		gp.execute(pr_set, doc);
		gp.save("test.xml");
		dbGate.close();
	}

}
