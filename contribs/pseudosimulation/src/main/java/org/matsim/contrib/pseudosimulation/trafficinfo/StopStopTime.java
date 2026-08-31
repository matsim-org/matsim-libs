package org.matsim.contrib.pseudosimulation.trafficinfo;

import org.matsim.api.core.v01.Id;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

/**
 * Average time a transit vehicle took to travel between two consecutive stops, by time of day.
 *
 * <p>
 * Originally {@code org.matsim.contrib.eventsBasedPTRouter.stopStopTimes.StopStopTime} by sergioo,
 * which was removed from matsim-libs in October 2023. Reintroduced here because pseudo-simulation
 * needs a transit performance measure that is stable from one lookup to the next.
 *
 * @author sergioo
 */
public interface StopStopTime {

	/**
	 * Never returns infinity: a pair with no observation in this time bin falls back to the time
	 * implied by the timetable, so a caller always gets a usable number.
	 */
	double getStopStopTime(Id<TransitStopFacility> fromStopId, Id<TransitStopFacility> toStopId, double time);
}
