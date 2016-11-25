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
package org.matsim.contrib.minibus.hook;

import java.util.Collections;

import javax.inject.Provider;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.controler.MatsimServices;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.RouteFactories;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.TransitRouterWrapper;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.AStarLandmarksFactory;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.FastAStarLandmarksFactory;
import org.matsim.core.router.util.FastDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.TransitRouter;

/**
 * This class exists only to allow the transit schedule to be updated in each iteration
 * 
 * Default factory, which sets the routing modules according to the
 * config file.
 * @author aneumann, thibautd
 */
class PTripRouterFactoryImpl implements Provider<TripRouter> {
	private static final Logger log =
		Logger.getLogger(PTripRouterFactoryImpl.class);

	private final MatsimServices controler;
    private final Provider<TransitRouter> transitRouterFactory;

    public PTripRouterFactoryImpl(final MatsimServices controler, Provider<TransitRouter> transitRouterFactory) {
		this.controler = controler;
		this.transitRouterFactory = transitRouterFactory;
	}


	/**
	 * Hook provided to change the {@link TripRouter}
	 * implementation without changing the configuration.
	 *
	 * @return a new unconfigured instance
	 */
    private TripRouter instanciateTripRouter() {
		return new TripRouter();
	}

	@Override
	public TripRouter get() {
		// initialize here - controller should be fully initialized by now
		// use fields to keep the rest of the code clean and comparable

        Config config = controler.getScenario().getConfig();
        Network network = controler.getScenario().getNetwork();
        TravelDisutilityFactory travelDisutilityFactory = controler.getTravelDisutilityFactory();
        TravelTime travelTime = controler.getLinkTravelTimes();
        LeastCostPathCalculatorFactory leastCostPathAlgorithmFactory = createDefaultLeastCostPathCalculatorFactory(controler.getScenario());
        RouteFactories modeRouteFactory = ((PopulationFactory) controler.getScenario().getPopulation().getFactory()).getRouteFactories();
        PopulationFactory populationFactory = controler.getScenario().getPopulation().getFactory();
        Scenario scenario = controler.getScenario();
		
		// end of own code
		
		TripRouter tripRouter = instanciateTripRouter();

		PlansCalcRouteConfigGroup routeConfigGroup = config.plansCalcRoute();
		TravelDisutility travelCost =
			travelDisutilityFactory.createTravelDisutility(
                    travelTime);

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

		if ( NetworkUtils.isMultimodal(network) ) {
			// note: LinkImpl has a default allowed mode of "car" so that all links
			// of a monomodal network are actually restricted to car, making the check
			// of multimodality unecessary from a behavioral point of view.
			// However, checking the mode restriction for each link is expensive,
			// so it is not worth doing it if it is not necessary. (td, oct. 2012)
			if (routeAlgo instanceof Dijkstra) {
				((Dijkstra) routeAlgo).setModeRestriction(
					Collections.singleton( TransportMode.car ));
				((Dijkstra) routeAlgoPtFreeFlow).setModeRestriction(
					Collections.singleton( TransportMode.car ));
			}
			else {
				// this is impossible to reach when using the algorithms of org.matsim.*
				// (all implement IntermodalLeastCostPathCalculator)
				log.warn( "network is multimodal but least cost path algorithm is not an instance of IntermodalLeastCostPathCalculator!" );
				throw new RuntimeException();
			}
		}

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
					DefaultRoutingModules.createTeleportationRouter(mainMode, populationFactory, 
						routeConfigGroup.getModeRoutingParams().get( mainMode ) ));
		}

		for ( String mainMode : routeConfigGroup.getNetworkModes() ) {
			tripRouter.setRoutingModule(
					mainMode,
					DefaultRoutingModules.createPureNetworkRouter(mainMode, populationFactory, 
					        network,
						routeAlgo));
		}

		if ( config.transit().isUseTransit() ) {
			tripRouter.setRoutingModule(
					TransportMode.pt,
					 new TransitRouterWrapper(
						transitRouterFactory.get(),
						
						// this line is the reason why this class exists in my playground 
						scenario.getTransitSchedule(),
						// end of modification
						
						scenario.getNetwork(), // use a walk router in case no PT path is found
						DefaultRoutingModules.createTeleportationRouter( TransportMode.transit_walk, populationFactory,
							routeConfigGroup.getModeRoutingParams().get( TransportMode.walk ) )));
		}

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

