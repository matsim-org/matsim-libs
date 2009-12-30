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

package org.matsim.core.replanning.modules;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * Uses {@link org.matsim.core.router.Dijkstra} for calculating the routes of plans during Replanning.
 *
 * @author mrieser
 */
public class ReRouteDijkstra extends AbstractMultithreadedModule {

	TravelCost costCalculator = null;
	TravelTime timeCalculator = null;
	Network network = null;
	private PlansCalcRouteConfigGroup configGroup = null;
	
	/**
	 * @deprecated use other constructor and give the PlansCalcRouteConfigGroup
	 * as argument 
	 */
	@Deprecated
	protected ReRouteDijkstra(GlobalConfigGroup globalConfigGroup, final Network network, final TravelCost costCalculator, final TravelTime timeCalculator) {
		super(globalConfigGroup);
		this.network = network;
		this.costCalculator = costCalculator;
		this.timeCalculator = timeCalculator;
	}
	
	public ReRouteDijkstra(Config config, final Network network, final TravelCost costCalculator, final TravelTime timeCalculator) {
		this(config.global(), network, costCalculator, timeCalculator);
		this.configGroup = config.plansCalcRoute();
	}
	

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlansCalcRoute(this.configGroup, this.network, this.costCalculator, this.timeCalculator, new DijkstraFactory());
	}

}
