/* *********************************************************************** *
 * project: org.matsim.*
 * FastBackwardsMultiNodeDijkstraFactory.java
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

package org.matsim.contrib.locationchoice.router;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.ArrayFastRouterDelegateFactory;
import org.matsim.core.router.FastRouterDelegateFactory;
import org.matsim.core.router.util.*;

import java.util.HashMap;
import java.util.Map;

/**
 * Creates a MultiNodeDijkstra which is routing backwards. To do so,
 * The network is converted into an inverse routing network, i.e. in- and
 * outgoing links are exchanged.
 * 
 * @author cdobler
 */
public class BackwardsFastMultiNodeDijkstraFactory implements LeastCostPathCalculatorFactory {
	
	private final boolean searchAllEndNodes;
	private final PreProcessDijkstra preProcessData;
	private final RoutingNetworkFactory routingNetworkFactory;
	private final Map<Network, RoutingNetwork> routingNetworks;

    public BackwardsFastMultiNodeDijkstraFactory(final boolean searchAllEndNodes) {
		this(null, searchAllEndNodes);
	}
		
	public BackwardsFastMultiNodeDijkstraFactory(final PreProcessDijkstra preProcessData, final boolean searchAllEndNodes) {
		this.preProcessData = preProcessData;
		this.searchAllEndNodes = searchAllEndNodes;
		
		this.routingNetworks = new HashMap<>();
		this.routingNetworkFactory = new InverseArrayRoutingNetworkFactory(preProcessData);
	}

	@Override
	public LeastCostPathCalculator createPathCalculator(final Network network, final TravelDisutility travelCosts, final TravelTime travelTimes) {
		
		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();
		RoutingNetwork routingNetwork = this.routingNetworks.get(network);
		if (routingNetwork == null) {
			routingNetwork = this.routingNetworkFactory.createRoutingNetwork(network);
			this.routingNetworks.put(network, routingNetwork);
		}
		
		return new BackwardFastMultiNodeDijkstra(routingNetwork, travelCosts, travelTimes, 
				this.preProcessData, fastRouterFactory, this.searchAllEndNodes);
	}
}