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
package playground.thibautd.negotiation.framework;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.gbl.MatsimRandom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author thibautd
 */
public class NegotiatingAgents<P extends Proposition> implements Iterable<NegotiationAgent<P>> {
	private final Map<Id<Person>, NegotiationAgent<P>> agents = new HashMap<>();
	private final List<NegotiationAgent<P>> agentList;

	private final Random random = MatsimRandom.getLocalInstance();

	public NegotiatingAgents( final Collection<NegotiationAgent<P>> allAgents ) {
		allAgents.forEach( a -> agents.put( a.getId() , a ) );
		agentList = new ArrayList<>( allAgents );
	}

	public NegotiationAgent<P> getRandomAgent() {
		return agentList.get( random.nextInt( agentList.size() ) );
	}

	public NegotiationAgent<P> get( final Id<Person> id ) {
		return agents.get( id );
	}

	public boolean contains( final Id<Person> id ) {
		return agents.containsKey( id );
	}

	public boolean contains( final Collection<Id<Person>> ids ) {
		return ids.stream().allMatch( agents::containsKey );
	}

	public NegotiationAgent<P> remove( final Id<Person> id ) {
		return agents.remove( id );
	}

	@Override
	public Iterator<NegotiationAgent<P>> iterator() {
		return agents.values().iterator();
	}
}

