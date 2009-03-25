/* *********************************************************************** *
 * project: org.matsim.*
 * CodesTableReader.java
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

package playground.yalcin;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.core.utils.io.tabularFileParser.TabularFileHandler;

/* Reads a file containing:
 *   code  |  personId  |  tripId  |  additional data gets ignored ... 
 */
public class CodesTableReader implements TabularFileHandler {

	/** LookupTable: PersonId-TripId ==> Code */
	final Map<String, String> lookupTable = new TreeMap<String, String>();
	
	public void startRow(final String[] row) {
		final String code = row[0];
		final String personId = row[1];
		final String tripId = row[2];
		this.lookupTable.put(personId + "-" + tripId, code);
	}
	
	public String getCode(final String personId, final String tripId) {
		return lookupTable.get(personId + "-" + tripId);
	}

}
