package gate;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;
import gate.creole.SerialAnalyserController;
import gate.persist.PersistenceException;

public class GateAnalyserController {
	private SerialAnalyserController controller;
	
	public GateAnalyserController() throws ResourceInstantiationException {
		controller = (SerialAnalyserController) Factory.createResource("gate.creole.SerialAnalyserController");
	}
	
	public void addProcessResource(ArrayList<LanguageAnalyser> pr_set) {
		for (LanguageAnalyser pr : pr_set) controller.add(pr);
	}
	
	public void execute(Document doc) throws ExecutionException, ResourceInstantiationException {
		Corpus corpus = Factory.newCorpus("corpus");
		corpus.add(doc);
		controller.setCorpus(corpus);
		controller.execute();
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
		controller = (SerialAnalyserController) gate.util.persistence.PersistenceManager.loadObjectFromFile(file);
	}
}
