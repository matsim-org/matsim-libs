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

package org.matsim.pt.router;

import java.util.List;

/**
 * 
 * @author aneumann
 *
 */
public class TransitPassengerRoute {

	private final double cost;
	private final List<RouteSegment> route;

	public TransitPassengerRoute(double cost, List<RouteSegment> leastCostRoute) {
		this.cost = cost;
		this.route = leastCostRoute;
		
	}

	public double getTravelCost() {
		return this.cost;
	}
	
	public List<RouteSegment> getRoute() {
		return this.route;
	}

	@Override
	public String toString() {
		return "Cost: " + this.cost + " via " + this.route;
	}
}
