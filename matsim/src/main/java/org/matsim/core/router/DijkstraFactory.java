/* *********************************************************************** *
 * project: org.matsim.*
 * DijkstraFactory.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import jakarta.inject.Inject;
import jakarta.inject.Singleton;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

@Singleton
public class DijkstraFactory implements LeastCostPathCalculatorFactory {

	private final boolean usePreProcessData;
	private final Map<Network, PreProcessDijkstra> preProcessData = new HashMap<>();

	@Inject
	public DijkstraFactory() {
		this.usePreProcessData = false;
	}

	public DijkstraFactory(final boolean usePreProcessData) {
		this.usePreProcessData = usePreProcessData;
	}

	// yy there is no guarantee that "createPathCalculator" is called with the same network as the one that was used for "preProcessData".
	// This can happen for example when LinkToLink routing is switched on.  kai & theresa, feb'15
	// To fix this, we create the PreProcessData when the first LeastCostPathCalculator object is created and store it in a map using
	// the network as key. For the PreProcessDijkstra data this is fine, since it does not take travel times and disutilities into account.
	// For the AStarLandmarks data, we would have to include the other two arguments into the lookup value as well... cdobler, sep'17
	@Override
	public synchronized LeastCostPathCalculator createPathCalculator(final Network network, final TravelDisutility travelCosts, final TravelTime travelTimes) {
		if (this.usePreProcessData) {
			PreProcessDijkstra preProcessDijkstra = this.preProcessData.get(network);
			if (preProcessDijkstra == null) {
				preProcessDijkstra = new PreProcessDijkstra();
				preProcessDijkstra.run(network);
				this.preProcessData.put(network, preProcessDijkstra);
			}
			return new Dijkstra(network, travelCosts, travelTimes, preProcessDijkstra);
		}
		return new Dijkstra(network, travelCosts, travelTimes);
	}
}
