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
package playground.thibautd.scripts.scenariohandling;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioUtils;
import playground.ivt.utils.MoreIOUtils;

import java.util.Arrays;

/**
 * @author thibautd
 */
public class MergeNetworksWithoutRenaming {
	public static void main( String... args ) {
		final String outNetwork = args[ args.length - 1 ];
		final String[] inputNetworks = Arrays.copyOf( args , args.length - 1 );

		MoreIOUtils.checkFile( outNetwork );

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		for ( String in : inputNetworks ) {
			new MatsimNetworkReader( sc.getNetwork() ).readFile( in );
		}
		new NetworkWriter( sc.getNetwork() ).write( outNetwork );
	}
}
