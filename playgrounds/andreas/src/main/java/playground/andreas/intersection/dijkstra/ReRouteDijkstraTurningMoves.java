/* *********************************************************************** *
 * project: org.matsim.*
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

package playground.andreas.intersection.dijkstra;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author dgrether
 */
public class ReRouteDijkstraTurningMoves extends AbstractMultithreadedModule {

	PersonalizableTravelCost costCalculator = null;

	PersonalizableTravelTime timeCalculator = null;

	NetworkImpl wrappedNetwork = null;

	Network originalNetwork = null;

	PlansCalcRouteConfigGroup config = null;

	public ReRouteDijkstraTurningMoves(Config config, final Network network, final PersonalizableTravelCost costCalculator,
			final PersonalizableTravelTime timeCalculator) {
		super(config.global());
		this.originalNetwork = network;
		this.wrappedNetwork = NetworkWrapper.wrapNetwork(network);
		this.costCalculator = costCalculator;
		this.timeCalculator = timeCalculator;
		this.config = config.plansCalcRoute();
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlansCalcRoute plansCalcRoute = new PlansCalcRoute(config, wrappedNetwork, costCalculator, timeCalculator);
		
		DijkstraLegHandler dijkstraLegHandler = new DijkstraLegHandler(this.originalNetwork, this.wrappedNetwork, costCalculator, timeCalculator);	
		plansCalcRoute.addLegHandler(TransportMode.car, dijkstraLegHandler);
		
		return plansCalcRoute;
	}
}