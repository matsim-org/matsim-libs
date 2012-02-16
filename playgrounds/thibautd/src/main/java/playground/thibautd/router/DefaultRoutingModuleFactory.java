/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultRoutingModuleFactory.java
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

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.NetworkLegRouter;
import org.matsim.core.router.PseudoTransitLegRouter;
import org.matsim.core.router.TeleportationLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
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
public class DefaultRoutingModuleFactory implements RoutingModuleFactory {
	private static final String UNDEFINED_MODE = "undefined";
	public static final List<String> HANDLED_MODES = Arrays.asList( new String[]
			{TransportMode.car, TransportMode.pt, TransportMode.ride,
				TransportMode.walk, TransportMode.bike, UNDEFINED_MODE});
	private final PlansCalcRouteConfigGroup routeConfigGroup;
	private final PlanCalcScoreConfigGroup scoreConfigGroup;

	/**
	 * Initialises an instance.
	 *
	 * @param routeConfigGroup the config group with routing-related parameters
	 * @param scoreConfigGroup the config group with score-related parameters
	 */
	public DefaultRoutingModuleFactory(
			final PlansCalcRouteConfigGroup routeConfigGroup,
			final PlanCalcScoreConfigGroup scoreConfigGroup) {
		this.routeConfigGroup = routeConfigGroup;
		this.scoreConfigGroup = scoreConfigGroup;
	}

	@Override
	public RoutingModule createModule(
			final String mainMode,
			final TripRouterFactory routerFactory) {
		Network network = routerFactory.getNetwork();
		PersonalizableTravelTime travelTime = routerFactory.getTravelTimeCalculatorFactory().createTravelTime();
		PersonalizableTravelCost travelCost = routerFactory.getTravelCostCalculatorFactory().createTravelCostCalculator( travelTime , scoreConfigGroup );

		LeastCostPathCalculatorFactory leastCostPathAlgoFactory = routerFactory.getLeastCostPathCalculatorFactory();
		LeastCostPathCalculator routeAlgo = leastCostPathAlgoFactory.createPathCalculator(network, travelCost, travelTime);

		FreespeedTravelTimeCost ptTimeCostCalc = new FreespeedTravelTimeCost(-1.0, 0.0, 0.0);
		LeastCostPathCalculator routeAlgoPtFreeFlow = leastCostPathAlgoFactory.createPathCalculator(network, ptTimeCostCalc, ptTimeCostCalc);

		if (routeAlgo instanceof IntermodalLeastCostPathCalculator) {
			((IntermodalLeastCostPathCalculator) routeAlgo).setModeRestriction(Collections.singleton(TransportMode.car));
			((IntermodalLeastCostPathCalculator) routeAlgoPtFreeFlow).setModeRestriction(Collections.singleton(TransportMode.car));
		}

		ModeRouteFactory routeFactory = routerFactory.getModeRouteFactory();

		// first check teleportation
		if (routeConfigGroup.getTeleportedModeFreespeedFactors().containsKey( mainMode )) {
			return new LegRouterWrapper(
					mainMode,
					new PseudoTransitLegRouter(
						network,
						routeAlgoPtFreeFlow,
						routeConfigGroup.getTeleportedModeFreespeedFactors().get( mainMode ),
						routeConfigGroup.getBeelineDistanceFactor(),
						routeFactory),
					travelCost,
					travelTime);
		}

		if (routeConfigGroup.getTeleportedModeSpeeds().containsKey( mainMode )) {
			return new LegRouterWrapper(
					mainMode,
					new TeleportationLegRouter(
						routeFactory,
						routeConfigGroup.getTeleportedModeSpeeds().get( mainMode ),
						routeConfigGroup.getBeelineDistanceFactor()),
					travelCost,
					travelTime);
		}

		// mode was not a teleported one: set the default routing module
		if ( mainMode.equals( TransportMode.car ) ) {
			return new LegRouterWrapper(
					TransportMode.car,
					new NetworkLegRouter(
						network,
						routeAlgo,
						routeFactory),
					travelCost,
					travelTime);
		}

		if ( mainMode.equals( TransportMode.ride ) ) {
			return new LegRouterWrapper(
					TransportMode.ride,
					new NetworkLegRouter(
						network,
						routeAlgo,
						routeFactory),
					travelCost,
					travelTime);
		}

		if ( mainMode.equals( TransportMode.bike ) ) {
			return new LegRouterWrapper(
					TransportMode.bike,
					new TeleportationLegRouter(
						routeFactory,
						routeConfigGroup.getBikeSpeed(),
						routeConfigGroup.getBeelineDistanceFactor()),
					travelCost,
					travelTime);
		}

		if ( mainMode.equals( TransportMode.walk ) ) {
			return new LegRouterWrapper(
					TransportMode.walk,
					new TeleportationLegRouter(
						routeFactory,
						routeConfigGroup.getWalkSpeed(),
						routeConfigGroup.getBeelineDistanceFactor()),
					travelCost,
					travelTime);
		}

		if ( mainMode.equals( UNDEFINED_MODE ) ) {
			return new LegRouterWrapper(
					UNDEFINED_MODE,
					new TeleportationLegRouter(
						routeFactory,
						routeConfigGroup.getUndefinedModeSpeed(),
						routeConfigGroup.getBeelineDistanceFactor()),
					travelCost,
					travelTime);
		}

		throw new IllegalArgumentException( "unhandled mode "+mainMode );
	}

}

