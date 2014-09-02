/* *********************************************************************** *
 * project: org.matsim.*
 * FastTransitDijkstraFactory.java
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

package org.matsim.pt.router.util;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.ArrayFastRouterDelegateFactory;
import org.matsim.core.router.FastRouterDelegateFactory;
import org.matsim.core.router.FastRouterType;
import org.matsim.core.router.util.ArrayRoutingNetworkFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.RoutingNetwork;
import org.matsim.core.router.util.RoutingNetworkFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.FastTransitMultiNodeDijkstra;

public class FastTransitDijkstraFactory implements LeastCostPathCalculatorFactory {
	
	private final PreProcessDijkstra preProcessData;
	private final RoutingNetworkFactory routingNetworkFactory;
	private final Map<Network, RoutingNetwork> routingNetworks;
	
	public FastTransitDijkstraFactory() {
		this(null, FastRouterType.ARRAY);
	}
	
	@Deprecated
	public FastTransitDijkstraFactory(FastRouterType fastRouterType) {
		this(null, fastRouterType);
	}
	
	public FastTransitDijkstraFactory(final PreProcessDijkstra preProcessData) {
		this(preProcessData, FastRouterType.ARRAY);
	}
	
	public FastTransitDijkstraFactory(final PreProcessDijkstra preProcessData, FastRouterType fastRouterType) {
		this.preProcessData = preProcessData;
		
		this.routingNetworks = new HashMap<Network, RoutingNetwork>();
		
		switch (fastRouterType) {
		case ARRAY:
			this.routingNetworkFactory = new ArrayRoutingNetworkFactory(preProcessData);
			break;
		case POINTER:
			throw new RuntimeException("PointerRoutingNetworks are no longer supported. "
					+ "Use ArrayRoutingNetworks instead. Aborting!");
		default:
			throw new RuntimeException("Undefined FastRouterType: " + fastRouterType);
		}
	}

	@Override
	public LeastCostPathCalculator createPathCalculator(final Network network, final TravelDisutility travelCosts, final TravelTime travelTimes) {
		
		RoutingNetwork routingNetwork = this.routingNetworks.get(network);
		if (routingNetwork == null) {
			routingNetwork = this.routingNetworkFactory.createRoutingNetwork(network);
			this.routingNetworks.put(network, routingNetwork);
		}
		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();	
		
		return new FastTransitMultiNodeDijkstra(network, travelCosts, travelTimes, preProcessData, routingNetwork, fastRouterFactory);
	}
}