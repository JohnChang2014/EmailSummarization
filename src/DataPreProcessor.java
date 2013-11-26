import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

import Date.DateTime;
import db.Transaction;
import parser.RegexMatches;
import file.FReader;
import file.FWriter;

public class DataPreProcessor {
	private static final String ip = "localhost";
	private static final String port = "3306";
	private static final String db = "nlp";
	private static final String username = "root";
	private static final String password = "";

	private static final String storage_path = "./data/raw/";
	private static final String dataset_path = "/Volumes/Data/03-ShareData/Dropbox/00-Courses Data/Fall 2013/Natural Language Processing/Final Project/Dataset/Original Mails/";
	// dataset ignores Permathreads 2.mbox and permathreads.mbox temperarily
	// Apt for Video Shoot.pdf not working
	// works     => 1, 2, 3, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19
	// not works => 4(no need to remove whitespace)
	private static final String[] dataset = { 
		"33 5th Floor.pdf", "Apt for Video Shoot.pdf", "Con Ed.pdf", 
		"Counter Top.pdf", "ElevatorStairs Key.pdf", "Gigzolo rehearsal.pdf", 
		"Introduction.pdf", "Jam Session.pdf", "Kim Charts.pdf", 
		"Meet Wendy.pdf", "Office Setup.pdf", "Performance.pdf", 
		"Re- Design.pdf", "Recruiting Premeds.pdf", "Stroke Patient Savings.pdf", 
		"ThankYouForOrder.pdf", "wedding band.pdf", "Will be in around 12-30.pdf", 
		"Work Stations.pdf" };
	private static final DateTime dt = new DateTime();
	
	private String getSQLDateTimeformat(String value) throws ParseException {
		long ds = dt.getDateObjectFromString(value, "EEEE, MMMM dd, yyyy hh:mm a").getTime();
		java.sql.Date date = new java.sql.Date(ds);
		java.sql.Time time = new java.sql.Time(ds);
		
		//prestmt.setTime(index, date);
		System.out.println(date);
		System.out.println(time);
		String datetime = date.toString() + " " + time.toString();
		return datetime;
	}
	
	private String join(ArrayList<String> sets, String delimiter) {
		if (sets.size() == 0) return "";
		int len_delimiter = delimiter.length();
		String result = new String();
		for (String s : sets)
			result += s + delimiter;
		result = result.substring(0, result.length() - (len_delimiter + 1));
		return result;
	}

	// transformDataSet in charge of three major tasks
	// a) parse data from raw email thread in PDF file
	// b) write data from pdf into plain text file
	// c) write data from pdf into MySQL database
	public void transformDataSet() throws IOException, SQLException, ParseException {
		String filename = new String();
		FReader reader = new FReader();
		ArrayList<String> content = new ArrayList<String>();
		ArrayList<HashMap<String, String>> emailset = new ArrayList<HashMap<String, String>>();
		int n = 0;

		// read each PDF file one by one
		for (String file : dataset) {
			n++;
			if (n != 4) continue;

			// parse raw data from PDF dataset
			filename = dataset_path + file;
			if (n == 4 ) content = reader.readPDFFile(filename, false);
			else content = reader.readPDFFile(filename, true);
			emailset = parseDataFromPDF(content);
			writeDataIntoStorage(file, emailset);
			emailset.clear();
		}
	}

	// parseDataFromPDF takes responsibilities for separating contents from PDF
	// into couple email ones and store in the ArrayList for each
	private ArrayList<HashMap<String, String>> parseDataFromPDF(ArrayList<String> content) throws ParseException {
		RegexMatches regMatcher = new RegexMatches();

		// collectors of parts of emails
		String subject = new String();
		HashMap<String, String> header = new HashMap<String, String>();
		ArrayList<HashMap<String, String>> headerset = new ArrayList<HashMap<String, String>>();
		ArrayList<String> receivers = new ArrayList<String>();
		ArrayList<String> ccreceivers = new ArrayList<String>();
		ArrayList<HashMap<String, String>> emailset = new ArrayList<HashMap<String, String>>();

		boolean skip = false;
		boolean receivers_check = false;
		boolean ccreceivers_check = false;
		String email_body = "";
		int n_emails = 0;
		int n_line   = 0;
		content.add("<-end line->");
		for (String sen : content) {
			n_line++;
			if (n_line == 3) subject = sen;
			
			//if (emailset.size() == 5) break;
			// skip the footer line located between pages
			if (sen.indexOf("http") >= 0) {
				skip = true;
				continue;
			}
			System.out.println(skip + " ---> " + sen);
			// check if it is a start of email block
			header = regMatcher.isEmailStart(sen);
			if (header.size() > 0 || sen == "<-end line->") {
				System.out.println(email_body);

				System.out.println("======> email body start!!");
				System.out.println(header.get("sender"));
				System.out.println(header.get("sendingtime"));
				//System.out.println("----------------------");
				headerset.add(header);
				// store email content before resetting for next round
				//System.out.println(n_emails + " <----> " + (emailset.size() + 1));
				if (n_emails == (emailset.size() + 1)) {
					System.out.println("store -> " + n_emails + " email content!");
					HashMap<String, String> email = new HashMap<String, String>();
					// transform date format into mysql datetime format
					HashMap<String, String> tmp_header = headerset.get(n_emails-1);
					String datetime = getSQLDateTimeformat(tmp_header.get("sendingtime").replaceAll("(\\d{1,4}\\S)[aA][tT]\\S", "$1").replaceAll("([\\w]+),\\S([\\w]+)\\S([\\d]{1,2}),\\S([\\d]{1,4})\\S([\\d]{1,2}:[\\d]{1,2})\\S([aApP][mM])", "$1, $2 $3, $4 $5 $6"));
					
					if (n_emails == 2) subject = "Re: " + subject;
					email.put("sender", headerset.get(n_emails - 1).get("sender"));
					email.put("sendingtime", datetime);
					email.put("receivers", join(receivers, ", "));
					email.put("ccreceivers", join(ccreceivers, ", "));
					email.put("subject", subject);
					email.put("content", email_body);
					emailset.add(email);
				}

				// reset collector for starting next email scan
				receivers.clear();
				ccreceivers.clear();
				email_body = "";
				n_emails++;

				skip = false;
				receivers_check = true;
				continue;
			} else if (skip) {
				skip = false;
				continue;
			}
			
			if (receivers_check) {
				ArrayList<String> tmp_receivers = regMatcher.getReceivers(sen);
				// System.out.println("receivers: " + tmp_receivers.size() +
				// " -> " + sen.indexOf("Cc:"));
				if (tmp_receivers.size() > 0 && sen.indexOf("Cc:") == -1) {
					receivers.addAll(tmp_receivers);
					continue;
				} else {
					System.out.println(receivers);
					receivers_check = false;
					ccreceivers_check = true;
				}
			}
			
			if (ccreceivers_check) {
				ArrayList<String> tmp_receivers = regMatcher.getReceivers(sen);
				if (tmp_receivers.size() > 0) {
					ccreceivers.addAll(tmp_receivers);
					continue;
				} else {
					ccreceivers_check = false;
					System.out.println(ccreceivers);
				}
			}
			// if (email_body_check)
			email_body = email_body + sen + System.getProperty("line.separator");
		}
		System.out.println("< " + emailset.size() + " >");
		return emailset;
	}

	// writeDataIntoStorage in charge of storing data parsing from PDF files
	// into
	// plain text files and MySQL database at the same time
	private void writeDataIntoStorage(String prefix, ArrayList<HashMap<String, String>> emailset) throws IOException, SQLException, ParseException {
		int email_index = 0;
		for (HashMap<String, String> email : emailset) {
			FWriter fw = new FWriter(storage_path + prefix + "_" + email_index + ".txt");

			String sender = email.get("sender");
			String time = email.get("sendingtime");
			String receivers = email.get("receivers");
			String ccreceivers = email.get("ccreceivers");
			String subject = email.get("subject");
			String content = email.get("content");
			String raw_content = sender + "\n" + time + "\n" + receivers + "\n" + ccreceivers + "\n" + subject + "\n" + content;
			email.put("raw_content", raw_content);
			
			// write to plain text file
			fw.write("Sender: " + sender);
			fw.write("Time: " + time);
			fw.write("To: " + receivers);
			fw.write("Cc: " + ccreceivers);
			fw.write("Subject: " + subject);
			fw.write(email.get("content"));
			email_index++;
			fw.close();
			
			// write to MySQL database
			//insertData(email);
		}
	}

	private void insertData(HashMap<String, String> email) throws SQLException, ParseException {
		Transaction mydb = new Transaction();
		if (mydb.connect(ip, port, db, username, password)) {
			String[] data = { email.get("sender"), email.get("receivers"), email.get("ccreceivers"), email.get("subject"), email.get("content"), email.get("raw_content"), email.get("sendingtime"), null, null };
			mydb.insert(data, "email");
		}
		mydb.close();
	}

	public static void main(String[] args) throws IOException, SQLException, ParseException {
		DataPreProcessor dp = new DataPreProcessor();
		dp.transformDataSet();
	}

}
