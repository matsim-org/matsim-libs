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
package playground.thibautd.socnetsim.framework.population;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.utils.misc.Counter;

/**
 * @author thibautd
 */
public class SocialNetworkImpl implements SocialNetwork {
	private static final Logger log =
		Logger.getLogger(SocialNetworkImpl.class);

	private final Counter tieCounter = new Counter( "SocialNetwork: (Monodirectional) Tie # " );
	private final Map<Id<Person>, Set<Id<Person>>> map = new HashMap< >();

	private final boolean isReflective;

	private final Map<String, String> metadata = new LinkedHashMap<String, String>();
	private long nTies = 0;

	public SocialNetworkImpl() {
		this( true );
	}

	public SocialNetworkImpl(final SocialNetwork socialNetwork) {
		this( socialNetwork.isReflective() );
		for ( Id<Person> ego : socialNetwork.getEgos() ) {
			for ( Id<Person> alter : socialNetwork.getAlters( ego ) ) {
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
		log.info( "initialize social network as "+(isReflective ? "reflective" : "non reflective" ) );
		this.isReflective = isReflective;
	}

	@Override
	public void addEgo(final Id id) {
		final Set<Id<Person>> alters = map.put( id , new HashSet<Id<Person>>() );
		if ( alters != null ) {
			throw new IllegalStateException( "network already contains ego "+id );
		}
	}

	@Override
	public void addEgos( final Iterable<? extends Id<Person>> ids ) {
		for ( Id id : ids ) addEgo( id );
	}

	@Override
	public void addBidirectionalTie(final Id id1, final Id id2) {
		addTieInternal( id1 , id2 );
		addTieInternal( id2 , id1 );
	}

	public void removeBidirectionalTie(final Id id1, final Id id2) {
		removeTieInternal( id1 , id2 );
		removeTieInternal( id2 , id1 );
	}

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
		final Set<Id<Person>> alters = map.get( ego );
		if ( alters == null ) throw new IllegalArgumentException(  "ego "+ego+" unknown" );
		final boolean added = alters.add( alter );
		if ( added ) tieCounter.incCounter();
		nTies++;
	}

	private void removeTieInternal(
			final Id ego,
			final Id alter) {
		final Set<Id<Person>> alters = map.get( ego );
		if ( alters == null ) throw new IllegalArgumentException(  "ego "+ego+" unknown" );
		final boolean rem = alters.remove( alter );
		if ( !rem ) throw new RuntimeException( alter+" not alter of ego "+ego );
		nTies--;
	}

	@Override
	public Set<Id<Person>> getAlters(final Id ego) {
		final Set<Id<Person>> alters = map.get( ego );
		if ( alters == null ) throw new IllegalArgumentException( "unknown ego "+ego );
		return Collections.unmodifiableSet( alters );
	}

	@Override
	public Set<Id<Person>> getEgos() {
		return Collections.unmodifiableSet( map.keySet() );
	}

	@Override
	public Map<Id<Person>, Set<Id<Person>>> getMapRepresentation() {
		return Collections.unmodifiableMap( map );
	}

	/* (non-Javadoc)
	 * @see playground.thibautd.socnetsim.jointtrips.population.SocialNetworkI#isReflective()
	 */
	@Override
	public boolean isReflective() {
		return isReflective;
	}

	@Override
	public Map<String, String> getMetadata() {
		return metadata;
	}

	@Override
	public void addMetadata(final String attribute, final String value) {
		final String old = metadata.put( attribute , value );
		if ( old != null ) log.warn( "replacing metadata \""+attribute+"\" from \""+old+"\" to \""+value+"\"" );
	}

	public void removeEgo(final Id ego) {
		if ( !isReflective() ) throw new IllegalStateException( "Cannot remove an ego in a non reflective network." );

		final Set<Id<Person>> alters = map.remove( ego );
		nTies -= alters.size();

		// this requires the network to be reflective to be correct.
		for ( Id<Person> alter : alters ) {
			final Set<Id<Person>> altersOfAlter = map.get( alter );
			if ( altersOfAlter.remove( ego ) ) nTies--;
		}
	}

	public long nTies() {
		return nTies;
	}
}

