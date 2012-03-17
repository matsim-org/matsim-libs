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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelDisutility;
import org.matsim.core.router.util.PersonalizableTravelTime;
import org.matsim.core.router.util.PersonalizableTravelTimeFactory;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.ptproject.qsim.multimodalsimengine.router.MultiModalLegRouter;
import org.matsim.ptproject.qsim.multimodalsimengine.router.util.MultiModalTravelTime;

public class ReplanningModule extends AbstractMultithreadedModule {

	protected Config config;
	protected Network network;
	protected TravelDisutilityFactory travelCostFactory;
	protected PersonalizableTravelTimeFactory travelTimeFactory;
	protected LeastCostPathCalculatorFactory pathFactory;
	private final ModeRouteFactory routeFactory;
	
	public ReplanningModule(Config config, Network network,
			TravelDisutilityFactory costFactory, PersonalizableTravelTimeFactory timeFactory,
			LeastCostPathCalculatorFactory pathFactory, ModeRouteFactory routeFactory) {
		super(config.global());

		this.config = config;
		this.network = network;
		this.travelCostFactory = costFactory;
		this.travelTimeFactory = timeFactory;
		this.pathFactory = pathFactory;
		this.routeFactory = routeFactory;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PersonalizableTravelTime travelTime = travelTimeFactory.createTravelTime();
		PersonalizableTravelDisutility travelCost = travelCostFactory.createTravelDisutility(travelTime, config.planCalcScore());
		
		PlansCalcRoute plansCalcRoute = new PlansCalcRoute(config.plansCalcRoute(), network, travelCost, travelTime, pathFactory, this.routeFactory);

		if (config.multiModal().isMultiModalSimulationEnabled()) {
			MultiModalTravelTime multiModalTravelTime = (MultiModalTravelTime) travelTime;
			IntermodalLeastCostPathCalculator routeAlgo = (IntermodalLeastCostPathCalculator) pathFactory.createPathCalculator(network, travelCost, travelTime);
			MultiModalLegRouter multiModalLegHandler = new MultiModalLegRouter(this.network, multiModalTravelTime, travelCost, routeAlgo);

			/*
			 * A MultiModalTravelTime calculator is used. Before creating a route for a given
			 * leg, the leg's mode has to be set in the travel time and travel cost objects.
			 * This is not done by the LegHandler used by default in PlansCalcRoute. Therefore,
			 * we have to use a multiModalLegHandler.
			 */
			plansCalcRoute.addLegHandler(TransportMode.car, multiModalLegHandler);
			
			String simulatedModes = this.config.multiModal().getSimulatedModes().toLowerCase(Locale.ROOT);
			if (simulatedModes.contains(TransportMode.walk)) plansCalcRoute.addLegHandler(TransportMode.walk, multiModalLegHandler);
			if (simulatedModes.contains(TransportMode.bike)) plansCalcRoute.addLegHandler(TransportMode.bike, multiModalLegHandler);
			if (simulatedModes.contains(TransportMode.ride)) plansCalcRoute.addLegHandler(TransportMode.ride, multiModalLegHandler);
			if (simulatedModes.contains(TransportMode.pt)) plansCalcRoute.addLegHandler(TransportMode.pt, multiModalLegHandler);
		}

		return plansCalcRoute;
	}
}