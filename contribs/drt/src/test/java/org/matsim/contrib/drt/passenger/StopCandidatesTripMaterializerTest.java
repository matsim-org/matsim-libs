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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.matsim.contrib.drt.passenger.StopCandidatesTestPlans.*;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.contrib.drt.routing.DrtRoute;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.TripStructureUtils;

/**
 * Tests {@link StopCandidatesTripMaterializer}.
 *
 * @author nkuehnel / MOIA
 */
public class StopCandidatesTripMaterializerTest {
	@Test
	void testMaterializeRewritesAccessAndEgressToTheChosenStops() {
		Plan plan = routedPlan();
		Leg drtLeg = drtLeg(plan);

		boolean changed = StopCandidatesTripMaterializer.materialize(plan, drtLeg, Id.createLinkId("B"),
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
		assertThat(((DrtRoute)drtLeg.getRoute()).getStopCandidates()).contains(CANDIDATES);
	}

	@Test
	void testMaterializeOnlyTheChangedSide() {
		Plan plan = routedPlan();
		Leg routedEgress = (Leg)plan.getPlanElements().get(5);

		assertThat(StopCandidatesTripMaterializer.materialize(plan, drtLeg(plan), Id.createLinkId("B"),
				Id.createLinkId("E"))).isTrue();

		assertThat(plan.getPlanElements().get(5)).isSameAs(routedEgress);
		assertThat(drtLeg(plan).getRoute().getEndLinkId()).isEqualTo(Id.createLinkId("E"));
	}

	@Test
	void testMaterializeKeepsTheModeOfTheRoutedAccessAndEgress() {
		// with custom access/egress routers (e.g. bike), the candidate durations come from these routers, so the
		// rewritten legs must keep their mode
		Plan plan = routedPlan();
		((Leg)plan.getPlanElements().get(1)).setMode(TransportMode.bike);
		((Leg)plan.getPlanElements().get(5)).setMode(TransportMode.bike);

		assertThat(StopCandidatesTripMaterializer.materialize(plan, drtLeg(plan), Id.createLinkId("B"),
				Id.createLinkId("F"))).isTrue();

		assertThat(((Leg)plan.getPlanElements().get(1)).getMode()).isEqualTo(TransportMode.bike);
		assertThat(((Leg)plan.getPlanElements().get(5)).getMode()).isEqualTo(TransportMode.bike);
	}

	@Test
	void testMaterializeDoesNothingForTheRoutedStops() {
		Plan plan = routedPlan();
		var before = List.copyOf(plan.getPlanElements());

		assertThat(StopCandidatesTripMaterializer.materialize(plan, drtLeg(plan), Id.createLinkId("A"),
				Id.createLinkId("E"))).isFalse();

		assertThat(plan.getPlanElements()).containsExactlyElementsOf(before);
	}

	@Test
	void testTripWithoutOwnWalksCannotBeMoved() {
		Plan routed = routedPlan();
		assertThat(StopCandidatesTripMaterializer.hasSingleAccessAndEgressLeg(routed, drtLeg(routed))).isTrue();

		// like a companion: the trip starts and ends at the stops
		Plan plan = PopulationUtils.createPlan();
		Activity origin = PopulationUtils.createActivityFromLinkId("home", Id.createLinkId("A"));
		origin.setEndTime(8 * 3600);
		plan.addActivity(origin);
		Leg drt = PopulationUtils.createLeg(MODE);
		drt.setRoute(routeWith(CANDIDATES));
		plan.addLeg(drt);
		plan.addActivity(PopulationUtils.createActivityFromLinkId("work", Id.createLinkId("E")));

		assertThat(StopCandidatesTripMaterializer.hasSingleAccessAndEgressLeg(plan, drt)).isFalse();

		// incomplete trip without destination activity
		Plan incomplete = PopulationUtils.createPlan();
		incomplete.addActivity(PopulationUtils.createActivityFromLinkId("home", Id.createLinkId("h")));
		Leg lastLeg = PopulationUtils.createLeg(MODE);
		incomplete.addLeg(lastLeg);
		assertThat(StopCandidatesTripMaterializer.hasSingleAccessAndEgressLeg(incomplete, lastLeg)).isFalse();
	}

	@Test
	void testTripWithSeveralAccessLegsCannotBeMoved() {
		// e.g. a walk and a bike leg to the stop: only a single access/egress leg can be replaced by a candidate's walk
		Plan plan = routedPlan();
		plan.getPlanElements().add(1, walk("h", "x", 60, 50));
		plan.getPlanElements().add(2, PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord(0, 0),
				Id.createLinkId("x"), TransportMode.bike));
		Leg drt = (Leg)plan.getPlanElements().get(5);

		assertThat(StopCandidatesTripMaterializer.hasSingleAccessAndEgressLeg(plan, drt)).isFalse();
		assertThatThrownBy(() -> StopCandidatesTripMaterializer.materialize(plan, drt, Id.createLinkId("B"),
				Id.createLinkId("F"))).isInstanceOf(IllegalArgumentException.class);
	}

	@Test
	void testDrtFeederToPtIsNotMoved() {
		// intermodal trip: home - walk - drt - walk - pt - walk - work. Moving the dropoff must not drop the pt part, so
		// such trips are not moved at all (a changed dropoff would also change the arrival at the transfer)
		Plan plan = routedPlan();
		Activity work = (Activity)plan.getPlanElements().remove(6);
		plan.addActivity(PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord(2100, 0),
				Id.createLinkId("w"), TransportMode.pt));
		Leg pt = PopulationUtils.createLeg(TransportMode.pt);
		TripStructureUtils.setRoutingMode(pt, TransportMode.pt);
		plan.addLeg(pt);
		plan.addActivity(PopulationUtils.createStageActivityFromCoordLinkIdAndModePrefix(new Coord(5000, 0),
				Id.createLinkId("w"), TransportMode.pt));
		plan.addLeg(walk("w", "w", 60, 50));
		plan.addActivity(work);
		var before = List.copyOf(plan.getPlanElements());

		assertThat(StopCandidatesTripMaterializer.hasSingleAccessAndEgressLeg(plan, drtLeg(plan))).isFalse();
		assertThatThrownBy(() -> StopCandidatesTripMaterializer.materialize(plan, drtLeg(plan), Id.createLinkId("A"),
				Id.createLinkId("F"))).isInstanceOf(IllegalArgumentException.class);
		assertThat(plan.getPlanElements()).containsExactlyElementsOf(before);
	}

	@Test
	void testOriginActivity() {
		Plan plan = routedPlan();
		assertThat(StopCandidatesTripMaterializer.originActivity(plan, drtLeg(plan))).isSameAs(plan.getPlanElements().get(0));
	}
}
