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

package org.matsim.core.router.costcalculators;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelMinCost;
import org.matsim.core.router.util.TravelTime;

/**
 * A simple cost calculator which only respects time and distance to calculate generalized costs
 *
 * @author mrieser
 */
public class TravelTimeDistanceCostCalculator implements TravelMinCost, PersonalizableTravelCost {

	protected final TravelTime timeCalculator;
	private final double travelCostFactor;
	private final double marginalUtlOfDistance;

	public TravelTimeDistanceCostCalculator(final TravelTime timeCalculator, CharyparNagelScoringConfigGroup cnScoringGroup) {
		this.timeCalculator = timeCalculator;
		/* Usually, the travel-utility should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 */
		this.travelCostFactor = (- cnScoringGroup.getTraveling() / 3600.0) + (cnScoringGroup.getPerforming() / 3600.0);
		this.marginalUtlOfDistance = cnScoringGroup.getMarginalUtlOfDistanceCar();
	}

	@Override
	public double getLinkTravelCost(final Link link, final double time) {
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time);
		if (this.marginalUtlOfDistance == 0.0) {
			return travelTime * this.travelCostFactor;
		}
		return travelTime * this.travelCostFactor - this.marginalUtlOfDistance * link.getLength();
	}

	@Override
	public double getLinkMinimumTravelCost(final Link link) {
		if (this.marginalUtlOfDistance == 0.0) {
			return (link.getLength() / link.getFreespeed()) * this.travelCostFactor;
		}
		return (link.getLength() / link.getFreespeed()) * this.travelCostFactor
		- this.marginalUtlOfDistance * link.getLength();
	}

	@Override
	public void setPerson(Person person) {
		// This cost function doesn't change with persons.
	}
	
	
	
}
