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
public class SocialNetworkImpl implements SocialNetwork {
	private final Counter tieCounter = new Counter( "SocialNetwork: (Monodirectional) Tie # " );
	private final Map<Id, Set<Id>> map = new HashMap<Id, Set<Id>>();

	private final boolean isReflective;

	public SocialNetworkImpl() {
		this( true );
	}

	public SocialNetworkImpl(final SocialNetwork socialNetwork) {
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
	public SocialNetworkImpl(final boolean isReflective) {
		this.isReflective = isReflective;
	}

	/* (non-Javadoc)
	 * @see playground.thibautd.socnetsim.population.SocialNetworkI#addEgo(org.matsim.api.core.v01.Id)
	 */
	@Override
	public void addEgo(final Id id) {
		final Set<Id> alters = map.put( id , new HashSet<Id>() );
		if ( alters != null ) {
			throw new IllegalStateException( "network already contains ego "+id );
		}
	}

	/* (non-Javadoc)
	 * @see playground.thibautd.socnetsim.population.SocialNetworkI#addEgos(java.lang.Iterable)
	 */
	@Override
	public void addEgos( final Iterable<Id> ids ) {
		for ( Id id : ids ) addEgo( id );
	}

	/* (non-Javadoc)
	 * @see playground.thibautd.socnetsim.population.SocialNetworkI#addBidirectionalTie(org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id)
	 */
	@Override
	public void addBidirectionalTie(final Id id1, final Id id2) {
		addTieInternal( id1 , id2 );
		addTieInternal( id2 , id1 );
	}

	public void removeBidirectionalTie(final Id id1, final Id id2) {
		removeTieInternal( id1 , id2 );
		removeTieInternal( id2 , id1 );
	}

	/* (non-Javadoc)
	 * @see playground.thibautd.socnetsim.population.SocialNetworkI#addMonodirectionalTie(org.matsim.api.core.v01.Id, org.matsim.api.core.v01.Id)
	 */
	@Override
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

	/* (non-Javadoc)
	 * @see playground.thibautd.socnetsim.population.SocialNetworkI#getAlters(org.matsim.api.core.v01.Id)
	 */
	@Override
	public Set<Id> getAlters(final Id ego) {
		final Set<Id> alters = map.get( ego );
		return alters == null ?
			null :
			Collections.unmodifiableSet( alters );
	}

	/* (non-Javadoc)
	 * @see playground.thibautd.socnetsim.population.SocialNetworkI#getEgos()
	 */
	@Override
	public Set<Id> getEgos() {
		return Collections.unmodifiableSet( map.keySet() );
	}

	/* (non-Javadoc)
	 * @see playground.thibautd.socnetsim.population.SocialNetworkI#getMapRepresentation()
	 */
	@Override
	public Map<Id, Set<Id>> getMapRepresentation() {
		return Collections.unmodifiableMap( map );
	}

	/* (non-Javadoc)
	 * @see playground.thibautd.socnetsim.population.SocialNetworkI#isReflective()
	 */
	@Override
	public boolean isReflective() {
		return isReflective;
	}
}

