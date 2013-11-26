import java.io.UnsupportedEncodingException;
import java.net.URL;
import java.sql.Date;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import Date.DateTime;

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
        
	public static void main(String[] args) throws ParseException, UnsupportedEncodingException {
		DateTime test = new DateTime();
		//String value = "Sunday, September 29, 2013 7:59:58 AM PDT";
		//String value = "Sunday, September 29, 2013 7:59 AM";
		//String value = "Tue, Jul 9, 2013 3:20 PM";
		//String value = "Tue, Jul 9, 2013 3:20 PM";
		String value = "Tue, Jul 9, 2013 3:20 PM";
		//String value = "Tue, Jul 9, 2013 3:20 PM";
		//test.dateTimeFormat(value);
		String newString = "";
		for (String s : value.split("")) {
			//System.out.println(c + " <> " + Character.getNumericValue(c));
			System.out.println(s);
			//value += s;
			//System.out.println(' ' + " <> " + Character.getNumericValue(' '));
			//if (c != ',' && Character.getNumericValue(c) == -1) newString += ' ';
			//else newString += c;
			//newString += Character.;
		}
		//newString = new String(value.getBytes("UTF-8"), "Windows-1252");
		//newString = new String(newString.getBytes("Windows-1252"), "Windows-1252");
		
		newString = value.replaceAll("([\\w]+),\\S([\\w]+)\\S([\\d]{1,2}),\\S([\\d]{1,4})\\S([\\d]{1,2}:[\\d]{1,2})\\S([aApP][mM])", "$1, $2 $3, $4 $5 $6");
		System.out.println(test.getDateInFormat(newString, "EEE, MMM dd, yyyy hh:mm aa"));
	}

}
