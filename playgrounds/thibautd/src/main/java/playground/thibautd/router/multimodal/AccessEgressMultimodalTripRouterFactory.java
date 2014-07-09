/* *********************************************************************** *
 * project: org.matsim.*
 * AccessEgressMultimodalTripRouterFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
package playground.thibautd.router.multimodal;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.multimodal.config.MultiModalConfigGroup;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.RoutingContext;
import org.matsim.core.router.TripRouter;
import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.costcalculators.TravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.CollectionUtils;

/**
 * @author thibautd
 */
public class AccessEgressMultimodalTripRouterFactory implements TripRouterFactory {
	private static final Logger log =
		Logger.getLogger(AccessEgressMultimodalTripRouterFactory.class);

	private final Scenario scenario;
	private final TravelDisutilityFactory travelDisutilityFactory;
	private final Map<String, TravelTime> multimodalTravelTimes;
	private final TripRouterFactory delegateFactory;
	private final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory;
	
	private final Map<String, Network> multimodalSubNetworks = new HashMap<String, Network>();
	
	public AccessEgressMultimodalTripRouterFactory(
			final Scenario scenario,
			final Map<String, TravelTime> multimodalTravelTimes,
			final TravelDisutilityFactory travelDisutilityFactory,
			final TripRouterFactory delegateFactory, 
			final LeastCostPathCalculatorFactory leastCostPathCalculatorFactory) {
		this.scenario = scenario;
		this.multimodalTravelTimes = multimodalTravelTimes;
		this.travelDisutilityFactory = travelDisutilityFactory;
		this.delegateFactory = delegateFactory;
		this.leastCostPathCalculatorFactory = leastCostPathCalculatorFactory;
	}
	
	@Override
	public TripRouter instantiateAndConfigureTripRouter(RoutingContext routingContext) {
		final TripRouter instance = this.delegateFactory.instantiateAndConfigureTripRouter(routingContext);
		
		final Network network = this.scenario.getNetwork();

        final MultiModalConfigGroup multiModalConfigGroup = (MultiModalConfigGroup) scenario.getConfig().getModule(MultiModalConfigGroup.GROUP_NAME);
        final Set<String> simulatedModes = CollectionUtils.stringToSet(multiModalConfigGroup.getSimulatedModes());
		for (String mode : simulatedModes) {

			if (instance.getRegisteredModes().contains(mode)) {
				log.warn("A routing algorithm for " + mode + " is already registered. It is replaced!");
			}
			
			final TravelTime travelTime = this.multimodalTravelTimes.get(mode);
			if (travelTime == null) {
				throw new RuntimeException("No travel time object was found for mode " + mode + "! Aborting.");
			}
			
			final Network subNetwork = getSubnetwork(network, mode);
			
			/*
			 * We cannot use the travel disutility object from the routingContext since it
			 * has not been created for the modes used here.
			 */
			final TravelDisutility travelDisutility = this.travelDisutilityFactory.createTravelDisutility(travelTime, scenario.getConfig().planCalcScore());		
			final LeastCostPathCalculator routeAlgo =
				new CachingLeastCostPathAlgorithmWrapper(
						travelTime,
						travelDisutility,
						this.leastCostPathCalculatorFactory.createPathCalculator(
								subNetwork,
								travelDisutility,
								travelTime) );
			final double crowFlyDistanceFactor = scenario.getConfig().plansCalcRoute().getBeelineDistanceFactor();
			final double crowFlySpeed = scenario.getConfig().plansCalcRoute().getTeleportedModeSpeeds().get( mode );
			instance.setRoutingModule(
					mode,
					new AccessEgressNetworkBasedTeleportationRoutingModule(
							mode,
							scenario,
							subNetwork,
							crowFlyDistanceFactor,
							crowFlySpeed,
							routeAlgo) ); 
		}

		return instance;
	}

	private Network getSubnetwork(final Network network,final String mode) {
		Network subNetwork = multimodalSubNetworks.get(mode);
		
		if (subNetwork == null) {
			subNetwork = NetworkImpl.createNetwork();
			final Set<String> restrictions = new HashSet<String>();
			restrictions.add(mode);
			final TransportModeNetworkFilter networkFilter = new TransportModeNetworkFilter(network);
			networkFilter.filter(subNetwork, restrictions);
			this.multimodalSubNetworks.put(mode, subNetwork);
		}
		
		return subNetwork;
	}
}

