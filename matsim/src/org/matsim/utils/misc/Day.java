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

package org.matsim.utils.misc;

public enum Day {

	MONDAY ("mon", "Mo"),
	TUESDAY ("tue", "Di"),
	WEDNESDAY ("wed", "Mi"),
	THURSDAY ("thu", "Do"),
	FRIDAY ("fri", "Fr"),
	SATURDAY ("sat", "Sa"),
	SUNDAY ("sun", "So");

	private final String abbrevEnglish;
	private final String abbrevGerman;

	Day(String abbrevEnglish, String abbrevGerman) {
		this.abbrevEnglish = abbrevEnglish;
		this.abbrevGerman = abbrevGerman;
	}

	public String getAbbrevGerman() {
		return abbrevGerman;
	}

	public String getAbbrevEnglish() {
		return abbrevEnglish;
	}

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
