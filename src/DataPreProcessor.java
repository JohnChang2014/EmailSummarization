import java.util.ArrayList;
import java.util.HashMap;

import parser.RegexMatches;
import file.FileReader;

public class DataPreProcessor {
	private static final String dataset_path = "/Volumes/Data/03-ShareData/Dropbox/00-Courses Data/Fall 2013/Natural Language Processing/Final Project/Dataset/Original Mails/";
	// dataset ignores Permathreads 2.mbox and permathreads.mbox temperarily
	private static final String[] dataset = { "33 5th Floor.pdf", "Apt for Video Shoot.pdf", "Con Ed.pdf", "Counter Top.pdf", "Elevator/Stairs Key.pdf", "Gigzolo rehearsal.pdf", "Introduction.pdf", "Jam Session.pdf", "Kim Charts.pdf", "Meet Wendy.pdf", "Office Setup.pdf", "Performance.pdf", "Re- Design.pdf", "Recruiting Premeds.pdf", "Stroke Patient Savings.pdf", "ThankYouForOrder.pdf", "wedding band.pdf", "Will be in around 12-30.pdf", "Work Stations.pdf" };

	public static void main(String[] args) {
		RegexMatches regMatcher = new RegexMatches();
		FileReader reader       = new FileReader();
		HashMap<String, String> result = new HashMap<String, String>();
		String filename = "";
		ArrayList<String> content = new ArrayList<String>();
		String email_body = "";
		int j = 0;
		
		// read raw data from dataset and store data into database
		for (String file : dataset) {
			j++;
			if (j != 2) continue;
			//
			filename = dataset_path + file;
			content  = reader.readPDFFile(filename);

			for (String sen : content) {
				result = regMatcher.isEmailStart(sen);
				if (result.size() > 0) {
					System.out.println(email_body);
					email_body = "";
					System.out.println("======> email body start!!");
					System.out.println(result.get("sender"));
					System.out.println(result.get("sendingtime"));
					System.out.println("----------------------");
					
				} else if (sen.indexOf("To:") > -1) {

				} else if (sen.indexOf("Cc:") > -1) {

				} else {
					email_body = email_body + sen + System
							.getProperty("line.separator");
				}

			}
			System.out.println(email_body);
		}
	}

}
