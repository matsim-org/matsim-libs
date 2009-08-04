/* *********************************************************************** *
 * project: org.matsim.*
 * KeyGenerator.java
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
package playground.johannes.socialnetworks.survey.ivt2009;

/**
 * @author illenberger
 *
 */
public class KeyGenerator {
	
	public static final int NUM_HOMELOCS = 20;
	
	private static final String HOME_LOC_KEY = "84967X53X128Loc";

	private static final String HOME_LOC_YEAR_KEY = "84967X53X128von";
	
	public static final int NUM_ALTERS_1 = 29;
	
	public static final int NUM_ALTERS_2 = 11;
	
	public static final String ALTER_1_KEY = "84967X55X143A";
	
	public static final String ALTER_2_KEY = "84967X55X144B";
	
	private static final String ALTER_AGE_KEY = "84967X54X137";
	
	private static final String ALTER_LOC_KEY = "84967X54X338Loc";
	
	public String egoHomeLocationCoordKey(int counter) {
		return indexKey(HOME_LOC_KEY, counter, 17);
	}
	
	public String egoHomeLocationYearKey(int counter) {
		return indexKey(HOME_LOC_YEAR_KEY, counter, 17);
	}
	
	public String alter1Key(int counter) {
		return indexKey(ALTER_1_KEY, counter, 15);
	}
	
	public String alter2Key(int counter) {
		return indexKey(ALTER_2_KEY, counter, 15);
	}
	
	public String alterLocation1CoordKey(int counter) {
		return alterLocationCoordKey(ALTER_1_KEY, counter);
	}
	
	public String alterLocation2CoordKey(int counter) {
		return alterLocationCoordKey(ALTER_2_KEY, counter);
	}
	
	public String alterLocationCoordKey(String alterkey, int counter) {
		StringBuffer buffer = new StringBuffer(31);
		buffer.append(alterkey);
		buffer.append(Integer.toString(counter));
		buffer.append("_");
		buffer.append(ALTER_LOC_KEY);
		return buffer.toString();
	}
	private String indexKey(String key, int index, int maxLength) {
		StringBuilder builder = new StringBuilder(maxLength);
		builder.append(key);
		builder.append(String.valueOf(index));
		return builder.toString();
	}
}
