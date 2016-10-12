/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.thibautd.initialdemandgeneration.empiricalsocnet.framework;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author thibautd
 */
class SampledSocialNetwork implements SocialNetwork {
	private final Map<Id<Person>, Ego> egos;

	public SampledSocialNetwork( final Map<Id<Person>, Ego> egos ) {
		this.egos = egos;
		metadata = new HashMap<>();
	}

	@Override
	public void addEgo( final Id<Person> id ) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addEgos( final Iterable<? extends Id<Person>> ids ) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void addBidirectionalTie( final Id<Person> id1, final Id<Person> id2 ) {
		throw new UnsupportedOperationException();

	}

	@Override
	public void addMonodirectionalTie( final Id<Person> ego, final Id<Person> alter ) {
		throw new UnsupportedOperationException();

	}

	@Override
	public Set<Id<Person>> getAlters( final Id<Person> ego ) {
		return egos.get( ego ).getAlters()
				.stream()
				.map( Ego::getId )
				.collect( Collectors.toSet() );
	}

	@Override
	public Set<Id<Person>> getEgos() {
		return egos.keySet();
	}

	@Override
	public Map<Id<Person>, Set<Id<Person>>> getMapRepresentation() {
		return egos.values().stream()
				.collect( Collectors.toMap(
						Ego::getId,
						e -> e.getAlters().stream()
								.map( Ego::getId )
								.collect( Collectors.toSet() ) ) );
	}

	@Override
	public boolean isReflective() {
		return true;
	}

	private final Map<String, String> metadata;

	@Override
	public Map<String, String> getMetadata() {
		return metadata;
	}

	@Override
	public void addMetadata( final String att, final String value ) {
		metadata.put( att, value );
	}
}
