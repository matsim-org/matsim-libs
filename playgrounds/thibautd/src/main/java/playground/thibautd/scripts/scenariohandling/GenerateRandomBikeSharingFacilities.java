/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateRandomBikeSharingFacilities.java
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
package playground.thibautd.scripts.scenariohandling;

import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilities;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacilitiesWriter;
import eu.eunoiaproject.bikesharing.framework.scenario.BikeSharingFacility;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import playground.ivt.utils.ArgParser;
import playground.ivt.utils.ArgParser.Args;

import java.util.Random;

/**
 * @author thibautd
 */
public class GenerateRandomBikeSharingFacilities {

	public static void main(final String[] args) {
		final ArgParser parser = new ArgParser();

		parser.setDefaultValue( "-n" , "--network-file" , null );
		parser.setDefaultValue( "-o" , "--outputFacilitiesFile" , null );

		parser.setDefaultValue( "-p" , "--p-accept" , "0.01" );
		parser.setDefaultValue( "-m" , "--max-capacity" , "100" );

		main( parser.parseArgs( args ) );
	}

	public static void main(final Args args) {
		final String networkFile = args.getValue( "-n" );
		final String outputFacilitiesFile = args.getValue( "-o" );

		final double pAcceptLink = args.getDoubleValue( "-p" );
		final int maxCapacity = args.getIntegerValue( "-m" );

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( sc ).parse( networkFile );

		final BikeSharingFacilities facilities = new BikeSharingFacilities();

		final Random r = new Random( 98564 );
		for ( Link l : sc.getNetwork().getLinks().values() ) {
			if ( r.nextDouble() > pAcceptLink ) continue;

			final int cap = r.nextInt( maxCapacity );
			facilities.addFacility(
					facilities.getFactory().createBikeSharingFacility(
						Id.create( "bs-"+l.getId() , BikeSharingFacility.class ),
						l.getCoord(),
						l.getId(),
						cap,
						(int) (r.nextDouble() * cap) ) );
		}

		new BikeSharingFacilitiesWriter( facilities ).write( outputFacilitiesFile );
	}
}

