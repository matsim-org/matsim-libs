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
package playground.ivt.scripts;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.network.algorithms.NetworkMergeDoubleLinks;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author thibautd
 */
public class SimplifyNetwork {
	public static void main( final String... args ) {
		final String inputNetworkFile = args[ 0 ];
		final String outputNetworkFile = args[ 1 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( sc ).readFile( inputNetworkFile );

		final NetworkSimplifier simplifier = new NetworkSimplifier();
		simplifier.setMergeLinkStats( true );
		simplifier.run( sc.getNetwork() );
		new NetworkMergeDoubleLinks( NetworkMergeDoubleLinks.MergeType.ADDITIVE ).run( sc.getNetwork() );
		new NetworkCleaner().run( sc.getNetwork() );

		new NetworkWriter( sc.getNetwork() ).write( outputNetworkFile );
	}
}
