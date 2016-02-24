/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateRandomFacilities.java
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
package playground.thibautd.scripts.scenariohandling;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.facilities.ActivityFacilitiesImpl;
import org.matsim.facilities.ActivityFacility;
import org.matsim.facilities.ActivityOption;
import org.matsim.facilities.FacilitiesWriter;
import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Random;

/**
 * @author thibautd
 */
public class GenerateRandomFacilities {
	private static final Logger log =
		Logger.getLogger(GenerateRandomFacilities.class);

	public static void main(final String[] args) throws IOException {
		final ArgParser parser = new ArgParser();
		parser.setDefaultMultipleValue(
				"--type",
				Arrays.asList( "home" , "work" , "leisure" ) );
		parser.setDefaultMultipleValue(
				"--number",
				Arrays.asList( "1000" , "500" , "5000" ) );
		// TODO opening hours
		parser.setDefaultValue(
				"--netfile",
				null );
		parser.setDefaultValue(
				"--outfile",
				null );
		parser.setDefaultValue(
				"--outf2l",
				null );

		final Args parsed = parser.parseArgs( args );
		final List<String> types = parsed.getValues( "--type" );
		final List<String> numbers = parsed.getValues( "--number" );
		final String inNet = parsed.getValue( "--netfile" );
		final String outFile = parsed.getValue( "--outfile" );
		final String outf2l = parsed.getValue( "--outf2l" );

		if ( types.size() != numbers.size() ) throw new IllegalArgumentException();

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader(sc.getNetwork()).readFile( inNet );

		final Random random = new Random( 20140224 );

		final BufferedWriter f2lwriter = IOUtils.getBufferedWriter( outf2l );
		f2lwriter.write( "fid\tlid" );

		for ( int i=0; i < types.size(); i++ ) {
			final List<Link> links = new ArrayList<Link>( sc.getNetwork().getLinks().values() );
			final String type = types.get( i );
			final int number = Integer.parseInt( numbers.get( i ) );

			log.info( "generate "+number+" facilities for type "+type );

			for ( int n=0; n < number; n++ ) {
				// no replacement
				if ( links.isEmpty() ) {
					log.warn( "no more link available for activity type "+type+" after "+n );
					break;
				}
				final Link l = links.remove( random.nextInt( links.size() ) );

				final ActivityFacility facility =
					sc.getActivityFacilities().getFactory().createActivityFacility(
							Id.create( "fac-"+type+"-"+n , ActivityFacility.class ),
							l.getCoord(), l.getId() );

				final ActivityOption option = 
					sc.getActivityFacilities().getFactory().createActivityOption(
							type );

				facility.addActivityOption( option );
				sc.getActivityFacilities().addActivityFacility( facility );

				f2lwriter.newLine();
				f2lwriter.write( facility.getId()+"\t"+l.getId() );
			}
		}
		f2lwriter.close();

		// metadata
		final StringBuilder meta = new StringBuilder();
		meta.append( "generated with " );
		meta.append( GenerateRandomFacilities.class.getName() );
		for ( String arg : args ) meta.append( " "+arg );
		meta.append( " on the " );
		meta.append( new Date().toString() );

		((ActivityFacilitiesImpl) sc.getActivityFacilities()).setName(
			meta.toString() );

		new FacilitiesWriter( sc.getActivityFacilities() ).write( outFile );

	}
}

