package gate;

import file.FReader;
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
	private static GatePluginManager gplugin = new GatePluginManager(plugins_home);
	
	private GateAnalyserController controller;
	private GateDocHandler docHandler;
	private String current_path;
    
	public GateProcessor(boolean view) throws InterruptedException, InvocationTargetException, GateException, MalformedURLException {
    	init(view);
    }
	
	public GateProcessor() throws GateException, InterruptedException, InvocationTargetException, MalformedURLException {
		init(false);
	}

	private void init(final boolean view) throws InterruptedException, InvocationTargetException, GateException, MalformedURLException {
		this.current_path = System.getProperty("user.dir");
		
		Gate.init();
		
		// load plugins: ANNIE, BWPGazetteer, KeyphraseAnalyser and TermRaider
		loadAllPlugins();
		
		// show the main window
		SwingUtilities.invokeAndWait(new Runnable() {
			public void run() {
				MainFrame.getInstance().setVisible(view);
			}
		});
		
		controller = new GateAnalyserController();
		docHandler = new GateDocHandler();
	}
	
	private void loadAllPlugins() throws MalformedURLException, GateException {
		gplugin.loadGatePlugins("ANNIE");
		gplugin.loadGatePlugins("BWPGazetteer");
		gplugin.loadGatePlugins("KeyphraseAnalyser");
		gplugin.loadGatePlugins("Groovy");
		gplugin.loadGatePlugins("TermRaider");
	}
	
	public String getCurrentPath() { return this.current_path; }
	
	public void setControlScript(String script) throws ResourceInstantiationException, IOException {
		controller.setCtrlScript(script);
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

	public void setCorpus(Corpus corpus) {
		controller.setCorpus(corpus);
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
		String current_path = gp.getCurrentPath();
		
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
		
		Corpus corpus = Factory.newCorpus("corpus");
		gp.setCorpus(corpus);
		
		// set control script for scriptable controller
		String contrlScript = current_path + "/resources/groovy/ControlScript.groovy"; 
		gp.setControlScript(contrlScript);
		
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
		LanguageAnalyser orthoMatcher = (LanguageAnalyser) Factory
				.createResource("gate.creole.orthomatcher.OrthoMatcher");
		ArrayList<String> anns = new ArrayList<String>();
		anns.add("Organization");
		anns.add("Person");
		anns.add("Location");
		anns.add("Date");
		orthoMatcher.setParameterValue("annotationTypes", anns);
		
		LanguageAnalyser selectTokens = (LanguageAnalyser) Factory.createResource("gate.creole.ANNIETransducer");
		URL transduceGrammarURL = new URL("file://" + current_path + "/resources/jape/select-tokens-en.jape");
		selectTokens.setParameterValue("grammarURL", transduceGrammarURL);
		
		LanguageAnalyser multiwordJape = (LanguageAnalyser) Factory.createResource("gate.creole.ANNIETransducer");
		transduceGrammarURL = new URL("file://" + current_path + "/resources/jape/multiword-main-en.jape");
		multiwordJape.setParameterValue("grammarURL", transduceGrammarURL);
		
		FeatureMap feats = Factory.newFeatureMap();
		URL transduceScriptURL = new URL("file://" + current_path + "/resources/groovy/DeduplicateMultiWord.groovy");
		feats.put("scriptURL", transduceScriptURL);
		LanguageAnalyser deduplicateMW = (LanguageAnalyser) Factory.createResource("gate.groovy.ScriptPR", feats);
		
		FeatureMap feats1 = Factory.newFeatureMap();
		transduceGrammarURL = new URL("file://" + current_path + "/resources/jape/augmentation.jape");
		feats1.put("grammarURL", transduceGrammarURL);
		LanguageAnalyser augmentation = (LanguageAnalyser) Factory.createResource("gate.creole.Transducer", feats1);
		
		LanguageAnalyser tfidfCopier = (LanguageAnalyser) Factory.createResource("gate.termraider.apply.TermScoreCopier");
		
		// set signature of each PR for scriptable controller to refer 
		docReset.setName("docReset");
		languageIdentifier.setName("languageIdentifier");
		tokenizer.setName("tokenizer");
		gazetteer.setName("gazetteer");
		bwpGazetteer.setName("bwpGazetteer");
		senSpliter.setName("senSpliter");
		posTagger.setName("posTagger");
		morphAnalyzer.setName("morphAnalyzer");
		stopWordMarker.setName("stopWordMarker");
		simpleNounChunker.setName("simpleNounChunker");
		neExtracter.setName("neExtracter");
		vpChunker.setName("vpChunker");
		orthoMatcher.setName("orthoMatcher");
		selectTokens.setName("selectTokens");
		multiwordJape.setName("multiwordJape");
		deduplicateMW.setName("deduplicateMW");
		augmentation.setName("augmentation");
		tfidfCopier.setName("tfidfCopier");
		
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
		pr_set.add(orthoMatcher);
		pr_set.add(selectTokens);
		pr_set.add(multiwordJape);
		pr_set.add(deduplicateMW);
		pr_set.add(augmentation);
		pr_set.add(tfidfCopier);
		
		// execute GATE application
		gp.execute(pr_set, doc);
		gp.save("test.xml");
		dbGate.close();
	}

}
