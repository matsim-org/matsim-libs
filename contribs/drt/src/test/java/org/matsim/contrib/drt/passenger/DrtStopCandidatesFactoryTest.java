/*
 * *********************************************************************** *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.drt.passenger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.drt.passenger.StopCandidatesTestPlans.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.dvrp.router.RoutedStopCandidates;

/**
 * Tests {@link DrtStopCandidatesFactory}.
 *
 * @author nkuehnel / MOIA
 */
public class DrtStopCandidatesFactoryTest {
	// not static: other tests may reset the id caches, which invalidates the link lookup of a shared network
	private final Network network = createNetwork();

	@Test
	void testDrtStopCandidatesReferToTheRoutedStops() {
		Plan plan = routedPlan();

		DrtStopCandidates c = DrtStopCandidatesFactory.fromRoute(drtLeg(plan).getRoute(), network, true)
				.orElseThrow();

		assertThat(c.accessWalkDuration()).isEqualTo(300);
		assertThat(c.egressWalkDuration()).isEqualTo(120);
		assertThat(c.alternativeAccess()).singleElement()
				.satisfies(a -> assertThat(a.link().getId()).isEqualTo(Id.createLinkId("B")))
				.satisfies(a -> assertThat(a.walkDuration()).isEqualTo(720));
		assertThat(c.alternativeEgress()).singleElement()
				.satisfies(e -> assertThat(e.link().getId()).isEqualTo(Id.createLinkId("F")));
	}

	@Test
	void testNoAlternativePickupsIfAccessMayNotChange() {
		Plan plan = routedPlan();

		DrtStopCandidates c = DrtStopCandidatesFactory.fromRoute(drtLeg(plan).getRoute(), network, false)
				.orElseThrow();

		assertThat(c.alternativeAccess()).isEmpty();
		assertThat(c.alternativeEgress()).hasSize(1);
	}

	@Test
	void testNoCandidatesWithoutAlternativesOrForAChangedRoute() {
		DrtRoute withoutCandidates = new DrtRoute(Id.createLinkId("A"), Id.createLinkId("E"));
		assertThat(DrtStopCandidatesFactory.fromRoute(withoutCandidates, network, true)).isEmpty();

		Plan plan = routedPlan();
		drtLeg(plan).getRoute().setStartLinkId(Id.createLinkId("B"));
		assertThat(DrtStopCandidatesFactory.fromRoute(drtLeg(plan).getRoute(), network, true)).isEmpty();

		// only pickup alternatives, but the pickup may not change any more
		RoutedStopCandidates onlyPickupAlternatives = new RoutedStopCandidates(
				List.of(candidate("A", 300, 250), candidate("B", 720, 600)), List.of(candidate("E", 120, 100)));
		DrtRoute route = new DrtRoute(Id.createLinkId("A"), Id.createLinkId("E"));
		route.setStopCandidates(onlyPickupAlternatives);
		assertThat(DrtStopCandidatesFactory.fromRoute(route, network, false)).isEmpty();
	}

	@Test
	void testGroupSharesOnlyCommonCandidatesWithTheLongestWalk() {
		DrtRoute first = routeWith(new RoutedStopCandidates(
				List.of(candidate("A", 300, 250), candidate("B", 720, 600)),
				List.of(candidate("E", 120, 100), candidate("F", 360, 300))));
		DrtRoute second = routeWith(new RoutedStopCandidates(
				List.of(candidate("A", 400, 330), candidate("B", 600, 500)),
				List.of(candidate("E", 100, 80))));

		DrtStopCandidates c = DrtStopCandidatesFactory.fromRoutes(List.of(first, second), network, true)
				.orElseThrow();

		assertThat(c.accessWalkDuration()).isEqualTo(400);
		assertThat(c.egressWalkDuration()).isEqualTo(120);
		assertThat(c.alternativeAccess()).singleElement()
				.satisfies(a -> assertThat(a.link().getId()).isEqualTo(Id.createLinkId("B")))
				.satisfies(a -> assertThat(a.walkDuration()).isEqualTo(720));
		assertThat(c.alternativeEgress()).isEmpty(); // F is not a candidate of the second passenger
	}

	@Test
	void testGroupUsesTheLongestWalkOfAnyMember() {
		// the second passenger walks farther to the alternative stop B than the first
		DrtRoute first = routeWith(new RoutedStopCandidates(
				List.of(candidate("A", 300, 250), candidate("B", 600, 500)), List.of(candidate("E", 120, 100))));
		DrtRoute second = routeWith(new RoutedStopCandidates(
				List.of(candidate("A", 300, 250), candidate("B", 900, 750)), List.of(candidate("E", 120, 100))));

		DrtStopCandidates c = DrtStopCandidatesFactory.fromRoutes(List.of(first, second), network, true)
				.orElseThrow();

		assertThat(c.alternativeAccess()).singleElement()
				.satisfies(a -> assertThat(a.walkDuration()).isEqualTo(900));
	}

	@Test
	void testGroupWithoutCandidatesForOneMemberHasNoCandidates() {
		DrtRoute first = routeWith(CANDIDATES);
		DrtRoute second = new DrtRoute(Id.createLinkId("A"), Id.createLinkId("E"));

		assertThat(DrtStopCandidatesFactory.fromRoutes(List.of(first, second), network, true)).isEmpty();
	}
}
