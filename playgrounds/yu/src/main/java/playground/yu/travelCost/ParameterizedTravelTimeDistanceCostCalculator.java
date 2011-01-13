/* *********************************************************************** *
 * project: org.matsim.*
 * ParameterizedTravelTimeDistanceCostCalculator.java
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

package playground.yu.travelCost;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;

/**
 * A simple cost calculator which only respects time and distance to calculate
 * generalized costs
 * 
 * @author mrieser
 */
public class ParameterizedTravelTimeDistanceCostCalculator implements
		TravelMinCost, PersonalizableTravelCost {

	protected final TravelTime timeCalculator;
	private final double travelCostFactor;
	private final double marginalUtlOfDistance;

	public ParameterizedTravelTimeDistanceCostCalculator(
			final TravelTime timeCalculator,
			CharyparNagelScoringConfigGroup cnScoringGroup, double A, double B) {
		this.timeCalculator = timeCalculator;
		/*
		 * Usually, the travel-utility should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 */
		travelCostFactor = (-cnScoringGroup.getTraveling_utils_hr() / 3600.0 + cnScoringGroup
				.getPerforming_utils_hr() / 3600.0)
				* A;

		// this.marginalUtlOfDistance =
		// cnScoringGroup.getMarginalUtlOfDistanceCar();
		marginalUtlOfDistance = cnScoringGroup
				.getMonetaryDistanceCostRateCar()
				* cnScoringGroup.getMarginalUtilityOfMoney() * B;

	}

	@Override
	public double getLinkGeneralizedTravelCost(final Link link,
			final double time) {
		double travelTime = timeCalculator.getLinkTravelTime(link, time);
		if (marginalUtlOfDistance == 0.0) {
			return travelTime * travelCostFactor;
		}
		return travelTime * travelCostFactor - marginalUtlOfDistance
				* link.getLength();
	}

	@Override
	public double getLinkMinimumTravelCost(final Link link) {
		if (marginalUtlOfDistance == 0.0) {
			return link.getLength() / link.getFreespeed()
					* travelCostFactor;
		}
		return link.getLength() / link.getFreespeed() * travelCostFactor
				- marginalUtlOfDistance * link.getLength();
	}

	@Override
	public void setPerson(Person person) {
		// This cost function doesn't change with persons.
	}

}
