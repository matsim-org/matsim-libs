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

package org.matsim.core.router;

import java.util.HashMap;
import java.util.Map;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

public class MultiNodeDijkstraFactory implements LeastCostPathCalculatorFactory {

	private final boolean searchAllEndNodes;
	private final boolean usePreProcessData;
	private final Map<Network, PreProcessDijkstra> preProcessData = new HashMap<>();
	
	public MultiNodeDijkstraFactory() {
		this.searchAllEndNodes = false;
		this.usePreProcessData = false;
	}
	
	public MultiNodeDijkstraFactory(final boolean searchAllEndNodes) {
		this.searchAllEndNodes = searchAllEndNodes;
		this.usePreProcessData = false;
	}

	public MultiNodeDijkstraFactory(final boolean usePreProcessData, final boolean searchAllEndNodes) {
		this.usePreProcessData = usePreProcessData;
		this.searchAllEndNodes = searchAllEndNodes;
	}

	@Override
	public synchronized LeastCostPathCalculator createPathCalculator(final Network network, final TravelDisutility travelCosts, final TravelTime travelTimes) {		
		if (this.usePreProcessData) {
			PreProcessDijkstra preProcessDijkstra = this.preProcessData.get(network);
			if (preProcessDijkstra == null) {
				preProcessDijkstra = new PreProcessDijkstra();
				preProcessDijkstra.run(network);
				this.preProcessData.put(network, preProcessDijkstra);
			}
			return new MultiNodeDijkstra(network, travelCosts, travelTimes, preProcessDijkstra, this.searchAllEndNodes);
		}
		
		return new MultiNodeDijkstra(network, travelCosts, travelTimes, this.searchAllEndNodes);
	}
}