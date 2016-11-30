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
package playground.thibautd.negotiation.locationnegotiation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.contrib.socnetsim.utils.QuadTreeRebuilder;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.thibautd.negotiation.framework.AlternativesGenerator;
import playground.thibautd.negotiation.framework.NegotiationAgent;
import playground.thibautd.negotiation.framework.PropositionUtility;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.function.DoubleFunction;
import java.util.function.ToDoubleFunction;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * @author thibautd
 */
@Singleton
public class LocationAlternativesGenerator implements AlternativesGenerator<LocationProposition> {
	private static final int N_OUT_OF_HOME = 100;
	private static final double OUT_OF_HOME_RADIUS_M = 30 * 1000;

	private final SocialNetwork socialNetwork;
	private final Population population;
	private final RandomSeedHelper seeds;
	private final LocationHelper locations;
	private final PropositionUtility<LocationProposition> utility;
	private final QuadTree<ActivityFacility> facilities;

	private final Random random = MatsimRandom.getLocalInstance();

	@Inject
	public LocationAlternativesGenerator(
			final SocialNetwork socialNetwork,
			final Population population,
			final ActivityFacilities facilities,
			final RandomSeedHelper seeds,
			final LocationHelper locations,
			final PropositionUtility<LocationProposition> utility ) {
		this.socialNetwork = socialNetwork;
		this.population = population;
		this.seeds = seeds;
		this.locations = locations;
		this.utility = utility;

		final QuadTreeRebuilder<ActivityFacility> qt = new QuadTreeRebuilder<>();
		// TODO: filter here, or outside, by activity type?
		facilities.getFacilities().values().forEach( f -> qt.put( f.getCoord() , f ) );
		this.facilities = qt.getQuadTree();
	}

	@Override
	public Collection<LocationProposition> generateAlternatives( final NegotiationAgent<LocationProposition> agent ) {
		final Collection<Id<Person>> alterIds = socialNetwork.getAlters( agent.getId() );
		final Person ego = population.getPersons().get( agent.getId() );

		final Collection<LocationProposition> propositions =
				new ArrayList<>(
						2 * alterIds.size() + // visits
								1 + // alone at home
								N_OUT_OF_HOME * alterIds.size() ); // out of home
		// visits
		for ( Id<Person> alter : alterIds ) {
			propositions.add(
					new LocationProposition(
							ego.getId() ,
							Collections.singleton( alter ) ,
							locations.getHomeLocation( ego ) ) );
			propositions.add(
					new LocationProposition(
							ego.getId() ,
							Collections.singleton( alter ) ,
							locations.getHomeLocation( population.getPersons().get( alter ) ) ) );
		}

		// alone at home
		propositions.add(
				new LocationProposition(
						ego.getId() ,
						Collections.emptyList(),
						locations.getHomeLocation( ego ) ) );

		// out-of-home locations
		propositions.addAll( generateOutOfHome( agent , alterIds ) );

		return propositions;
	}

	private Collection<LocationProposition> generateOutOfHome(
			final NegotiationAgent<LocationProposition> agent,
			final Collection<Id<Person>> alters ) {
		final Coord home = locations.getHomeLocation( agent.getId() ).getCoord();
		final Collection<ActivityFacility> close = facilities.getDisk( home.getX() , home.getY() , OUT_OF_HOME_RADIUS_M );

		// no need to be too smart (e.g. only select a few of the best facilities):
		// being smart requires a lot of utility computations here, plus in the negotiator.
		// being dumb but returning more options leads to those options only being evaluated
		// once.
		final List<ActivityFacility> subsample= RandomUtils.sublist_withSideEffect(
				random,
				(List<ActivityFacility>) close,
				N_OUT_OF_HOME );

		return subsample.stream()
				.flatMap( facility ->
						alters.stream().map( alter ->
								new LocationProposition(
										agent.getId(),
										Collections.singleton( alter ),
										facility ) ) )
				.collect( Collectors.toList() );

	}

}
