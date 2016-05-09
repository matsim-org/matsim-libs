/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

package playground.ikaddoura.incidents;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import org.matsim.core.utils.misc.StringUtils;
import org.matsim.core.utils.misc.Time;



/**
* Expected Input date+time format: YYYY-MM-DD HH:MM:SS, other formats are not accepted.
* Only YYYY-MM-DD or HH:MM:SS should also work.  
* 
* @author ikaddoura
*/

public class DateTime {
	
	public static final double parseDateTimeToDateTimeSeconds( String dateTimeString ) throws ParseException {
				
		double timeSec = 0.;
		double dateSec = 0.;
		
		String dateTimeDelimiter = " ";
		String[] datetime = StringUtils.explode(dateTimeString, dateTimeDelimiter.charAt(0));
		if (datetime.length == 1) {
			if (datetime[0].contains(":")) {
				// only time
				timeSec = Time.parseTime(datetime[0]);
			} else if (datetime[0].contains("-")) {
				// only date
				dateSec = computeDateSec(datetime[0]);
			} else {
				throw new RuntimeException("Expecting the following date/time/date+time formats: YYYY-MM-DD HH:MM:SS OR YYYY-MM-DD OR HH:MM:SS. Cannot interpret the following text: " + dateTimeString + " Aborting...");
			}
			
		} else if (datetime.length == 2) {
			// date + time
			dateSec = computeDateSec(datetime[0]);
			timeSec = Time.parseTime(datetime[1]);
		} else {
			throw new RuntimeException("Expecting the following date+time format: YYYY-MM-DD HH:MM:SS. Cannot interpret the following text: " + dateTimeString + " Aborting...");
		}
		
		double seconds = dateSec + timeSec;
		return seconds;
	}

	private static double computeDateSec(String dateString) throws ParseException {
		
		String dateDelimiter = "-";
		String[] dateArray = StringUtils.explode(dateString, dateDelimiter.charAt(0));
				
		if (dateArray.length != 3 || dateArray[0].length() != 4 || dateArray[1].length() != 2 || dateArray[2].length() != 2) {
			throw new RuntimeException("Expecting the following date format: YYYY-MM-DD.  Cannot interpret the following text: " + dateString + " Aborting...");
		}
					
		SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd hh:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
		Date date = sdf.parse(dateString + " 00:00:00");
		long dateMilliSec = date.getTime();
		double dateSec = (double) dateMilliSec / 1000.;
		return dateSec;
	}

	public static double parseDateTimeToTimeSeconds(String dateTimeString) {
		double timeSec = 0.;
		
		String dateTimeDelimiter = " ";
		String[] datetime = StringUtils.explode(dateTimeString, dateTimeDelimiter.charAt(0));
		if (datetime.length == 1) {
			if (datetime[0].contains(":")) {
				// only time
				timeSec = Time.parseTime(datetime[0]);
			} else if (datetime[0].contains("-")) {
				// only date
				throw new RuntimeException("Expecting the following time format: HH:MM:SS. Cannot interpret the following text: " + dateTimeString + " Aborting...");
			}
			
		} else if (datetime.length == 2) {
			// date + time
			timeSec = Time.parseTime(datetime[1]);
		} else {
			throw new RuntimeException("Expecting the following date+time format: YYYY-MM-DD HH:MM:SS. Cannot interpret the following text: " + dateTimeString + " Aborting...");
		}
		
		return timeSec;
	}

	public static String secToDateTimeString(double dateTimeInSec) {
		
		Date date = new Date((long) (dateTimeInSec * 1000));
		SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd hh:mm:ss");
		sdf.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
		return sdf.format(date);
	}
	
	public static String secToDateString(double dateTimeInSec) {
		
		BigDecimal dateTimeInSec2 = BigDecimal.valueOf( dateTimeInSec ) ;
		Date date = new Date( dateTimeInSec2.multiply(BigDecimal.valueOf(1000)).longValue() ) ;
		SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd");
		sdf.setTimeZone(TimeZone.getTimeZone("Europe/Berlin"));
		String dateString = sdf.format(date);
		return dateString;

//		BigInteger secsAsInt = BigInteger.valueOf( (long) dateTimeInSec );
//		if ( secsAsInt.longValue() == dateTimeInSec ) {
//			Date date = new Date( secsAsInt.multiply(BigInteger.valueOf(1000)).longValue() ) ;
//			SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd");
//			String dateString = sdf.format(date);
//			return dateString;
//		} else {
//			Date date = new Date((long) (dateTimeInSec * 1000));
//			SimpleDateFormat sdf = new SimpleDateFormat("yyy-MM-dd");
//			String dateString = sdf.format(date);
//			return dateString;
//		}
	}
}

