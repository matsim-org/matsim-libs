/* *********************************************************************** *
 * project: org.matsim.*
 * ReplanningModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.withinday.replanning.modules;

import java.util.Locale;
import java.util.Map;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.old.PlansCalcRoute;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelTime;
import org.matsim.population.algorithms.PlanAlgorithm;

public class ReplanningModule extends AbstractMultithreadedModule {

	protected Config config;
	protected Network network;
	protected TravelDisutilityFactory travelCostFactory;
	private Map<String, TravelTime> travelTime;
	protected LeastCostPathCalculatorFactory pathFactory;
	private final ModeRouteFactory routeFactory;
	
	public ReplanningModule(Config config, Network network,
			TravelDisutilityFactory costFactory, Map<String, TravelTime> travelTimes,
			LeastCostPathCalculatorFactory pathFactory, ModeRouteFactory routeFactory) {
		super(config.global());

		this.config = config;
		this.network = network;
		this.travelCostFactory = costFactory;
		this.travelTime = travelTimes;
		this.pathFactory = pathFactory;
		this.routeFactory = routeFactory;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlansCalcRoute plansCalcRoute = new PlansCalcRoute(config.plansCalcRoute(), network, travelCostFactory.createTravelDisutility(travelTime.get(TransportMode.car), config.planCalcScore()), travelTime.get(TransportMode.car), pathFactory, this.routeFactory);

		if (config.multiModal().isMultiModalSimulationEnabled()) {
			plansCalcRoute.addLegHandler(TransportMode.car, new NetworkLegRouter(this.network, pathFactory.createPathCalculator(network, travelCostFactory.createTravelDisutility(travelTime.get(TransportMode.car), config.planCalcScore()), travelTime.get(TransportMode.car)), this.routeFactory));			
			String simulatedModes = this.config.multiModal().getSimulatedModes().toLowerCase(Locale.ROOT);
			if (simulatedModes.contains(TransportMode.walk)) plansCalcRoute.addLegHandler(TransportMode.walk, new NetworkLegRouter(this.network, pathFactory.createPathCalculator(network, travelCostFactory.createTravelDisutility(travelTime.get(TransportMode.walk), config.planCalcScore()), travelTime.get(TransportMode.walk)), this.routeFactory));
			if (simulatedModes.contains(TransportMode.bike)) plansCalcRoute.addLegHandler(TransportMode.bike, new NetworkLegRouter(this.network, pathFactory.createPathCalculator(network, travelCostFactory.createTravelDisutility(travelTime.get(TransportMode.bike), config.planCalcScore()), travelTime.get(TransportMode.bike)), this.routeFactory));
			if (simulatedModes.contains(TransportMode.ride)) plansCalcRoute.addLegHandler(TransportMode.ride, new NetworkLegRouter(this.network, pathFactory.createPathCalculator(network, travelCostFactory.createTravelDisutility(travelTime.get(TransportMode.ride), config.planCalcScore()), travelTime.get(TransportMode.ride)), this.routeFactory));
			if (simulatedModes.contains(TransportMode.pt)) plansCalcRoute.addLegHandler(TransportMode.pt, new NetworkLegRouter(this.network, pathFactory.createPathCalculator(network, travelCostFactory.createTravelDisutility(travelTime.get(TransportMode.pt), config.planCalcScore()), travelTime.get(TransportMode.pt)), this.routeFactory));
		}

		return plansCalcRoute;
	}
}
