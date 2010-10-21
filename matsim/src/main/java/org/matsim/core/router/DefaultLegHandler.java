/* *********************************************************************** *
 * project: org.matsim.*
 * DefaultLegHandler.java
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

package org.matsim.core.router;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;

public class DefaultLegHandler implements LegHandler {

	private static final Logger log = Logger.getLogger(DefaultLegHandler.class);

	private static final String NO_CONFIGGROUP_SET_WARNING = "No PlansCalcRouteConfigGroup"
		+ " is set in PlansCalcRoute, using the default values. Make sure that those values" +
				"fit your needs, otherwise set it expclicitly.";

	private PlansCalcRouteConfigGroup configGroup = new PlansCalcRouteConfigGroup();
	private final Network network;
	private final LeastCostPathCalculator routeAlgo;
	private final LeastCostPathCalculator routeAlgoPtFreeflow;
	private final NetworkFactoryImpl routeFactory;

	public DefaultLegHandler(final PlansCalcRouteConfigGroup group, final Network network,
			final LeastCostPathCalculator routeAlgo, final LeastCostPathCalculator routeAlgoPtFreeflow) {
		this.network = network;
		this.routeAlgo = routeAlgo;
		this.routeAlgoPtFreeflow = routeAlgoPtFreeflow;
		this.routeFactory = (NetworkFactoryImpl) network.getFactory();

		if (group != null) {
			this.configGroup = group;
		}
		else {
			log.warn(NO_CONFIGGROUP_SET_WARNING);
		}
	}

	@Override
	public double handleLeg(Person person, Leg leg, Activity fromAct, Activity toAct, double depTime) {
		String legMode = leg.getMode();
		if (TransportMode.car.equals(legMode)) {
			return handleCarLeg(leg, fromAct, toAct, depTime);
		} else if (TransportMode.ride.equals(legMode)) {
			return handleRideLeg(leg, fromAct, toAct, depTime);
		} else if (TransportMode.pt.equals(legMode)) {
			return handlePtLeg(leg, fromAct, toAct, depTime);
		} else if (TransportMode.walk.equals(legMode)) {
			return handleWalkLeg(leg, fromAct, toAct, depTime);
		} else if (TransportMode.bike.equals(legMode)) {
			return handleBikeLeg(leg, fromAct, toAct, depTime);
		} else if ("undefined".equals(legMode)) {
			return handleUndefinedLeg(leg, fromAct, toAct, depTime);
		} else {
			throw new RuntimeException("cannot handle legmode '" + legMode + "'.");
		}
	}

	private double handleCarLeg(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		double travTime = 0;
		Link fromLink = this.network.getLinks().get(fromAct.getLinkId());
		Link toLink = this.network.getLinks().get(toAct.getLinkId());
		if (fromLink == null) throw new RuntimeException("fromLink missing.");
		if (toLink == null) throw new RuntimeException("toLink missing.");

		Node startNode = fromLink.getToNode();	// start at the end of the "current" link
		Node endNode = toLink.getFromNode(); // the target is the start of the link

//		CarRoute route = null;
		Path path = null;
		if (toLink != fromLink) {
			// do not drive/walk around, if we stay on the same link
			path = this.routeAlgo.calcLeastCostPath(startNode, endNode, depTime);
			if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
			NetworkRoute route = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, fromLink.getId(), toLink.getId());
			route.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
			route.setTravelTime((int) path.travelTime);
			route.setTravelCost(path.travelCost);
			route.setDistance(RouteUtils.calcDistance(route, this.network));
			leg.setRoute(route);
			travTime = (int) path.travelTime;
		} else {
			// create an empty route == staying on place if toLink == endLink
			NetworkRoute route = (NetworkRoute) this.routeFactory.createRoute(TransportMode.car, fromLink.getId(), toLink.getId());
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

	private double handleRideLeg(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		// handle a ride exactly the same was as a car
		// the simulation has to take care that this leg is not really simulated as a stand-alone driver
		return handleCarLeg(leg, fromAct, toAct, depTime);
	}

	private double handlePtLeg(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {

		int travTime = 0;
		final Link fromLink = this.network.getLinks().get(fromAct.getLinkId());
		final Link toLink = this.network.getLinks().get(toAct.getLinkId());
		if (fromLink == null) throw new RuntimeException("fromLink missing.");
		if (toLink == null) throw new RuntimeException("toLink missing.");

		Path path = null;
//		CarRoute route = null;
		if (toLink != fromLink) {
			Node startNode = fromLink.getToNode();	// start at the end of the "current" link
			Node endNode = toLink.getFromNode(); // the target is the start of the link
			// do not drive/walk around, if we stay on the same link
			path = this.routeAlgoPtFreeflow.calcLeastCostPath(startNode, endNode, depTime);
			if (path == null) throw new RuntimeException("No route found from node " + startNode.getId() + " to node " + endNode.getId() + ".");
			// we're still missing the time on the final link, which the agent has to drive on in the java mobsim
			// so let's calculate the final part.
			double travelTimeLastLink = ((LinkImpl) toLink).getFreespeedTravelTime(depTime + path.travelTime);
			travTime = (int) (((int) path.travelTime + travelTimeLastLink) * this.configGroup.getPtSpeedFactor());
			Route route = this.routeFactory.createRoute(TransportMode.pt, fromLink.getId(), toLink.getId());
			route.setTravelTime(travTime);
			double dist = 0;
			if ((fromAct.getCoord() != null) && (toAct.getCoord() != null)) {
				dist = CoordUtils.calcDistance(fromAct.getCoord(), toAct.getCoord());
			} else {
				dist = CoordUtils.calcDistance(fromLink.getCoord(), toLink.getCoord());
			}
			route.setDistance(dist * 1.5);
//			route.setTravelCost(path.travelCost);
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

	private double handleWalkLeg(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		// make simple assumption about distance and walking speed
		double dist = CoordUtils.calcDistance(fromAct.getCoord(), toAct.getCoord());
		// create an empty route, but with realistic traveltime
		Route route = this.routeFactory.createRoute(TransportMode.walk, fromAct.getLinkId(), toAct.getLinkId());
		int travTime = (int)(dist / this.configGroup.getWalkSpeed());
		route.setTravelTime(travTime);
		route.setDistance(dist * 1.5);
		leg.setRoute(route);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		((LegImpl) leg).setArrivalTime(depTime + travTime); // yy something needs to be done once there are alternative implementations of the interface.  kai, apr'10
		return travTime;
	}

	private double handleBikeLeg(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		// make simple assumption about distance and cycling speed
		double dist = CoordUtils.calcDistance(fromAct.getCoord(), toAct.getCoord());
		// create an empty route, but with realistic traveltime
		Route route = this.routeFactory.createRoute(TransportMode.bike, fromAct.getLinkId(), toAct.getLinkId());
		int travTime = (int)(dist / this.configGroup.getBikeSpeed());
		route.setTravelTime(travTime);
		route.setDistance(dist * 1.5);
		leg.setRoute(route);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		((LegImpl) leg).setArrivalTime(depTime + travTime); // yy something needs to be done once there are alternative implementations of the interface.  kai, apr'10
		return travTime;
	}

	private double handleUndefinedLeg(final Leg leg, final Activity fromAct, final Activity toAct, final double depTime) {
		// make simple assumption about distance and a dummy speed (50 km/h)
		double dist = CoordUtils.calcDistance(fromAct.getCoord(), toAct.getCoord());
		// create an empty route, but with realistic traveltime
		Route route = this.routeFactory.createRoute("undefined", fromAct.getLinkId(), toAct.getLinkId());
		int travTime = (int)(dist / this.configGroup.getUndefinedModeSpeed());
		route.setTravelTime(travTime);
		route.setDistance(dist * 1.5);
		leg.setRoute(route);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		((LegImpl) leg).setArrivalTime(depTime + travTime); // yy something needs to be done once there are alternative implementations of the interface.  kai, apr'10
		return travTime;
	}

}
