/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.pt.router;

import java.util.Arrays;
import java.util.HashMap;

import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;

public class DepartureTimeCache {
	public HashMap<TransitRoute, double[]> sortedDepartureCache;

	public DepartureTimeCache() {
		this.sortedDepartureCache = new HashMap<TransitRoute, double[]>();
	}

	public final double getNextDepartureTime(final TransitRoute route, final TransitRouteStop stop, final double depTime) {
	
		double earliestDepartureTimeAtTerminus = depTime - stop.getDepartureOffset();
		// This shifts my time back to the terminus.
	
		if (earliestDepartureTimeAtTerminus >= TransitRouterNetworkTravelTimeAndDisutility.MIDNIGHT) {
			earliestDepartureTimeAtTerminus = earliestDepartureTimeAtTerminus % TransitRouterNetworkTravelTimeAndDisutility.MIDNIGHT;
		}
		if (earliestDepartureTimeAtTerminus < 0) {
			// this may happen when depTime < departureOffset, e.g. I want to start at 24:03, but the bus departs at 23:55 at terminus
			earliestDepartureTimeAtTerminus += TransitRouterNetworkTravelTimeAndDisutility.MIDNIGHT;
		}
	
		// this will search for the terminus departure that corresponds to my departure at the stop:
		double[] cache = sortedDepartureCache.get(route);
		if (cache == null) {
			cache = new double[route.getDepartures().size()];
			int i = 0;
			for (Departure dep : route.getDepartures().values()) {
				cache[i++] = dep.getDepartureTime();
			}
			Arrays.sort(cache);
			sortedDepartureCache.put(route, cache);
		}
		int pos = Arrays.binarySearch(cache, earliestDepartureTimeAtTerminus);
		if (pos < 0) {
			// (if the departure time is not found _exactly_, binarySearch returns (-(insertion point) - 1).  That is
			// retval = -(insertion point) - 1  or insertion point = -(retval+1) .
			// This will, in fact, be the normal situation, so it is important to understand this.)
			pos = -(pos + 1);
		}
		if (pos >= cache.length) {
			pos = 0; // there is no later departure time, take the first in the morning
		}
		double bestDepartureTime = cache[pos];
		// (departure time at terminus)
	
		bestDepartureTime += stop.getDepartureOffset();
		// (resulting departure time at stop)
		
		while (bestDepartureTime < depTime) {
			bestDepartureTime += TransitRouterNetworkTravelTimeAndDisutility.MIDNIGHT;
			// (add enough "MIDNIGHT"s until we are _after_ the desired departure time)
		}
		return bestDepartureTime;
	}
}