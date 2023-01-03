/* *********************************************************************** *
 * project: org.matsim.*
 * FastAStarEuclideanFactory.java
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

package org.matsim.core.router;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.ArrayRoutingNetworkFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessEuclidean;
import org.matsim.core.router.util.RoutingNetwork;
import org.matsim.core.router.util.RoutingNetworkFactory;
import org.matsim.core.router.util.RoutingNetworkNode;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author cdobler
 */
public class FastAStarEuclideanFactory implements LeastCostPathCalculatorFactory {

	private final RoutingNetworkFactory routingNetworkFactory;
	private final Map<Network, RoutingNetwork> routingNetworks = new HashMap<>();
	private final Map<Network, PreProcessEuclidean> preProcessData = new HashMap<>();
	private final double overdoFactor;

	public FastAStarEuclideanFactory() {
		this(1);
	}

	public FastAStarEuclideanFactory(double overdoFactor) {
		this(FastRouterType.ARRAY, overdoFactor);
	}

	private FastAStarEuclideanFactory(final FastRouterType fastRouterType, double overdoFactor) {
		this.overdoFactor = overdoFactor;
		switch (fastRouterType) {
			case ARRAY:
				this.routingNetworkFactory = new ArrayRoutingNetworkFactory();
				break;
			case POINTER:
				throw new RuntimeException(
						"PointerRoutingNetworks are no longer supported. Use ArrayRoutingNetworks instead. Aborting!");
			default:
				throw new RuntimeException("Undefined FastRouterType: " + fastRouterType);
		}
	}

	@Override
	public synchronized LeastCostPathCalculator createPathCalculator(final Network network,
			final TravelDisutility travelCosts, final TravelTime travelTimes) {
		RoutingNetwork routingNetwork = this.routingNetworks.get(network);
		PreProcessEuclidean preProcessEuclidean = this.preProcessData.get(network);

		if (routingNetwork == null) {
			routingNetwork = this.routingNetworkFactory.createRoutingNetwork(network);

			preProcessEuclidean = new PreProcessEuclidean(travelCosts);
			preProcessEuclidean.run(network);
			this.preProcessData.put(network, preProcessEuclidean);

			for (RoutingNetworkNode node : routingNetwork.getNodes().values()) {
				node.setDeadEndData(preProcessEuclidean.getNodeData(node.getNode()));
			}

			this.routingNetworks.put(network, routingNetwork);
		}
		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();

		return new FastAStarEuclidean(routingNetwork, preProcessEuclidean, travelCosts, travelTimes, overdoFactor,
				fastRouterFactory);
	}
}