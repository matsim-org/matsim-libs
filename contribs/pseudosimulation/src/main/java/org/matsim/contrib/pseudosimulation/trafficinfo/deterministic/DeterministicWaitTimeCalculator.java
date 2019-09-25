package org.matsim.contrib.pseudosimulation.trafficinfo.deterministic;

import java.util.LinkedList;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTime;
import org.matsim.contrib.eventsBasedPTRouter.waitTimes.WaitTimeCalculator;
import org.matsim.pt.transitSchedule.api.Departure;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Inject;
import com.google.inject.Singleton;

@Singleton
public class DeterministicWaitTimeCalculator implements WaitTimeCalculator {
	private final TransitSchedule schedule;

	@Inject
	public DeterministicWaitTimeCalculator(TransitSchedule schedule) {
		this.schedule = schedule;
	}

	@Override
	public double getRouteStopWaitTime(Id<TransitLine> lineId, Id<TransitRoute> routeId, Id<org.matsim.facilities.Facility> stopId,
			double time) {
		TransitRoute route = schedule.getTransitLines().get(lineId).getRoutes().get(routeId);

		// First collect all the offsets on the route for a specific transit stop id.
		// Note that a single transit facility may be contained multiple times on a
		// route!
		List<Double> facilityOffsets = new LinkedList<>();

		for (TransitRouteStop stop : route.getStops()) {
			if (stop.getStopFacility().getId().equals(stopId)) {
				facilityOffsets.add(stop.getDepartureOffset());
			}
		}

		if (facilityOffsets.size() == 0) {
			// The facility is not contained on this route
			throw new IllegalStateException();
		}

		// Now loop through all the departures and routes and find the actual stop
		// departure that is right after the given time.

		double minimumWaitTime = Double.POSITIVE_INFINITY;

		for (double offset : facilityOffsets) {
			for (Departure departure : route.getDepartures().values()) {
				double stopDepartureTime = departure.getDepartureTime() + offset;

				while (stopDepartureTime < time) {
					// Consistent with TransitRouterNetworkTravelTimeAndDisutility.MIDNIGHT
					stopDepartureTime += 24.0 * 3600;
				}

				double waitTime = time - stopDepartureTime;

				if (waitTime < minimumWaitTime) {
					minimumWaitTime = waitTime;
				}
			}
		}

		return minimumWaitTime;
	}

	@Override
	public WaitTime get() {
		return new WaitTime() {
			@Override
			public double getRouteStopWaitTime(Id<TransitLine> lineId, Id<TransitRoute> routeId,
					Id<org.matsim.facilities.Facility> stopId, double time) {
				return DeterministicWaitTimeCalculator.this.getRouteStopWaitTime(lineId, routeId, stopId, time);
			}
		};
	}
}
