/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2025 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dvrp.router;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.Facility;

/**
 * A composite {@link AccessEgressFacilityFinder} which selects a delegate based on the departure time of the routing
 * request. If no time window covers the departure time, no facilities are found, so that no trip with this mode is
 * built at all (see {@link DvrpRoutingModule#calcRoute}).
 * <p>
 * This is where a service time is enforced on the routing side. The delegates themselves stay unaware of time; for a
 * door-to-door service, all windows may share the same delegate, so that only the time acts as a restriction.
 *
 * @author nkuehnel / MOIA
 */
public class TimeDependentAccessEgressFacilityFinder implements AccessEgressFacilityFinder {

	/**
	 * A time window and the finder that applies within it. The window is half-open, i.e. {@code [startTime, endTime)}.
	 * The windows of one finder are expected to be pairwise disjoint; the first matching one is used.
	 */
	public record TimeWindow(double startTime, double endTime, AccessEgressFacilityFinder delegate) {
		public boolean covers(double time) {
			return startTime <= time && time < endTime;
		}
	}

	private final List<TimeWindow> timeWindows;

	public TimeDependentAccessEgressFacilityFinder(List<TimeWindow> timeWindows) {
		this.timeWindows = List.copyOf(timeWindows);
	}

	@Override
	public Optional<Pair<Facility, Facility>> findFacilities(RoutingRequest request) {
		return timeWindows.stream()
				.filter(timeWindow -> timeWindow.covers(request.getDepartureTime()))
				.findFirst()
				.flatMap(timeWindow -> timeWindow.delegate().findFacilities(request));
	}
}
