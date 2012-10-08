/* *********************************************************************** *
 * project: org.matsim.*
 * MultimodalSimulationTripRouterFactory.java
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

package org.matsim.core.router;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.PopulationFactory;
import org.matsim.core.config.groups.MultiModalConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.PopulationFactoryImpl;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;

/**
 * @author cdobler
 * @author thibautd
 */
public class MultimodalSimulationTripRouterFactory implements TripRouterFactory {
	
	protected static final Logger log = Logger.getLogger(MultimodalSimulationTripRouterFactory.class);
	
	private final TripRouterFactory delegateFactory;
	private final Network network;
	private final LeastCostPathCalculatorFactory leastCostAlgoFactory;
	private final TravelDisutility travelDisutility;
	private final MultiModalConfigGroup configGroup;
	private final PopulationFactory populationFactory;
	private final Map<String, TravelTime> multimodalTravelTimes;
	private final Map<String, Network> multimodalSubNetworks;
	private final Map<String, Set<String>> modeRestrictions;
	
	public MultimodalSimulationTripRouterFactory(
			final Network network,
			final PopulationFactory populationFactory,
			final LeastCostPathCalculatorFactory leastCostAlgoFactory,
			final TravelDisutility travelDisutility,
			final Map<String, TravelTime> multimodalTravelTimes,
			final MultiModalConfigGroup configGroup,
			final TripRouterFactory delegateFactory) {
		this.network = network;
		this.populationFactory = populationFactory;
		this.leastCostAlgoFactory = leastCostAlgoFactory;
		this.configGroup = configGroup;
		this.travelDisutility = travelDisutility;
		this.multimodalTravelTimes = multimodalTravelTimes;
		this.delegateFactory = delegateFactory;
		
		this.modeRestrictions = new HashMap<String, Set<String>>();
		this.multimodalSubNetworks = new HashMap<String, Network>();
		
		initModeRestrictions();
	}

	/*
	 * Define restrictions for the supported modes.
	 */
	/*package*/ void initModeRestrictions() {
		/*
		 * Car
		 */	
		Set<String> carModeRestrictions = new HashSet<String>();
		carModeRestrictions.add(TransportMode.car);
		this.modeRestrictions.put(TransportMode.car, carModeRestrictions);
		
		/*
		 * Walk
		 */	
		Set<String> walkModeRestrictions = new HashSet<String>();
		walkModeRestrictions.add(TransportMode.bike);
		walkModeRestrictions.add(TransportMode.walk);
		this.modeRestrictions.put(TransportMode.walk, walkModeRestrictions);
				
		/*
		 * Bike
		 * Besides bike mode we also allow walk mode - but then the
		 * agent only travels with walk speed (handled in MultiModalTravelTimeCost).
		 */
		Set<String> bikeModeRestrictions = new HashSet<String>();
		bikeModeRestrictions.add(TransportMode.walk);
		bikeModeRestrictions.add(TransportMode.bike);
		this.modeRestrictions.put(TransportMode.bike, bikeModeRestrictions);
		
		/*
		 * PT
		 * We assume PT trips are possible on every road that can be used by cars.
		 * 
		 * Additionally we also allow pt trips to use walk and / or bike only links.
		 * On those links the traveltimes are quite high and we can assume that they
		 * are only use e.g. to walk from the origin to the bus station or from the
		 * bus station to the destination.
		 */
		Set<String> ptModeRestrictions = new HashSet<String>();
		ptModeRestrictions.add(TransportMode.pt);
		ptModeRestrictions.add(TransportMode.car);
		ptModeRestrictions.add(TransportMode.bike);
		ptModeRestrictions.add(TransportMode.walk);
		this.modeRestrictions.put(TransportMode.pt, ptModeRestrictions);
		
		/*
		 * Ride
		 * We assume ride trips are possible on every road that can be used by cars.
		 * Additionally we also allow ride trips to use walk and / or bike only links.
		 * For those links walk travel times are used.
		 */
		Set<String> rideModeRestrictions = new HashSet<String>();
		rideModeRestrictions.add(TransportMode.car);
		rideModeRestrictions.add(TransportMode.bike);
		rideModeRestrictions.add(TransportMode.walk);
		this.modeRestrictions.put(TransportMode.ride, rideModeRestrictions);
	}
	
	@Override
	public TripRouter createTripRouter() {
		
		/*
		 * If a delegateFactory is set, use it to create a TripRouter. By doing so,
		 * modes not simulated by the multi-modal simulation are also taken into account.
		 */
		TripRouter instance = null;
		if (delegateFactory != null) instance = delegateFactory.createTripRouter();
		else instance = new TripRouter();
					
		for (String mode : CollectionUtils.stringToArray( configGroup.getSimulatedModes() )) {
			
			if (instance.getRegisteredModes().contains(mode)) {
				log.warn("A routing algorithm for " + mode + " is already registered. It is replaced!");
			}
			
			Set<String> restrictions;
			restrictions = this.modeRestrictions.get(mode);
			if (restrictions == null) continue;
			
			Network subNetwork = multimodalSubNetworks.get(mode);
			if (subNetwork == null) {
				subNetwork = NetworkImpl.createNetwork();
				TransportModeNetworkFilter networkFilter = new TransportModeNetworkFilter(this.network);
				networkFilter.filter(subNetwork, restrictions);
				multimodalSubNetworks.put(mode, subNetwork);
			}
						
			LeastCostPathCalculator routeAlgo =
				leastCostAlgoFactory.createPathCalculator(
						subNetwork,
						travelDisutility,
						multimodalTravelTimes.get(mode));

			instance.setRoutingModule(
					mode,
					new LegRouterWrapper(
						mode,
						populationFactory,
						new NetworkLegRouter(
							network,
							routeAlgo,
							((PopulationFactoryImpl) populationFactory).getModeRouteFactory())));
		}

		return instance;
	}
}
