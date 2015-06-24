/* *********************************************************************** *
 * project: org.matsim.*
 * WasteUtils.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

/**
 * 
 */
package playground.jjoubert.projects.wasteCollection.dataHandling;

import java.util.Calendar;
import java.util.GregorianCalendar;

/**
 * A few general utilities to use with the City of Cape Town waste collection
 * data handling.
 * 
 * @author jwjoubert
 */
public class WasteUtils {
	
	private WasteUtils() {
		/* Hide the constructor. */
	}
	
	public static String convertGregorianCalendarToDate(GregorianCalendar cal){
		String s = "";
		
		int year = cal.get(Calendar.YEAR);
		int month = cal.get(Calendar.MONTH) + 1;
		int day = cal.get(Calendar.DAY_OF_MONTH);
		
		int hour = cal.get(Calendar.HOUR_OF_DAY);
		int min = cal.get(Calendar.MINUTE);
		int sec = cal.get(Calendar.SECOND);
		
		s = String.format("%02d/%02d/%04d %02d:%02d:%02d", day, month, year, hour, min, sec);
		return s;
	}

}
