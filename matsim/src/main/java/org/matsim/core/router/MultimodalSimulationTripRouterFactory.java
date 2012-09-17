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

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

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
 * @author thibautd
 */
public class MultimodalSimulationTripRouterFactory implements TripRouterFactory {
	private final Network network;
	private final LeastCostPathCalculatorFactory leastCostAlgoFactory;
	private final TravelDisutility travelDisutility;
	private final MultiModalConfigGroup configGroup;
	private final PopulationFactory populationFactory;
	private final Map<String, TravelTime> multimodalTravelTimes;

	public MultimodalSimulationTripRouterFactory(
			final Network network,
			final PopulationFactory populationFactory,
			final LeastCostPathCalculatorFactory leastCostAlgoFactory,
			final TravelDisutility travelDisutility,
			final Map<String, TravelTime> multimodalTravelTimes,
			final MultiModalConfigGroup configGroup) {
		this.network = network;
		this.populationFactory = populationFactory;
		this.leastCostAlgoFactory = leastCostAlgoFactory;
		this.configGroup = configGroup;
		this.travelDisutility = travelDisutility;
		this.multimodalTravelTimes = multimodalTravelTimes;
	}

	@Override
	public TripRouter createTripRouter() {
		TripRouter instance = new TripRouter();

			
		// Define restrictions for the different modes.
		/*
		 * Car
		 */	
		Set<String> carModeRestrictions = new HashSet<String>();
		carModeRestrictions.add(TransportMode.car);
		
		/*
		 * Walk
		 */	
		Set<String> walkModeRestrictions = new HashSet<String>();
		walkModeRestrictions.add(TransportMode.bike);
		walkModeRestrictions.add(TransportMode.walk);
				
		/*
		 * Bike
		 * Besides bike mode we also allow walk mode - but then the
		 * agent only travels with walk speed (handled in MultiModalTravelTimeCost).
		 */
		Set<String> bikeModeRestrictions = new HashSet<String>();
		bikeModeRestrictions.add(TransportMode.walk);
		bikeModeRestrictions.add(TransportMode.bike);
		
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
		
		TransportModeNetworkFilter networkFilter = new TransportModeNetworkFilter(this.network);
		for (String mode : CollectionUtils.stringToArray( configGroup.getSimulatedModes() )) {
			
			Set<String> modeRestrictions;
			if (mode.equals(TransportMode.car)) {
				modeRestrictions = carModeRestrictions;
			} else if (mode.equals(TransportMode.walk)) {
				modeRestrictions = walkModeRestrictions;
			} else if (mode.equals(TransportMode.bike)) {
				modeRestrictions = bikeModeRestrictions;
			} else if (mode.equals(TransportMode.ride)) {
				modeRestrictions = rideModeRestrictions;
			} else if (mode.equals(TransportMode.pt)) {
				modeRestrictions = ptModeRestrictions;
			} else continue;
			
			Network subNetwork = NetworkImpl.createNetwork();
			networkFilter.filter(subNetwork, modeRestrictions);
			
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

