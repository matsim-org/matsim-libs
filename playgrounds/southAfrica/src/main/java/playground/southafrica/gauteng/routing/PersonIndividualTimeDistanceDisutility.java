/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeDistanceCostCalculator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.southafrica.gauteng.routing;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * A  travel disutility calculator respecting (only) time and distance, but taking person-individual effective marginal disutilities into account.
 *
 * @author nagel
 */
public class PersonIndividualTimeDistanceDisutility implements TravelDisutility {

	protected final TravelTime timeCalculator;

	private EffectiveMarginalUtilitiesContainer muc ;
	
	private double marginalCostOfTimeMin = 0. ;
	private double marginalCostOfDistanceMin = 0. ;

	public PersonIndividualTimeDistanceDisutility(final TravelTime timeCalculator, EffectiveMarginalUtilitiesContainer muc) {
		this.timeCalculator = timeCalculator;

		this.marginalCostOfTimeMin = - muc.getEffectiveMarginalUtilityOfTravelTimeMAX() ;
		if ( this.marginalCostOfTimeMin < 0. ) {
			throw new RuntimeException( "marginal cost of time < 0.; probably sign error somewhere ... ") ;
		}
		
		this.marginalCostOfDistanceMin = - muc.getEffectiveMarginalUtilityOfDistanceMAX() ;
		if ( this.marginalCostOfDistanceMin < 0. ) {
			throw new RuntimeException( "marginal cost of distance < 0. ; probably sign error somewhere ... ") ;
		}
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) 
	{
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);

		double marginalCostOfTime = - muc.getEffectiveMarginalUtilityOfTravelTime().get(person) ;
		if ( marginalCostOfTime < 0. ) {
			throw new RuntimeException("marginalCostOfTime < 0; probably a sign error somewhere") ;
		}
		double marginalCostOfDistance = - muc.getMarginalUtilityOfDistance().get(person) ;
		if ( marginalCostOfDistance < 0. ) {
			throw new RuntimeException("marginalCostOfDistance < 0; probably a sign error somewhere") ;
		}

		return marginalCostOfTime * travelTime + marginalCostOfDistance * link.getLength();
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		// Person is not available here, so we cannot make it person dependent.  However, this may be used for the router preprocessing,
		// in which case a dependence on person-specific attributes does not make sense anyways. kai, nov'13

		double travelTime = link.getLength() / link.getFreespeed() ;

		return this.marginalCostOfTimeMin * travelTime + this.marginalCostOfDistanceMin * link.getLength();
	}

}
