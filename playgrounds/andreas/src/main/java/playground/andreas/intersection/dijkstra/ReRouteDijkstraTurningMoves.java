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

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author dgrether
 */
public class ReRouteDijkstraTurningMoves extends AbstractMultithreadedModule {

	PersonalizableTravelCost costCalculator = null;

	TravelTime timeCalculator = null;

	NetworkLayer wrappedNetwork = null;

	Network originalNetwork = null;

	PlansCalcRouteConfigGroup config = null;

	public ReRouteDijkstraTurningMoves(Config config, final Network network, final PersonalizableTravelCost costCalculator,
			final TravelTime timeCalculator) {
		super(config.global());
		this.originalNetwork = network;
		this.wrappedNetwork = NetworkWrapper.wrapNetwork(network);
		this.costCalculator = costCalculator;
		this.timeCalculator = timeCalculator;
		this.config = config.plansCalcRoute();
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlansCalcRouteDijkstra(this.config, this.originalNetwork, this.wrappedNetwork, this.costCalculator, this.timeCalculator);
	}

}
