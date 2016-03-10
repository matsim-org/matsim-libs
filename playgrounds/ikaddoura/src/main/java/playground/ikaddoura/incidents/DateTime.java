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

import org.matsim.core.utils.misc.StringUtils;
import org.matsim.core.utils.misc.Time;


/**
* Expected Input date+time format: YYYY/MM/DD HH:MM:SS, other formats are not accepted.
* Only YYYY/MM/DD or HH:MM:SS should also work.  
* 
* @author ikaddoura
*/

public class DateTime {
	
	public static final double parseDateTimeToDateTimeSeconds( String dateTimeString ) {
		double timeSec = 0.;
		double dateSec = 0.;
		
		String dateTimeDelimiter = " ";
		String[] datetime = StringUtils.explode(dateTimeString, dateTimeDelimiter.charAt(0));
		if (datetime.length == 1) {
			if (datetime[0].contains(":")) {
				// only time
				timeSec = Time.parseTime(datetime[0]);
			} else if (datetime[0].contains("/")) {
				// only date
				dateSec = computeDateSec(datetime[0]);
			}
			
		} else if (datetime.length == 2) {
			// date + time
			dateSec = computeDateSec(datetime[0]);
			timeSec = Time.parseTime(datetime[1]);
		} else {
			throw new RuntimeException("Expecting the traffic incidents to have the date+time format YYYY/MM/DD HH:MM:SS. Cannot interpret the following text: " + dateTimeString + " Aborting...");
		}
		
		double seconds = dateSec + timeSec;
		return seconds;
	}

	private static double computeDateSec(String dateString) {
		
		String dateDelimiter = "/";
		String[] date = StringUtils.explode(dateString, dateDelimiter.charAt(0));
				
		if (date.length != 3 || date[0].length() != 4 || date[1].length() != 2 || date[2].length() != 2) {
			throw new RuntimeException("Expecting the traffic incidents to have the date format YYYY/MM/DD.  Cannot interpret the following text: " + dateString + " Aborting...");
		}
		
		double year = Double.valueOf(date[0]);
		double month = Double.valueOf(date[1]);
		double day = Double.valueOf(date[2]);
			
		double dateSec = year * 12 * 30.436875 * 24 * 3600 + month * 30.436875 * 24 * 3600 + day * 24 * 3600;
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
			} else if (datetime[0].contains("/")) {
				// only date
				throw new RuntimeException("Expecting the time format HH:MM:SS. Cannot interpret the following text: " + dateTimeString + " Aborting...");
			}
			
		} else if (datetime.length == 2) {
			// date + time
			timeSec = Time.parseTime(datetime[1]);
		} else {
			throw new RuntimeException("Expecting the date+time format YYYY/MM/DD HH:MM:SS. Cannot interpret the following text: " + dateTimeString + " Aborting...");
		}
		
		return timeSec;
	}

	public static String secToDateTimeString(double dateTimeInSec) {
		
		int year = (int) ( dateTimeInSec / (12 * 30.436875 * 24 * 3600) );
		int month = (int) ( (dateTimeInSec - (year * 12 * 30.436875 * 24 * 3600)) / (30.436875 * 24 * 3600) );
		int day = (int) ( (dateTimeInSec - (year * 12 * 30.436875 * 24 * 3600) - (month * 30.436875 * 24 * 3600) ) / (24 * 3600) );
		
		double seconds = (dateTimeInSec - (year * 12 * 30.436875 * 24 * 3600.) - (month * 30.436875 * 24 * 3600.) - (day * 24 * 3600.)) / 3600.;
		String time = Time.writeTime(seconds);

		String dateTime = year + "-" + month + "-" + day;

		if (seconds > 0.) {
			dateTime = dateTime + "_" + time;
		}
		
		return dateTime;
	}
}

