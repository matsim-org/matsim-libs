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

package org.matsim.contrib.parking.parkingsearch.routing;

import javax.inject.Inject;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.dvrp.trafficmonitoring.DvrpTravelTimeModule;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;

import com.google.inject.name.Named;

/**
 * @author jbischoff
 *
 */
public class WithinDayParkingRouter implements ParkingRouter {

	private Network network;

	private TravelTime travelTime;
	private TravelDisutility travelDisutility;
	private LeastCostPathCalculator pathCalculator;

	@Inject
	WithinDayParkingRouter(@Named(DvrpTravelTimeModule.DVRP_ESTIMATED) TravelTime travelTime, Network network) {
		this.travelTime = travelTime;
		travelDisutility = new TimeAsTravelDisutility(this.travelTime);
		this.network = network;
		pathCalculator = new DijkstraFactory().createPathCalculator(network, travelDisutility, this.travelTime);
	}

	@Override
	public NetworkRoute getRouteFromParkingToDestination(Id<Link> destinationLinkId, double departureTime,
			Id<Link> startLinkId) {

		
		Link startLink = this.network.getLinks().get(startLinkId);
		Link endLink = this.network.getLinks().get(destinationLinkId);

		Path path = this.pathCalculator.calcLeastCostPath(startLink.getToNode(), endLink.getFromNode(), departureTime,
				null, null);
		NetworkRoute carRoute = RouteUtils.createLinkNetworkRouteImpl(startLinkId, endLink.getId());
		carRoute.setLinkIds(startLink.getId(), NetworkUtils.getLinkIds(path.links), endLink.getId());
		carRoute.setTravelTime(path.travelTime);
		double distance = RouteUtils.calcDistance(carRoute, 1.0, 1.0, network);
		carRoute.setDistance(distance);

		return carRoute;
	}

}
