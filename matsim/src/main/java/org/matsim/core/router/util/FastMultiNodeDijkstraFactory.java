/* *********************************************************************** *
 * project: org.matsim.*
 * FastMultiNodeDijkstraFactory.java
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

package org.matsim.core.router.util;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.ArrayFastRouterDelegateFactory;
import org.matsim.core.router.FastMultiNodeDijkstra;
import org.matsim.core.router.FastRouterDelegateFactory;

public class FastMultiNodeDijkstraFactory implements LeastCostPathCalculatorFactory {
	
	private final boolean searchAllEndNodes;
	private final PreProcessDijkstra preProcessData;
	private final RoutingNetworkFactory routingNetworkFactory;
	private final Map<Network, RoutingNetwork> routingNetworks;
	
	public FastMultiNodeDijkstraFactory() {
		this(false);
	}
	
	public FastMultiNodeDijkstraFactory(boolean searchAllEndNodes) {
		this(null, searchAllEndNodes);
	}
		
	public FastMultiNodeDijkstraFactory(final PreProcessDijkstra preProcessData, final boolean searchAllEndNodes) {
		this.preProcessData = preProcessData;
		this.searchAllEndNodes = searchAllEndNodes;
		
		this.routingNetworks = new HashMap<Network, RoutingNetwork>();
		this.routingNetworkFactory = new ArrayRoutingNetworkFactory(preProcessData);
	}

	@Override
	public LeastCostPathCalculator createPathCalculator(final Network network, final TravelDisutility travelCosts, final TravelTime travelTimes) {
		
		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();
		RoutingNetwork routingNetwork = this.routingNetworks.get(network);
		if (routingNetwork == null) {
			routingNetwork = this.routingNetworkFactory.createRoutingNetwork(network);
			this.routingNetworks.put(network, routingNetwork);
		}
		
		return new FastMultiNodeDijkstra(routingNetwork, travelCosts, travelTimes, 
				this.preProcessData, fastRouterFactory, this.searchAllEndNodes);
	}
}