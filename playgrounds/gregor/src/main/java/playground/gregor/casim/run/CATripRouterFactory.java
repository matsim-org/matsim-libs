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
package playground.gregor.casim.run;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.old.LegRouterWrapper;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.old.TeleportationLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;

public class CATripRouterFactory implements TripRouterFactory {

	private static final Logger log = Logger
			.getLogger(CATripRouterFactory.class);

	private final String mainMode = "walkca";

	private Scenario scenario;
	private LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;

	public CATripRouterFactory(Scenario sc,
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
				LegRouterWrapper.createLegRouterWrapper(mainMode, scenario.getPopulation()
						.getFactory(), new NetworkLegRouter(scenario
				.getNetwork(), routeAlgo,
				((PopulationFactoryImpl) scenario.getPopulation()
						.getFactory()).getModeRouteFactory())));
		PlansCalcRouteConfigGroup routeConfigGroup = this.scenario.getConfig()
				.plansCalcRoute();

		for (String mainMode : routeConfigGroup.getTeleportedModeSpeeds()
				.keySet()) {
			final RoutingModule old = tr.setRoutingModule(
					mainMode,
					LegRouterWrapper.createLegRouterWrapper(mainMode, scenario.getPopulation()
							.getFactory(), new TeleportationLegRouter(
					((PopulationFactoryImpl) scenario.getPopulation()
							.getFactory()).getModeRouteFactory(),
					routeConfigGroup.getTeleportedModeSpeeds().get(
							mainMode), 
					        routeConfigGroup.getModeRoutingParams().get( mainMode ).getBeelineDistanceFactor() ))) ;
//									routeConfigGroup.getBeelineDistanceFactor())));
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
