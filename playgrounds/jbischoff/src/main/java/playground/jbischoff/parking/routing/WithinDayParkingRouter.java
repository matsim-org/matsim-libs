/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

package playground.jbischoff.parking.routing;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;

import playground.jbischoff.parking.ParkingUtils;

/**
 * @author  jbischoff
 *
 */
public class WithinDayParkingRouter implements ParkingRouter {

	private LeastCostPathCalculator pathCalculator; 
	private Network network;
	public WithinDayParkingRouter(LeastCostPathCalculator pathCalculator, Network network ) {
		this.pathCalculator = pathCalculator;
		this.network = network;
	}

	@Override
	public NetworkRoute getRouteFromParkingToDestination(NetworkRoute originalIntendedRoute, double departureTime, Id<Link> startLinkId) {
		Link startLink = this.network.getLinks().get(startLinkId);
		Link endLink = this.network.getLinks().get(originalIntendedRoute.getEndLinkId());
		
		Path path = this.pathCalculator.calcLeastCostPath(startLink.getToNode(), endLink.getFromNode(), 
				departureTime, null, null) ;
		NetworkRoute carRoute = new LinkNetworkRouteImpl(startLinkId, endLink.getId());
		carRoute.setLinkIds(startLink.getId(), NetworkUtils.getLinkIds( path.links), endLink.getId());
		carRoute.setTravelTime( path.travelTime );
		carRoute.setDistance(path.travelCost);
		
		return carRoute;
	}

}
