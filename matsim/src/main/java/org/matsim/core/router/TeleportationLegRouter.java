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

package org.matsim.core.router;

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author mrieser
 */
public class TeleportationLegRouter implements LegRouter {

	private final NetworkFactoryImpl routeFactory;
	private final double beelineTravelSpeed;

	public TeleportationLegRouter(final NetworkFactoryImpl routeFactory, final double beelineTravelSpeed) {
		this.routeFactory = routeFactory;
		this.beelineTravelSpeed = beelineTravelSpeed;
	}

	@Override
	public double routeLeg(Person person, Leg leg, Activity fromAct, Activity toAct, double depTime) {
		// make simple assumption about distance and walking speed
		double dist = CoordUtils.calcDistance(fromAct.getCoord(), toAct.getCoord());
		// create an empty route, but with realistic traveltime
		Route route = this.routeFactory.createRoute(leg.getMode(), fromAct.getLinkId(), toAct.getLinkId());
		int travTime = (int)(dist / beelineTravelSpeed);
		route.setTravelTime(travTime);
		route.setDistance(dist * 1.5);
		leg.setRoute(route);
		leg.setDepartureTime(depTime);
		leg.setTravelTime(travTime);
		((LegImpl) leg).setArrivalTime(depTime + travTime); // yy something needs to be done once there are alternative implementations of the interface.  kai, apr'10
		return travTime;
	}

}
