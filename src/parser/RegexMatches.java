package parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class RegexMatches {
	
	// the second pattern for abnormal encoded pdf file
	private static final String EmailStart_1  = "(.+<[_A-Za-z0-9\\-\\+]+(\\.[_A-Za-z0-9\\-]+)*@[A-Za-z0-9\\-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})>)(\\w{3},\\S\\w{3}\\S\\d{1,2},\\S\\d{1,4}\\Sat\\S\\d{1,2}:\\d{1,2}\\S[aApP][mM])";
	private static final String Receivers_1   = "<?([\\w\\d\\-\\.\\_]+@[\\w\\d\\-]+(\\.[\\w\\d]+)*(\\.[\\w]{2,}))>?";
	private static final Pattern p_emailStart_1 = Pattern.compile(EmailStart_1);
    private static final Pattern p_receivers_1 = Pattern.compile(Receivers_1);
    
    // the second pattern for normal encoded pdf file
    private static final String EmailStart_2  = "(.+\\s<[_A-Za-z0-9\\-\\+]+(\\.[_A-Za-z0-9\\-]+)*@[A-Za-z0-9\\-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})>)\\s(\\w{3},\\s\\w{3}\\s\\d{1,2},\\s\\d{1,4}\\sat\\s\\d{1,2}:\\d{1,2}\\s[aApP][mM])";
  	private static final String Receivers_2   = "<?([\\w\\d\\-\\.\\_]+@[\\w\\d\\-]+(\\.[\\w\\d]+)*(\\.[\\w]{2,}))>?";
    private static final Pattern p_emailStart_2 = Pattern.compile(EmailStart_2);
    private static final Pattern p_receivers_2 = Pattern.compile(Receivers_2);
    
	public HashMap<String, String> isEmailStart(String input, int type) {
		HashMap<String, String> resultset = new HashMap<String, String>();
		Matcher m = null;
		if (type == 1) m = p_emailStart_1.matcher(input); // get a matcher object
		else if (type == 2) m = p_emailStart_2.matcher(input); // get a matcher object
		
		if (m.lookingAt()) {
			resultset.put("sender", m.group(1));
			resultset.put("sendingtime", m.group(5));
		}
		return resultset;
	}
	
	public ArrayList<String> getReceivers(String input, int type) {
		Matcher m = null;
		if (type == 1) m = p_receivers_1.matcher(input); // get a matcher object
		else if (type == 2) m = p_receivers_2.matcher(input); // get a matcher object
		ArrayList<String> receivers = new ArrayList<String>();
		
		while (m.find()) receivers.add(m.group(1));
		return receivers;
	}
}
