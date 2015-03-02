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

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.routes.ModeRouteFactory;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author mrieser
 */
class TeleportationLegRouter implements LegRouter {

	private final ModeRouteFactory routeFactory;
	
	private final double beelineDistanceFactor;
	private final double networkTravelSpeed;
	
	 TeleportationLegRouter(final ModeRouteFactory routeFactory, final double networkTravelSpeed, final double beelineDistanceFactor) {
		this.routeFactory = routeFactory;
		this.networkTravelSpeed = networkTravelSpeed;
		this.beelineDistanceFactor = beelineDistanceFactor;
	}

	@Override
	public double routeLeg(Person person, Leg leg, Activity fromAct, Activity toAct, double depTime) {
		// make simple assumption about distance and walking speed
		double dist = CoordUtils.calcDistance(fromAct.getCoord(), toAct.getCoord());
		// create an empty route, but with realistic travel time
		Route route = this.routeFactory.createRoute(leg.getMode(), fromAct.getLinkId(), toAct.getLinkId());
		double estimatedNetworkDistance = dist * beelineDistanceFactor;
		int travTime = (int) (estimatedNetworkDistance / networkTravelSpeed);
		route.setTravelTime(travTime);
		route.setDistance(estimatedNetworkDistance);
		leg.setRoute(route);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		((LegImpl) leg).setArrivalTime(depTime + travTime); // yy something needs to be done once there are alternative implementations of the interface.  kai, apr'10
		return travTime;
	}

}
