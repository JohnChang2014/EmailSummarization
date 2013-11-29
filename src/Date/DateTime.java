package date;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DateTime {
	
	// 
	// reference: http://docs.oracle.com/javase/7/docs/api/java/text/SimpleDateFormat.html
	public String getDateInFormat(String value, String format) throws ParseException {
		//if (format == "default") format = "yyyy-MM-dd k:mm:ss";
		if (format == "default") format = "EEEE, MMMM dd, yyyy h:mm:ss a zzz";
		
		//value = "Sunday, September 29, 2013 7:59:58 AM PDT";
		// String format = "EEEE, MMMM dd, yyyy h:mm:ss a zzz";
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		System.out.println(value + " <> " + format);
		Date date = sdf.parse(value);
		System.out.println(date);
		System.out.println(sdf.format(date));
		return sdf.format(date);
	}
	
	public String dateTimeFormat(String value) throws ParseException {
		return this.getDateInFormat(value, "default");
	}
	
	public Date getDateObjectFromString(String value, String format) throws ParseException {
		SimpleDateFormat sdf = new SimpleDateFormat(format);
		return sdf.parse(value);
	}
}
