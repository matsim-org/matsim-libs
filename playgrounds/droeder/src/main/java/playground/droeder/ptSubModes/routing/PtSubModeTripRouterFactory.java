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
package playground.droeder.ptSubModes.routing;

import com.google.inject.Provider;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.util.*;
import org.matsim.pt.router.TransitRouter;
import org.matsim.pt.transitSchedule.api.TransitSchedule;

/**
 * @author droeder
 *
 */
public class PtSubModeTripRouterFactory implements TripRouterFactory{



	@SuppressWarnings("unused")
	private static final Logger log = Logger
			.getLogger(PtSubModeTripRouterFactory.class);
	
	private Config config;
	private Network network;
	private TravelDisutilityFactory travelDisutilityFactory;
	private TravelTime travelTime;
	private LeastCostPathCalculatorFactory leastCostPathAlgorithmFactory;
	private ModeRouteFactory modeRouteFactory;
	private PopulationFactory populationFactory;
	private Provider<TransitRouter> transitRouterFactory;
	private TransitSchedule transitSchedule;

	private Controler controler;

	/**
	 * based on {@link org.matsim.core.router.TripRouterProviderImpl}. Own Implementation is just necessary to add pt-submodes.
	 * @param controler
	 * @param transitRouterFactory 
	 */
	public PtSubModeTripRouterFactory(final Controler controler, Provider<TransitRouter> transitRouterFactory) {
		this.controler = controler;
		this.transitRouterFactory = transitRouterFactory;
//		this.config = controler.getScenario().getConfig();
//		this.network = controler.getScenario().getNetwork();
//		this.travelDisutilityFactory = controler.getTravelDisutilityFactory();
//		this.travelTime = controler.getTravelTimeCalculator();
//		this.leastCostPathAlgorithmFactory = controler.getLeastCostPathCalculatorFactory();
//		this.modeRouteFactory = ((PopulationFactoryImpl) controler.getScenario().getPopulation().getFactory()).getModeRouteFactory();
//		this.populationFactory = controler.getScenario().getPopulation().getFactory();
//		this.transitRouterFactory = controler.getTransitRouterFactory();
//		this.transitSchedule = controler.getScenario().getTransitSchedule();
	}
	
	@Override
	public TripRouter instantiateAndConfigureTripRouter(RoutingContext iterationContext) {
		
		this.config = controler.getScenario().getConfig();
		this.network = controler.getScenario().getNetwork();
		this.travelDisutilityFactory = controler.getTravelDisutilityFactory();
		this.travelTime = controler.getLinkTravelTimes();
//        LeastCostPathCalculator routeAlgo =
//                leastCostPathCalculatorFactory.createPathCalculator(
//                        scenario.getNetwork(),
//                        routingContext.getTravelDisutility(),
//                        routingContext.getTravelTime());
		this.leastCostPathAlgorithmFactory = createDefaultLeastCostPathCalculatorFactory(controler.getScenario());
		this.modeRouteFactory = ((PopulationFactoryImpl) controler.getScenario().getPopulation().getFactory()).getModeRouteFactory();
		this.populationFactory = controler.getScenario().getPopulation().getFactory();
//		this.transitRouterFactory = controler.getTransitRouterFactory();
		this.transitSchedule = controler.getScenario().getTransitSchedule();
		
		
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
					DefaultRoutingModules.createPseudoTransitRouter( mainMode, populationFactory, 
						network,
						routeAlgoPtFreeFlow,
					    routeConfigGroup.getModeRoutingParams().get( mainMode ) ) ) ;
		}

		for (String mainMode : routeConfigGroup.getTeleportedModeSpeeds().keySet()) {
			tripRouter.setRoutingModule(
					mainMode,
					DefaultRoutingModules.createTeleportationRouter( mainMode, populationFactory, 
					    routeConfigGroup.getModeRoutingParams().get( mainMode ) )) ;
		}

		for ( String mainMode : routeConfigGroup.getNetworkModes() ) {
			tripRouter.setRoutingModule(
					mainMode,
					DefaultRoutingModules.createNetworkRouter(mainMode, populationFactory,
						network,
						routeAlgo ));
		}

		// add a Routing-Module for each Transit(sub)mode.
		System.out.println(config.transit().getTransitModes());
		for(String mode: config.transit().getTransitModes()) {
			tripRouter.setRoutingModule(
					mode,
					new TransitRouterWrapper(
							((PtSubModeRouterSet) transitRouterFactory.get()).getModeRouter(mode),
							transitSchedule,
							network, // use a walk router in case no path is found
							DefaultRoutingModules.createTeleportationRouter(TransportMode.transit_walk, populationFactory, 
									routeConfigGroup.getModeRoutingParams().get( TransportMode.walk )  ))) ;
		}
		// add pt as fallback-solution
		tripRouter.setRoutingModule(
				TransportMode.pt,
				new TransitRouterWrapper(
						((PtSubModeRouterSet) transitRouterFactory.get()).getModeRouter(TransportMode.pt),
						transitSchedule,
						network, // use a walk router in case no PT path is found
						DefaultRoutingModules.createTeleportationRouter( TransportMode.transit_walk, populationFactory, 
						        routeConfigGroup.getModeRoutingParams().get( TransportMode.walk ) ))) ;
		return tripRouter;
		
	}
	
	private LeastCostPathCalculatorFactory createDefaultLeastCostPathCalculatorFactory(Scenario scenario) {
		Config config = scenario.getConfig();
		if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.Dijkstra)) {
            return new DijkstraFactory();
        } else if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.AStarLandmarks)) {
            return new AStarLandmarksFactory(
                    scenario.getNetwork(), new FreespeedTravelTimeAndDisutility(config.planCalcScore()), config.global().getNumberOfThreads());
        } else if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.FastDijkstra)) {
            return new FastDijkstraFactory();
        } else if (config.controler().getRoutingAlgorithmType().equals(ControlerConfigGroup.RoutingAlgorithmType.FastAStarLandmarks)) {
            return new FastAStarLandmarksFactory(
                    scenario.getNetwork(), new FreespeedTravelTimeAndDisutility(config.planCalcScore()));
        } else {
            throw new IllegalStateException("Enumeration Type RoutingAlgorithmType was extended without adaptation of Controler!");
        }
	}

}

