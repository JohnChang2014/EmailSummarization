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

public class GateAnalyserController {
	private ScriptableController controller;
	private Corpus corpus;
	
	public GateAnalyserController() throws ResourceInstantiationException {
		controller = (ScriptableController) Factory.createResource("gate.groovy.ScriptableController");
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
	
	public void execute(Document doc) throws ExecutionException, ResourceInstantiationException {
		corpus.add(doc);
		this.execute(corpus);
	}
	
	public void execute(Corpus corpus) throws ExecutionException {
		controller.setCorpus(corpus);
		controller.execute();
	}
	
	public void saveGateApplication(String file_name) throws PersistenceException, IOException {
		File file = new File(file_name);
		gate.util.persistence.PersistenceManager.saveObjectToFile(controller, file); 
	}
	
	public void loadGateApplication(File file) throws PersistenceException, ResourceInstantiationException, IOException {
		controller = (ScriptableController) gate.util.persistence.PersistenceManager.loadObjectFromFile(file);
	}
}
