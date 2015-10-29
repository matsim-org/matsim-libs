/* *********************************************************************** *
 * project: org.matsim.*
 * CreateMultiModalLegRouters.java
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

package playground.christoph.evacuation.population;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.multimodal.router.util.BikeTravelTimeFactory;
import org.matsim.contrib.multimodal.router.util.WalkTravelTimeFactory;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.population.routes.LinkNetworkRouteFactory;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility.Builder;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.old.LegRouter;
import org.matsim.core.router.old.NetworkLegRouter;
import org.matsim.core.router.util.*;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class CreateMultiModalLegRouters {

	public static Map<String, LegRouter> createLegRouters(Config config, Network network, TravelTime carTravelTime) {
		
		Set<String> modesToReroute = new HashSet<String>();
		modesToReroute.add(TransportMode.car);
		modesToReroute.add(TransportMode.ride);
		modesToReroute.add(TransportMode.bike);
		modesToReroute.add(TransportMode.walk);
		modesToReroute.add(TransportMode.pt);

		PlansCalcRouteConfigGroup configGroup = config.plansCalcRoute();
		Map<String, TravelTime> travelTimes = new HashMap<String, TravelTime>();
		travelTimes.put(TransportMode.car, carTravelTime);
		travelTimes.put(TransportMode.walk, new WalkTravelTimeFactory(configGroup).get());
		travelTimes.put(TransportMode.bike, new BikeTravelTimeFactory(configGroup).get());
		// TODO: bugfix this...
//		travelTimes.put(TransportMode.ride, new RideTravelTimeFactory(carTravelTime,
//				new WalkTravelTimeFactory(configGroup).createTravelTime()).createTravelTime());
//		travelTimes.put(TransportMode.pt, new PTTravelTimeFactory(configGroup, 
//				carTravelTime, new WalkTravelTimeFactory(configGroup).createTravelTime()).createTravelTime());
		
		ModeRouteFactory modeRouteFactory = new ModeRouteFactory();
		modeRouteFactory.setRouteFactory(TransportMode.car, new LinkNetworkRouteFactory());
		modeRouteFactory.setRouteFactory(TransportMode.ride, new LinkNetworkRouteFactory());
		modeRouteFactory.setRouteFactory(TransportMode.bike, new LinkNetworkRouteFactory());
		modeRouteFactory.setRouteFactory(TransportMode.walk, new LinkNetworkRouteFactory());
		modeRouteFactory.setRouteFactory(TransportMode.pt, new LinkNetworkRouteFactory());
				
		// create Router Factory
		LeastCostPathCalculatorFactory routerFactory = new FastAStarLandmarksFactory(network, new FreespeedTravelTimeAndDisutility(config.planCalcScore()));

		Map<String, LegRouter> legRouters = new HashMap<String, LegRouter>();
				
		// Define restrictions for the different modes.
		// Car
		Set<String> carModeRestrictions = new HashSet<String>();
		carModeRestrictions.add(TransportMode.car);
		
		// Walk
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
		
		TravelTime travelTime;
		TravelDisutility travelDisutility;
		LeastCostPathCalculator routeAlgo;
		TravelDisutilityFactory travlDisutilityFactory = new Builder();
		TransportModeNetworkFilter networkFilter = new TransportModeNetworkFilter(network);
		for (String mode : modesToReroute) {
			
			Set<String> modeRestrictions;
			if (mode.equals(TransportMode.car)) {
				modeRestrictions = carModeRestrictions;
			}
			else if (mode.equals(TransportMode.walk)) {
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
			
			travelTime = travelTimes.get(mode); 
			
			travelDisutility = travlDisutilityFactory.createTravelDisutility(travelTime, config.planCalcScore());
			
			routeAlgo = routerFactory.createPathCalculator(subNetwork, travelDisutility, travelTime);
			legRouters.put(mode, new NetworkLegRouter(network, routeAlgo, modeRouteFactory));			
		}
		
		return legRouters;
	}

}
