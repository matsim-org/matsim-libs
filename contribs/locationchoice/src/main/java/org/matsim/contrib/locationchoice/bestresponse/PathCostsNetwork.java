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

import java.util.ArrayList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;

public class PathCostsNetwork extends PathCosts {
	
	public PathCostsNetwork(Network network) {
		super(network);
	}

	public void createRoute(List<Link> linksAllIncluded, double traveltime) {
		List<Id<Link>> linkIds = new ArrayList<Id<Link>>(); 
		for (int i = 1; i < linksAllIncluded.size() - 2; i++) {
			linkIds.add(linksAllIncluded.get(i).getId());
		}
		this.route = new LinkNetworkRouteImpl(linksAllIncluded.get(0).getId(), linkIds, linksAllIncluded.get(linksAllIncluded.size()-1).getId());
		this.calculateNetworkRouteLength();
		this.route.setTravelTime(traveltime);
	}
		
	private void calculateNetworkRouteLength() {		
		this.route.setDistance(RouteUtils.calcDistance((NetworkRoute) route, network));
	}
}
