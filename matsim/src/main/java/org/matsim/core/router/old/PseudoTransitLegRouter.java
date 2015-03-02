/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.router.old;

import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * Calculates a route along links in a network, but doesn't really use that route, but
 * only the travel time of it for teleportation purposes.
 * Typically, this class is used with a free-flow travel speed router and some
 * speedfactor > 1.0 (often 2.0), so "estimate" travel times with a non-car mode.
 *
 * @author mrieser
 */
final class PseudoTransitLegRouter implements LegRouter {

	private final Network network;
	private final ModeRouteFactory routeFactory;
	private final LeastCostPathCalculator routeAlgo;
	private final double speedFactor;
	private final double beelineDistanceFactor;

	 PseudoTransitLegRouter(final Network network, final LeastCostPathCalculator routeAlgo, final double speedFactor, double beelineDistanceFactor, final ModeRouteFactory routeFactory) {
		this.network = network;
		this.routeAlgo = routeAlgo;
		this.speedFactor = speedFactor;
		this.beelineDistanceFactor = beelineDistanceFactor;
		this.routeFactory = routeFactory;
	}

	@Override
	public double routeLeg(Person person, Leg leg, Activity fromAct, Activity toAct, double depTime) {
		int travTime = 0;
		final Link fromLink = this.network.getLinks().get(fromAct.getLinkId());
		final Link toLink = this.network.getLinks().get(toAct.getLinkId());
		if (fromLink == null) throw new RuntimeException("fromLink missing.");
		if (toLink == null) throw new RuntimeException("toLink missing.");
		if (toLink != fromLink) {
			Node startNode = fromLink.getToNode();	// start at the end of the "current" link
			Node endNode = toLink.getFromNode(); // the target is the start of the link
			// do not drive/walk around, if we stay on the same link
			Path path = this.routeAlgo.calcLeastCostPath(startNode, endNode, depTime, person, null);
			if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
			// we're still missing the time on the final link, which the agent has to drive on in the java mobsim
			// so let's calculate the final part.
			double travelTimeLastLink = ((LinkImpl) toLink).getFreespeedTravelTime(depTime + path.travelTime);
			travTime = (int) (((int) path.travelTime + travelTimeLastLink) * this.speedFactor);
			Route route = this.routeFactory.createRoute(TransportMode.pt, fromLink.getId(), toLink.getId());
			route.setTravelTime(travTime);
			double dist = 0;
			if ((fromAct.getCoord() != null) && (toAct.getCoord() != null)) {
				dist = CoordUtils.calcDistance(fromAct.getCoord(), toAct.getCoord());
			} else {
				dist = CoordUtils.calcDistance(fromLink.getCoord(), toLink.getCoord());
			}
			route.setDistance(dist * beelineDistanceFactor);
			leg.setRoute(route);
		} else {
			// create an empty route == staying on place if toLink == endLink
			Route route = this.routeFactory.createRoute(TransportMode.pt, fromLink.getId(), toLink.getId());
			route.setTravelTime(0);
			route.setDistance(0.0);
			leg.setRoute(route);
			travTime = 0;
		}
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		((LegImpl) leg).setArrivalTime(depTime + travTime); // yy something needs to be done once there are alternative implementations of the interface.  kai, apr'10
		return travTime;
	}

}
