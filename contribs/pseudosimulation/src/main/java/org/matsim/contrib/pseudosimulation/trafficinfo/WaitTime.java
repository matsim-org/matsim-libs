package org.matsim.contrib.pseudosimulation.trafficinfo;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Average time a passenger waited at a stop for a given line and route, by time of day.
 *
 * <p>
 * Originally {@code org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime} by sergioo, which
 * was removed from matsim-libs in October 2023. Reintroduced here because pseudo-simulation needs
 * a transit performance measure that is stable from one lookup to the next.
 *
 * @author sergioo
 */
public interface WaitTime {

	/**
	 * Never returns infinity: a stop with no observation in this time bin falls back to the wait
	 * implied by the timetable, so a caller always gets a usable number.
	 */
	double getRouteStopWaitTime(Id<TransitLine> lineId, Id<TransitRoute> routeId,
			Id<TransitStopFacility> stopId, double time);
}
