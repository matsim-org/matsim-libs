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

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkReader;

import java.util.Map;

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
		return convertToNetwork( sn , null );
	}

	public static Network convertToNetwork(final SocialNetwork sn, final Map<Id<Person>, Coord> agentCoords) {
		final Network net = NetworkImpl.createNetwork();
	
		final Coord dummyCoord = new CoordImpl( 0 , 0 );
		for ( Id ego : sn.getEgos() ) {
			final Coord coord = agentCoords == null ? dummyCoord : agentCoords.get( ego );
			net.addNode(
					net.getFactory().createNode(
						ego,
						coord ) );

		}
	
		for ( Id ego : sn.getEgos() ) {
			final Iterable<Id<Person>> alters = sn.getAlters( ego );
			for ( Id<Person> alter : alters ) {
				net.addLink(
						net.getFactory().createLink(
							Id.create( ego+"---"+alter, Link.class ),
							net.getNodes().get( ego ),
							net.getNodes().get( alter ) ) );
			}
		}
	
		return net;
	}
}

