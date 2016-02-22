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
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.RoutingModule;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.DefaultRoutingModules;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;

import javax.inject.Provider;

/**
 * @author cdobler
 */
public class MultimodalTripRouterFactory implements Provider<TripRouter> {
	
	private static final Logger log = Logger.getLogger(MultimodalTripRouterFactory.class);
	
	private final Scenario scenario;
	private final TravelDisutilityFactory travelDisutilityFactory;
	private final Map<String, TravelTime> multimodalTravelTimes;
	private final Provider<TripRouter> delegateFactory;
	private final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
	
	private final Map<String, Network> multimodalSubNetworks = new HashMap<>();
	
	@Deprecated
	public MultimodalTripRouterFactory(Scenario scenario, Map<String, TravelTime> multimodalTravelTimes,
			TravelDisutilityFactory travelDisutilityFactory) {
		this.scenario = scenario;
		this.multimodalTravelTimes = multimodalTravelTimes;
		this.travelDisutilityFactory = travelDisutilityFactory;

        // commented this out because constructor is deprecated. hope it's ok. mz
//		DefaultTripRouterFactoryImpl tripRouterFactory = DefaultTripRouterFactoryImpl.createDefaultTripRouterFactoryImpl(scenario);
//		this.leastCostPathCalculatorFactory = tripRouterFactory.getLeastCostPathCalculatorFactory();
//		this.delegateFactory = tripRouterFactory;
        this.leastCostPathCalculatorFactory = null;
        this.delegateFactory = null;
	}
	
	public MultimodalTripRouterFactory(Scenario scenario, Map<String, TravelTime> multimodalTravelTimes,
			TravelDisutilityFactory travelDisutilityFactory, Provider<TripRouter> delegateFactory,
			LeastCostPathCalculatorFactory leastCostPathCalculatorFactory) {
		this.scenario = scenario;
		this.multimodalTravelTimes = multimodalTravelTimes;
		this.travelDisutilityFactory = travelDisutilityFactory;
		this.delegateFactory = delegateFactory;
		this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory;
	}
	
	@Override
	public TripRouter get() {

		TripRouter instance = this.delegateFactory.get();
		
		Network network = this.scenario.getNetwork();
		PopulationFactory populationFactory = this.scenario.getPopulation().getFactory();
		MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) scenario.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
        Set<String> simulatedModes = CollectionUtils.stringToSet(multiModalConfigGroup.getSimulatedModes());
		for (String mode : simulatedModes) {

			if (instance.getRegisteredModes().contains(mode)) {
				log.warn("A routing algorithm for " + mode + " is already registered. It is replaced!");
			}
			
			TravelTime travelTime = this.multimodalTravelTimes.get(mode);
			if (travelTime == null) {
				throw new RuntimeException("No travel time object was found for mode " + mode + "! Aborting.");
			}
			
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
			 * We cannot use the travel disutility object from the routingContext since it
			 * has not been created for the modes used here.
			 */
			TravelDisutility travelDisutility = this.travelDisutilityFactory.createTravelDisutility(travelTime);		
			LeastCostPathCalculator routeAlgo = this.leastCostPathCalculatorFactory.createPathCalculator(subNetwork, travelDisutility, travelTime);
			RoutingModule legRouterWrapper = DefaultRoutingModules.createPureNetworkRouter(mode, populationFactory, subNetwork, routeAlgo ); 
			instance.setRoutingModule(mode, legRouterWrapper);
		}

		return instance;
	}
}
