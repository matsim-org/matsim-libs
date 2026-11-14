/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.routing;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.dvrp.router.StopCandidates;
import org.matsim.core.population.routes.GenericRouteImpl;

/**
 * @author nkuehnel / MOIA
 */
class DefaultDrtRouteUpdaterTest {

	private final StopCandidates candidates = new StopCandidates(
			List.of(candidate("A", 300), candidate("B", 720)),
			List.of(candidate("E", 120)));

	@Test
	void updatedRouteKeepsTheStopCandidates() {
		DrtRoute oldRoute = new DrtRoute(Id.createLinkId("A"), Id.createLinkId("E"));
		oldRoute.setStopCandidates(candidates);
		DrtRoute newRoute = new DrtRoute(Id.createLinkId("A"), Id.createLinkId("E"));

		DefaultDrtRouteUpdater.keepStopCandidates(oldRoute, newRoute);

		assertThat(newRoute.getStopCandidates()).contains(candidates);
	}

	@Test
	void routesWithoutCandidatesStayWithout() {
		DrtRoute oldRoute = new DrtRoute(Id.createLinkId("A"), Id.createLinkId("E"));
		DrtRoute newRoute = new DrtRoute(Id.createLinkId("A"), Id.createLinkId("E"));

		DefaultDrtRouteUpdater.keepStopCandidates(oldRoute, newRoute);
		// other route types are ignored
		DefaultDrtRouteUpdater.keepStopCandidates(new GenericRouteImpl(Id.createLinkId("A"), Id.createLinkId("E")),
				newRoute);

		assertThat(newRoute.getStopCandidates()).isEmpty();
	}

	private static StopCandidates.Candidate candidate(String link, double walkDuration) {
		return new StopCandidates.Candidate(Id.createLinkId(link), new Coord(0, 0), walkDuration, walkDuration);
	}
}
