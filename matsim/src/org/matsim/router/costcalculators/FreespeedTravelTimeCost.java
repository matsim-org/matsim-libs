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
import org.matsim.network.Link;
import org.matsim.router.util.TravelMinCostI;
import org.matsim.router.util.TravelTimeI;

/**
 * CostCalculator and TravelTimeCalculator for Links based on freespeed on links
 *
 * @author mrieser
 */
public class FreespeedTravelTimeCost implements TravelMinCostI, TravelTimeI {
	private final double travelCostFactor;

	public FreespeedTravelTimeCost() {
		// usually, the travel-utility should be negative (it's a disutility)
		// but for the cost, the cost should be positive.
		this.travelCostFactor = -Double.parseDouble(Gbl.getConfig().getParam("planCalcScore", "traveling")) / 3600.0;

		if (this.travelCostFactor < 0) {
			Gbl.errorMsg("The travel cost in " + this.getClass().getName() + " must be > 1. " +
					"Currently, it is " + this.travelCostFactor + ". Please adjust the parameter " +
							"'traveling' in the module 'planCalcScore' in your config file to be" +
							" lower or equal than -3600");
		}
	}

	public double getLinkTravelCost(Link link, double time) {
		return (link.getLength() / link.getFreespeed()) * this.travelCostFactor;
	}

	public double getLinkTravelTime(Link link, double time) {
		return link.getLength() / link.getFreespeed();
	}

	public double getLinkMinimumTravelCost(Link link) {
		return (link.getLength() / link.getFreespeed()) * this.travelCostFactor;
	}

}
