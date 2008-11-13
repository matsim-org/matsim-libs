/* *********************************************************************** *
 * project: org.matsim.*
 * Day.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk.org.matsim.facilities.algorithms;

import org.matsim.basic.v01.BasicOpeningTime.DayType;
@Deprecated
//refactor and use DayType instead
public enum Day {

	MONDAY (DayType.mon, "Mo"),
	TUESDAY (DayType.tue, "Di"),
	WEDNESDAY (DayType.wed, "Mi"),
	THURSDAY (DayType.thu, "Do"),
	FRIDAY (DayType.fri, "Fr"),
	SATURDAY (DayType.sat, "Sa"),
	SUNDAY (DayType.sun, "So");

	private final DayType abbrevEnglish;
	private final String abbrevGerman;
	@Deprecated
	Day(DayType abbrevEnglish, String abbrevGerman) {
		this.abbrevEnglish = abbrevEnglish;
		this.abbrevGerman = abbrevGerman;
	}
	@Deprecated
	public String getAbbrevGerman() {
		return abbrevGerman;
	}
	@Deprecated
	public DayType getAbbrevEnglish() {
		return abbrevEnglish;
	}
	@Deprecated
	public static Day getDayByGermanAbbrev(String germanAbbrev) {

		Day theDay = null;

		Day[] days = Day.values();
		for (Day day : days) {
			if (day.getAbbrevGerman().equals(germanAbbrev)) {
				theDay = day;
			}
		}

		return theDay;

	}

}
