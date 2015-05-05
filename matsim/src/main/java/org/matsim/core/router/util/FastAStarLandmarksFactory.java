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

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.ArrayFastRouterDelegateFactory;
import org.matsim.core.router.FastAStarLandmarks;
import org.matsim.core.router.FastRouterDelegateFactory;
import org.matsim.core.router.FastRouterType;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.HashMap;
import java.util.Map;

/**
 * @author cdobler
 */
@Singleton
public class FastAStarLandmarksFactory implements LeastCostPathCalculatorFactory {

	private final PreProcessLandmarks preProcessData;
	private final RoutingNetworkFactory routingNetworkFactory;
	private final Map<Network, RoutingNetwork> routingNetworks;

	@Inject
	public FastAStarLandmarksFactory(Network network, final TravelDisutility fsttc) {
		this(network, fsttc, FastRouterType.ARRAY);
	}

	private FastAStarLandmarksFactory(Network network, final TravelDisutility fsttc,
			FastRouterType fastRouterType) {
		this.preProcessData = new PreProcessLandmarks(fsttc);
		this.preProcessData.run(network);
		
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
	public LeastCostPathCalculator createPathCalculator(Network network,
			TravelDisutility travelCosts, TravelTime travelTimes) {
		
		RoutingNetwork routingNetwork = this.routingNetworks.get(network);
		if (routingNetwork == null) {
			routingNetwork = this.routingNetworkFactory.createRoutingNetwork(network);
			this.routingNetworks.put(network, routingNetwork);
		}
		FastRouterDelegateFactory fastRouterFactory = new ArrayFastRouterDelegateFactory();

		return new FastAStarLandmarks(routingNetwork, this.preProcessData, travelCosts, travelTimes, 1,
				fastRouterFactory);
	}
}