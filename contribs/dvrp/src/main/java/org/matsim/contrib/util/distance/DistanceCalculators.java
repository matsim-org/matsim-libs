/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.util.distance;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.router.DijkstraWithDijkstraTreeCache;
import org.matsim.contrib.dvrp.router.DistanceAsTravelDisutility;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.util.TimeDiscretizer;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

public class DistanceCalculators {
	public static DistanceCalculator crateFreespeedDistanceCalculator(Network network) {
		return crateFreespeedBasedCalculator(network, new DistanceAsTravelDisutility());
	}

	public static DistanceCalculator crateFreespeedTimeCalculator(Network network) {
		return crateFreespeedBasedCalculator(network, new TimeAsTravelDisutility(new FreeSpeedTravelTime()));
	}

	private static DistanceCalculator crateFreespeedBasedCalculator(Network network,
			TravelDisutility travelDisutility) {
		final DijkstraWithDijkstraTreeCache dijkstraTree = new DijkstraWithDijkstraTreeCache(network, travelDisutility,
				new FreeSpeedTravelTime(), TimeDiscretizer.CYCLIC_24_HOURS);

		return new DistanceCalculator() {
			@Override
			public double calcDistance(Coord from, Coord to) {
				Network networkImpl = (Network)network;
				final Coord coord = from;
				Node fromNode = NetworkUtils.getNearestNode(networkImpl, coord);
				final Coord coord1 = to;
				Node toNode = NetworkUtils.getNearestNode(networkImpl, coord1);
				return dijkstraTree.calcLeastCostPath(fromNode, toNode, 0, null, null).travelCost;
			}
		};
	}
}
