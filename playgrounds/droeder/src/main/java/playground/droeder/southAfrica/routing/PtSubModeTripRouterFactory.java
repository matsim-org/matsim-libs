/* *********************************************************************** *
 * project: org.matsim.*
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
package playground.droeder.southAfrica.routing;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.LegRouterWrapper;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.old.PseudoTransitLegRouter;
import org.matsim.core.router.old.TeleportationLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.TransitRouterFactory;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * @author droeder
 *
 */
public class PtSubModeTripRouterFactory implements TripRouterFactory{



	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(PtSubModeTripRouterFactory.class);
	
	private final Config config;
	private final Network network;
	private final TravelDisutilityFactory travelDisutilityFactory;
	private final TravelTime travelTime;
	private final LeastCostPathCalculatorFactory leastCostPathAlgorithmFactory;
	private final ModeRouteFactory modeRouteFactory;
	private final PopulationFactory populationFactory;
	private final TransitRouterFactory transitRouterFactory;
	private final TransitSchedule transitSchedule;

	/**
	 * based on {@link TripRouterFactoryImpl}. Own Implementation is just necessary to add pt-submodes.
	 * @param controler
	 */
	public PtSubModeTripRouterFactory(final Controler controler) {
		
		this.config = controler.getScenario().getConfig();
		this.network = controler.getScenario().getNetwork();
		this.travelDisutilityFactory = controler.getTravelDisutilityFactory();
		this.travelTime = controler.getTravelTimeCalculator();
		this.leastCostPathAlgorithmFactory = controler.getLeastCostPathCalculatorFactory();
		this.modeRouteFactory = ((PopulationFactoryImpl) controler.getScenario().getPopulation().getFactory()).getModeRouteFactory();
		this.populationFactory = controler.getScenario().getPopulation().getFactory();
		this.transitRouterFactory = controler.getTransitRouterFactory();
		this.transitSchedule = controler.getScenario().getTransitSchedule();
	}
	
	@Override
	public TripRouter createTripRouter() {
		TripRouter tripRouter = new TripRouter();
		
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

		// add a Routing-Module for each Transit(sub)mode.
		System.out.println(config.transit().getTransitModes());
		for(String mode: config.transit().getTransitModes()) {
			tripRouter.setRoutingModule(
					mode,
					new TransitRouterWrapper(
							((PtSubModeRouter) transitRouterFactory.createTransitRouter()).getModeRouter(mode),
							transitSchedule,
							// use a walk router in case no path is found
							new LegRouterWrapper(
									TransportMode.transit_walk,
									populationFactory,
									new TeleportationLegRouter(
											modeRouteFactory,
											routeConfigGroup.getTeleportedModeSpeeds().get( TransportMode.walk),
											routeConfigGroup.getBeelineDistanceFactor()))));
		}
		// add pt as fallback-solution
		tripRouter.setRoutingModule(
				TransportMode.pt,
				new TransitRouterWrapper(
						((PtSubModeRouter) transitRouterFactory.createTransitRouter()).getModeRouter(TransportMode.pt),
						transitSchedule,
						// use a walk router in case no PT path is found
						new LegRouterWrapper(
								TransportMode.transit_walk,
								populationFactory,
								new TeleportationLegRouter(
										modeRouteFactory,
										routeConfigGroup.getTeleportedModeSpeeds().get( TransportMode.walk),
										routeConfigGroup.getBeelineDistanceFactor()))));
		return tripRouter;
		
	}

}

