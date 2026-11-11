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

package org.matsim.contrib.drt.prebooking;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.drt.passenger.DrtStopCandidates;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.dvrp.router.StopCandidates;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripStructureUtils;

/**
 * @author nkuehnel / MOIA
 */
public class PrebookingStopCandidatesTest {
	private static final String MODE = "drt";

	private final Network network = createNetwork();

	// routed (preferred) stops A and E, alternatives B (pickup) and F (dropoff)
	private final StopCandidates candidates = new StopCandidates(
			List.of(candidate("A", 300, 250), candidate("B", 720, 600)),
			List.of(candidate("E", 120, 100), candidate("F", 360, 300)));

	@Test
	void testDrtStopCandidatesReferToTheRoutedStops() {
		Plan plan = routedPlan();

		DrtStopCandidates c = PrebookingStopCandidates.toDrtStopCandidates(drtLeg(plan).getRoute(), network, true)
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

		DrtStopCandidates c = PrebookingStopCandidates.toDrtStopCandidates(drtLeg(plan).getRoute(), network, false)
				.orElseThrow();

		assertThat(c.alternativeAccess()).isEmpty();
		assertThat(c.alternativeEgress()).hasSize(1);
	}

	@Test
	void testNoCandidatesWithoutAlternativesOrForAChangedRoute() {
		DrtRoute withoutCandidates = new DrtRoute(Id.createLinkId("A"), Id.createLinkId("E"));
		assertThat(PrebookingStopCandidates.toDrtStopCandidates(withoutCandidates, network, true)).isEmpty();

		Plan plan = routedPlan();
		drtLeg(plan).getRoute().setStartLinkId(Id.createLinkId("B"));
		assertThat(PrebookingStopCandidates.toDrtStopCandidates(drtLeg(plan).getRoute(), network, true)).isEmpty();

		// only pickup alternatives, but the pickup may not change any more
		StopCandidates onlyPickupAlternatives = new StopCandidates(
				List.of(candidate("A", 300, 250), candidate("B", 720, 600)), List.of(candidate("E", 120, 100)));
		DrtRoute route = new DrtRoute(Id.createLinkId("A"), Id.createLinkId("E"));
		route.setStopCandidates(onlyPickupAlternatives);
		assertThat(PrebookingStopCandidates.toDrtStopCandidates(route, network, false)).isEmpty();
	}

	@Test
	void testMaterializeRewritesAccessAndEgressToTheChosenStops() {
		Plan plan = routedPlan();
		Leg drtLeg = drtLeg(plan);

		boolean changed = PrebookingStopCandidates.materialize(plan, drtLeg, Id.createLinkId("B"),
				Id.createLinkId("F"));

		assertThat(changed).isTrue();
		var elements = plan.getPlanElements();
		assertThat(elements).hasSize(7);
		assertThat(((Activity)elements.get(0)).getType()).isEqualTo("home");

		Leg access = (Leg)elements.get(1);
		assertThat(access.getMode()).isEqualTo(TransportMode.walk);
		assertThat(TripStructureUtils.getRoutingMode(access)).isEqualTo(MODE);
		assertThat(access.getRoute().getStartLinkId()).isEqualTo(Id.createLinkId("h"));
		assertThat(access.getRoute().getEndLinkId()).isEqualTo(Id.createLinkId("B"));
		assertThat(access.getTravelTime().seconds()).isEqualTo(720);
		assertThat(access.getRoute().getDistance()).isEqualTo(600);
		assertThat(access.getDepartureTime().seconds()).isEqualTo(8 * 3600);

		Activity pickupStage = (Activity)elements.get(2);
		assertThat(pickupStage.getType()).isEqualTo(MODE + " interaction");
		assertThat(pickupStage.getLinkId()).isEqualTo(Id.createLinkId("B"));
		assertThat(pickupStage.getCoord()).isEqualTo(new Coord(-700, 0));

		assertThat(elements.get(3)).isSameAs(drtLeg);
		assertThat(drtLeg.getRoute().getStartLinkId()).isEqualTo(Id.createLinkId("B"));
		assertThat(drtLeg.getRoute().getEndLinkId()).isEqualTo(Id.createLinkId("F"));
		assertThat(drtLeg.getDepartureTime().seconds()).isEqualTo(8 * 3600 + 720);

		assertThat(((Activity)elements.get(4)).getLinkId()).isEqualTo(Id.createLinkId("F"));
		Leg egress = (Leg)elements.get(5);
		assertThat(egress.getRoute().getStartLinkId()).isEqualTo(Id.createLinkId("F"));
		assertThat(egress.getRoute().getEndLinkId()).isEqualTo(Id.createLinkId("w"));
		assertThat(egress.getTravelTime().seconds()).isEqualTo(360);
		assertThat(((Activity)elements.get(6)).getType()).isEqualTo("work");

		// the candidates of the route still refer to the routed stops
		assertThat(((DrtRoute)drtLeg.getRoute()).getStopCandidates()).contains(candidates);
	}

	@Test
	void testMaterializeOnlyTheChangedSide() {
		Plan plan = routedPlan();
		Leg routedEgress = (Leg)plan.getPlanElements().get(5);

		assertThat(PrebookingStopCandidates.materialize(plan, drtLeg(plan), Id.createLinkId("B"),
				Id.createLinkId("E"))).isTrue();

		assertThat(plan.getPlanElements().get(5)).isSameAs(routedEgress);
		assertThat(drtLeg(plan).getRoute().getEndLinkId()).isEqualTo(Id.createLinkId("E"));
	}

	@Test
	void testMaterializeDoesNothingForTheRoutedStops() {
		Plan plan = routedPlan();
		var before = List.copyOf(plan.getPlanElements());

		assertThat(PrebookingStopCandidates.materialize(plan, drtLeg(plan), Id.createLinkId("A"),
				Id.createLinkId("E"))).isFalse();

		assertThat(plan.getPlanElements()).containsExactlyElementsOf(before);
	}

	@Test
	void testGroupSharesOnlyCommonCandidatesWithTheLongestWalk() {
		DrtRoute first = routeWith(new StopCandidates(
				List.of(candidate("A", 300, 250), candidate("B", 720, 600)),
				List.of(candidate("E", 120, 100), candidate("F", 360, 300))));
		DrtRoute second = routeWith(new StopCandidates(
				List.of(candidate("A", 400, 330), candidate("B", 600, 500)),
				List.of(candidate("E", 100, 80))));

		DrtStopCandidates c = PrebookingStopCandidates.toDrtStopCandidates(List.of(first, second), network, true)
				.orElseThrow();

		assertThat(c.accessWalkDuration()).isEqualTo(400);
		assertThat(c.egressWalkDuration()).isEqualTo(120);
		assertThat(c.alternativeAccess()).singleElement()
				.satisfies(a -> assertThat(a.link().getId()).isEqualTo(Id.createLinkId("B")))
				.satisfies(a -> assertThat(a.walkDuration()).isEqualTo(720));
		assertThat(c.alternativeEgress()).isEmpty(); // F is not a candidate of the second passenger
	}

	@Test
	void testGroupWithoutCandidatesForOneMemberHasNoCandidates() {
		DrtRoute first = routeWith(candidates);
		DrtRoute second = new DrtRoute(Id.createLinkId("A"), Id.createLinkId("E"));

		assertThat(PrebookingStopCandidates.toDrtStopCandidates(List.of(first, second), network, true)).isEmpty();
	}

	@Test
	void testTripWithoutOwnWalksCannotBeMoved() {
		Plan routed = routedPlan();
		assertThat(PrebookingStopCandidates.hasAccessAndEgressLegs(routed, drtLeg(routed))).isTrue();

		// like a companion: the trip starts and ends at the stops
		Plan plan = PopulationUtils.createPlan();
		Activity origin = PopulationUtils.createActivityFromLinkId("home", Id.createLinkId("A"));
		origin.setEndTime(8 * 3600);
		plan.addActivity(origin);
		Leg drt = PopulationUtils.createLeg(MODE);
		drt.setRoute(routeWith(candidates));
		plan.addLeg(drt);
		plan.addActivity(PopulationUtils.createActivityFromLinkId("work", Id.createLinkId("E")));

		assertThat(PrebookingStopCandidates.hasAccessAndEgressLegs(plan, drt)).isFalse();

		// incomplete trip without destination activity
		Plan incomplete = PopulationUtils.createPlan();
		incomplete.addActivity(PopulationUtils.createActivityFromLinkId("home", Id.createLinkId("h")));
		Leg lastLeg = PopulationUtils.createLeg(MODE);
		incomplete.addLeg(lastLeg);
		assertThat(PrebookingStopCandidates.hasAccessAndEgressLegs(incomplete, lastLeg)).isFalse();
	}

	private static DrtRoute routeWith(StopCandidates stopCandidates) {
		DrtRoute route = new DrtRoute(Id.createLinkId("A"), Id.createLinkId("E"));
		route.setStopCandidates(stopCandidates);
		return route;
	}

	@Test
	void testOriginActivityIndex() {
		Plan plan = routedPlan();
		assertThat(PrebookingStopCandidates.originActivityIndex(plan, drtLeg(plan))).isZero();
	}

	private Leg drtLeg(Plan plan) {
		return (Leg)plan.getPlanElements().get(3);
	}

	/**
	 * home(h) - walk - interaction(A) - drt(A to E) - interaction(E) - walk - work(w)
	 */
	private Plan routedPlan() {
		Plan plan = PopulationUtils.createPlan();
		Activity home = PopulationUtils.createActivityFromLinkId("home", Id.createLinkId("h"));
		home.setEndTime(8 * 3600);
		plan.addActivity(home);
		plan.addLeg(walk("h", "A", 300, 250));
		plan.addActivity(PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord(-300, 0),
				Id.createLinkId("A"), MODE));
		Leg drt = PopulationUtils.createLeg(MODE);
		TripStructureUtils.setRoutingMode(drt, MODE);
		DrtRoute route = new DrtRoute(Id.createLinkId("A"), Id.createLinkId("E"));
		route.setStopCandidates(candidates);
		drt.setRoute(route);
		plan.addLeg(drt);
		plan.addActivity(PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord(2000, 0),
				Id.createLinkId("E"), MODE));
		plan.addLeg(walk("E", "w", 120, 100));
		plan.addActivity(PopulationUtils.createActivityFromLinkId("work", Id.createLinkId("w")));
		return plan;
	}

	private static Leg walk(String from, String to, double duration, double distance) {
		Leg leg = PopulationUtils.createLeg(TransportMode.walk);
		TripStructureUtils.setRoutingMode(leg, MODE);
		var route = RouteUtils.createGenericRouteImpl(Id.createLinkId(from), Id.createLinkId(to));
		route.setTravelTime(duration);
		route.setDistance(distance);
		leg.setRoute(route);
		leg.setTravelTime(duration);
		return leg;
	}

	private static StopCandidates.Candidate candidate(String link, double walkDuration, double walkDistance) {
		Coord coord = switch (link) {
			case "A" -> new Coord(-300, 0);
			case "B" -> new Coord(-700, 0);
			case "E" -> new Coord(2000, 0);
			case "F" -> new Coord(2300, 0);
			default -> throw new IllegalArgumentException(link);
		};
		return new StopCandidates.Candidate(Id.createLinkId(link), coord, walkDuration, walkDistance);
	}

	private static Network createNetwork() {
		Network network = NetworkUtils.createNetwork();
		Node n1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("1"), new Coord(0, 0));
		Node n2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("2"), new Coord(100, 0));
		for (String link : List.of("A", "B", "E", "F", "h", "w")) {
			NetworkUtils.createAndAddLink(network, Id.createLinkId(link), n1, n2, 100, 10, 1000, 1);
		}
		return network;
	}
}
