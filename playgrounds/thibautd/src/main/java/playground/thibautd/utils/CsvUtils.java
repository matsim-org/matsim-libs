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

import gnu.trove.map.TObjectIntMap;
import gnu.trove.map.hash.TObjectIntHashMap;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;

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
		boolean escape = false;
		for ( char c : line.toCharArray() ) {
			if ( !inquote && c == sep ) {
				fields.add( currentField.toString() );
				currentField = new StringBuilder();
			}
			else if ( !escape && c == quote ) {
				inquote = !inquote;
			}
			else if (escape || c != '\\' ) {
				currentField.append( c );
			}
			escape = !escape && c == '\\';
		}
		fields.add( currentField.toString() );

		return fields.toArray( new String[ fields.size() ] );
	}

	public static TitleLine parseTitleLine(
			final char sep,
			final char quote,
			final String line ) {
		final String[] names = parseCsvLine( sep , quote , line );
		return new TitleLine( names );
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

	public static String escapeField(
			final char sep,
			final char quote,
			final String string) {
		// only escape quotation marks and separators
		if ( string.indexOf( sep ) < 0 &&
				string.indexOf( quote ) < 0 &&
				string.indexOf( '\\' ) < 0 ) return string;

		final StringBuffer buffer = new StringBuffer();
		buffer.append( '"' );
		for ( char c : string.toCharArray() ) {
			if ( c == quote || c == '\\' ) {
				buffer.append( '\\' );
			}
			buffer.append( c );
		}
		buffer.append( '"' );

		return buffer.toString();
	}

	public static class TitleLine {
		private final TObjectIntMap<String> map = new TObjectIntHashMap<>();
		private final String[] names;

		public TitleLine( final Collection<String> names ) {
			this( names.toArray( new String[ names.size() ] ) );
		}

		public TitleLine( final String... names ) {
			for ( int i = 0; i < names.length; i++ ) {
				map.put( names[ i ] , i );
			}
			this.names = names;
		}

		public String[] getNames() {
			return names;
		}

		public int getIndexOfField( final String name ) {
			return map.get( name );
		}

		public int getNField() {
			return map.size();
		}
	}
}

