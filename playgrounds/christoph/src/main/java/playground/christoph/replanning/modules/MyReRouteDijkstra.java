/* *********************************************************************** *
 * project: org.matsim.*
 * MyReRouteDijkstra.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.christoph.replanning.modules;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.replanning.modules.ReRouteDijkstra;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.christoph.router.CloneablePlansCalcRoute;
import playground.christoph.router.util.CloningDijkstraFactory;

/*
 * Basically we could also extend AbstractMultithreadedModule -
 * currently we don't use methods from ReRouteDijkstra but maybe
 * somewhere is checked if our Class is instanceof ReRouteDijkstra...
 */
public class MyReRouteDijkstra extends ReRouteDijkstra {

	PersonalizableTravelCost costCalculator = null;
	TravelTime timeCalculator = null;
	Network network = null;
	
	private PlansCalcRouteConfigGroup configGroup = null;
	
	public MyReRouteDijkstra(Config config, final Network network, final PersonalizableTravelCost costCalculator, final TravelTime timeCalculator) {
		super(config, network, costCalculator, timeCalculator);
		this.network = network;
		this.costCalculator = costCalculator;
		this.timeCalculator = timeCalculator;
		this.configGroup = config.plansCalcRoute();
	}
	
	/*
	 * If possible, we should probably clone the Cost- and TimeCalculator.
	 * Maybe MATSim runs MultiThreaded and will use more Threads to do the
	 * replanning.
	 */
	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new CloneablePlansCalcRoute(this.configGroup, this.network, this.costCalculator, this.timeCalculator, new CloningDijkstraFactory());
	}
}
