/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.scripts;

import org.matsim.core.utils.io.UncheckedIOException;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlReader;
import playground.thibautd.utils.CsvParser;
import playground.thibautd.utils.CsvUtils;
import playground.thibautd.utils.CsvWriter;

import java.io.IOException;
import java.util.Arrays;

/**
 * @author thibautd
 */
public class AddHouseholdAttributesInMZBiogemeDataset {
	public static void main( final String... args ) {
		final String inDataSet = args[ 0 ];
		final String inAttributes = args[ 1 ];
		final String outDataSet = args[ 2 ];

		final ObjectAttributes attributes = new ObjectAttributes();
		new ObjectAttributesXmlReader( attributes ).parse( inAttributes );

		try ( final CsvParser parser = new CsvParser( '\t' , '"' , inDataSet );
				  final CsvWriter writer = new CsvWriter( '\t' , '"' , expand( parser.getTitleLine() ), outDataSet ) ) {
			while ( parser.nextLine() ) {
				for ( String name : parser.getTitleLine().getNames() ) {
					writer.setField( name , parser.getField( name ) );
				}
				// additional fields
				writer.setField(
						"HH_SIZE",
						getHouseholdSize(
								parser.getField( "P_ID" ),
								attributes ) );
				writer.setField(
						"HH_MONTHINCOME",
						getHouseholdIncome(
								parser.getField( "P_ID" ),
								attributes ) );
				writer.nextLine();
			}
		}
		catch ( IOException e ) {
			throw new UncheckedIOException( e );
		}
	}

	private static CsvUtils.TitleLine expand( CsvUtils.TitleLine titleLine ) {
		final String[] newTitleLine = new String[ titleLine.getNField() + 2 ];
		final int index = titleLine.getIndexOfField( "C_CHOICE" );

		for ( int i = 0; i < titleLine.getNField(); i++ ) {
			if ( i < index ) newTitleLine[ i ] = titleLine.getNames()[ i ];
			else newTitleLine[ i + 2 ] = titleLine.getNames()[ i ];
		}

		newTitleLine[ index ] = "HH_SIZE";
		newTitleLine[ index + 1 ] = "HH_MONTHINCOME";

		return new CsvUtils.TitleLine( newTitleLine );
	}

	private static String getHouseholdIncome( String personId, ObjectAttributes personAttributes ) {
		final String income = ( String ) personAttributes.getAttribute(
				personId,
				"householdIncome" );
		switch ( income ) {
			case "no Answer":
				return "-98";
			case "do not know":
				return "-97";
			case "less than CHF 2000":
				return "1";
			case "CHF 2000 to 4000":
				return "2";
			case "CHF 4001 to 6000":
				return "3";
			case "CHF 6001 to 8000":
				return "4";
			case "CHF 8001 to 10000":
				return "5";
			case "CHF 10001 to 12000":
				return "6";
			case "CHF 12001 to 14000":
				return "7";
			case "CHF 14001 to 16000":
				return "8";
			case "greater than CHF 16000":
				return "9";
		}
		throw new IllegalArgumentException( income );
	}

	private static String getHouseholdSize( String personId, ObjectAttributes personAttributes ) {
		final Integer size = ( Integer ) personAttributes.getAttribute(
				personId,
				"householdSize" );
		if ( size == null ) throw new NullPointerException( "null hh size for "+personId );
		return size.toString();
	}
}

