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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;

/**
 * @author  jbischoff
 *
 */
public interface ParkingRouter {
	/**
	 * *
	 * @param intendedRoute: may be a network route (car trips) or may be generic (carsharing etc.) 
	 * @param departureTime
	 * @param startLinkId
	 * @return
	 */

	NetworkRoute getRouteFromParkingToDestination(Id<Link> destinationLinkId, double departureTime, Id<Link> startLinkId);
	
}
