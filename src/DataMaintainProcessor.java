import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.LanguageAnalyser;
import gate.creole.ExecutionException;
import gate.creole.ResourceInstantiationException;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

public class DataMaintainProcessor extends IEProcessor {

	public DataMaintainProcessor(boolean view) {
		super.init(view);
	}
	
	protected ArrayList<LanguageAnalyser> setProcessingResources() throws ResourceInstantiationException, MalformedURLException {

		// create language analysers tokenizer
		LanguageAnalyser docReset    = (LanguageAnalyser) Factory.createResource("gate.creole.annotdelete.AnnotationDeletePR");
		LanguageAnalyser tfIdfCopier = (LanguageAnalyser) Factory.createResource("gate.termraider.apply.TermScoreCopier");

		// set signature of each PR for scriptable controller to refer
		docReset.setName("docReset");
		tfIdfCopier.setName("tfIdfCopier");

		// add all processing resources into pipline
		pr_set.add(docReset);
		pr_set.add(tfIdfCopier);
		return pr_set;
	}
	
	public Corpus run(String contrlScript, Corpus corpus) {
		// assign groovy script file to Scriptable Controller
		try {
			this.setControlScript(contrlScript);
			return this.execute(this.setProcessingResources(), corpus);
			
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
