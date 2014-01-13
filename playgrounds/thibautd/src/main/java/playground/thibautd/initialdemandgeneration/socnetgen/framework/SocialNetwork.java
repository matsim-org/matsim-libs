/* *********************************************************************** *
 * project: org.matsim.*
 * SocialNetwork.java
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
package playground.thibautd.initialdemandgeneration.socnetgen.framework;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Identifiable;

/**
 * @author thibautd
 */
public class SocialNetwork {
	private final Map<Id, Set<Id>> alterEgoMap = new HashMap<Id, Set<Id>>();
	private final boolean failOnUnknownEgo;

	// mainly to simplify writing tests
	public SocialNetwork( final boolean failOnUnknownEgo ) {
		this.failOnUnknownEgo = failOnUnknownEgo;
	}

	public SocialNetwork() {
		this( true );
	}

	public SocialNetwork(final SocialPopulation<?> pop) {
		this( pop.getAgents() );
	}


	public SocialNetwork(final Iterable<? extends Identifiable> egos) {
		this();
		addEgos( egos );
	}

	public void addTie(final Id id1, final Id id2) {
		addAlter( id1 , id2 );
		addAlter( id2 , id1 );
	}

	public void addEgos(final Iterable<? extends Identifiable> egos) {
		for ( Identifiable ego : egos ) addEgo( ego.getId() );
	}

	public void addEgoIds(final Iterable<? extends Id> ids) {
		for ( Id id : ids ) addEgo( id );
	}

	public void addEgo(final Id id) {
		if ( !alterEgoMap.containsKey( id ) ) {
			alterEgoMap.put( id , new HashSet<Id>() );
		}
	}

	private void addAlter(
			final Id ego,
			final Id alter) {
		Set<Id> alters = alterEgoMap.get( ego );

		if ( alters == null ) {
			if (failOnUnknownEgo) {
				throw new IllegalArgumentException( "ego "+ego+" is unknown" );
			}
			alters = new HashSet<Id>();
			alterEgoMap.put( ego , alters );
		}

		alters.add( alter );
	}

	public Set<Id> getAlters(final Id ego) {
		final Set<Id> alters = alterEgoMap.get( ego );

		return alters != null ?
			Collections.unmodifiableSet( alters ) :
			Collections.<Id>emptySet();
	}

	public Set<Id> getEgos() {
		return Collections.unmodifiableSet( alterEgoMap.keySet() );
	}
}

