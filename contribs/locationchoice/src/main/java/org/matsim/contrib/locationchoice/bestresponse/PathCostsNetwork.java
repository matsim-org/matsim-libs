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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

public class PathCostsNetwork extends PathCosts {
	
	public PathCostsNetwork(Network network) {
		super(network);
	}
	
	public void createRoute(Id<Link> fromLinkId, Path path, Id<Link> toLinkId) {
		
		NetworkRoute networkRoute = new LinkNetworkRouteImpl(fromLinkId, toLinkId);
		if (!fromLinkId.equals(toLinkId)) {
			// do not drive/walk around, if we stay on the same link
//			path = this.routeAlgo.calcLeastCostPath(startNode, endNode, depTime, person, null);
//			if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
//			NetworkRoute route = this.routeFactory.createRoute(NetworkRoute.class, fromLink.getId(), toLink.getId());
			networkRoute.setLinkIds(fromLinkId, NetworkUtils.getLinkIds(path.links), toLinkId);
			networkRoute.setTravelTime(path.travelTime);
			networkRoute.setTravelCost(path.travelCost);
			networkRoute.setDistance(RouteUtils.calcDistanceExcludingStartEndLink(networkRoute, this.network));
		} else {
			// create an empty route == staying on place if toLink == endLink
			route.setTravelTime(0);
			route.setDistance(0.0);
		}
		this.route = networkRoute;
		
//		List<Id<Link>> linkIds = new ArrayList<Id<Link>>(); 
//		for (int i = 1; i < linksAllIncluded.size() - 2; i++) {
//			linkIds.add(linksAllIncluded.get(i).getId());
//		}
//		this.route = new LinkNetworkRouteImpl(linksAllIncluded.get(0).getId(), linkIds, linksAllIncluded.get(linksAllIncluded.size()-1).getId());
//		this.calculateNetworkRouteLength();
//		this.route.setTravelTime(traveltime);
	}
}
