package parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class RegexMatches {
	//private static final String EmailStart  = "(([\\w]+[\\S\\s])+<.+@.+>)(.+[\\S\\s][aAmM])";
	private static final String EmailStart  = "(.+<[_A-Za-z0-9\\-\\+]+(\\.[_A-Za-z0-9\\-]+)*@[A-Za-z0-9\\-]+(\\.[A-Za-z0-9]+)*(\\.[A-Za-z]{2,})>)(\\w{3},\\S\\w{3}\\S\\d{1,2},\\S\\d{1,4}\\Sat\\S\\d{1,2}:\\d{1,2}\\S[aApP][mM])";
	//private static final String Receivers   = "<([\\w\\d\\.]+@[\\w]+\\.[\\w]+)>";
	private static final String Receivers   = "<([\\w\\d\\-\\.\\_]+@[\\w\\d\\-]+(\\.[\\w\\d]+)*(\\.[\\w]{2,}))>";
    private static final Pattern p_emailStart = Pattern.compile(EmailStart);
    private static final Pattern p_receivers = Pattern.compile(Receivers);
    
	public HashMap<String, String> isEmailStart(String input) {
		HashMap<String, String> resultset = new HashMap<String, String>();
		//Pattern p = Pattern.compile(EmailStart);
		Matcher m = p_emailStart.matcher(input); // get a matcher object
		if (m.lookingAt()) {
			resultset.put("sender", m.group(1));
			resultset.put("sendingtime", m.group(5));
		}
		return resultset;
	}
	public ArrayList<String> getReceivers(String input) {
		//System.out.println(input);
		Matcher m = p_receivers.matcher(input); // get a matcher object
		ArrayList<String> receivers = new ArrayList<String>();
		
		while (m.find()) receivers.add(m.group(1));
		return receivers;
	}
}
