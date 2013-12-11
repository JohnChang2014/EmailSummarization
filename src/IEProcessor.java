

import file.FReader;
import gate.Annotation;
import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.GateGroovyController;
import gate.GateDocsHandler;
import gate.GatePluginManager;
import gate.LanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.gui.MainFrame;
import gate.persist.PersistenceException;
import gate.termraider.bank.HyponymyTermbank;
import gate.termraider.bank.TfIdfTermbank;
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

public class IEProcessor {
	protected static GatePluginManager gplugin = new GatePluginManager(Config.plugins_home);

	protected GateGroovyController controller;
	protected GateDocsHandler docHandler;
	protected String current_path;
	protected ArrayList<LanguageAnalyser> pr_set = new ArrayList<LanguageAnalyser>();
	
	public IEProcessor(boolean view) throws InterruptedException, InvocationTargetException, GateException, MalformedURLException {
		init(view);
	}

	public IEProcessor() {
		init(false);
	}

	protected void init(final boolean view) {
		this.current_path = System.getProperty("user.dir");

		try {
			Gate.init();
			
			// load plugins: ANNIE, BWPGazetteer, KeyphraseAnalyser and TermRaider
			loadAllPlugins();
		
			// show the main window
			SwingUtilities.invokeAndWait(new Runnable() {
				public void run() {
					MainFrame.getInstance().setVisible(view);
				}
			});

			controller = new GateGroovyController();
			docHandler = new GateDocsHandler();
			
		} catch (GateException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (InvocationTargetException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	protected void loadAllPlugins() throws MalformedURLException, GateException {
		gplugin.loadGatePlugins("ANNIE");
		gplugin.loadGatePlugins("BWPGazetteer");
		gplugin.loadGatePlugins("KeyphraseAnalyser");
		gplugin.loadGatePlugins("Groovy");
		gplugin.loadGatePlugins("TermRaider");
	}

	public String getCurrentPath() {
		return this.current_path;
	}

	public void setControlScript(String script) throws ResourceInstantiationException, IOException {
		script = this.current_path + script;
		controller.setCtrlScript(script);
	}

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
	public void setCorpus(Corpus corpus) {
		controller.setCorpus(corpus);
	}
	
	public void setCorpus(ArrayList<Document> docs) {
		controller.setCorpus(docs);
	}
	
	public Corpus getCorpus() {
		return controller.getCorpus();
	}
		
	public Corpus execute(ArrayList<LanguageAnalyser> pr_set, ArrayList<Document> docs) throws ExecutionException, ResourceInstantiationException {
		controller.addProcessResource(pr_set);
		controller.execute(docs);
		return controller.getCorpus();
	}

	public Corpus execute(ArrayList<LanguageAnalyser> pr_set, Corpus corpus) throws ExecutionException, ResourceInstantiationException {
		controller.addProcessResource(pr_set);
		controller.execute(corpus);
		return controller.getCorpus();
	}

/*
	public void save(String file_name) throws PersistenceException, IOException {
		String file_path = gapp_folder + "/" + file_name;
		controller.saveGateApplication(file_path);
	}
	
	public void saveCorpus(String file_name) throws PersistenceException, IOException {
		String file_path = gapp_folder + "/" + file_name;
		controller.saveGateCorpus(file_path);
	}
*/
	public Document createDocument(String docName, String content) throws Exception {
		return docHandler.createDoc(docName, content);
	}

	public Document createDocument(String docName, String content, FeatureMap feats) throws Exception {
		return docHandler.createDoc(docName, content, feats);
	}

	protected ArrayList<LanguageAnalyser> setProcessingResources() throws ResourceInstantiationException, MalformedURLException {
		
		// create language analysers tokenizer
		LanguageAnalyser docReset = (LanguageAnalyser) Factory
				.createResource("gate.creole.annotdelete.AnnotationDeletePR");
		LanguageAnalyser languageIdentifier = (LanguageAnalyser) Factory
				.createResource("ie.deri.sw.smile.nlp.gate.language.LanguageIdentifier");
		LanguageAnalyser tokenizer = (LanguageAnalyser) Factory
				.createResource("gate.creole.tokeniser.DefaultTokeniser");
		//LanguageAnalyser gazetteer = (LanguageAnalyser) Factory.createResource("gate.creole.gazetteer.DefaultGazetteer");
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

		LanguageAnalyser selectTokens = (LanguageAnalyser) Factory
				.createResource("gate.creole.ANNIETransducer");
		URL transduceGrammarURL = new URL("file://" + current_path + "/resources/jape/select-tokens-en.jape");
		selectTokens.setParameterValue("grammarURL", transduceGrammarURL);

		LanguageAnalyser multiwordJape = (LanguageAnalyser) Factory
				.createResource("gate.creole.ANNIETransducer");
		transduceGrammarURL = new URL("file://" + current_path + "/resources/jape/multiword-main-en.jape");
		multiwordJape.setParameterValue("grammarURL", transduceGrammarURL);

		FeatureMap feats = Factory.newFeatureMap();
		URL transduceScriptURL = new URL("file://" + current_path + "/resources/groovy/DeduplicateMultiWord.groovy");
		feats.put("scriptURL", transduceScriptURL);
		LanguageAnalyser deduplicateMW = (LanguageAnalyser) Factory
				.createResource("gate.groovy.ScriptPR", feats);
		/*
		FeatureMap feats1 = Factory.newFeatureMap();
		transduceGrammarURL = new URL("file://" + current_path + "/resources/jape/augmentation.jape");
		feats1.put("grammarURL", transduceGrammarURL);
		LanguageAnalyser augmentation = (LanguageAnalyser) Factory
				.createResource("gate.creole.Transducer", feats1);
*/
		LanguageAnalyser tfIdfCopier = (LanguageAnalyser) Factory
				.createResource("gate.termraider.apply.TermScoreCopier");
		/*
		LanguageAnalyser augTfIdfCopier = (LanguageAnalyser) Factory
				.createResource("gate.termraider.apply.TermScoreCopier");

		LanguageAnalyser kyotoCopier = (LanguageAnalyser) Factory
				.createResource("gate.termraider.apply.TermScoreCopier");
		*/
		// set signature of each PR for scriptable controller to refer
		docReset.setName("docReset");
		languageIdentifier.setName("languageIdentifier");
		tokenizer.setName("tokenizer");
		//gazetteer.setName("gazetteer");
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
		//augmentation.setName("augmentation");
		tfIdfCopier.setName("tfIdfCopier");
		//augTfIdfCopier.setName("augTfIdfCopier");
		//kyotoCopier.setName("kyotoCopier");
		
		// add all processing resources into pipline
		pr_set.add(docReset);
		pr_set.add(languageIdentifier);
		pr_set.add(tokenizer);
		//pr_set.add(gazetteer);
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
		//pr_set.add(augmentation);
		pr_set.add(tfIdfCopier);
		//pr_set.add(augTfIdfCopier);
		//pr_set.add(kyotoCopier);
		return pr_set;
	}

	public void cleanup() {
		controller.cleanup();
	}
	
	public Corpus run(String contrlScript, ArrayList<Document> docs) {
		
		// assign groovy script file to Scriptable Controller
		try {
			this.setControlScript(contrlScript);
			return this.execute(this.setProcessingResources(), docs);
			
		} catch (ResourceInstantiationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (ExecutionException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		return null;
	}
}
