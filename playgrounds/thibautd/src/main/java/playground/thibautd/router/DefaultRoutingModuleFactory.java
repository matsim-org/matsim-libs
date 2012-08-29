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

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.NetworkLegRouter;
import org.matsim.core.router.PseudoTransitLegRouter;
import org.matsim.core.router.TeleportationLegRouter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * A {@link RoutingModuleFactory} able to generate default routing
 * modules for the default set of modes.
 *
 * @author thibautd
 */
public class DefaultRoutingModuleFactory implements RoutingModuleFactory {
	private static final String UNDEFINED_MODE = "undefined";
	/**
	 * The list of modes for which this class is able to create routing modules.
	 */
	public static final List<String> HANDLED_MODES = Arrays.asList( new String[]
			{TransportMode.car, TransportMode.pt, TransportMode.ride,
				TransportMode.walk, TransportMode.transit_walk, TransportMode.bike, UNDEFINED_MODE});
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
		TravelTime travelTime = routerFactory.getTravelTimeFactory().createTravelTime();
		TravelDisutility travelCost = routerFactory.getTravelDisutilityFactory().createTravelDisutility( travelTime , scoreConfigGroup );

		LeastCostPathCalculatorFactory leastCostPathAlgoFactory = routerFactory.getLeastCostPathCalculatorFactory();
		LeastCostPathCalculator routeAlgo = leastCostPathAlgoFactory.createPathCalculator(network, travelCost, travelTime);

		FreespeedTravelTimeAndDisutility ptTimeCostCalc = new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);
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
					routerFactory.getPopulationFactory(),
					new PseudoTransitLegRouter(
						network,
						routeAlgoPtFreeFlow,
						routeConfigGroup.getTeleportedModeFreespeedFactors().get( mainMode ),
						routeConfigGroup.getBeelineDistanceFactor(),
						routeFactory));
		}

		if (routeConfigGroup.getTeleportedModeSpeeds().containsKey( mainMode )) {
			return new LegRouterWrapper(
					mainMode,
					routerFactory.getPopulationFactory(),
					new TeleportationLegRouter(
						routeFactory,
						routeConfigGroup.getTeleportedModeSpeeds().get( mainMode ),
						routeConfigGroup.getBeelineDistanceFactor()));
		}

		// mode was not a teleported one: set the default routing module
		if ( mainMode.equals( TransportMode.car ) ) {
			return new LegRouterWrapper(
					TransportMode.car,
					routerFactory.getPopulationFactory(),
					new NetworkLegRouter(
						network,
						routeAlgo,
						routeFactory));
		}

		if ( mainMode.equals( TransportMode.ride ) ) {
			return new LegRouterWrapper(
					TransportMode.ride,
					routerFactory.getPopulationFactory(),
					new NetworkLegRouter(
						network,
						routeAlgo,
						routeFactory));
		}

		if ( mainMode.equals( TransportMode.bike ) ) {
			return new LegRouterWrapper(
					TransportMode.bike,
					routerFactory.getPopulationFactory(),
					new TeleportationLegRouter(
						routeFactory,
						routeConfigGroup.getBikeSpeed(),
						routeConfigGroup.getBeelineDistanceFactor()));
		}

		if ( mainMode.equals( TransportMode.walk ) || mainMode.equals( TransportMode.transit_walk ) ) {
			return new LegRouterWrapper(
					mainMode,
					routerFactory.getPopulationFactory(),
					new TeleportationLegRouter(
						routeFactory,
						routeConfigGroup.getWalkSpeed(),
						routeConfigGroup.getBeelineDistanceFactor()));
		}

		if ( mainMode.equals( UNDEFINED_MODE ) ) {
			return new LegRouterWrapper(
					UNDEFINED_MODE,
					routerFactory.getPopulationFactory(),
					new TeleportationLegRouter(
						routeFactory,
						routeConfigGroup.getUndefinedModeSpeed(),
						routeConfigGroup.getBeelineDistanceFactor()));
		}

		throw new IllegalArgumentException( "unhandled mode "+mainMode );
	}

}

