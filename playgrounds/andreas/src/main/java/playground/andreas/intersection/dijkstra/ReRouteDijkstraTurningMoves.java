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
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.routes.RouteFactoryImpl;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author dgrether
 */
public class ReRouteDijkstraTurningMoves extends AbstractMultithreadedModule {

	TravelDisutility costCalculator = null;

	TravelTime timeCalculator = null;

	NetworkImpl wrappedNetwork = null;

	Network originalNetwork = null;

	PlansCalcRouteConfigGroup config = null;
	
	private final RouteFactoryImpl routeFactory; 

	public ReRouteDijkstraTurningMoves(Config config, final Network network, final TravelDisutility costCalculator,
			final TravelTime timeCalculator, final RouteFactoryImpl routeFactory) {
		super(config.global());
		this.originalNetwork = network;
		this.wrappedNetwork = NetworkWrapper.wrapNetwork(network);
		this.costCalculator = costCalculator;
		this.timeCalculator = timeCalculator;
		this.config = config.plansCalcRoute();
		this.routeFactory = routeFactory;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		//PlansCalcRoute plansCalcRoute = new PlansCalcRoute(config, wrappedNetwork, costCalculator, timeCalculator, routeFactory);
		//
		//DijkstraLegHandler dijkstraLegHandler = new DijkstraLegHandler(this.originalNetwork, this.wrappedNetwork, costCalculator, timeCalculator);	
		//plansCalcRoute.addLegHandler(TransportMode.car, dijkstraLegHandler);
		//
		//return plansCalcRoute;
		throw new UnsupportedOperationException( "this cannot be done anymore. It looks like what you need to do is only to input the two lines of this method in your own TripRouterFactory. If you do not know how to do that, contact me. Sorry about that... thibautd, 10 oct 2013" );
	}
}
