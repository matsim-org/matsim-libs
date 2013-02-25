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

package org.matsim.contrib.locationchoice.utils;

public class ActTypeConverter {
	
	private boolean isV1;
	
	public ActTypeConverter(boolean isV1) {
		this.isV1 = isV1;
	}
	
	public String convertType(String actType) {
		if (this.isV1) {
			return convert2MinimalType(actType);
		}
		else return actType;
	}
	
	public static String convert2FullType(String type) {
		String fullType = "tta";
		if (type.startsWith("h")) {
			fullType = "home";
		}
		else if (type.startsWith("w")) {
			fullType = "work";
		}
		else if (type.startsWith("e")) {
			fullType = "education";
		}
		else if (type.startsWith("s")) {
			fullType = "shop";
		}
		else if (type.startsWith("l")) { 
			fullType = "leisure";
		}
		return fullType;
	}
	
	public static String convert2MinimalType(String type) {
		String minimalType = "tta";
		if (type.startsWith("h")) {
			minimalType = "h";
		}
		else if (type.startsWith("w")) {
			minimalType = "w";
		}
		else if (type.startsWith("e")) {
			minimalType = "e";
		}
		else if (type.startsWith("s")) {
			minimalType = "s";
		}
		else if (type.startsWith("l")) { 
			minimalType = "l";
		}
		return minimalType;
	}
}
