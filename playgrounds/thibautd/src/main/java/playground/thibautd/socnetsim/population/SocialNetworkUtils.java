/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetworkUtils.java
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
package playground.thibautd.socnetsim.population;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Counter;

import playground.thibautd.socnetsim.population.SocialNetworkUtils;
import playground.thibautd.utils.CollectionUtils;

/**
 * @author thibautd
 */
public final class SocialNetworkUtils {
	private SocialNetworkUtils() {}

	public static SocialNetwork createNetworkOfUnknownFriendsOfFriends( final SocialNetwork socialNetwork ) {
		final Counter counter = new Counter( "search secondary friends of agent # ");
		final SocialNetwork secondaryNetwork = new SocialNetworkImpl( );

		for ( Id ego : socialNetwork.getEgos() ) {
			final Set<Id> alters = socialNetwork.getAlters( ego );
			counter.incCounter();

			for ( Id alter : alters ) {
				final Set<Id> altersOfAlter = socialNetwork.getAlters( alter );
				
				for ( Id alterOfAlter : altersOfAlter ) {
					// is the ego?
					if ( alterOfAlter.equals( ego ) ) continue;
					// already a friend?
					if ( alters.contains( alterOfAlter ) ) continue;

					secondaryNetwork.addBidirectionalTie( ego , alterOfAlter );
				}
			}
		}
		counter.printCounter();

		return secondaryNetwork;
	}

	public static Map<Id, Set<Id>> getSubnetwork(
			final SocialNetwork network,
			final Set<Id> egos ) {
		final Map<Id, Set<Id>> subnet = new LinkedHashMap<Id, Set<Id>>();

		for ( Id ego : egos ) {
			final Set<Id> alters = network.getAlters( ego );
			subnet.put( ego , CollectionUtils.intersectSorted( egos , alters ) );
		}

		return subnet;
	}
}

