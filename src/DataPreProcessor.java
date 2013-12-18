import java.io.IOException;
import java.sql.SQLException;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.HashMap;

import date.DateTime;
import db.Transaction;
import parser.RegexMatches;
import file.Directory;
import file.FReader;
import file.FWriter;

public class DataPreProcessor {
	private String dataset_path;
	private String storage_path1 = Config.storage_path1;
	private String storage_path2 = Config.storage_path2;
	
	// for training set here ignores Permathreads 2.mbox and permathreads.mbox
	// since it is not in PDF format.
	private static final DateTime dt = new DateTime();
	private static Transaction mydb;
	private int mode;
	
	public DataPreProcessor(int mode) {
		this.mode = mode;
		this.mydb = new Transaction();
		String dbname = "";
		
		if (mode == 0) dbname = Config.db_development;
		else if (mode == 1) dbname = Config.db;
		
		if (!this.mydb.connect(Config.ip, Config.port, dbname, Config.username, Config.password)) {
			System.out.println("failed to connect database!");
			System.exit(0);
		}
	}
	
	private String getSQLDateTimeformat(String value) throws ParseException {
		long ds = dt.getDateObjectFromString(value, "EEEE, MMMM dd, yyyy hh:mm a").getTime();
		java.sql.Date date = new java.sql.Date(ds);
		java.sql.Time time = new java.sql.Time(ds);
		
		//prestmt.setTime(index, date);
		String datetime = date.toString() + " " + time.toString();
		return datetime;
	}
	
	private String join(ArrayList<String> sets, String delimiter) {
		if (sets.size() == 0) return "";
		int len_delimiter = delimiter.length();
		String result = new String();
		for (String s : sets)
			result += s + delimiter;
		result = result.substring(0, result.length() - len_delimiter).trim();
		return result;
	}

	// transformDataSet in charge of three major tasks
	// a) parse data from raw email thread in PDF file
	// b) write data from pdf into plain text file
	// c) write data from pdf into MySQL database
	public void transformDataSet() throws IOException, SQLException, ParseException {
		String filename = new String();
		FReader reader = new FReader();
		Directory dir = new Directory();
		ArrayList<String> content = new ArrayList<String>();
		ArrayList<HashMap<String, String>> emailset = new ArrayList<HashMap<String, String>>();
		int n = 0;
		
		if (mode == 0) dataset_path = Config.dataset_root_path;
		if (mode == 1) dataset_path = Config.testset_root_path;
		
		// read each PDF file one by one
		for (String file : dir.getDirList(dataset_path + Config.datafile_path)) {
			if (file.equals(".DS_Store")) continue;
			n++;
			//if (n < 3) continue;
			// parse raw data from PDF dataset
			filename = dataset_path + Config.datafile_path + file;
			System.out.println(filename);
			if ((mode == 0 && n == 4)) {
				content = reader.readPDFFile(filename, false);
				emailset = parseDataFromPDF(content, 2);
			} else {
				content = reader.readPDFFile(filename, true);
				emailset = parseDataFromPDF(content, 1);
			}
			
			writeDataIntoStorage(file, emailset);
			emailset.clear();
		}
		mydb.close();
	}

	// parseDataFromPDF takes responsibilities for separating contents from PDF
	// into couple email ones and store in the ArrayList for each
	private ArrayList<HashMap<String, String>> parseDataFromPDF(ArrayList<String> content, int type) throws ParseException {
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
		content.add("<-end line->");  // a line to tell program it reaches the end of file
		
		for (String sen : content) {
			n_line++;
			if (n_line == 3) subject = sen;
			
			// skip the footer line located between pages
			if (sen.indexOf("http") >= 0) {
				skip = true;
				continue;
			}
			// check if it is a start of email block
			header = regMatcher.isEmailStart(sen, type);
			if (header.size() > 0 || sen == "<-end line->") {
				System.out.println(email_body);

				System.out.println("======> email body start!!");
				System.out.println(header.get("sender"));
				System.out.println(header.get("sendingtime"));
				headerset.add(header);
				// store email content before resetting for next round
				if (n_emails == (emailset.size() + 1)) {
					System.out.println("store -> " + n_emails + " email content!");
					HashMap<String, String> email = new HashMap<String, String>();
					
					// transform date format into mysql datetime format
					HashMap<String, String> tmp_header = headerset.get(n_emails-1);
					String datetime = new String();
					if (type == 1) datetime = getSQLDateTimeformat(tmp_header.get("sendingtime").replaceAll("(\\d{1,4}\\S)[aA][tT]\\S", "$1").replaceAll("([\\w]+),\\S([\\w]+)\\S([\\d]{1,2}),\\S([\\d]{1,4})\\S([\\d]{1,2}:[\\d]{1,2})\\S([aApP][mM])", "$1, $2 $3, $4 $5 $6"));
					else if (type == 2) datetime = getSQLDateTimeformat(tmp_header.get("sendingtime").replaceAll("(\\d{1,4}\\s)[aA][tT]\\s", "$1").replaceAll("([\\w]+),\\S([\\w]+)\\s([\\d]{1,2}),\\s([\\d]{1,4})\\s([\\d]{1,2}:[\\d]{1,2})\\s([aApP][mM])", "$1, $2 $3, $4 $5 $6"));
					
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
				ArrayList<String> tmp_receivers = regMatcher.getReceivers(sen, type);
				if (tmp_receivers.size() > 0 && sen.indexOf("Cc:") == -1) {
					receivers.addAll(tmp_receivers);
					continue;
				} else {
					receivers_check = false;
					ccreceivers_check = true;
				}
			}
			
			if (ccreceivers_check) {
				ArrayList<String> tmp_receivers = regMatcher.getReceivers(sen, type);
				if (tmp_receivers.size() > 0) {
					ccreceivers.addAll(tmp_receivers);
					continue;
				} else {
					ccreceivers_check = false;
				}
			}
			email_body = email_body + sen + System.getProperty("line.separator");
		}
		System.out.println("< " + emailset.size() + " >");
		return emailset;
	}

	// writeDataIntoStorage in charge of storing data parsing from PDF files
	// into plain text files and MySQL database at the same time
	private void writeDataIntoStorage(String prefix, ArrayList<HashMap<String, String>> emailset) throws IOException, SQLException, ParseException {
		int email_index = 0;
		for (HashMap<String, String> email : emailset) {
			FWriter fw1 = new FWriter(dataset_path + storage_path1 + prefix + "_" + email_index + ".txt");
			FWriter fw2 = new FWriter(dataset_path + storage_path2 + prefix + "_" + email_index + ".txt");

			String sender = email.get("sender");
			String time = email.get("sendingtime");
			String receivers = email.get("receivers");
			String ccreceivers = email.get("ccreceivers");
			String raw_subject = email.get("subject");
			String content = email.get("content");
			String subject_prefix = new String();
			
			// add prefix string to each data field except content field
			sender         = "From: " + sender;
			time           = "Date: " + time;
			receivers      = "To: " + receivers;
			ccreceivers    = "Cc: " + ccreceivers;
			subject_prefix = "Subject: " + raw_subject;
			
			String raw_content = sender + "\n" + time + "\n" + receivers + "\n" + ccreceivers + "\n" + subject_prefix + "\n" + content;
			System.out.println("\n\n\n");
			System.out.println(raw_content);
			email.put("raw_content", raw_content);
			
			// write to plain text file
			fw1.write(sender);
			fw1.write(time);
			fw1.write(receivers);
			fw1.write(ccreceivers);
			fw1.write(subject_prefix);
			fw1.write(content);
			fw1.close();
			
			fw2.write(raw_subject + ".");
			fw2.write(content);
			fw2.close();
		
			email_index++;
			// write to MySQL database
			insertData(email);
		}
	}

	private void insertData(HashMap<String, String> email) throws SQLException, ParseException {
		String[] data = { email.get("sender"), email.get("receivers"), email
				.get("ccreceivers"), email.get("subject"), email.get("content"), email
				.get("raw_content"), email.get("sendingtime") };
		mydb.insert(data, "emails");
	}
	
	public void run() {
		try {
			mydb.emptyData("emails");
			transformDataSet();
		} catch (IOException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		}
	}
	
	public static void main(String[] args) throws IOException, SQLException, ParseException {
		int mode = 1;
		DataPreProcessor dp = new DataPreProcessor(mode);
		dp.run();
	}

}
