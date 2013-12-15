import java.util.ArrayList;

import gate.Annotation;
import gate.Corpus;
import gate.Document;
import static gate.Utils.*;

public class IFProcessor {
	
	
	public void run(Corpus corpus) {
		ArrayList<String> sentences = new ArrayList<String>();
		
		for (Document doc : corpus) {
			for (Annotation sen : doc.getAnnotations().get("Sentence") ) {
				long s_pos = start(sen);
				long e_pos = end(sen);
			}
		}
	}
}
