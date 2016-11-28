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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.socnetsim.framework.population.SocialNetwork;
import org.matsim.facilities.ActivityFacility;
import playground.thibautd.negotiation.framework.AlternativesGenerator;
import playground.thibautd.negotiation.framework.NegotiationAgent;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author thibautd
 */
@Singleton
public class LocationAlternativesGenerator implements AlternativesGenerator<LocationProposition> {
	private final SocialNetwork socialNetwork;
	private final Population population;
	private final RandomSeedHelper seeds;
	private final LocationHelper locations;

	@Inject
	public LocationAlternativesGenerator(
			final SocialNetwork socialNetwork,
			final Population population,
			final RandomSeedHelper seeds,
			final LocationHelper locations ) {
		this.socialNetwork = socialNetwork;
		this.population = population;
		this.seeds = seeds;
		this.locations = locations;
	}

	@Override
	public Collection<LocationProposition> generateAlternatives( final NegotiationAgent<LocationProposition> agent ) {
		final Collection<Id<Person>> alterIds = socialNetwork.getAlters( agent.getId() );
		final Person ego = population.getPersons().get( agent.getId() );

		final Collection<LocationProposition> propositions = new ArrayList<>( 2 * alterIds.size() );
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

		// TODO sample possible out-of-home locations

		return propositions;
	}

}
