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

package org.matsim.core.router;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.router.util.ArrayRoutingNetworkFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.RoutingNetwork;
import org.matsim.core.router.util.RoutingNetworkFactory;
import org.matsim.core.router.util.RoutingNetworkNode;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author cdobler
 */
@Singleton
public class FastAStarLandmarksFactory implements LeastCostPathCalculatorFactory {
	
	private final RoutingNetworkFactory routingNetworkFactory;
	private final Map<Network, RoutingNetwork> routingNetworks = new HashMap<>();
	private final Map<Network, PreProcessLandmarks> preProcessData = new HashMap<>();
	
	@Inject GlobalConfigGroup globalConfig ;

	@Inject
	public FastAStarLandmarksFactory() {
		this(FastRouterType.ARRAY);
	}

	private FastAStarLandmarksFactory(final FastRouterType fastRouterType) {
		switch (fastRouterType) {
		case ARRAY:
			this.routingNetworkFactory = new ArrayRoutingNetworkFactory();
			break;
		case POINTER:
			throw new RuntimeException("PointerRoutingNetworks are no longer supported. Use ArrayRoutingNetworks instead. Aborting!");
		default:
			throw new RuntimeException("Undefined FastRouterType: " + fastRouterType);
		}
	}

	@Override
	public synchronized LeastCostPathCalculator createPathCalculator(final Network network, final TravelDisutility travelCosts, final TravelTime travelTimes) {
		RoutingNetwork routingNetwork = this.routingNetworks.get(network);
		PreProcessLandmarks preProcessLandmarks = this.preProcessData.get(network);
		
		if (routingNetwork == null) {
			routingNetwork = this.routingNetworkFactory.createRoutingNetwork(network);
			
			if (preProcessLandmarks == null) {
				preProcessLandmarks = new PreProcessLandmarks(travelCosts);
				if ( globalConfig==null ) {
					preProcessLandmarks.setNumberOfThreads(8);
					// (if used without injection.  not so beautiful. kai, nov'17)
				} else {
					preProcessLandmarks.setNumberOfThreads(globalConfig.getNumberOfThreads());
				}
				preProcessLandmarks.run(network);
				this.preProcessData.put(network, preProcessLandmarks);
				
				for (RoutingNetworkNode node : routingNetwork.getNodes().values()) {
					node.setDeadEndData(preProcessLandmarks.getNodeData(node.getNode()));
				}
			}				
			
			this.routingNetworks.put(network, routingNetwork);
		}
		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();
		
		final double overdoFactor = 1.0;
		return new FastAStarLandmarks(routingNetwork, preProcessLandmarks, travelCosts, travelTimes, overdoFactor, fastRouterFactory);
	}
}