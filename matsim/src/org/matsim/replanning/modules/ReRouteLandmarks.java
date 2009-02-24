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
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.router.PlansCalcRoute;
import org.matsim.router.util.AStarLandmarksFactory;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

public class ReRouteLandmarks extends ReRouteDijkstra {

	private PreProcessLandmarks commonRouterData = null;

	public ReRouteLandmarks(NetworkLayer network, TravelCost costCalculator,
			TravelTime timeCalculator, PreProcessLandmarks commonRouterData) {
		super(network, costCalculator, timeCalculator);
		this.commonRouterData  = commonRouterData;
	}
	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlansCalcRoute(
				this.network, this.costCalculator, this.timeCalculator, new AStarLandmarksFactory(this.commonRouterData));
	}

}
