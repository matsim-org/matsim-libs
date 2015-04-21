/* *********************************************************************** *
 * project: org.matsim.*
 * GenerateMultimodalNetworkFromSpeeds.java
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.NetworkWriter;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.contrib.multimodal.tools.MultiModalNetworkCreator;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.algorithms.NetworkCleaner;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.CollectionUtils;
import playground.ivt.matsim2030.Matsim2030Utils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author thibautd
 */
public class GenerateMultimodalNetworkFromSpeeds {
	public static void main(final String[] args) {
		final String inputNetwork = args[ 0 ];
		final String outputNetwork = args[ 1 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new MatsimNetworkReader( sc ).readFile( inputNetwork );

		final MultiModalConfigGroup conf = new MultiModalConfigGroup();
		conf.setCreateMultiModalNetwork( true );
		new MultiModalNetworkCreator( conf ).run( sc.getNetwork() );

		// only keep the biggest connected component for non-vehicular modes.
		// Otherwise, small clusters of low speed links, such as gas stations
		// on the highway, pose problems.
		final Set<String> nvModes = 
				CollectionUtils.stringToSet( 
					conf.getSimulatedModes() );
		final Network subnet =
			Matsim2030Utils.filterLinksWithAllModes(
				sc.getNetwork(),
				nvModes );

		final Set<Id<Node>> biggestCluster = new NetworkCleaner().searchBiggestCluster( subnet ).keySet();
		for ( Node n : sc.getNetwork().getNodes().values() ) {
			if ( !biggestCluster.remove( n.getId() ) ) {
				for ( Link l : n.getInLinks().values() ) removeModes( l , nvModes );
				for ( Link l : n.getOutLinks().values() ) removeModes( l , nvModes );
			}
		}

		new NetworkWriter( sc.getNetwork() ).write( outputNetwork );
	}

	private static void removeModes(
			final Link l,
			final Set<String> nvModes) {
		final Set<String> modes = new HashSet<String>( l.getAllowedModes() );
		modes.removeAll( nvModes );
		if ( l.getAllowedModes().size() != modes.size() ) l.setAllowedModes( modes );
	}
}

