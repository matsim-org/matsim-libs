/* *********************************************************************** *
 * project: org.matsim.*
 * FastAStarLandmarksFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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

package org.matsim.core.router.util;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.ArrayFastRouterDelegateFactory;
import org.matsim.core.router.FastAStarLandmarks;
import org.matsim.core.router.FastRouterDelegateFactory;
import org.matsim.core.router.FastRouterType;
import org.matsim.core.router.PointerFastRouterDelegateFactory;

/**
 * @author cdobler
 */
public class FastAStarLandmarksFactory implements LeastCostPathCalculatorFactory {

	private final FastRouterType fastRouterType;
	private final PreProcessLandmarks preProcessData;
	private final RoutingNetworkFactory routingNetworkFactory;
	private final Map<Network, RoutingNetwork> routingNetworks;
	
	public FastAStarLandmarksFactory(Network network, final TravelDisutility fsttc) {
		this(network, fsttc, FastRouterType.ARRAY);
	}
	
	public FastAStarLandmarksFactory(Network network, final TravelDisutility fsttc,
			FastRouterType fastRouterType) {
		this.preProcessData = new PreProcessLandmarks(fsttc);
		this.preProcessData.run(network);
		this.fastRouterType = fastRouterType;
		
		this.routingNetworks = new HashMap<Network, RoutingNetwork>();
		
		if (fastRouterType == FastRouterType.ARRAY) {
			this.routingNetworkFactory = new ArrayRoutingNetworkFactory(preProcessData);
		} else if (fastRouterType == FastRouterType.POINTER) {			
			this.routingNetworkFactory = new PointerRoutingNetworkFactory(preProcessData);
		} else {
			throw new RuntimeException("Undefined FastRouterType: " + fastRouterType);
		}
	}

	@Override
	public LeastCostPathCalculator createPathCalculator(Network network,
			TravelDisutility travelCosts, TravelTime travelTimes) {
		
		FastRouterDelegateFactory fastRouterFactory = null;
		RoutingNetwork rn = null;
		
		if (fastRouterType == FastRouterType.ARRAY) {
			RoutingNetwork routingNetwork = this.routingNetworks.get(network);
			if (routingNetwork == null) {
				routingNetwork = this.routingNetworkFactory.createRoutingNetwork(network);
				this.routingNetworks.put(network, routingNetwork);
			}
			rn = routingNetwork;
			fastRouterFactory = new ArrayFastRouterDelegateFactory();
		} else if (fastRouterType == FastRouterType.POINTER) {
			// Create a new instance since routing data is stored in the network!
			rn = this.routingNetworkFactory.createRoutingNetwork(network);
			fastRouterFactory = new PointerFastRouterDelegateFactory();			
		}		
		
		return new FastAStarLandmarks(network, this.preProcessData, travelCosts, travelTimes, 1,
				rn, fastRouterFactory);
	}
}