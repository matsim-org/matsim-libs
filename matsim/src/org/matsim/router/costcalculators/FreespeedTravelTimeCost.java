/* *********************************************************************** *
 * project: org.matsim.*
 * FreespeedTravelTimeCost.java
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
import org.matsim.interfaces.core.v01.Link;
import org.matsim.router.util.TravelMinCost;
import org.matsim.router.util.TravelTime;
import org.matsim.utils.misc.Time;

/**
 * CostCalculator and TravelTimeCalculator for Links based on freespeed on links
 *
 * @author mrieser
 */
public class FreespeedTravelTimeCost implements TravelMinCost, TravelTime {
	private final double travelCostFactor;

	public FreespeedTravelTimeCost() {
		// usually, the travel-utility should be negative (it's a disutility)
		// but for the cost, the cost should be positive.
		this.travelCostFactor = -Gbl.getConfig().charyparNagelScoring().getTraveling() / 3600.0;

		if (this.travelCostFactor < 0) {
			Gbl.errorMsg("The travel cost in " + this.getClass().getName() + " must be >= 0. " +
					"Currently, it is " + this.travelCostFactor + ". Please adjust the parameter " +
							"'traveling' in the module 'planCalcScore' in your config file to be" +
							" lower or equal than 0");
		}
	}

	public double getLinkTravelCost(Link link, double time) {
		return (link.getLength() / link.getFreespeed(time)) * this.travelCostFactor;
	}

	public double getLinkTravelTime(Link link, double time) {
		return link.getLength() / link.getFreespeed(time);
	}

	public double getLinkMinimumTravelCost(Link link) {
		return (link.getLength() / link.getFreespeed(Time.UNDEFINED_TIME)) * this.travelCostFactor;
	}

}
