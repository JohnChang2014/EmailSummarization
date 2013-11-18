import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import com.google.code.jconfig.ConfigurationManager;
import com.google.code.jconfig.listener.IConfigurationChangeListener;

import config.ServerConfigurationContainer;
import config.ServerConfigurationPlugin;
import db.*;

public class Summary {
    public static void loadConfiguration() {
    	String location                                     = System.getProperty("user.dir");
		String configPath                                   = location + "/inc/config.xml";
		System.out.println(configPath);
		Map<String, IConfigurationChangeListener> listeners = new HashMap<String, IConfigurationChangeListener>();
		listeners.put("server_list", new ServerConfigurationContainer());
		ConfigurationManager.configureAndWatch(listeners, configPath, 200L);
    }
    
	public static void main(String[] args) {
		String dbURL    = "jdbc:mysql://localhost:3306/nlp";
		String username = "root";
		String password = "";
		MySQL db = new MySQL();
		boolean test = db.connect(dbURL, username, password);
		System.out.println(test);
		db.close();
	}

}
