/* *********************************************************************** *
 * project: org.matsim.*
 * AssociateFacilitiesToCarLinks.java
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
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.facilities.MatsimFacilitiesReader;
import org.matsim.facilities.algorithms.WorldConnectLocations;

import java.util.ArrayList;
import java.util.List;

/**
 * @author thibautd
 */
public class AssociateFacilitiesToCarLinks {

	public static void main(final String[] args) {
		final String netFile = args[ 0 ];
		final String facFile = args[ 1 ];
		final String outputf2l = args[ 2 ];

		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		final NetworkImpl network = (NetworkImpl) sc.getNetwork();

		new MatsimNetworkReader( sc ).readFile( netFile );
		new MatsimFacilitiesReader( sc ).readFile( facFile );

		removeNonCarLinks( network );
		removeIsolatedNodes( network );

		final ConfigGroup f2l = new ConfigGroup( WorldConnectLocations.CONFIG_F2L );
		f2l.addParam( WorldConnectLocations.CONFIG_F2L_OUTPUTF2LFile , outputf2l );

		final Config conf = new Config();
		conf.addModule( f2l );

		new WorldConnectLocations( conf ).connectFacilitiesWithLinks(
				sc.getActivityFacilities(),
				network );
	}

	private static void removeIsolatedNodes(final Network network) {
		final List<Id> toRemove = new ArrayList<Id>();
		for ( Node n : network.getNodes().values() ) {
			if ( n.getInLinks().isEmpty() || n.getOutLinks().isEmpty() ) {
				// node isolated or dead end
				toRemove.add( n.getId() );
			}
		}
		for ( Id id : toRemove ) network.removeNode( id );
	}

	private static void removeNonCarLinks(final Network network) {
		final List<Id> toRemove = new ArrayList<Id>();
		for ( Link l : network.getLinks().values() ) {
			if ( !l.getAllowedModes().contains( TransportMode.car ) ) {
				toRemove.add( l.getId() );
			}
		}
		for ( Id id : toRemove ) network.removeLink( id );
	}
}

