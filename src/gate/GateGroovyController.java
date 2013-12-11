package gate;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;

import file.FReader;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.groovy.ScriptableController;
import gate.persist.PersistenceException;
import gate.termraider.apply.TermScoreCopier;
import gate.termraider.bank.HyponymyTermbank;
import gate.termraider.bank.TfIdfTermbank;

public class GateGroovyController {
	private ScriptableController controller;
	private Corpus corpus;
	
	public GateGroovyController() throws ResourceInstantiationException {
		controller = (ScriptableController) Factory.createResource("gate.groovy.ScriptableController");
		
	}
	
	public void setCorpus(ArrayList<Document> docs) throws ResourceInstantiationException {
		corpus = Factory.newCorpus("tmpCorpus");
		corpus.addAll(docs);
	}
	
	public void setCorpus(Corpus corpus) {
		this.corpus = corpus;
	}
	
	public void setCtrlScript(String script) throws IOException {
		FReader fr            = new FReader(script);
		String script_content = new String();
		String line           = new String();
		do {
			line            = fr.readLine();
			script_content += line + "\n";
		} while(line != null );
		
		controller.setControlScript(script_content);
	}
	
	public void addProcessResource(ArrayList<LanguageAnalyser> pr_set) {
		for (LanguageAnalyser pr : pr_set) controller.add(pr);
	}
	
	public void execute() throws ExecutionException {
		controller.execute();
	}
	
	public void execute(ArrayList<Document> docs) throws ExecutionException, ResourceInstantiationException {
		this.setCorpus(docs);
		this.execute(corpus);
	}
	
	public Corpus execute(Corpus corpus) throws ExecutionException, ResourceInstantiationException {
		controller.setCorpus(corpus);
		controller.execute();
		return controller.getCorpus();
	}
	
	public Corpus getCorpus() {
		return controller.getCorpus();
	}
	
	public void cleanup() {
		//controller.cleanup();
		//controller.flushBeanInfoCache();
	}
	
	/*
	public void saveGateApplication(String file_name) throws PersistenceException, IOException {
		File file = new File(file_name);
		gate.util.persistence.PersistenceManager.saveObjectToFile(controller, file); 
	}
	
	public void saveGateCorpus(String file_name) throws PersistenceException, IOException {
		File file = new File(file_name);
		gate.util.persistence.PersistenceManager.saveObjectToFile(controller.getCorpus(), file); 
	}
	
	public void loadGateApplication(File file) throws PersistenceException, ResourceInstantiationException, IOException {
		controller = (ScriptableController) gate.util.persistence.PersistenceManager.loadObjectFromFile(file);
	}
	*/
}
