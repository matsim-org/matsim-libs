/* *********************************************************************** *
 * project: org.matsim.*
 * ConvertXYToBikeSharingStations.java
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
package playground.thibautd.eunoia.generation;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.objectattributes.ObjectAttributes;
import org.matsim.utils.objectattributes.ObjectAttributesXmlWriter;

import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilities;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilitiesWriter;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacility;

/**
 * @author thibautd
 */
public class ConvertXYToBikeSharingStations {
	public static void main(final String[] args) throws IOException {
		final ArgParser parser = new ArgParser();
		parser.setDefaultMultipleValue( "-xy" , "--xy-file" , Collections.<String>emptyList() );
		parser.setDefaultValue( "-n" , "--network-file" , null );
		parser.setDefaultValue( "-o" , "--out-file" , null );
		parser.setDefaultValue( "-c" , "--capacity" , "24" );
		parser.setDefaultValue( "-i" , "--initial" , "15" );
		parser.setDefaultValue( "-p" , "--id-prefix" , "bikeSharingStation-" );
		parser.setDefaultValue( "-a" , "--out-attributes" , null );

		parser.setDefaultValue( "--name" , null );
		main( parser.parseArgs( args ) );
	}

	private static void main(final Args args) throws IOException {
		final List<String> xyFiles = args.getValues( "-xy" );
		final String netFile = args.getValue( "-n" );
		final String outFile = args.getValue( "-o" );
		final String outAttributes = args.getValue( "-a" );

		final int capacity = args.getIntegerValue( "-c" );
		final int initial = args.getIntegerValue( "-i" );

		final String prefix = args.getValue( "-p" );

		final NetworkImpl network = readNetwork( netFile );

		final ObjectAttributes attributes = new ObjectAttributes();

		final BikeSharingFacilities facilities = new BikeSharingFacilities();
		facilities.addMetadata( "generation date" , new Date().toString() );
		facilities.addMetadata( "generation script" , ConvertXYToBikeSharingStations.class.getName() );
		facilities.addMetadata(
				"network file",
				new File( netFile ).getAbsolutePath() );
		for ( int i=0; i < xyFiles.size(); i++ ) {
			facilities.addMetadata(
					"input xy file "+(i+1),
					new File( xyFiles.get( i ) ).getAbsolutePath() );
		}
		if ( args.getValue( "--name" ) != null ) facilities.addMetadata( "name" , args.getValue( "--name" ) );

		int filecount = 0;
		for ( String xyFile : xyFiles ) {
			final BufferedReader reader = IOUtils.getBufferedReader( xyFile );
			final Collection<AttributeIndex> atts = parseHeader( reader.readLine() );

			filecount++;
			int linecount = 0;
			for ( String line = reader.readLine();
					line != null;
					line = reader.readLine() ) {
				final String[] fields = line.split( "," );

				final Coord coord =
						new Coord(Double.parseDouble(fields[0]), Double.parseDouble(fields[1]));
				final Id<Link> link =
					NetworkUtils.getNearestLink(network, coord).getId();

				final Id<BikeSharingFacility> id = Id.create( prefix+filecount+"-"+(linecount++) , BikeSharingFacility.class );

				facilities.addFacility(
						facilities.getFactory().createBikeSharingFacility(
							id,
							coord,
							link,
							capacity,
							initial ) );

				for ( AttributeIndex a : atts ) {
					attributes.putAttribute(
							id.toString(),
							a.name,
							// XXX not that nice to assume all fields are double...
							// this is correct for my current input (altitudes),
							// but might have to be modified.
							Double.parseDouble(
								fields[ a.index ] ) );
				}
			}
			reader.close();
		}

		new BikeSharingFacilitiesWriter( facilities ).write( outFile );
		if ( outAttributes != null ) new ObjectAttributesXmlWriter( attributes ).writeFile( outAttributes );
	}

	private static NetworkImpl readNetwork(final String netFile) {
		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( sc ).readFile( netFile );
		return (NetworkImpl) sc.getNetwork();
	}

	private static Collection<AttributeIndex> parseHeader( final String line ) {
		final String[] header = line.split( "," );
		if ( !header[ 0 ].equals( "X" ) ) throw new IllegalArgumentException( "bad first field "+header[ 0 ] );
		if ( !header[ 1 ].equals( "Y" ) ) throw new IllegalArgumentException( "bad second field "+header[ 1 ] );

		final List<AttributeIndex> indexes = new ArrayList<AttributeIndex>();
		for ( int i = 2; i < header.length; i++ ) {
			indexes.add( new AttributeIndex( i , header[ i ] ) );
		}
		return indexes;
	}

	private static class AttributeIndex {
		public final int index;
		public final String name;

		public AttributeIndex(
				final int index,
				final String name) {
			this.index = index;
			this.name = name;
		}
	}
}

