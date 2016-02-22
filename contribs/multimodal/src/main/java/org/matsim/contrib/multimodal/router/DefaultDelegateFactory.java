/* *********************************************************************** *
 * project: org.matsim.*
 * MultimodalTripRouterFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.multimodal.router;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * Due to several reasons, the DefaultTripRouterFactoryImpl should not be used
 * as delegate for the MultiModalTripRouterFactory.
 * 
 * <ul>
 * 	<li>The multi-modal modes cannot be set as network modes. Doing so would
 * 		result in a RuntimeException. First, a teleportation RoutingModule would
 * 		be created as a result of the mode speed from the config file. Then,
 * 		a second RoutingModule for the same mode should be created since the
 * 		mode is also set as NetworkMode, which results in the exception.</li>
 * 	<li>Up to now, I am hot sure how to use a A* routing algorithm for the
 * 		multi-modal modes. MATSim's default implementations assume that a link's
 * 		travel time is based on its length and free speed. However, this is not
 * 		true for modes like walk or bike. Therefore, the pre-processing will
 * 		probably have to be adapted.</li>
 * 	<li>MATSim's default routing algorithms are not able to handle sub-networks
 * 		correct. Their calcLeastCostPath(...) methods gets Nodes from the original
 * 		as input parameters. They are not replaced by their equivalents in the
 * 		mode's sub-network. Therefore, the router would use the entire network
 * 		and would not take mode restrictions into account. This problem is avoided
 * 		by using the "fast" router implementations (FastDijstra, FastAstarLandmarks).
 * 		They use internally a routing network which automatically fixes this problem
 * 		since the given nodes are mapped to the routing network.</li>
 * </ul>
 * 
 * @author cdobler
 */
public class DefaultDelegateFactory implements Provider<TripRouter> {

	private static final Logger log = Logger.getLogger(DefaultDelegateFactory.class);
	
	private final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
    private final Scenario scenario;
    
    private final Map<String, Network> multimodalSubNetworks = new HashMap<>();

	@Inject
	Map<String, TravelTime> travelTimes;

	@Inject
	Map<String, TravelDisutilityFactory> travelDisutilityFactories;

    public DefaultDelegateFactory(Scenario scenario, LeastCostPathCalculatorFactory leastCostPathCalculatorFactory) {
    	this.scenario = scenario;
    	this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory;
    }
	
	@Override
	public TripRouter get() {
        
		TripRouter tripRouter = new TripRouter();

		TravelTime travelTime = travelTimes.get("car");
		TravelDisutility travelDisutility = travelDisutilityFactories.get("car").createTravelDisutility(travelTimes.get("car"));

		Network network = this.scenario.getNetwork();
		PlansCalcRouteConfigGroup routeConfigGroup = scenario.getConfig().plansCalcRoute();
		MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) scenario.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
		PopulationFactory populationFactory = this.scenario.getPopulation().getFactory();
		ModeRouteFactory modeRouteFactory = ((PopulationFactoryImpl) populationFactory).getModeRouteFactory();

		for (String mode : routeConfigGroup.getTeleportedModeFreespeedFactors().keySet()) {
			
        	/*
        	 * Skip modes that are handled by the multi-modal simulation. Their
        	 * RoutingModules are created by the MultiModalTripRouterFactory.
        	 */
        	if (multiModalConfigGroup.getSimulatedModes().contains(mode)) continue;
        	
			Network subNetwork = multimodalSubNetworks.get(mode);
			if (subNetwork == null) {
				subNetwork = NetworkImpl.createNetwork();
				Set<String> restrictions = new HashSet<>();
				restrictions.add(mode);
				TransportModeNetworkFilter networkFilter = new TransportModeNetworkFilter(network);
				networkFilter.filter(subNetwork, restrictions);
				this.multimodalSubNetworks.put(mode, subNetwork);
			}
        	
			/*
			 * TODO: discuss whether travel time and travel disutility should be taken from
			 * the routing context object.
			 * : In all our papers, we write that "pseudo" with "freespeed factor" is x times the freespeed travel times.  So at least according to
			 * that description, it should be uncongested, i.e. not from the routing context. kai, apr'15
			 */
    		FreespeedTravelTimeAndDisutility ptTimeCostCalc = new FreespeedTravelTimeAndDisutility(-1.0, 0.0, 0.0);
    		LeastCostPathCalculator routeAlgoPtFreeFlow = this.leastCostPathCalculatorFactory.createPathCalculator(subNetwork, ptTimeCostCalc, ptTimeCostCalc);
        	
			RoutingModule legRouterWrapper = DefaultRoutingModules.createPseudoTransitRouter(mode, populationFactory, 
					scenario.getNetwork(), routeAlgoPtFreeFlow, 
					routeConfigGroup.getModeRoutingParams().get( mode ) ) ;
			final RoutingModule old = tripRouter.setRoutingModule(mode, legRouterWrapper);
       }

        for (String mode : routeConfigGroup.getTeleportedModeSpeeds().keySet()) {
        	
        	/*
        	 * Skip modes that are handled by the multi-modal simulation. Their
        	 * RoutingModules are created by the MultiModalTripRouterFactory.
        	 */
        	if (multiModalConfigGroup.getSimulatedModes().contains(mode)) continue;
        	
			RoutingModule legRouterWrapper = DefaultRoutingModules.createTeleportationRouter(mode, populationFactory, 
					routeConfigGroup.getModeRoutingParams().get( mode )
					); 
			final RoutingModule old = tripRouter.setRoutingModule(mode, legRouterWrapper);
        	
            if (old != null) {
                log.error("inconsistent router configuration for mode " + mode);
                throw new RuntimeException("there was already a module set when trying to set teleporting routing module for mode "+ mode + ": " + old);
            }
        }

        for (String mode : routeConfigGroup.getNetworkModes()) {
        	
        	/*
        	 * Skip modes that are handled by the multi-modal simulation. Their
        	 * RoutingModules are created by the MultiModalTripRouterFactory.
        	 */
        	if (multiModalConfigGroup.getSimulatedModes().contains(mode)) continue;
        	
			Network subNetwork = multimodalSubNetworks.get(mode);
			if (subNetwork == null) {
				subNetwork = NetworkImpl.createNetwork();
				Set<String> restrictions = new HashSet<>();
				restrictions.add(mode);
				TransportModeNetworkFilter networkFilter = new TransportModeNetworkFilter(network);
				networkFilter.filter(subNetwork, restrictions);
				this.multimodalSubNetworks.put(mode, subNetwork);
			}
			
			LeastCostPathCalculator routeAlgo = this.leastCostPathCalculatorFactory.createPathCalculator(subNetwork, travelDisutility, travelTime);
			RoutingModule legRouterWrapper = DefaultRoutingModules.createPureNetworkRouter(mode, populationFactory, subNetwork, routeAlgo ); 
			final RoutingModule old = tripRouter.setRoutingModule(mode, legRouterWrapper);
        	
            if (old != null) {
                log.error("inconsistent router configuration for mode " + mode);
                throw new RuntimeException("there was already a module set when trying to set network routing module for mode "+ mode + ": " + old);
            }
        }
        
        return tripRouter;
	}
}
