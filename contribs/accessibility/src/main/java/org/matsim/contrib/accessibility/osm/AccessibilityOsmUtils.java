/* *********************************************************************** *
 * project: org.matsim.*
 * MyShoppingReader.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package org.matsim.contrib.accessibility.osm;

/**
 * @author dziemke
 */
public class AccessibilityOsmUtils {
	
	public static String simplifyString(String name) {
		if(name != null) {
			if (name.contains("&")) {							
				name = name.replaceAll("&", "u");
			}
			if (name.contains("\"")) {							
				name = name.replaceAll("\"", "");
			}
		}
		return name;
	}
}