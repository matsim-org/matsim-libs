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
package playground.thibautd.socnetsim.population;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Id;
import org.matsim.core.utils.misc.Counter;

/**
 * @author thibautd
 */
public class SocialNetwork {
	public static final String ELEMENT_NAME = "SocialNetwork";
	private final Counter tieCounter = new Counter( "SocialNetwork: (Monodirectional) Tie # " );
	private final Map<Id, Set<Id>> map = new HashMap<Id, Set<Id>>();

	private final boolean isReflective;

	public SocialNetwork() {
		this( true );
	}

	public SocialNetwork(final SocialNetwork socialNetwork) {
		this( socialNetwork.isReflective() );
		for ( Id ego : socialNetwork.getEgos() ) {
			for ( Id alter : socialNetwork.getAlters( ego ) ) {
				addTieInternal( ego , alter );
			}
		}
	}

	/**
	 * @param isReflective
	 * indicates whether social ties are directed.
	 * tagging a social network as reflective allows to reduce
	 * the size of the output by half.
	 */
	public SocialNetwork(final boolean isReflective) {
		this.isReflective = isReflective;
	}

	/**
	 * Must be called before adding ties.
	 * This allows to make sure that isolated agents (without social contacts)
	 * are identified as such in the network.
	 * This is necessary for analysing the topology of the network, but also
	 * for more safety in simulation (fail if the social network does not cover the
	 * whole population).
	 */
	public void addEgo(final Id id) {
		final Set<Id> alters = map.put( id , new HashSet<Id>() );
		if ( alters != null ) {
			throw new IllegalStateException( "network already contains ego "+id );
		}
	}

	public void addEgos( final Iterable<Id> ids ) {
		for ( Id id : ids ) addEgo( id );
	}

	public void addBidirectionalTie(final Id id1, final Id id2) {
		addTieInternal( id1 , id2 );
		addTieInternal( id2 , id1 );
	}

	public void removeBidirectionalTie(final Id id1, final Id id2) {
		removeTieInternal( id1 , id2 );
		removeTieInternal( id2 , id1 );
	}

	public void addMonodirectionalTie(
			final Id ego,
			final Id alter) {
		if ( isReflective ) {
			throw new IllegalStateException( "cannot add a monodirectional tie to a reflective social network." );
		}
		addTieInternal( ego , alter );
	}

	public void removeMonodirectionalTie(
			final Id ego,
			final Id alter) {
		if ( isReflective ) {
			throw new IllegalStateException( "cannot remove a monodirectional tie to a reflective social network." );
		}
		removeTieInternal( ego , alter );
	}

	private void addTieInternal(
			final Id ego,
			final Id alter) {
		final Set<Id> alters = map.get( ego );
		if ( alters == null ) throw new IllegalArgumentException(  "ego "+ego+" unknown" );
		final boolean added = alters.add( alter );
		if ( added ) tieCounter.incCounter();
	}

	private void removeTieInternal(
			final Id ego,
			final Id alter) {
		final Set<Id> alters = map.get( ego );
		if ( alters == null ) throw new IllegalArgumentException(  "ego "+ego+" unknown" );
		final boolean rem = alters.remove( alter );
		if ( !rem ) throw new RuntimeException( alter+" not alter of ego "+ego );
	}

	public Set<Id> getAlters(final Id ego) {
		final Set<Id> alters = map.get( ego );
		return alters == null ?
			null :
			Collections.unmodifiableSet( alters );
	}

	public Set<Id> getEgos() {
		return Collections.unmodifiableSet( map.keySet() );
	}

	public Map<Id, Set<Id>> getMapRepresentation() {
		return Collections.unmodifiableMap( map );
	}

	public boolean isReflective() {
		return isReflective;
	}
}

