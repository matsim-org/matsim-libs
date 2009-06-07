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

package org.matsim.core.replanning.modules;

import org.matsim.core.api.network.Network;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

public class ReRouteLandmarks extends ReRouteDijkstra {

	private final AStarLandmarksFactory factory;
	private PlansCalcRouteConfigGroup configGroup = null;

	public ReRouteLandmarks(Network network, TravelCost costCalculator,
			TravelTime timeCalculator, PreProcessLandmarks commonRouterData) {
		super(network, costCalculator, timeCalculator);
		this.factory = new AStarLandmarksFactory(commonRouterData);
		// TODO balmermi: PLEASE DOUBLECHECK/TRIPPLECHECK THE USE OF PlansCalcRouteConfigGroup
		configGroup = new PlansCalcRouteConfigGroup();
	}
	
	public ReRouteLandmarks(PlansCalcRouteConfigGroup configGroup, Network network, TravelCost costCalculator,
			TravelTime timeCalculator, PreProcessLandmarks commonRouterData) {
		this(network, costCalculator, timeCalculator, commonRouterData);
		this.configGroup = configGroup;
		// TODO balmermi: PLEASE DOUBLECHECK/TRIPPLECHECK THE USE OF PlansCalcRouteConfigGroup
		if (this.configGroup == null) { System.err.println(">>> ARE YOU SURE THAT THE PlansCalcRouteConfigGroup IS SET TO NULL??? <<<"); }
	}
	
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlansCalcRoute(this.configGroup, 
				this.network, this.costCalculator, this.timeCalculator, this.factory);
	}

}
