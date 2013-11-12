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

package playground.kai.conceptual.autosensingmargutls;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * A simple cost calculator which only respects time and distance to calculate generalized costs
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


	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) 
	{
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);

		double marginalCostOfTime = muc.getEffectiveMarginalUtilityOfTravelTime().get(person) ;
		double marginalCostOfDistance = muc.getMarginalUtilityOfDistance().get(person) ;
		return marginalCostOfTime * travelTime + marginalCostOfDistance * link.getLength();
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {

		return (link.getLength() / link.getFreespeed()) * this.marginalCostOfTimeMin
				+ this.marginalCostOfDistanceMin * link.getLength();
	}

}
