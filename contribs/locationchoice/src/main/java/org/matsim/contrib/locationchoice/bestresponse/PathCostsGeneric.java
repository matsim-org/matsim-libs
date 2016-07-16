/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice.bestresponse;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.utils.geometry.CoordUtils;

public class PathCostsGeneric extends PathCosts {
	
	public PathCostsGeneric(Network network) {
		super(network);
	}

	public void createRoute(final Link startLink, final Link endLink, double beelineDistanceFactor, double speed) {
			this.route = new GenericRouteImpl(startLink.getId(), endLink.getId());
			this.calculateDirectDistance(startLink, endLink, beelineDistanceFactor);
			this.route.setTravelTime(this.route.getDistance() / speed);
	}
	
	private void calculateDirectDistance(Link startLink, Link endLink, double beelineDistanceFactor) {
		double distance = CoordUtils.calcEuclideanDistance(startLink.getCoord(), endLink.getCoord());
		this.route.setDistance(beelineDistanceFactor * distance);
	}
}