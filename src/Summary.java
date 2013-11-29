import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.sql.Date;
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
	public static void loadConfiguration() {
		String location = System.getProperty("user.dir");
		String configPath = location + "/inc/config.xml";
		System.out.println(configPath);
		Map<String, IConfigurationChangeListener> listeners = new HashMap<String, IConfigurationChangeListener>();
		listeners.put("server_list", new ServerConfigurationContainer());
		ConfigurationManager.configureAndWatch(listeners, configPath, 200L);
	}

	public static void main(String[] args) {
		// read and check if there is new email coming to inbox

		// parse email data and store into database

		/*
		 * information extraction and annotation using GATE then compute tf and
		 * store into database
		 */

		// cluster the incoming email to email group

		// compute tfidf for this incoming email

		/*
		 * summarize the email group 
		 * read each email sorted by date in the group
		 * apply inferece rules here in GATE
		 */
	}
}
