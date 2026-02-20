/* *********************************************************************** *

 * project: org.matsim.*
 * LegRouterWrapper.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package org.matsim.core.router;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.*;
import org.matsim.core.config.groups.RoutingConfigGroup.TeleportedModeParams;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.facilities.Facility;

import java.util.Arrays;
import java.util.List;

public final class FreespeedFactorRoutingModule implements RoutingModule {

	private final String mode;
	private final PopulationFactory populationFactory;

	private final Network network;
	private final LeastCostPathCalculator routeAlgo;
	private final TeleportedModeParams params;

	FreespeedFactorRoutingModule(final String mode, final PopulationFactory populationFactory, final Network network,
			final LeastCostPathCalculator routeAlgo, TeleportedModeParams params) {
		this.network = network;
		this.routeAlgo = routeAlgo;
		this.params = params;
		this.mode = mode;
		this.populationFactory = populationFactory;
	}

	@Override
	public List<? extends PlanElement> calcRoute(RoutingRequest request) {
		final Facility fromFacility = request.getFromFacility();
		final Facility toFacility = request.getToFacility();
		final double departureTime = request.getDepartureTime();
		final Person person = request.getPerson();

		Leg newLeg = this.populationFactory.createLeg(this.mode);
		newLeg.setDepartureTime(departureTime);

		double travTime = routeLeg(person, newLeg, new FacilityWrapperActivity(fromFacility), new FacilityWrapperActivity(toFacility), departureTime);

		// otherwise, information may be lost
		newLeg.setTravelTime(travTime);

		return Arrays.asList(newLeg);
	}

	@Override
	public String toString() {
		return "[LegRouterWrapper: mode=" + this.mode + "]";
	}

	/* package (for tests) */ final double routeLeg(Person person, Leg leg, Activity fromAct, Activity toAct, double depTime) {
		int travTime = 0;
		final Link fromLink = this.network.getLinks().get(fromAct.getLinkId());
		final Link toLink = this.network.getLinks().get(toAct.getLinkId());
		if (fromLink == null)
			throw new RuntimeException("fromLink missing.");
		if (toLink == null)
			throw new RuntimeException("toLink missing.");
		if (toLink != fromLink) {
			// do not drive/walk around, if we stay on the same link
			Path path = this.routeAlgo.calcLeastCostPath(fromLink, toLink, depTime, person, null);
			if (path == null)
				throw new RuntimeException("No route found from link " + fromLink.getId() + " to link " + toLink.getId() + ".");

			// we're still missing the time on the final link, which the agent has to drive on in the java mobsim
			// so let's calculate the final part.
			double speed = toLink.getFreespeed(depTime + path.travelTime);

			// correct by speed limit:
			if (speed > params.getTeleportedModeFreespeedLimit()) {
				speed = params.getTeleportedModeFreespeedLimit();
			}

			// now correct the travel time:
			double travelTimeLastLink = toLink.getLength() / speed;

			travTime = (int) (((int) path.travelTime + travelTimeLastLink) * this.params.getTeleportedModeFreespeedFactor());

			// calculate the distance from the path and the last link:
			double distance = RouteUtils.calcDistance(path) + toLink.getLength();

			// create the route and set the travel time and distance
			Route route = this.populationFactory.getRouteFactories().createRoute(Route.class, fromLink.getId(), toLink.getId());
			route.setTravelTime(travTime);
			route.setDistance(distance);
			leg.setRoute(route);
		} else {
			// create an empty route == staying on place if toLink == endLink
			Route route = this.populationFactory.getRouteFactories().createRoute(Route.class, fromLink.getId(), toLink.getId());
			route.setTravelTime(0);
			route.setDistance(0.0);
			leg.setRoute(route);
			travTime = 0;
		}
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		Leg r = (leg);
		r.setTravelTime(depTime + travTime - r.getDepartureTime().seconds()); // yy something needs to be done once there are alternative
																				// implementations of the interface. kai, apr'10
		return travTime;
	}

}
