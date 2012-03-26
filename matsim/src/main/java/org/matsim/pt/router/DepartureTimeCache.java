package org.matsim.pt.router;

import java.util.Arrays;
import java.util.HashMap;

import org.apache.log4j.Logger;
import org.matsim.core.gbl.Gbl;
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
	
		if ( earliestDepartureTimeAtTerminus < 0. && TransitRouterNetworkTravelTimeAndDisutility.wrnCnt < 1 ) {
			TransitRouterNetworkTravelTimeAndDisutility.wrnCnt++ ;
			Logger.getLogger(this.getClass()).warn("if departure at terminus is before midnight, this router may not work correctly" +
					" (will take the first departure at terminus AFTER midnight).\n" + Gbl.ONLYONCE ) ;
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