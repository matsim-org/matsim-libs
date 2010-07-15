/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.mrieser.routedistance;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.routes.NetworkRoute;

/**
 * @author mrieser
 */
public class NetworkRouteDistanceCalculator implements RouteDistanceCalculator {

	private final Network network;

	public NetworkRouteDistanceCalculator(Network network) {
		this.network = network;
	}

	@Override
	public double calcDistance(Route route) {
		if (!(route instanceof NetworkRoute)) {
			throw new IllegalArgumentException("wrong type of route, expected NetworkRoute, but was " + route.getClass());
		}
		NetworkRoute r = (NetworkRoute) route;
		double dist = 0;
		for (Id linkId : r.getLinkIds()) {
			dist += this.network.getLinks().get(linkId).getLength();
		}
		return dist;
	}

}
