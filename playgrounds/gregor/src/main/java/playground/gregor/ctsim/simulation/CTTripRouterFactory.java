/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.gregor.ctsim.simulation;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.router.*;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import playground.gregor.sim2d_v4.scenario.TransportMode;

public class CTTripRouterFactory implements TripRouterFactory {

	private static final Logger log = Logger
			.getLogger(CTTripRouterFactory.class);

	private final String mainMode = TransportMode.walkca;

	private Scenario scenario;
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

	public CTTripRouterFactory(Scenario sc,
							   LeastCostPathCalculatorFactory leastCostPathClaculatorFactory) {
		this.scenario = sc;
		this.leastCostPathCalculatorFactory = leastCostPathClaculatorFactory;
	}

	@Override
	public TripRouter instantiateAndConfigureTripRouter(
			RoutingContext routingContext) {

		LeastCostPathCalculator routeAlgo = leastCostPathCalculatorFactory
				.createPathCalculator(scenario.getNetwork(),
						routingContext.getTravelDisutility(),
						routingContext.getTravelTime());

		TripRouter tr = new TripRouter();

		tr.setRoutingModule(
				mainMode,
				DefaultRoutingModules.createNetworkRouter(mainMode, scenario.getPopulation()
						.getFactory(), scenario
						.getNetwork(), routeAlgo));
		PlansCalcRouteConfigGroup routeConfigGroup = this.scenario.getConfig()
				.plansCalcRoute();

		for (String mainMode : routeConfigGroup.getTeleportedModeSpeeds()
				.keySet()) {
			final RoutingModule old = tr.setRoutingModule(
					mainMode,
					DefaultRoutingModules.createTeleportationRouter(mainMode, scenario.getPopulation()
									.getFactory(),
							routeConfigGroup.getModeRoutingParams().get(mainMode)));
			if (old != null) {
				log.error("inconsistent router configuration for mode "
						+ mainMode);
				log.error("One situation which triggers this warning: setting both speed and speedFactor for a mode (this used to be possible).");
				throw new RuntimeException(
						"there was already a module set when trying to set teleporting module for mode "
								+ mainMode + ": " + old);
			}
		}
		return tr;
	}

}
