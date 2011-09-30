package playground.tnicolai.matsim4opus.utils;

import java.text.SimpleDateFormat;
import java.util.Calendar;

public class DateUtil {

	/**
	 * returns current date time in given dateFormat
	 * @param dateFormat
	 * @return
	 */
	public static String now(String dateFormat) {
	    Calendar cal = Calendar.getInstance();
	    SimpleDateFormat sdf = new SimpleDateFormat(dateFormat);
	    return sdf.format(cal.getTime());
	 }
	
	/**
	 * returns current date time in default dateFormat
	 * @return
	 */
	public static String now(){
		Calendar cal = Calendar.getInstance();
		// Format: YearMonthDay_HourMinute
	    SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMdd_HHmm");
	    return sdf.format(cal.getTime());
	}
	  
	/**
	 * Testing
	 * @param arg
	 */
	public static void  main(String arg[]) {
		
	   System.out.println( DateUtil.now() );
 	   System.out.println(DateUtil.now("dd MMMMM yyyy"));
 	   System.out.println(DateUtil.now("yyyyMMdd"));
 	   System.out.println(DateUtil.now("dd.MM.yy"));
 	   System.out.println(DateUtil.now("MM/dd/yy"));
 	   System.out.println(DateUtil.now("yyyy.MM.dd G 'at' hh:mm:ss z"));
 	   System.out.println(DateUtil.now("EEE, MMM d, ''yy"));
 	   System.out.println(DateUtil.now("h:mm a"));
 	   System.out.println(DateUtil.now("H:mm:ss:SSS"));
 	   System.out.println(DateUtil.now("K:mm a,z"));
 	   System.out.println(DateUtil.now("yyyy.MMMMM.dd GGG hh:mm aaa"));
 	}
}
