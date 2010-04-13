/* *********************************************************************** *
 * project: org.matsim.*
 * SubNetworkDijkstraFactory.java
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

package playground.christoph.router.util;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

import playground.christoph.router.SubNetworkDijkstra;
import playground.christoph.router.costcalculators.SubNetworkDijkstraTravelCostWrapper;

public class SubNetworkDijkstraFactory extends DijkstraFactory {

	private static final Logger log = Logger.getLogger(SubNetworkDijkstraFactory.class);
	
	@Override
	public LeastCostPathCalculator createPathCalculator(final Network network, final TravelCost travelCosts, final TravelTime travelTimes) {
		
		if (travelCosts instanceof SubNetworkDijkstraTravelCostWrapper)
		{
			SubNetworkDijkstra subNetworkDijkstra = new SubNetworkDijkstra(network, travelCosts, travelTimes);
			((SubNetworkDijkstraTravelCostWrapper) travelCosts).setSubNetworkDijkstra(subNetworkDijkstra);
			return subNetworkDijkstra;
		}
		
		log.warn("A " + SubNetworkDijkstraTravelCostWrapper.class.toString() + " object was expected but a " + 
				travelCosts.getClass().toString()+  " was found! Returning a default Dijkstra.");
		
		return new Dijkstra(network, travelCosts, travelTimes);
	}
}
