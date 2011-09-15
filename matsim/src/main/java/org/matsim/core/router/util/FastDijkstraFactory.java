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

package org.matsim.core.router.util;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.FastDijkstra;

public class FastDijkstraFactory implements LeastCostPathCalculatorFactory {

	private final PreProcessDijkstra preProcessData;

	public FastDijkstraFactory() {
		this.preProcessData = null;
	}

	public FastDijkstraFactory(final PreProcessDijkstra preProcessData) {
		this.preProcessData = preProcessData;
	}

	@Override
	public LeastCostPathCalculator createPathCalculator(final Network network, final TravelCost travelCosts, final TravelTime travelTimes) {
		if (this.preProcessData == null) {
			return new FastDijkstra(network, travelCosts, travelTimes);
		}
		return new FastDijkstra(network, travelCosts, travelTimes, preProcessData);
	}
}