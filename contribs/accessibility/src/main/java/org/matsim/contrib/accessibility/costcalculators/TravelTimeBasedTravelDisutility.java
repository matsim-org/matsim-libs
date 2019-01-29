/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,     *
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

/**
 * 
 */
package org.matsim.contrib.accessibility.costcalculators;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * A simple cost calculator which only respects time to calculate generalized costs
 * This is based on org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility 
 *
 * @author mrieser, thomas
 */
public class TravelTimeBasedTravelDisutility implements TravelDisutility{
	
	protected final TravelTime timeCalculator;
	private final double marginalCostOfTime;

	public TravelTimeBasedTravelDisutility(final TravelTime timeCalculator, PlanCalcScoreConfigGroup cnScoringGroup) {
		this.timeCalculator = timeCalculator;
		/* Usually, the travel-utility should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 */
		this.marginalCostOfTime = (-cnScoringGroup.getModes().get(TransportMode.car).getMarginalUtilityOfTraveling() / 3600.0) + (cnScoringGroup.getPerforming_utils_hr() / 3600.0);
	}

	@Override
	public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle) {
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time, person, vehicle);

		return this.marginalCostOfTime * travelTime;
	}

	@Override
	public double getLinkMinimumTravelDisutility(final Link link) {
		return (link.getLength() / link.getFreespeed()) * this.marginalCostOfTime;
	}

}
