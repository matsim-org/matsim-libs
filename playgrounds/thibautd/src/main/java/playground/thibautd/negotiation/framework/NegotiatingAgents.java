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

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.core.gbl.MatsimRandom;
import playground.ivt.utils.ConcurrentStopWatch;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Random;

/**
 * @author thibautd
 */
@Singleton
public class NegotiatingAgents<P extends Proposition> implements Iterable<NegotiationAgent<P>> {
	private final ConcurrentStopWatch<StopWatchMeasurement> stopwatch;

	private static final Logger log = Logger.getLogger( NegotiatingAgents.class );
	private final Map<Id<Person>, NegotiationAgent<P>> agents = new HashMap<>();

	private final List<NegotiationAgent<P>> agentList;
	private int currentIndex = -1;

	private final Random random = MatsimRandom.getLocalInstance();

	private final PropositionUtility<P> utility;
	private final AlternativesGenerator<P> alternativesGenerator;

	@Inject
	public NegotiatingAgents(
			final SocialNetwork socialNetwork,
			final Population population,
			final ConcurrentStopWatch<StopWatchMeasurement> stopwatch,
			final PropositionUtility<P> utility,
			final AlternativesGenerator<P> alternativesGenerator ) {
		agentList = new ArrayList<>( socialNetwork.getEgos().size() );
		this.stopwatch = stopwatch;
		this.utility = utility;
		this.alternativesGenerator = alternativesGenerator;

		socialNetwork.getEgos().stream()
				.map( population.getPersons()::get )
				.map( p -> new NegotiationAgent<>( p.getId() , this, this.stopwatch ) )
				.forEach( a -> {
					agentList.add( a );
					agents.put( a.getId() , a );
				} );

		log.info( "Negotiating Agents initialized with "+agentList.size()
				+" agents from population "+population.getPersons().size()
				+" with social network with "+socialNetwork.getEgos().size() );
	}

	public NegotiationAgent<P> getRandomAgent() {
		// repeat sampling without replacement as often as necessary.
		// ensures "fairness" in the number of times each agent is called, not only on average
		currentIndex++;
		currentIndex %= agentList.size();
		if ( currentIndex == 0 ) Collections.shuffle( agentList , random );
		return agentList.get( currentIndex );
	}

	public Collection<NegotiationAgent<P>> getAllAgents() {
		return agentList;
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

	public PropositionUtility<P> getUtility() {
		return utility;
	}

	public AlternativesGenerator<P> getAlternativesGenerator() {
		return alternativesGenerator;
	}
}

