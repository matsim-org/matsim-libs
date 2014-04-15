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
	public static String[] parseCsvLine( final String line ) {
		return parseCsvLine( ',' , '"' , line );
	}

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

	public static String buildCsvLine(
			final String... fields) {
		return buildCsvLine( ',' , '"' , fields );
	}

	public static String buildCsvLine(
			final char sep,
			final char quote,
			final String... fields) {
		final StringBuilder line = new StringBuilder();

		int c = 0;
		for ( String f : fields ) {
			if ( c++ > 0 ) line.append( sep );
			line.append( escapeField( sep , quote , f ) ); 
		}

		return line.toString();
	}

	private static String escapeField(
			final char sep,
			final char quote,
			final String string) {
		if  ( sep == quote ) {} // just to get rid of warning...
		// TODO: quote only if contains sep or white space.
		// TODO: escape quotes!
		return quote+string+quote;
	}
}

