/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkAsMatsimNetworkUtils.java
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
package playground.thibautd.initialdemandgeneration.socnetgen.analysis;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.thibautd.socnetsim.population.SocialNetwork;
import playground.thibautd.socnetsim.population.SocialNetworkReader;
import playground.thibautd.utils.CollectionUtils;

/**
 * @author thibautd
 */
public class SocialNetworkAsMatsimNetworkUtils {

	public static Network parseSocialNetworkAsMatsimNetwork(
			final String file) {
		final Scenario sc = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
		new SocialNetworkReader( sc ).parse( file );
	
		final SocialNetwork sn = (SocialNetwork) sc.getScenarioElement( SocialNetwork.ELEMENT_NAME );
		return convertToNetwork( sn );
	}

	public static Network convertToNetwork(final SocialNetwork sn) {
		final Network net = NetworkImpl.createNetwork();
	
		final Coord dummyCoord = new CoordImpl( 0 , 0 );
		for ( Id ego : sn.getEgos() ) {
			net.addNode(
					net.getFactory().createNode(
						ego,
						dummyCoord ) );
		}
	
		for ( Id ego : sn.getEgos() ) {
			for ( Id alter : sn.getAlters( ego ) ) {
				net.addLink(
						net.getFactory().createLink(
							new IdImpl( ego+"---"+alter ),
							net.getNodes().get( ego ),
							net.getNodes().get( alter ) ) );
			}
		}
	
		return net;
	}


	public static Collection<Set<Id>> identifyConnectedComponents(
			final SocialNetwork sn) {
		if ( !sn.isReflective() ) {
			throw new IllegalArgumentException( "the algorithm is valid only with reflective networks" );
		}
		final Map<Id, Set<Id>> altersMap = new LinkedHashMap<Id, Set<Id>>( sn.getMapRepresentation() );
		final Collection< Set<Id> > components = new ArrayList< Set<Id> >();

		while ( !altersMap.isEmpty() ) {
			// DFS implemented as a loop (recursion results in a stackoverflow on
			// big networks)
			final Id seed = CollectionUtils.getElement( 0 , altersMap.keySet() );

			final Set<Id> component = new HashSet<Id>();
			components.add( component );
			component.add( seed );

			final Queue<Id> stack = Collections.asLifoQueue( new ArrayDeque<Id>( altersMap.size() ) );
			stack.add( seed );

			while ( !stack.isEmpty() ) {
				final Id current = stack.remove();
				final Set<Id> alters = altersMap.remove( current );

				for ( Id alter : alters ) {
					if ( component.add( alter ) ) {
						stack.add( alter );
					}
				}
			}

		}

		return components;
	}
}

