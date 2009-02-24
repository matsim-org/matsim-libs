/* *********************************************************************** *
 * project: org.matsim.*
 * ReRoute.java
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
import org.matsim.router.util.DijkstraFactory;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;

/**
 * Uses {@link org.matsim.router.Dijkstra} for calculating the routes of plans during Replanning.
 *
 * @author mrieser
 */
public class ReRouteDijkstra extends MultithreadedModuleA {

	TravelCost costCalculator = null;
	TravelTime timeCalculator = null;
	NetworkLayer network = null;

	public ReRouteDijkstra(final NetworkLayer network, final TravelCost costCalculator, final TravelTime timeCalculator) {
		this.network = network;
		this.costCalculator = costCalculator;
		this.timeCalculator = timeCalculator;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlansCalcRoute(this.network, this.costCalculator, this.timeCalculator, new DijkstraFactory());
	}

}
