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
 * *********************************************************************** */

package org.matsim.contrib.drt.passenger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.drt.passenger.DrtMinimumTripDistanceRequestValidator.BELOW_MINIMUM_TRIP_DISTANCE_CAUSE;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.drt.optimizer.constraints.DrtOptimizationConstraintsSetImpl;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.network.NetworkUtils;

/**
 * @author nkuehnel / MOIA
 */
class DrtMinimumTripDistanceRequestValidatorTest {

	private final Network network = NetworkUtils.createNetwork();

	// the link coordinates (midpoints) are 500 m apart
	private final Link fromLink = link("from", 0);
	private final Link toLink = link("to", 500);

	@Test
	void testRequestAtOrAboveTheMinimumIsAccepted() {
		assertThat(new DrtMinimumTripDistanceRequestValidator(400).validateRequest(request(fromLink, toLink))).isEmpty();
		// the minimum itself is allowed, consistent with the routing
		assertThat(new DrtMinimumTripDistanceRequestValidator(500).validateRequest(request(fromLink, toLink))).isEmpty();
	}

	@Test
	void testRequestBelowTheMinimumIsRejected() {
		assertThat(new DrtMinimumTripDistanceRequestValidator(500.1).validateRequest(request(fromLink, toLink)))
				.containsExactly(BELOW_MINIMUM_TRIP_DISTANCE_CAUSE);
	}

	@Test
	void testDisabledMinimumAcceptsEveryRequest() {
		// equal links are left to the DefaultPassengerRequestValidator
		assertThat(new DrtMinimumTripDistanceRequestValidator(0).validateRequest(request(fromLink, fromLink))).isEmpty();
	}

	@Test
	void testTheSmallestMinimumOfAllConstraintsSetsIsApplied() {
		var validator = DrtMinimumTripDistanceRequestValidator.forConstraintsSets(
				List.of(constraintsSet("strict", 1000), constraintsSet("lenient", 300)));

		// a request accepted under the lenient set must not be rejected, as the validator cannot tell the sets apart
		assertThat(validator.getMinimumTripDistance()).isEqualTo(300);
		assertThat(validator.validateRequest(request(fromLink, toLink))).isEmpty();
	}

	@Test
	void testWithoutConstraintsSetsTheMinimumIsDisabled() {
		assertThat(DrtMinimumTripDistanceRequestValidator.forConstraintsSets(List.of()).getMinimumTripDistance())
				.isZero();
	}

	private static DrtOptimizationConstraintsSetImpl constraintsSet(String name, double minimumTripDistance) {
		var constraintsSet = new DrtOptimizationConstraintsSetImpl();
		constraintsSet.setConstraintSetName(name);
		constraintsSet.setMinimumTripDistance(minimumTripDistance);
		return constraintsSet;
	}

	private static DrtRequest request(Link fromLink, Link toLink) {
		return DrtRequest.newBuilder()
				.id(Id.create("request", Request.class))
				.submissionTime(8 * 3600)
				.earliestDepartureTime(8 * 3600)
				.mode("drt")
				.fromLink(fromLink)
				.toLink(toLink)
				.build();
	}

	private Link link(String id, double x) {
		Node fromNode = NetworkUtils.createAndAddNode(network, Id.createNodeId(id + "_from"), new Coord(x, 0));
		Node toNode = NetworkUtils.createAndAddNode(network, Id.createNodeId(id + "_to"), new Coord(x, 100));
		return NetworkUtils.createAndAddLink(network, Id.createLinkId(id), fromNode, toNode, 100, 10, 1000, 1);
	}
}
