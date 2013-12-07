import gate.Corpus;
import gate.Document;
import gate.Factory;
import gate.FeatureMap;
import gate.Gate;
import gate.GateProcessor;
import gate.creole.annic.Term;
import gate.groovy.ScriptableController;
import gate.termraider.bank.HyponymyTermbank;
import gate.termraider.bank.TfIdfTermbank;
import gate.util.GateException;

import java.io.File;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.URL;
import java.sql.Date;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import com.google.code.jconfig.ConfigurationManager;
import com.google.code.jconfig.listener.IConfigurationChangeListener;

import config.ServerConfigurationContainer;
import config.ServerConfigurationPlugin;
import date.DateTime;
import db.*;

public class Summary {
	private static final String ip = "localhost";
	private static final String port = "3306";
	private static final String db = "nlp";
	private static final String username = "root";
	private static final String password = "";
	private static Transaction dbGate = new Transaction();

	public static void loadConfiguration() {
		String location = System.getProperty("user.dir");
		String configPath = location + "/inc/config.xml";
		System.out.println(configPath);
		Map<String, IConfigurationChangeListener> listeners = new HashMap<String, IConfigurationChangeListener>();
		listeners.put("server_list", new ServerConfigurationContainer());
		ConfigurationManager.configureAndWatch(listeners, configPath, 200L);
	}

	public static void main(String[] args) throws Exception {
		GateProcessor gp = new GateProcessor(true);

		// read and check if there is new email coming to inbox

		// parse email data and store into database

		/*
		 * information extraction and annotation using GATE then compute tf and
		 * store into database
		 */
		// fetch email data from database
		ArrayList<Document> docs = new ArrayList<Document>();
		if (dbGate.connect(ip, port, db, username, password)) {
			HashMap<String, String> params = new HashMap<String, String>();

			params.put("cols", "content, e_id, subject");
			params.put("cond", "e_id<2");
			ResultSet rs = dbGate.query("emails", params);

			while (rs.next()) {
				String docName = rs.getString("subject") + "_" + rs
						.getString("e_id");
				String content = rs.getString("subject") + "\n"+ rs
						.getString("content");
				Document doc = gp.createDocument(docName, content);
				docs.add(doc);
			}
		}

		// set control script for scriptable controller
		String contrlScript = gp.getCurrentPath() + "/resources/groovy/ControlScript.groovy";
		Corpus corpus       = gp.run(contrlScript, docs);

		// store tf data of each term into database
		HyponymyTermbank termBank = (HyponymyTermbank) corpus.getFeatures().get("hyponymyTermbank");
		Map<gate.termraider.util.Term, Integer> termFrequency = termBank.getTermFrequencies();
		for (gate.termraider.util.Term term : termFrequency.keySet()) {
			System.out.println(term.getTermString() + " = " + term.getType() + " => " + termFrequency.get(term));
			String[] data = { "1", term.getTermString(), term.getType(), String.valueOf(termFrequency.get(term))};
			dbGate.insert(data, "words");
		}
		
		
		// cluster the incoming email to email group

		// compute tfidf for this incoming email

		/*
		 * summarize the email group read each email sorted by date in the group
		 * apply inferece rules here in GATE
		 */

		// File pluginsDir = Gate.getPluginsHome();
		String plugins_home = "./plugins";
		File pluginsDir = new File(plugins_home);

		// Let’s load the Tools plugin
		File aPluginDir = new File(pluginsDir, "Groovy");

		// load the plugin.
		Gate.getCreoleRegister()
				.registerDirectories(aPluginDir.toURI().toURL());

	}
}
