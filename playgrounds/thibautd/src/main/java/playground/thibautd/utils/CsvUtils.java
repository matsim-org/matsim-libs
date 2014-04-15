/* *********************************************************************** *
 * project: org.matsim.*
 * CsvUtils.java
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
package playground.thibautd.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * @author thibautd
 */
public class CsvUtils {
	public static String[] parseCsvLine(
			final char sep,
			final char quote,
			final String line) {
		final List<String> fields = new ArrayList<String>();

		StringBuilder currentField = new StringBuilder();
		boolean inquote = false;
		for ( char c : line.toCharArray() ) {
			if ( !inquote && c == sep ) {
				fields.add( currentField.toString() );
				currentField = new StringBuilder();
			}
			else if ( c == quote ) {
				inquote = !inquote;
			}
			else {
				currentField.append( c );
			}
		}
		fields.add( currentField.toString() );

		return fields.toArray( new String[ fields.size() ] );
	}
}

