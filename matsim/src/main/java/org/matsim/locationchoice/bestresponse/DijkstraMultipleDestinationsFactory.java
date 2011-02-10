/* *********************************************************************** *
 * project: org.matsim.*
 * FullNetworkDijkstraFactory.java
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

package org.matsim.locationchoice.bestresponse;

import org.matsim.api.core.v01.network.Network;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculatorFactory;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

public class DijkstraMultipleDestinationsFactory implements LeastCostPathCalculatorFactory {

	private String type = "forward";

	public void setType(String type) {
		this.type = type;
	}
	
	@Override
	public LeastCostPathCalculator createPathCalculator(Network network, TravelCost travelCosts, TravelTime travelTimes) {
		if (type.equals("forward")) {
			return new ForwardDijkstraMultipleDestinations(network, travelCosts, travelTimes);
		}
		else {
			return new BackwardDijkstraMultipleDestinations(network, travelCosts, travelTimes);
		}
	}
}