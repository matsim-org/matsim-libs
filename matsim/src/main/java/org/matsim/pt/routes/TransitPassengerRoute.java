/* *********************************************************************** *
 * project: org.matsim.*
 * ExperimentalTransitRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.pt.routes;

import org.matsim.api.core.v01.Id;
import org.matsim.dsim.scoring.PassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Represents a route of a transit passenger. A route can be chained. This is useful for cases where transit vehicles change while a passenger is
 * inside them. For example, a long train is divided into two parts which go to different destinations. For chained transit routes, the outermost
 * route element represents the entire route, while each chained part represents the remaining part of the transit leg. Example time:
 * <p>
 * We have stations A, B, and C. A route starts at station A. The vehicle is divided into two parts at station B and the passenger continues its journey
 * to station C with one half of the train.
 *
 * <pre>
 *     {@code
 *     void main() {
 *         TransitPassengerRoute route = ... // init somehow with stations A, B, and C
 *         double totalDistance = route.getDistance(); // Distance from A to C
 *         TransitPassengerRoute secondPart = route.getChainedRoute();
 *         double secondDistance = secondPart.getDistance(); // Distance from B to C
 *     }
 *     }
 * </pre>
 */
public interface TransitPassengerRoute extends PassengerRoute {
	Id<TransitStopFacility> getAccessStopId();

	Id<TransitStopFacility> getEgressStopId();

	Id<TransitLine> getLineId();

	Id<TransitRoute> getRouteId();

	TransitPassengerRoute getChainedRoute();

}
