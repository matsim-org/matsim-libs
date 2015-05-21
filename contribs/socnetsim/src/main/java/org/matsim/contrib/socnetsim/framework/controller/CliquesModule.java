/* *********************************************************************** *
 * project: org.matsim.*
 * CliquesModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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
package org.matsim.contrib.socnetsim.framework.controller;

import java.util.Collection;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.controler.AbstractModule;

import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.framework.population.SocialNetworkImpl;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.FixedGroupsIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupIdentifier;

import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Scopes;

/**
 * @author thibautd
 */
public class CliquesModule extends AbstractModule {

	@Override
	public void install() {
		bind( GroupIdentifier.class )
			.toProvider( FixedGroupsIdentifier.FixedGroupsProvider.class )
			.in( Scopes.SINGLETON );
		bind( SocialNetwork.class )
			.toProvider( SocialNetworkProvider.class )
			.in( Scopes.SINGLETON );
	}

	private static class SocialNetworkProvider implements Provider<SocialNetwork> {
		final Scenario sc;
		final FixedGroupsIdentifier cliques;

		@Inject
		public SocialNetworkProvider(
				final Scenario sc,
				final GroupIdentifier groupIdentifier ) {
			this.sc = sc;
			this.cliques = (FixedGroupsIdentifier) groupIdentifier;
		}

		@Override
		public SocialNetwork get() {
			final SocialNetwork socNet = new SocialNetworkImpl();
			for ( Collection<Id<Person>> clique : cliques.getGroupInfo() ) {
				final Id[] ids = clique.toArray( new Id[ clique.size() ] );
				socNet.addEgos( clique );

				// we cannot just add monodirectional ties in a reflective social network.
				for ( int i=0; i < ids.length; i++ ) {
					for ( int j=i; j < ids.length; j++ ) {
						socNet.addBidirectionalTie( ids[ i ] , ids[ j ] );
					}
				}
			}

			sc.addScenarioElement( SocialNetwork.ELEMENT_NAME , sc );

			return socNet;
		}
	}
}

