/* *********************************************************************** *
 * project: org.matsim.*
 * ReRouteLandmarks.java
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

package org.matsim.replanning.modules;

import org.matsim.network.NetworkLayer;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.router.PlansCalcRouteLandmarks;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;

public class ReRouteLandmarks extends ReRouteDijkstra {

	private PreProcessLandmarks commonRouterData = null;

	public ReRouteLandmarks(NetworkLayer network, TravelCostI costCalculator,
			TravelTimeI timeCalculator, PreProcessLandmarks commonRouterData) {
		super(network, costCalculator, timeCalculator);
		this.commonRouterData  = commonRouterData;
	}
	
	@Override
	public PlanAlgorithmI getPlanAlgoInstance() {
		return new PlansCalcRouteLandmarks(
				this.network, this.commonRouterData,
				this.costCalculator, this.timeCalculator);
	}

}
