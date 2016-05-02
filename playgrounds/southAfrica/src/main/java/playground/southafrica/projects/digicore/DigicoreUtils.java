package playground.southafrica.projects.digicore;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

import org.apache.log4j.Logger;
import org.matsim.core.utils.io.IOUtils;

import playground.southafrica.utilities.Header;

public class DigicoreUtils {
	final private static Logger LOG = Logger.getLogger(DigicoreUtils.class);

	/* Epoch offset: 1 Jan 1996 */
	private final static long OFFSET = 820447200000l;
	
	public static String getDateSince1996(Long l){
		Calendar c = getCalendarSince1996(l);
		String date = String.format("%d/%02d/%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH));
		String time = String.format("%02d:%02d:%02d.%03d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND), c.get(Calendar.MILLISECOND));
		String full = date + " " + time;
		return full;
	}
	
	public static Calendar getCalendarSince1996(Long l){
		Calendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		c.setTimeInMillis(l + OFFSET);
		return c;
	}
	
	public static String getTimeOfDayFromCalendar(Calendar cal){
		String time = String.format("%02d:%02d:%02d.%03d", 
				cal.get(Calendar.HOUR_OF_DAY),
				cal.get(Calendar.MINUTE),
				cal.get(Calendar.SECOND),
				cal.get(Calendar.MILLISECOND));
		return time;
	}
	
	public static void main(String[] args) throws IOException{
		Header.printHeader(DigicoreUtils.class.toString(), args);
		int option = Integer.parseInt(args[0]);
		
		switch (option) {
		case 1:
			/* Check twenty lines. */
			BufferedReader br = IOUtils.getBufferedReader("/home/jwjoubert/workspace/JoubertEtAl2014a-accelerometerProfiles-Manuscript/data/TwentyLines.csv");
			try{
				String line = null;
				while((line = br.readLine()) != null){
					String[] sa = line.split(",");
					long date1996 = Long.parseLong(sa[2]);
					LOG.info(DigicoreUtils.getDateSince1996(date1996));
				}
			} finally{
				br.close();
			}
			break;
		case 2:
			/* Test conversion back from normal time to milliseconds-since-1996. */
			LOG.info(" 1996/01/01 00:00:01 = " + getLongSince1996FromSeconds("1996/01/01 00:00:01"));
			LOG.info(" 1996/01/01 00:00:02 = " + getLongSince1996FromSeconds("1996/01/01 00:00:02"));
			LOG.info(" 1996/01/01 00:00:03 = " + getLongSince1996FromSeconds("1996/01/01 00:00:03"));
			LOG.info("        :");
			LOG.info(" 2014/08/01 07:00:00 = " + getLongSince1996FromSeconds("2014/08/01 07:00:00"));
		default:
			break;
		}
		Header.printFooter();
	}
	
	public static String getDateToSecondsSince1996(Long l){
		Calendar c = getCalendarSince1996(l);
		String date = String.format("%d/%02d/%02d", c.get(Calendar.YEAR), c.get(Calendar.MONTH)+1, c.get(Calendar.DAY_OF_MONTH));
		String time = String.format("%02d:%02d:%02d", c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), c.get(Calendar.SECOND));
		String full = date + " " + time;
		return full;
	}
	
	public static long getLongSince1996FromSeconds(String s){
		Long l = null;
		Calendar c = new GregorianCalendar(TimeZone.getTimeZone("GMT+2"), Locale.ENGLISH);
		String[] sa = s.split(" ");
		
		/* Date */
		String date = sa[0];
		String[] saDate = date.split("/");
		
		/* Time */
		String time = sa[1];
		String[] saTime = time.split(":");
		
		c.set(	Integer.parseInt(saDate[0]), 	// Year
				Integer.parseInt(saDate[1])-1, 	// Month
				Integer.parseInt(saDate[2]), 	// Day
				Integer.parseInt(saTime[0]), 	// Hour
				Integer.parseInt(saTime[1]), 	// Minute
				Integer.parseInt(saTime[2]));	// Second
		l = c.getTimeInMillis() - OFFSET;		
		
		/* The millisecond portion does not get set and depends on the local
		 * time instance when the method was called. I therefore round it to 
		 * the closes thousand. */
		String thousands = l.toString().substring(0, l.toString().length()-3) + "000";

		return Long.parseLong(thousands);
	}
	
	public static String getPrettyDateAndTime(GregorianCalendar cal){
		String result = String.format("%04d/%02d/%02d %02d:%02d:%02d.%03d",
				cal.get(Calendar.YEAR),
				cal.get(Calendar.MONTH)+1,
				cal.get(Calendar.DAY_OF_MONTH),
				cal.get(Calendar.HOUR_OF_DAY),
				cal.get(Calendar.MINUTE),
				cal.get(Calendar.SECOND),
				cal.get(Calendar.MILLISECOND));
		
		return result;
	}
	
	
	
}
