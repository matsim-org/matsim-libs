/*
 * Copyright 2018 Gunnar Flötteröd
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 *
 * contact: gunnar.flotterod@gmail.com
 *
 */
package org.matsim.contrib.pseudosimulation.mobsim.transitperformance;

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.contrib.pseudosimulation.trafficinfo.StopStopTime;
import org.matsim.contrib.pseudosimulation.trafficinfo.WaitTime;
import org.matsim.pt.routes.TransitPassengerRoute;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import com.google.inject.Inject;

/**
 * Emulates a transit trip from the wait time at the access stop and the stop-to-stop times along
 * the route, both measured during the preceding queue simulation.
 *
 * <p>
 * This is the stable alternative to {@link TransitPerformanceFromPSimSpecificImplementation}, which
 * answers each query by drawing a random one of the last few observed departures and applying a
 * stochastic boarding model. Repeated lookups of the same leg there disagree, so a plan can be
 * retained on the strength of a lucky draw. Here both inputs are deterministic bin means with a
 * timetable fallback, so a plan's emulated performance changes only when the queue simulation
 * measures something different.
 *
 * @author sergioo
 * @author Gunnar Flötteröd
 */
public class TransitPerformanceFromEventBasedRouterInterfaces implements TransitEmulator {

	private final WaitTime waitTimes;
	private final StopStopTime stopStopTimes;
	private final Map<Id<TransitLine>, TransitLine> transitLines;

	@Inject
	public TransitPerformanceFromEventBasedRouterInterfaces(WaitTime waitTimes, StopStopTime stopStopTimes,
			TransitSchedule transitSchedule) {
		this.waitTimes = waitTimes;
		this.stopStopTimes = stopStopTimes;
		this.transitLines = transitSchedule.getTransitLines();
	}

	@Override
	public Trip findTrip(Leg prevLeg, double earliestDepartureTime_s) {
		TransitPassengerRoute route = (TransitPassengerRoute) prevLeg.getRoute();
		TransitLine line = this.transitLines.get(route.getLineId());
		if (line == null) {
			return null;
		}
		TransitRoute transitRoute = line.getRoutes().get(route.getRouteId());
		if (transitRoute == null) {
			return null;
		}

		final double accessTime_s = earliestDepartureTime_s + this.waitTimes.getRouteStopWaitTime(route.getLineId(),
				transitRoute.getId(), route.getAccessStopId(), earliestDepartureTime_s);
		final double inVehicleTime_s = this.findTransitTravelTime(transitRoute, route.getAccessStopId(),
				route.getEgressStopId(), accessTime_s);
		if (Double.isInfinite(inVehicleTime_s)) {
			return null;
		}
		return new Trip(null, accessTime_s, accessTime_s + inVehicleTime_s);
	}

	/**
	 * Walks the route from the access stop to the egress stop, accumulating the measured time of
	 * each stop-to-stop segment. The clock is advanced by each segment as it is traversed, so a
	 * segment late in the route is looked up at the time of day the vehicle actually reaches it.
	 *
	 * <p>
	 * Returns positive infinity when the egress stop is not reached, matching what
	 * {@link org.matsim.contrib.pseudosimulation.distributed.listeners.events.transit.TransitPerformance}
	 * returns for an impossible trip. Returning negative infinity, as this method previously did,
	 * would have produced a negative leg duration.
	 */
	private double findTransitTravelTime(TransitRoute transitRoute, Id<TransitStopFacility> accessStopId,
			Id<TransitStopFacility> egressStopId, double boardingTime_s) {
		List<TransitRouteStop> stops = transitRoute.getStops();
		int boardingIndex = indexOfStop(stops, accessStopId);
		if (boardingIndex < 0) {
			return Double.POSITIVE_INFINITY;
		}

		double travelTime_s = 0;
		for (int index = boardingIndex; index < stops.size() - 1; index++) {
			Id<TransitStopFacility> fromId = stops.get(index).getStopFacility().getId();
			Id<TransitStopFacility> toId = stops.get(index + 1).getStopFacility().getId();
			travelTime_s += this.stopStopTimes.getStopStopTime(fromId, toId, boardingTime_s + travelTime_s);
			if (toId.equals(egressStopId)) {
				return travelTime_s;
			}
		}
		return Double.POSITIVE_INFINITY;
	}

	/**
	 * A route may call at the same facility more than once, and the passenger boards at the first
	 * such call at or after the one their route names.
	 */
	private static int indexOfStop(List<TransitRouteStop> stops, Id<TransitStopFacility> stopId) {
		for (int index = 0; index < stops.size(); index++) {
			if (stops.get(index).getStopFacility().getId().equals(stopId)) {
				return index;
			}
		}
		return -1;
	}
}
