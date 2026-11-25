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

import java.util.List;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.contrib.dvrp.router.RoutedStopCandidates;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.RouteUtils;
import org.matsim.core.router.TripStructureUtils;

/**
 * Plans and routes with stop candidates for {@link DrtStopCandidatesFactoryTest} and
 * {@link StopCandidatesTripMaterializerTest}.
 *
 * @author nkuehnel / MOIA
 */
final class StopCandidatesTestPlans {
	static final String MODE = "drt";

	// routed (preferred) stops A and E, alternatives B (pickup) and F (dropoff)
	static final RoutedStopCandidates CANDIDATES = new RoutedStopCandidates(
			List.of(candidate("A", 300, 250), candidate("B", 720, 600)),
			List.of(candidate("E", 120, 100), candidate("F", 360, 300)));

	private StopCandidatesTestPlans() {
	}

	static DrtRoute routeWith(RoutedStopCandidates stopCandidates) {
		DrtRoute route = new DrtRoute(Id.createLinkId("A"), Id.createLinkId("E"));
		route.setStopCandidates(stopCandidates);
		return route;
	}

	static Leg drtLeg(Plan plan) {
		return (Leg)plan.getPlanElements().get(3);
	}

	/**
	 * home(h) - walk - interaction(A) - drt(A to E) - interaction(E) - walk - work(w)
	 */
	static Plan routedPlan() {
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
		route.setStopCandidates(CANDIDATES);
		drt.setRoute(route);
		plan.addLeg(drt);
		plan.addActivity(PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord(2000, 0),
				Id.createLinkId("E"), MODE));
		plan.addLeg(walk("E", "w", 120, 100));
		plan.addActivity(PopulationUtils.createActivityFromLinkId("work", Id.createLinkId("w")));
		return plan;
	}

	static Leg walk(String from, String to, double duration, double distance) {
		Leg leg = PopulationUtils.createLeg(TransportMode.walk);
		TripStructureUtils.setRoutingMode(leg, MODE);
		var route = RouteUtils.createGenericRouteImpl(Id.createLinkId(from), Id.createLinkId(to));
		route.setTravelTime(duration);
		route.setDistance(distance);
		leg.setRoute(route);
		leg.setTravelTime(duration);
		return leg;
	}

	static RoutedStopCandidates.Candidate candidate(String link, double walkDuration, double walkDistance) {
		Coord coord = switch (link) {
			case "A" -> new Coord(-300, 0);
			case "B" -> new Coord(-700, 0);
			case "E" -> new Coord(2000, 0);
			case "F" -> new Coord(2300, 0);
			default -> throw new IllegalArgumentException(link);
		};
		return new RoutedStopCandidates.Candidate(Id.createLinkId(link), coord, walkDuration, walkDistance);
	}

	static Network createNetwork() {
		Network network = NetworkUtils.createNetwork();
		Node n1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("1"), new Coord(0, 0));
		Node n2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("2"), new Coord(100, 0));
		for (String link : List.of("A", "B", "E", "F", "h", "w")) {
			NetworkUtils.createAndAddLink(network, Id.createLinkId(link), n1, n2, 100, 10, 1000, 1);
		}
		return network;
	}
}
