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
	private final boolean usePreProcessData;
	private final RoutingNetworkFactory routingNetworkFactory;
	private final Map<Network, RoutingNetwork> routingNetworks = new HashMap<>();
	private final Map<Network, PreProcessDijkstra> preProcessData = new HashMap<>();
	
	public FastMultiNodeDijkstraFactory() {
		this(false);
	}
	
	public FastMultiNodeDijkstraFactory(boolean searchAllEndNodes) {
		this(false, searchAllEndNodes);
	}
		
	public FastMultiNodeDijkstraFactory(final boolean usePreProcessData, final boolean searchAllEndNodes) {
		this.usePreProcessData = usePreProcessData;
		this.searchAllEndNodes = searchAllEndNodes;
		this.routingNetworkFactory = new ArrayRoutingNetworkFactory();
	}

	@Override
	public synchronized LeastCostPathCalculator createPathCalculator(final Network network, final TravelDisutility travelCosts, final TravelTime travelTimes) {
		
		RoutingNetwork routingNetwork = this.routingNetworks.get(network);
		PreProcessDijkstra preProcessDijkstra = this.preProcessData.get(network);

		if (routingNetwork == null) {
			routingNetwork = this.routingNetworkFactory.createRoutingNetwork(network);
			
			if (this.usePreProcessData) {
				if (preProcessDijkstra == null) {
					preProcessDijkstra = new PreProcessDijkstra();
					preProcessDijkstra.run(network);
					this.preProcessData.put(network, preProcessDijkstra);
				}
				if (preProcessDijkstra.containsData()) {
					for (RoutingNetworkNode node : routingNetwork.getNodes().values()) {
						node.setDeadEndData(preProcessDijkstra.getNodeData(node.getNode()));
					}
				}
			}
			
			this.routingNetworks.put(network, routingNetwork);
		}
		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();
		
		return new FastMultiNodeDijkstra(routingNetwork, travelCosts, travelTimes, preProcessDijkstra, fastRouterFactory, this.searchAllEndNodes);
	}
}