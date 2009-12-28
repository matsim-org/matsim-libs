/* *********************************************************************** *
 * project: org.matsim.*
 * Properties.java
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

package playground.mrieser;

import java.util.Properties;
import java.util.Map.Entry;

public class PropertyListing {

	/**
	 * @param args
	 */
	public static void main(final String[] args) {
		Properties properties = System.getProperties();
		for (Entry<Object, Object> entry : properties.entrySet()) {
			System.out.println(entry.getKey() + " = " + entry.getValue());
		}

	}

}
