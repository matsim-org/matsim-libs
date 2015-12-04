/* *********************************************************************** *
 * project: org.matsim.*
 * FilterDoublonsFromCsv.java
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
package playground.thibautd.geocoding;

import org.matsim.core.utils.io.IOUtils;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Scanner;

/**
 * Geolocalizer should attempt to simplify results as much as possible, but this
 * can be useful for a last manual clean.
 * @author thibautd
 */
public class FilterDoublonsFromCsv {
	public static void main(final String[] args) throws IOException {
		final String inFile = args[ 0 ];
		final String outFile = args[ 1 ];

		final BufferedReader reader = IOUtils.getBufferedReader( inFile );
		final BufferedWriter writer = IOUtils.getBufferedWriter( outFile );

		String lastId = null;
		final ArrayList<String> linesOfId = new ArrayList<String>();

		for ( String line = reader.readLine();
				line != null;
				line = reader.readLine() ) {
			final String id = line.split( "," )[ 0 ];

			if ( !linesOfId.isEmpty() && !id.equals( lastId ) ) {
				writer.write( requestUser( linesOfId ) );
				writer.newLine();
				linesOfId.clear();
				lastId = id;
			}

			linesOfId.add( line );
		}
		writer.write( requestUser( linesOfId ) );

		reader.close();
		writer.close();
	}

	private static String requestUser(final ArrayList<String> linesOfId) {
		if ( linesOfId.size() == 1 ) return linesOfId.get( 0 );

		System.out.println( );
		System.out.println( );
		System.out.println( "Please select one of the following lines:" );
		for ( int i=0; i < linesOfId.size(); i++ ) {
			System.out.println( i+":    "+linesOfId.get( i ) );
		}
		System.out.println( "r:    reject all" );
		System.out.println( );
		System.out.print( "your answer: " );
		final Scanner s = new Scanner( System.in );
		while ( true ) {
			final String answer = s.next();
			if ( answer.equals( "r" ) ) return "";
			try {
				final int i = Integer.valueOf( answer );
				try {
					return linesOfId.get( i );
				}
				catch (IndexOutOfBoundsException e) {
					System.err.println( "wrong index "+i );
				}
			}
			catch (Exception e) {
				System.err.println( "error while reading input. try again" );
			}
		}
	}
}

