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
import org.apache.commons.math3.util.Combinations;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.Counter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.thibautd.negotiation.framework.AlternativesGenerator;
import playground.thibautd.negotiation.framework.NegotiationAgent;
import playground.thibautd.utils.RandomUtils;
import playground.thibautd.utils.spatialcollections.VPTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/**
 * @author thibautd
 */
@Singleton
public class LocationAlternativesGenerator implements AlternativesGenerator<LocationProposition> {
	private static final Logger log = Logger.getLogger( LocationAlternativesGenerator.class );
	
	private final SocialNetwork socialNetwork;
	private final Population population;
	private final LocationHelper locations;
	private final LocationAlternativesConfigGroup configGroup;
	private final VPTree<Coord,ActivityFacility> facilities;
	private final RandomSeedHelper seeds;

	@Inject
	public LocationAlternativesGenerator(
			final SocialNetwork socialNetwork,
			final Population population,
			final ActivityFacilities facilities,
			final LocationHelper locations,
			final LocationAlternativesConfigGroup configGroup,
			final RandomSeedHelper seeds ) {
		this.socialNetwork = socialNetwork;
		this.population = population;
		this.locations = locations;
		this.configGroup = configGroup;
		this.seeds = seeds;

		this.facilities =
				new VPTree<>(
						CoordUtils::calcEuclideanDistance,
						ActivityFacility::getCoord );
		this.facilities.add(
				facilities.getFacilities().values().stream()
					.filter( f -> f.getActivityOptions().containsKey( configGroup.getActivityType() ) )
					.collect( Collectors.toList() ));
	}

	@Override
	public Collection<LocationProposition> generateAlternatives( final NegotiationAgent<LocationProposition> agent ) {
		final Collection<Person> alters =
				socialNetwork.getAlters( agent.getId() ).stream()
						.map( population.getPersons()::get )
						.collect( Collectors.toList() );
		final Person ego = population.getPersons().get( agent.getId() );

		final Collection<LocationProposition> propositions =
				new ArrayList<>(
						2 * alters.size() + // visits
								1 + // alone at home
								configGroup.getnOutOfHomeAlternatives() * (alters.size() + 1) ); // out of home

		if ( log.isTraceEnabled() ) log.trace( alters.size()+" alters" );

		// visits
		log.trace( "generate visits" );
		final Counter visitCounter = new Counter( "visit # ");
		for ( Collection<Person> group : getGroups( alters ) ) {
			if ( log.isTraceEnabled() ) visitCounter.incCounter();
			propositions.add(
					LocationProposition.create(
							ego ,
							group,
							locations.getHomeLocation( ego ),
							LocationProposition.Type.visit ) );
		}
		if ( log.isTraceEnabled() ) visitCounter.printCounter();

		// alone at home
		log.trace( "generate alone at home" );
		propositions.add(
				LocationProposition.create(
						ego ,
						Collections.emptyList(),
						locations.getHomeLocation( ego ),
						LocationProposition.Type.alone ) );

		// out-of-home locations
		log.trace( "generate out of home" );
		propositions.addAll( generateOutOfHome( ego, alters ) );

		return propositions;
	}

	private Collection<LocationProposition> generateOutOfHome(
			final Person ego,
			final Collection<Person> alters ) {
		log.trace( "    sample locations" );
		final Coord home = locations.getHomeLocation( ego.getId() ).getCoord();
		final Collection<ActivityFacility> close =
				facilities.getBall(
						home,
						configGroup.getMaxOutOfHomeRadius_km() * 1000 );

		// always select the same "awareness set"
		// assumes the VP tree returns elements in ball in deterministic order (it should)
		final List<ActivityFacility> subsample = RandomUtils.sublist_withSideEffect(
				new Random( seeds.getSeed( ego )),
				(List<ActivityFacility>) close,
				//new ArrayList<>( close ),
				configGroup.getnOutOfHomeAlternatives() );


		log.trace( "    generate out of home with friends" );
		final List<LocationProposition> propositions = new ArrayList<>();
		// with friends
		subsample.stream()
				.flatMap( facility ->
						getGroupStream( alters )
								.map( group ->
										LocationProposition.create(
												ego,
												group,
												facility,
												LocationProposition.Type.outOfHome ) ) )
				.forEach( propositions::add );

		// alone
		log.trace( "    generate out of home alone" );
		subsample.stream()
				.map( facility ->
						LocationProposition.create(
								ego,
								Collections.emptyList(),
								facility,
								LocationProposition.Type.outOfHome ) )
				.forEach( propositions::add );

		return propositions;
	}

	private Iterable<Collection<Person>> getGroups( final Collection<Person> alters ) {
		return () -> new AltersGroupIterator( alters , configGroup.getMaxGroupSize() - 1 );
	}

	private Stream<Collection<Person>> getGroupStream( final Collection<Person> alters ) {
		return StreamSupport.stream( getGroups( alters ).spliterator() , false );
	}

	private class AltersGroupIterator implements Iterator<Collection<Person>> {
		private final Person[] alters;

		private Iterator<int[]> combinations = null;
		private final int maxSize;
		private int currSize = 0;

		private List<Person> currentCombination;

		private AltersGroupIterator( final Collection<Person> alters , final int maxSize ) {
			this.alters = alters.toArray( new Person[ alters.size() ] );
			this.maxSize = Math.min( alters.size() , maxSize );
			nextCombinations();
		}

		private void nextCombinations() {
			do {
				if ( combinations == null || !combinations.hasNext() ) {
					currSize++;
					if ( currSize > maxSize ) {
						combinations = null;
						currentCombination = null;
						return;
					}

					combinations = new Combinations( alters.length, currSize ).iterator();
				}

				this.currentCombination = new ArrayList<>( currSize );
				for ( int i : combinations.next() ) currentCombination.add( alters[ i ] );
			} while ( !isClique() );
		}

		private boolean isClique() {
			for ( int i = 0; i < currentCombination.size(); i++ ) {
				final Id<Person> ego = currentCombination.get( i ).getId();
				for ( int j = i + 1; j < currentCombination.size(); j++ ) {
					final Id<Person> alter = currentCombination.get( j ).getId();
					if ( !socialNetwork.getAlters( ego ).contains( alter ) ) return false;
				}
			}

			return true;
		}

		@Override
		public boolean hasNext() {
			return currentCombination != null;
		}

		@Override
		public Collection<Person> next() {
			final Collection<Person> val = currentCombination;
			nextCombinations();
			return val;
		}
	}

}
