/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.speedup;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.contrib.dvrp.passenger.PassengerRequest;
import org.matsim.contrib.dvrp.passenger.TeleportingPassengerEngine.TeleportedRouteCalculator;
import org.matsim.core.population.routes.GenericRouteImpl;
import org.matsim.core.utils.geometry.CoordUtils;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DrtTeleportedRouteCalculator implements TeleportedRouteCalculator {
	private final double averageWaitingTime;
	private final double averageInVehicleBeelineSpeed;

	DrtTeleportedRouteCalculator(double averageWaitingTime, double averageInVehicleBeelineSpeed) {
		this.averageWaitingTime = averageWaitingTime;
		this.averageInVehicleBeelineSpeed = averageInVehicleBeelineSpeed;
	}

	@Override
	public Route calculateRoute(PassengerRequest request) {
		Link startLink = request.getFromLink();
		Link endLink = request.getToLink();
		final Coord fromActCoord = startLink.getToNode().getCoord();
		final Coord toActCoord = endLink.getToNode().getCoord();
		double dist = CoordUtils.calcEuclideanDistance(fromActCoord, toActCoord);
		Route route = new GenericRouteImpl(startLink.getId(), endLink.getId());
		//TODO move wait time outside the route (handle it explicitly by the TeleportingPassengerEngine)
		int travTime = (int)(averageWaitingTime + (dist / averageInVehicleBeelineSpeed));
		route.setTravelTime(travTime);
		route.setDistance(dist);
		return route;
	}
}
