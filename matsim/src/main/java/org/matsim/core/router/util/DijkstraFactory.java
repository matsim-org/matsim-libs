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

package org.matsim.core.router.util;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.Dijkstra;

public class DijkstraFactory implements LeastCostPathCalculatorFactory {

	private final PreProcessDijkstra preProcessData;

	public DijkstraFactory() {
		this.preProcessData = null;
	}

	public DijkstraFactory(final PreProcessDijkstra preProcessData) {
		this.preProcessData = preProcessData;
	}

	@Override
	public LeastCostPathCalculator createPathCalculator(final Network network, final TravelDisutility travelCosts, final TravelTime travelTimes) {
		if (this.preProcessData == null) {
			return new Dijkstra(network, travelCosts, travelTimes);
		}
		return new Dijkstra(network, travelCosts, travelTimes, preProcessData);

		// yy there is no guarantee that "createPathCalculator" is called with the same network as the one that was used for "preProcessData".
		// This can happen for example when LinkToLink routing is switched on.  kai & theresa, feb'15,
		
	}

}
