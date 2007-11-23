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

package org.matsim.router.costcalculators;

import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.router.util.TravelMinCostI;
import org.matsim.router.util.TravelTimeI;

/**
 * A simple cost calculator which only respects time and distance to calculate generalized costs
 *
 * @author mrieser
 */
public class TravelTimeDistanceCostCalculator implements TravelMinCostI {

	protected final TravelTimeI timeCalculator;
	private final double travelCostFactor;
	private final double distanceCost;

	public TravelTimeDistanceCostCalculator(final TravelTimeI timeCalculator) {
		this.timeCalculator = timeCalculator;
		/* Usually, the travel-utility should be negative (it's a disutility)
		 * but the cost should be positive. Thus negate the utility.
		 */
		this.travelCostFactor = -Gbl.getConfig().charyparNagelScoring().getTraveling() / 3600.0;
		this.distanceCost = Gbl.getConfig().charyparNagelScoring().getDistanceCost() / 1000.0;
	}

	public double getLinkTravelCost(final Link link, final double time) {
		double travelTime = this.timeCalculator.getLinkTravelTime(link, time);
		if (this.distanceCost == 0.0) {
			return travelTime * this.travelCostFactor;
		}
		return travelTime * this.travelCostFactor + this.distanceCost * link.getLength();
	}

	public double getLinkMinimumTravelCost(final Link link) {
		return (link.getLength() / link.getFreespeed()) * this.travelCostFactor;
	}
}
