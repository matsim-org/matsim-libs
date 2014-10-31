/* *********************************************************************** *
 * project: org.matsim.*
 * FastDijkstraFactory.java
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

package playground.pieter.distributed;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.ArrayFastRouterDelegateFactory;
import org.matsim.core.router.FastDijkstra;
import org.matsim.core.router.FastRouterDelegateFactory;
import org.matsim.core.router.FastRouterType;
import org.matsim.core.router.util.*;

import java.util.HashMap;
import java.util.Map;

public class FastDijkstraFactoryWithCustomTravelTimes implements LeastCostPathCalculatorFactory {

	private final PreProcessDijkstra preProcessData;
	private final RoutingNetworkFactory routingNetworkFactory;
	private final Map<Network, RoutingNetwork> routingNetworks;

    public void setTravelTime(TravelTime travelTime) {
        this.travelTime = travelTime;
    }

    private TravelTime travelTime;

	public FastDijkstraFactoryWithCustomTravelTimes() {
		this(null, FastRouterType.ARRAY);
	}

	@Deprecated
	public FastDijkstraFactoryWithCustomTravelTimes(FastRouterType fastRouterType) {
		this(null, fastRouterType);
	}

	public FastDijkstraFactoryWithCustomTravelTimes(final PreProcessDijkstra preProcessData) {
		this(preProcessData, FastRouterType.ARRAY);
	}

	@Deprecated
	public FastDijkstraFactoryWithCustomTravelTimes(final PreProcessDijkstra preProcessData, FastRouterType fastRouterType) {
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

        if(travelTime == null)
            setTravelTime(travelTimes);
        //ignore travel times sent to the method and use the ones set for this class
		return new FastDijkstra(routingNetwork, travelCosts, travelTime, preProcessData, fastRouterFactory);
	}
}