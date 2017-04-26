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
import org.matsim.api.core.v01.Customizable;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacility;
import org.matsim.utils.objectattributes.attributable.Attributable;
import org.matsim.utils.objectattributes.attributable.Attributes;
import playground.thibautd.negotiation.framework.NegotiationAgent;
import playground.thibautd.negotiation.framework.PropositionUtility;

import java.util.Collection;

/**
 * @author thibautd
 */
@Singleton
public class LocationUtility implements PropositionUtility<LocationProposition> {
	private final RandomSeedHelper seeds;
	private final LocationHelper locations;
	private final LocationUtilityConfigGroup configGroup;
	private final Population population;

	@Inject
	public LocationUtility( final RandomSeedHelper seeds,
			final LocationHelper locations,
			final LocationUtilityConfigGroup configGroup,
			final Population population ) {
		this.seeds = seeds;
		this.locations = locations;
		this.configGroup = configGroup;
		this.population = population;
	}

	@Override
	public double utility(
			final NegotiationAgent<LocationProposition> agent,
			final LocationProposition proposition ) {
		if ( proposition == null ) return Double.NEGATIVE_INFINITY;
		final Person ego = population.getPersons().get( agent.getId() );
		final Collection<Person> alters = proposition.getGroup();

		final ActivityFacility location = proposition.getFacility();

		final double sumOfAlterUtils =
				alters.stream()
						.filter( a -> !a.getId().equals( agent.getId() ) )
						.mapToDouble( a -> configGroup.getFixedUtilContact() + contactErrorTerm( ego, a ) )
						.sum();

		final double utilLocation = locationErrorTerm( ego, location );

		final double utilTravelTime = getTravelDistance( ego, location ) * configGroup.getBetaDistance();

		return sumOfAlterUtils + utilLocation + utilTravelTime;
	}

	private double locationErrorTerm( final Person ego, final ActivityFacility location ) {
		switch ( configGroup.getFacilityErrorTermDistribution() ) {
			case normal:
				return seeds.getGaussianErrorTerm( ego, asAttr( location ) ) * configGroup.getSigmaFacility();
			case uniform:
				return seeds.getUniformErrorTerm( ego, asAttr( location ) ) * configGroup.getSigmaFacility();
		}
		throw new RuntimeException();
	}

	private double contactErrorTerm( final Person ego, final Person a ) {
		switch ( configGroup.getContactErrorTermDistribution() ) {
			case normal:
				return seeds.getGaussianErrorTerm( a, ego ) * configGroup.getMuContact();
			case uniform:
				return seeds.getUniformErrorTerm( a, ego ) * configGroup.getMuContact();
		}
		throw new RuntimeException();
	}

	// TODO: make facilities actually implement attributable!
	private Attributable asAttr( final Customizable facility ) {
		if ( !facility.getCustomAttributes().containsKey( "attributes" ) ) {
			facility.getCustomAttributes().put( "attributes" , new Attributes() );
		}
		return () -> (Attributes) facility.getCustomAttributes().get( "attributes" );
	}

	private double getTravelDistance( final Person ego, final ActivityFacility location ) {
		switch ( configGroup.getTravelTimeType() ) {
			case crowFly:
				final Coord homeCoord = locations.getHomeLocation( ego ).getCoord();
				return CoordUtils.calcEuclideanDistance( homeCoord , location.getCoord() );
		}
		throw new RuntimeException( );
	}
}
