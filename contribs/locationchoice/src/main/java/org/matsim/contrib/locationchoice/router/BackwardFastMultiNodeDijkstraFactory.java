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

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.ArrayFastRouterDelegateFactory;
import org.matsim.core.router.FastRouterDelegateFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.RoutingNetwork;
import org.matsim.core.router.util.RoutingNetworkFactory;
import org.matsim.core.router.util.RoutingNetworkNode;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * Creates a MultiNodeDijkstra which is routing backwards. To do so,
 * The network is converted into an inverse routing network, i.e. in- and
 * outgoing links are exchanged.
 *
 * @author cdobler
 */
public class BackwardFastMultiNodeDijkstraFactory implements LeastCostPathCalculatorFactory {

	private final boolean searchAllEndNodes;
	private final boolean usePreProcessData;
	private final RoutingNetworkFactory routingNetworkFactory;
	private final Map<Network, RoutingNetwork> routingNetworks = new HashMap<>();
	private final Map<Network, PreProcessDijkstra> preProcessData = new HashMap<>();

	public BackwardFastMultiNodeDijkstraFactory() {
		this(false);
	}

	public BackwardFastMultiNodeDijkstraFactory(final boolean searchAllEndNodes) {
		this(false, searchAllEndNodes);
	}

	public BackwardFastMultiNodeDijkstraFactory(final boolean usePreProcessData, final boolean searchAllEndNodes) {
		this.usePreProcessData = usePreProcessData;
		this.searchAllEndNodes = searchAllEndNodes;
		this.routingNetworkFactory = new InverseArrayRoutingNetworkFactory();
	}

	@Override
	public LeastCostPathCalculator createPathCalculator(final Network network, final TravelDisutility travelCosts,
			final TravelTime travelTimes) {

		RoutingNetwork routingNetwork = this.routingNetworks.get(network);
		PreProcessDijkstra preProcessDijkstra = this.preProcessData.get(network);

		if (routingNetwork == null) {
			routingNetwork = this.routingNetworkFactory.createRoutingNetwork(network);

			if (this.usePreProcessData) {
				preProcessDijkstra = new PreProcessDijkstra();
				preProcessDijkstra.run(network);
				this.preProcessData.put(network, preProcessDijkstra);
				if (preProcessDijkstra.containsData()) {
					for (RoutingNetworkNode node : routingNetwork.getNodes().values()) {
						node.setDeadEndData(preProcessDijkstra.getNodeData(node.getNode()));
					}
				}
			}

			this.routingNetworks.put(network, routingNetwork);
		}
		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();

		return new BackwardFastMultiNodeDijkstra(routingNetwork, travelCosts, travelTimes, preProcessDijkstra,
				fastRouterFactory, this.searchAllEndNodes);
	}
}