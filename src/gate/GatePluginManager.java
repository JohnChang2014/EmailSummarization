package gate;

import gate.util.GateException;

import java.io.File;
import java.net.MalformedURLException;

public class GatePluginManager {
	private String plugins_folder;
	
	public GatePluginManager(String plugins_home) {
		this.plugins_folder = plugins_home;
	}
	
	public void loadGatePlugins(String plugin_name) throws MalformedURLException, GateException {
		// get the root plugins dir
		// File pluginsDir = Gate.getPluginsHome();
		File pluginsDir = new File(plugins_folder);
		// Let’s load the Tools plugin
		File aPluginDir = new File(pluginsDir, plugin_name);
		// load the plugin.
		Gate.getCreoleRegister().registerDirectories(aPluginDir.toURI().toURL());
	}
}
