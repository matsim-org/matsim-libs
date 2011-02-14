/* *********************************************************************** *
 * project: org.matsim.*
 * DijkstraWithTightTurnPenaltyFactory.java
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

package playground.yu.replanning;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

public class DijkstraWithTightTurnPenaltyFactory implements
		LeastCostPathCalculatorFactory {

	private final PreProcessDijkstra preProcessData;

	public DijkstraWithTightTurnPenaltyFactory() {
		preProcessData = null;
	}

	public DijkstraWithTightTurnPenaltyFactory(
			final PreProcessDijkstra preProcessData) {
		this.preProcessData = preProcessData;
	}

	public LeastCostPathCalculator createPathCalculator(final Network network,
			final TravelCost travelCosts, final TravelTime travelTimes) {
		if (preProcessData == null) {
			return new DijkstraWithTightTurnPenalty(network, travelCosts,
					travelTimes);
		}
		return new DijkstraWithTightTurnPenalty(network, travelCosts,
				travelTimes, preProcessData);
	}

}
