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
package playground.thibautd.scripts;

import java.util.Random;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;

import eu.eunoiaproject.bikesharing.scenario.BikeSharingFacilities;
import eu.eunoiaproject.bikesharing.scenario.BikeSharingFacilitiesWriter;

/**
 * @author thibautd
 */
public class GenerateRandomBikeSharingFacilities {
	private static final double P_ACCEPT_LINK = 0.1;
	private static final int MAX_CAPACITY = 10;

	public static void main(final String[] args) {
		final String networkFile = args[ 0 ];
		final String outputFacilitiesFile = args[ 1 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( sc ).parse( networkFile );

		final BikeSharingFacilities facilities = new BikeSharingFacilities();

		final Random r = new Random( 98564 );
		for ( Link l : sc.getNetwork().getLinks().values() ) {
			if ( r.nextDouble() > P_ACCEPT_LINK ) continue;

			final int cap = r.nextInt( MAX_CAPACITY );
			facilities.addFacility(
					facilities.getFactory().createBikeSharingFacility(
						new IdImpl( "bs-"+l.getId() ),
						l.getCoord(),
						l.getId(),
						cap,
						(int) (r.nextDouble() * cap) ) );
		}

		new BikeSharingFacilitiesWriter( facilities ).write( outputFacilitiesFile );
	}
}

