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
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.IntermodalLeastCostPathCalculator;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.old.PseudoTransitLegRouter;
import org.matsim.core.router.old.TeleportationLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.TravelTimeFactory;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * Default factory, which sets the routing modules according to the
 * config file.
 * @author thibautd
 */
public class TripRouterFactoryImpl implements TripRouterFactory {
	private final Config config;
	private final Network network;
	private final TravelDisutilityFactory travelDisutilityFactory;
	private final TravelTime travelTime;
	private final LeastCostPathCalculatorFactory leastCostPathAlgorithmFactory;
	private final ModeRouteFactory modeRouteFactory;
	private final PopulationFactory populationFactory;
	private final TransitRouterFactory transitRouterFactory;
	private final TransitSchedule transitSchedule;

	public TripRouterFactoryImpl(final Controler controler) {
		this(
				controler.getConfig(),
				controler.getScenario().getNetwork(),
				controler.getTravelDisutilityFactory(),
				controler.getTravelTimeCalculator(),
				controler.getLeastCostPathCalculatorFactory(),
				controler.getPopulation().getFactory(),
				((PopulationFactoryImpl) (controler.getPopulation().getFactory())).getModeRouteFactory(),
				controler.getTransitRouterFactory(),
				controler.getScenario().getTransitSchedule());
	}

	public TripRouterFactoryImpl(
			final Config config,
			final Network network,
			final TravelDisutilityFactory travelDisutilityFactory,
			final TravelTime travelTime,
			final LeastCostPathCalculatorFactory leastCostPathAlgoFactory,
			final PopulationFactory populationFactory,
			final ModeRouteFactory modeRouteFactory,
			final TransitRouterFactory transitRouterFactory,
			final TransitSchedule transitSchedule) {
		this.config = config;
		this.network = network;
		this.travelDisutilityFactory = travelDisutilityFactory;
		this.travelTime = travelTime;
		this.leastCostPathAlgorithmFactory = leastCostPathAlgoFactory;
		this.modeRouteFactory = modeRouteFactory;
		this.populationFactory = populationFactory;
		this.transitRouterFactory = transitRouterFactory;
		this.transitSchedule = transitSchedule;
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

		PlansCalcRouteConfigGroup routeConfigGroup = config.plansCalcRoute();
		TravelDisutility travelCost =
			travelDisutilityFactory.createTravelDisutility(
					travelTime,
					config.planCalcScore() );

		LeastCostPathCalculator routeAlgo =
			leastCostPathAlgorithmFactory.createPathCalculator(
					network,
					travelCost,
					travelTime);

		FreespeedTravelTimeAndDisutility ptTimeCostCalc =
			new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);
		LeastCostPathCalculator routeAlgoPtFreeFlow =
			leastCostPathAlgorithmFactory.createPathCalculator(
					network,
					ptTimeCostCalc,
					ptTimeCostCalc);

		//if (routeAlgo instanceof IntermodalLeastCostPathCalculator) {
		//	((IntermodalLeastCostPathCalculator) routeAlgo).setModeRestriction(Collections.singleton(TransportMode.car));
		//	((IntermodalLeastCostPathCalculator) routeAlgoPtFreeFlow).setModeRestriction(Collections.singleton(TransportMode.car));
		//}

		for (String mainMode : routeConfigGroup.getTeleportedModeFreespeedFactors().keySet()) {
			tripRouter.setRoutingModule(
					mainMode,
					new LegRouterWrapper(
						mainMode,
						populationFactory,
						new PseudoTransitLegRouter(
							network,
							routeAlgoPtFreeFlow,
							routeConfigGroup.getTeleportedModeFreespeedFactors().get( mainMode ),
							routeConfigGroup.getBeelineDistanceFactor(),
							modeRouteFactory)));
		}

		for (String mainMode : routeConfigGroup.getTeleportedModeSpeeds().keySet()) {
			tripRouter.setRoutingModule(
					mainMode,
					new LegRouterWrapper(
						mainMode,
						populationFactory,
						new TeleportationLegRouter(
							modeRouteFactory,
							routeConfigGroup.getTeleportedModeSpeeds().get( mainMode ),
							routeConfigGroup.getBeelineDistanceFactor())));
		}

		for ( String mainMode : routeConfigGroup.getNetworkModes() ) {
			tripRouter.setRoutingModule(
					mainMode,
					new LegRouterWrapper(
						mainMode,
						populationFactory,
						new NetworkLegRouter(
							network,
							routeAlgo,
							modeRouteFactory)));
		}

		if ( config.scenario().isUseTransit() ) {
			tripRouter.setRoutingModule(
					TransportMode.pt,
					 new TransitRouterWrapper(
						transitRouterFactory.createTransitRouter(),
						transitSchedule,
						// use a walk router in case no PT path is found
						new LegRouterWrapper(
							TransportMode.transit_walk,
							populationFactory,
							new TeleportationLegRouter(
								modeRouteFactory,
								routeConfigGroup.getTeleportedModeSpeeds().get( TransportMode.walk ),
								routeConfigGroup.getBeelineDistanceFactor()))));
		}

		return tripRouter;
	}
}

