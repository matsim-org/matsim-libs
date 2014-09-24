/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.johannes.gsv.synPop.invermo;

/**
 * @author johannes
 *
 */
public class ColumnKeys {

	public static final String HOUSEHOLD_ID = "ID";
	
	public static final String PERSON_ID = "persnr";
	
	public static final String STATION_NAME = "hhbhfname";
	
	public static final String STATION_DIST = "hhbhfkm";
	
	public static final String HOME_TOWN = "wohnort";
	
	public static final String HOME_ZIPCODE = "wohnplz";
	
	public static final String NA = "nan";
	
	public static final String START1_TRIP1 = "e1start1";
	
	public static final String START2_TRIP1 = "e1start2";
	
	public static final String START1_TRIP2 = "e2start1";
	
	public static final String START2_TRIP2 = "e2start2";
	
	public static final String START1_TRIP3 = "e3start1";
	
	public static final String START2_TRIP3 = "e3start2";
	
	public static final String START1_TRIP4 = "e4start1";
	
	public static final String MOB_WEIGTH = "gewmobil";
	
	public static boolean validate(String value) {
		return (value != null && !value.equalsIgnoreCase(NA) && !value.isEmpty());
	}
}
