/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.anhorni.surprice;

import org.matsim.facilities.OpeningTime.DayType;

public class DayConverter {
	
	public static String getDayString(int day) {
		return Surprice.days.get(day);
	}
	
	public static int getDayInt(String day) {
		return Surprice.days.indexOf(day);
	}
	
	public static DayType getDayType(String day) {
		if (day.equals("mon")) {
			return DayType.mon;
		} else if (day.equals("tue")) {
			return DayType.tue;
		} else if (day.equals("tue")) {
			return DayType.tue;
		} else if (day.equals("wed")) {
			return DayType.wed;
		} else if (day.equals("thu")) {
			return DayType.thu;
		} else if (day.equals("fri")) {
			return DayType.fri;
		} else if (day.equals("sat")) {
			return DayType.sat;
		} else if (day.equals("sun")) {
			return DayType.sun;
		} else {
			return DayType.wk;
		}
	}
}
