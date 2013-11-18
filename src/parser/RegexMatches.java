package parser;

import java.util.HashMap;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class RegexMatches {
	private static final String EmailStart  = "(([\\w]+[\\S\\s])+<.+>)(.+[\\S\\s][aAmM])";
	private static final String Receivers   = "To: (.+)";
    
	public HashMap<String, String> isEmailStart(String input) {
		HashMap<String, String> resultset = new HashMap<String, String>();
		Pattern p = Pattern.compile(EmailStart);
		Matcher m = p.matcher(input); // get a matcher object
		if (m.lookingAt()) {
			resultset.put("sender", m.group(1));
			resultset.put("sendingtime", m.group(3));
		}
		return resultset;
	}
	public String getReceivers(String input) {
		Pattern p = Pattern.compile(Receivers);
		Matcher m = p.matcher(input); // get a matcher object
		if (m.lookingAt()) return m.group(1);
		return null;
	}
	
	/*
	public ArrayList<String> getEmailContent(String input) {
		Pattern p = Pattern.compile(Email);
		Matcher m = p.matcher(input); // get a matcher object
		int count = 0;
		ArrayList<String> emails = new ArrayList<String>(); 
		
		
		while (m.find()) {
			count++;
			System.out.println("Match number " + count);
			System.out.println("start(): " + m.start());
			System.out.println("end(): " + m.end());
			//String text = input.substring(m.start(), m.end());
			String text = m.group(0);
			System.out.println(">>>" + text);
			emails.add(text);
		}
		return emails;
	}*/
}
