/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.core.router.speedy;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

/**
 * @author Michal Maciejewski (michalm)
 */
public class SpeedyMultiSourceALTFactory {
	private final Map<Network, SpeedyGraph> graphs = new ConcurrentHashMap<>();
	private final Map<Pair<SpeedyGraph, TravelDisutility>, SpeedyALTData> landmarksData = new ConcurrentHashMap<>();

	public SpeedyMultiSourceALT createPathCalculator(Network network, TravelDisutility travelCosts,
			TravelTime travelTimes) {
		SpeedyGraph graph = this.graphs.computeIfAbsent(network, SpeedyGraphBuilder::build);

		var graphTravelCostsPair = Pair.of(graph, travelCosts);
		int landmarksCount = Math.min(16, graph.nodeCount);
		SpeedyALTData landmarks = this.landmarksData.computeIfAbsent(graphTravelCostsPair,
				p -> new SpeedyALTData(p.getLeft(), landmarksCount, p.getRight()));

		return new SpeedyMultiSourceALT(landmarks, travelTimes, travelCosts);
	}
}
