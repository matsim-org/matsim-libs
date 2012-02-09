/* *********************************************************************** *
 * project: org.matsim.*
 * TeleportationPtTripRouterBuilder.java
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

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.PseudoTransitLegRouter;
import org.matsim.core.router.TeleportationLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.PersonalizableTravelTime;

/**
 * Sets the handler for teleportation pt. No other mode is handled here!
 * @author thibautd
 */
public class TeleportationPtTripRouterBuilder implements TripRouterBuilder {
	private final PlansCalcRouteConfigGroup routeConfigGroup;
	private final PlanCalcScoreConfigGroup scoreConfigGroup;

	/**
	 * Initialises an instance.
	 *
	 * @param routeConfigGroup the config group with routing-related parameters
	 * @param scoreConfigGroup the config group with score-related parameters
	 */
	public TeleportationPtTripRouterBuilder(
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
		LeastCostPathCalculatorFactory leastCostPathAlgoFactory = routerFactory.getLeastCostPathCalculatorFactory();

		FreespeedTravelTimeCost ptTimeCostCalc = new FreespeedTravelTimeCost(-1.0, 0.0, 0.0);
		LeastCostPathCalculator routeAlgoPtFreeFlow = leastCostPathAlgoFactory.createPathCalculator(network, ptTimeCostCalc, ptTimeCostCalc);
		ModeRouteFactory routeFactory = routerFactory.getModeRouteFactory();

		if (routeConfigGroup.getPtSpeedMode() == PlansCalcRouteConfigGroup.PtSpeedMode.freespeed) {
			tripRouter.setModeHandler(
					TransportMode.pt,
					new LegRouterWrapper(
						TransportMode.pt,
						new PseudoTransitLegRouter(
							network,
							routeAlgoPtFreeFlow,
							routeConfigGroup.getPtSpeedFactor(),
							routeConfigGroup.getBeelineDistanceFactor(),
							routeFactory),
						travelCost,
						travelTime));
		}
		else if (routeConfigGroup.getPtSpeedMode() == PlansCalcRouteConfigGroup.PtSpeedMode.beeline) {
			tripRouter.setModeHandler(
					TransportMode.pt,
					new LegRouterWrapper(
						TransportMode.pt,
						new TeleportationLegRouter(
							routeFactory,
							routeConfigGroup.getPtSpeed(),
							routeConfigGroup.getBeelineDistanceFactor()),
						travelCost,
						travelTime));
		}
	}
}

