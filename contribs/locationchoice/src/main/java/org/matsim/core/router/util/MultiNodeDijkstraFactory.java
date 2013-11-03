/* *********************************************************************** *
 * project: org.matsim.*
 * MultiNodeDijkstraFactory.java
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

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.MultiNodeDijkstra;

public class MultiNodeDijkstraFactory implements LeastCostPathCalculatorFactory {

	private final boolean searchAllEndNodes;
	private final PreProcessDijkstra preProcessData;
	
	public MultiNodeDijkstraFactory() {
		this.preProcessData = null;
		this.searchAllEndNodes = false;
	}
	
	public MultiNodeDijkstraFactory(final boolean searchAllEndNodes) {
		this.preProcessData = null;
		this.searchAllEndNodes = searchAllEndNodes;
	}

	public MultiNodeDijkstraFactory(final PreProcessDijkstra preProcessData, final boolean searchAllEndNodes) {
		this.preProcessData = preProcessData;
		this.searchAllEndNodes = searchAllEndNodes;
	}

	@Override
	public LeastCostPathCalculator createPathCalculator(final Network network, final TravelDisutility travelCosts, final TravelTime travelTimes) {
		if (this.preProcessData == null) {
			return new MultiNodeDijkstra(network, travelCosts, travelTimes, searchAllEndNodes);
		}
		return new MultiNodeDijkstra(network, travelCosts, travelTimes, preProcessData, searchAllEndNodes);
	}

}
