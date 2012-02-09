/* *********************************************************************** *
 * project: org.matsim.*
 * BaseTripRouterBuilder.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.router;

import java.util.Collections;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.NetworkLegRouter;
import org.matsim.core.router.TeleportationLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;

/**
 * Initialises default handlers for all modes except pt.
 * Used together with {@link TeleportationPtTripRouterBuilder}, it provides
 * MATSim defaults (the old PlansCalcRoute class).
 * It is compatible with transit (if it obtains an {@link IntermodalLeastCostPathCalculator},
 * it restrains the usable part to car).
 *
 * @author thibautd
 */
public class BaseTripRouterBuilder implements TripRouterBuilder {
	private static final String UNDEFINED_MODE = "undefined";
	private final PlansCalcRouteConfigGroup routeConfigGroup;
	private final PlanCalcScoreConfigGroup scoreConfigGroup;

	/**
	 * Initialises an instance.
	 *
	 * @param routeConfigGroup the config group with routing-related parameters
	 * @param scoreConfigGroup the config group with score-related parameters
	 */
	public BaseTripRouterBuilder(
			final PlansCalcRouteConfigGroup routeConfigGroup,
			final PlanCalcScoreConfigGroup scoreConfigGroup) {
		this.routeConfigGroup = routeConfigGroup;
		this.scoreConfigGroup = scoreConfigGroup;
	}

	@Override
	public void setModeHandlers(
			final TripRouterFactory routerFactory,
			final TripRouter tripRouter) {
		Network network = routerFactory.getNetwork();
		PersonalizableTravelTime travelTime = routerFactory.getTravelTimeCalculatorFactory().createTravelTime();
		PersonalizableTravelCost travelCost = routerFactory.getTravelCostCalculatorFactory().createTravelCostCalculator( travelTime , scoreConfigGroup );

		LeastCostPathCalculator routeAlgo = routerFactory.getLeastCostPathCalculatorFactory().createPathCalculator(network, travelCost, travelTime);

		if (routeAlgo instanceof IntermodalLeastCostPathCalculator) {
			((IntermodalLeastCostPathCalculator) routeAlgo).setModeRestriction(Collections.singleton(TransportMode.car));
		}

		ModeRouteFactory routeFactory = routerFactory.getModeRouteFactory();

		tripRouter.setModeHandler(
				TransportMode.car,
				new LegRouterWrapper(
					TransportMode.car,
					new NetworkLegRouter(
						network,
						routeAlgo,
						routeFactory),
					travelCost,
					travelTime));

		tripRouter.setModeHandler(
				TransportMode.ride,
				new LegRouterWrapper(
					TransportMode.ride,
					new NetworkLegRouter(
						network,
						routeAlgo,
						routeFactory),
					travelCost,
					travelTime));

		tripRouter.setModeHandler(
				TransportMode.bike,
				new LegRouterWrapper(
					TransportMode.bike,
					new TeleportationLegRouter(
						routeFactory,
						routeConfigGroup.getBikeSpeed(),
						routeConfigGroup.getBeelineDistanceFactor()),
					travelCost,
					travelTime));

		tripRouter.setModeHandler(
				TransportMode.walk,
				new LegRouterWrapper(
					TransportMode.walk,
					new TeleportationLegRouter(
						routeFactory,
						routeConfigGroup.getWalkSpeed(),
						routeConfigGroup.getBeelineDistanceFactor()),
					travelCost,
					travelTime));

		tripRouter.setModeHandler(
				UNDEFINED_MODE,
				new LegRouterWrapper(
					UNDEFINED_MODE,
					new TeleportationLegRouter(
						routeFactory,
						routeConfigGroup.getUndefinedModeSpeed(),
						routeConfigGroup.getBeelineDistanceFactor()),
					travelCost,
					travelTime));
	}

}

