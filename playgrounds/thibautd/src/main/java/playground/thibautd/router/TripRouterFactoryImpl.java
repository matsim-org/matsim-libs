/* *********************************************************************** *
 * project: org.matsim.*
 * TripRouterFactoryImpl.java
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
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.NetworkLegRouter;
import org.matsim.core.router.PseudoTransitLegRouter;
import org.matsim.core.router.TeleportationLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * Default factory, which sets the routing modules according to the
 * config file.
 * @author thibautd
 */
public class TripRouterFactoryImpl implements TripRouterFactory {
	private final RoutingElements data;

	public TripRouterFactoryImpl(final RoutingElements data) {
		this.data = data;
	}

	/**
	 * Hook provided to change the {@link TripRouter}
	 * implementation whithout changing the configuration.
	 *
	 * @return a new unconfigured instance
	 */
	protected TripRouter instanciateTripRouter() {
		return new TripRouter();
	}

	@Override
	public TripRouter createTripRouter() {
		TripRouter tripRouter = instanciateTripRouter();

		PlansCalcRouteConfigGroup routeConfigGroup = data.getConfig().plansCalcRoute();
		Network network = data.getNetwork();
		TravelTime travelTime = data.getTravelTimeFactory().createTravelTime();
		TravelDisutility travelCost =
			data.getTravelDisutilityFactory().createTravelDisutility(
					travelTime,
					data.getConfig().planCalcScore() );

		LeastCostPathCalculatorFactory leastCostPathAlgoFactory =
			data.getLeastCostPathCalculatorFactory();
		LeastCostPathCalculator routeAlgo =
			leastCostPathAlgoFactory.createPathCalculator(
					network,
					travelCost,
					travelTime);

		FreespeedTravelTimeAndDisutility ptTimeCostCalc =
			new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);
		LeastCostPathCalculator routeAlgoPtFreeFlow =
			leastCostPathAlgoFactory.createPathCalculator(
					network,
					ptTimeCostCalc,
					ptTimeCostCalc);

		//if (routeAlgo instanceof IntermodalLeastCostPathCalculator) {
		//	((IntermodalLeastCostPathCalculator) routeAlgo).setModeRestriction(Collections.singleton(TransportMode.car));
		//	((IntermodalLeastCostPathCalculator) routeAlgoPtFreeFlow).setModeRestriction(Collections.singleton(TransportMode.car));
		//}

		ModeRouteFactory routeFactory = data.getModeRouteFactory();

		for (String mainMode : routeConfigGroup.getTeleportedModeFreespeedFactors().keySet()) {
			tripRouter.setRoutingModule(
					mainMode,
					new LegRouterWrapper(
						mainMode,
						data.getPopulationFactory(),
						new PseudoTransitLegRouter(
							network,
							routeAlgoPtFreeFlow,
							routeConfigGroup.getTeleportedModeFreespeedFactors().get( mainMode ),
							routeConfigGroup.getBeelineDistanceFactor(),
							routeFactory)));
		}

		for (String mainMode : routeConfigGroup.getTeleportedModeSpeeds().keySet()) {
			tripRouter.setRoutingModule(
					mainMode,
					new LegRouterWrapper(
						mainMode,
						data.getPopulationFactory(),
						new TeleportationLegRouter(
							routeFactory,
							routeConfigGroup.getTeleportedModeSpeeds().get( mainMode ),
							routeConfigGroup.getBeelineDistanceFactor())));
		}

		for ( String mainMode : routeConfigGroup.getNetworkModes() ) {
			tripRouter.setRoutingModule(
					mainMode,
					new LegRouterWrapper(
						mainMode,
						data.getPopulationFactory(),
						new NetworkLegRouter(
							network,
							routeAlgo,
							routeFactory)));
		}

		if ( data.getConfig().scenario().isUseTransit() ) {
			tripRouter.setRoutingModule(
					TransportMode.pt,
					 new TransitRouterWrapper(
						data.getTransitRouterFactory().createTransitRouter(),
						data.getTransitSchedule(),
						// use a walk router in case no PT path is found
						new LegRouterWrapper(
							TransportMode.transit_walk,
							data.getPopulationFactory(),
							new TeleportationLegRouter(
								routeFactory,
								routeConfigGroup.getTeleportedModeSpeeds().get( TransportMode.walk ),
								routeConfigGroup.getBeelineDistanceFactor()))));
		}

		return tripRouter;
	}
}

