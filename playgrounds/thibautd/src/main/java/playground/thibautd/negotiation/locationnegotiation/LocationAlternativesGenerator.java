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
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import playground.thibautd.negotiation.framework.AlternativesGenerator;
import playground.thibautd.negotiation.framework.NegotiationAgent;
import playground.thibautd.utils.RandomUtils;
import playground.thibautd.utils.spatialcollections.VPTree;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

/**
 * @author thibautd
 */
@Singleton
public class LocationAlternativesGenerator implements AlternativesGenerator<LocationProposition> {
	private final SocialNetwork socialNetwork;
	private final Population population;
	private final LocationHelper locations;
	private final LocationAlternativesConfigGroup configGroup;
	private final VPTree<Coord,ActivityFacility> facilities;

	private final Random random = MatsimRandom.getLocalInstance();

	@Inject
	public LocationAlternativesGenerator(
			final SocialNetwork socialNetwork,
			final Population population,
			final ActivityFacilities facilities,
			final LocationHelper locations,
			final LocationAlternativesConfigGroup configGroup ) {
		this.socialNetwork = socialNetwork;
		this.population = population;
		this.locations = locations;
		this.configGroup = configGroup;

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
								configGroup.getnOutOfHomeAlternatives() * alters.size() ); // out of home
		// visits
		for ( Person alter : alters ) {
			propositions.add(
					LocationProposition.create(
							ego ,
							Collections.singleton( alter ) ,
							locations.getHomeLocation( ego ),
							LocationProposition.Type.visit ) );
			propositions.add(
					LocationProposition.create(
							ego ,
							Collections.singleton( alter ) ,
							locations.getHomeLocation( alter ),
							LocationProposition.Type.visit ) );
		}

		// alone at home
		propositions.add(
				LocationProposition.create(
						ego ,
						Collections.emptyList(),
						locations.getHomeLocation( ego ),
						LocationProposition.Type.alone ) );

		// out-of-home locations
		propositions.addAll( generateOutOfHome( ego, alters ) );

		return propositions;
	}

	private Collection<LocationProposition> generateOutOfHome(
			final Person ego,
			final Collection<Person> alters ) {
		final Coord home = locations.getHomeLocation( ego.getId() ).getCoord();
		final Collection<ActivityFacility> close =
				facilities.getBall(
						home,
						configGroup.getMaxOutOfHomeRadius_km() * 1000 );

		// no need to be too smart (e.g. only select a few of the best facilities):
		// being smart requires a lot of utility computations here, plus in the negotiator.
		// being dumb but returning more options leads to those options only being evaluated
		// once.
		final List<ActivityFacility> subsample = RandomUtils.sublist_withSideEffect(
				random,
				(List<ActivityFacility>) close,
				configGroup.getnOutOfHomeAlternatives() );

		return subsample.stream()
				.flatMap( facility ->
						alters.stream().map( alter ->
								LocationProposition.create(
										ego,
										Collections.singleton( alter ),
										facility,
										LocationProposition.Type.outOfHome ) ) )
				.collect( Collectors.toList() );
	}

}
