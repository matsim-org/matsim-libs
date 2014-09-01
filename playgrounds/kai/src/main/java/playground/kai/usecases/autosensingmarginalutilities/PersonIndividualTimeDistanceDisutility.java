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

package playground.kai.usecases.autosensingmarginalutilities;

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

	private final UtilityOfTtimeI uot ;
	private final UtilityOfDistanceI uod ;
	
	private double marginalCostOfTimeMin = 0. ;
	private double marginalCostOfDistanceMin = 0. ;
	
	private Person prevPerson = null ;

	private double marginalCostOfTime;

	private double marginalCostOfDistance;

	public PersonIndividualTimeDistanceDisutility(final TravelTime timeCalculator, final UtilityOfTtimeI uot, final UtilityOfDistanceI uod ) {
		this.timeCalculator = timeCalculator;
		this.uot = uot ;
		this.uod = uod ;

		this.marginalCostOfTimeMin = - uot.getEffectiveMarginalUtilityOfTtimeMAX() ;
		if ( this.marginalCostOfTimeMin < 0. ) {
			throw new RuntimeException( "marginal cost of time < 0.; probably sign error somewhere ... ") ;
		}
		
		this.marginalCostOfDistanceMin = - uod.getMarginalUtilityOfDistanceMAX() ;
		if ( this.marginalCostOfDistanceMin < 0. ) {
			throw new RuntimeException( "marginal cost of distance < 0. ; probably sign error somewhere ... ") ;
		}
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) 
	{
		if ( person != prevPerson ) {
			prevPerson = person ;
			this.marginalCostOfTime = - uot.getEffectiveMarginalUtilityOfTtime(person.getId()) ;
			if ( this.marginalCostOfTime < 0. ) {
				throw new RuntimeException("marginalCostOfTime < 0; probably a sign error somewhere") ;
			}
			this.marginalCostOfDistance = - uod.getMarginalUtilityOfDistance(person.getId()) ;
			if ( this.marginalCostOfDistance < 0. ) {
				throw new RuntimeException("marginalCostOfDistance < 0; probably a sign error somewhere") ;
			}

		}
		
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);

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
