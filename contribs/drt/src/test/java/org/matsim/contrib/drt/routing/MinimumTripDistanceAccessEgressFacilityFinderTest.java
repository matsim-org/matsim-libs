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

package org.matsim.contrib.drt.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.dvrp.router.AccessEgressFacilityCandidates;
import org.matsim.contrib.dvrp.router.DvrpRoutingModule.AccessEgressFacilityFinder;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingRequest;
import org.matsim.facilities.FacilitiesUtils;
import org.matsim.facilities.Facility;

/**
 * @author nkuehnel / MOIA
 */
class MinimumTripDistanceAccessEgressFacilityFinderTest {

	private final Network network = NetworkUtils.createNetwork();

	// the link coordinates (midpoints) are 500 m apart
	private final Link accessLink = link("access", 0);
	private final Link egressLink = link("egress", 500);

	private final Facility accessFacility = FacilitiesUtils.wrapLink(accessLink);
	private final Facility egressFacility = FacilitiesUtils.wrapLink(egressLink);

	private final AccessEgressFacilityFinder delegate = request -> Optional.of(
			new ImmutablePair<>(accessFacility, egressFacility));

	private final Person person = PopulationUtils.getFactory().createPerson(Id.createPersonId("person"));
	private final RoutingRequest request = DefaultRoutingRequest.withoutAttributes(accessFacility, egressFacility,
			8 * 3600, person);

	@Test
	void testTripDistanceIsTheBeelineBetweenTheLinkCoordinates() {
		assertThat(MinimumTripDistanceAccessEgressFacilityFinder.calcTripDistance(accessLink, egressLink)).isEqualTo(
				500);
	}

	@Test
	void testTripsAtOrAboveTheMinimumAreKept() {
		assertThat(finder(400).findFacilities(request)).isPresent();
		// the minimum itself is allowed
		assertThat(finder(500).findFacilities(request)).isPresent();
	}

	@Test
	void testTripsBelowTheMinimumAreRefused() {
		assertThat(finder(500.1).findFacilities(request)).isEmpty();
	}

	@Test
	void testDisabledMinimumKeepsEveryTrip() {
		assertThat(finder(0).findFacilities(request)).contains(new ImmutablePair<>(accessFacility, egressFacility));
	}

	@Test
	void testNothingFoundByTheDelegateStaysEmpty() {
		var finder = new MinimumTripDistanceAccessEgressFacilityFinder(request -> Optional.empty(),
				(request, access, egress) -> fail("the minimum must not be queried without facilities"), network);

		assertThat(finder.findFacilities(request)).isEmpty();
	}

	@Test
	void testMinimumIsQueriedPerRequestWithTheFoundFacilities() {
		Person otherPerson = PopulationUtils.getFactory().createPerson(Id.createPersonId("otherPerson"));
		var finder = new MinimumTripDistanceAccessEgressFacilityFinder(delegate, (request, access, egress) -> {
			assertThat(access).isSameAs(accessFacility);
			assertThat(egress).isSameAs(egressFacility);
			// only the first person is bound to a minimum trip distance above the trip distance
			return request.getPerson() == person ? 1000 : 0;
		}, network);

		assertThat(finder.findFacilities(request)).isEmpty();
		assertThat(finder.findFacilities(
				DefaultRoutingRequest.withoutAttributes(accessFacility, egressFacility, 8 * 3600, otherPerson)))
				.isPresent();
	}

	@Test
	void testStopCandidatesOfTheDelegateArePassedOn() {
		// alternative access 100 m before the access link: every combination is at least 500 m long
		Facility otherAccess = FacilitiesUtils.wrapLink(link("otherAccess", -100));
		var finder = new MinimumTripDistanceAccessEgressFacilityFinder(
				candidatesDelegate(List.of(accessFacility, otherAccess), List.of(egressFacility)),
				(request, access, egress) -> 400, network);

		assertThat(finder.findCandidates(request)).hasValueSatisfying(candidates -> {
			assertThat(candidates.access()).containsExactly(accessFacility, otherAccess);
			assertThat(candidates.egress()).containsExactly(egressFacility);
		});
	}

	@Test
	void testStopCandidatesTooCloseToTheOtherSideAreDropped() {
		// alternative egress 100 m from the access link, alternative access 100 m from the egress link
		Facility nearEgress = FacilitiesUtils.wrapLink(link("nearEgress", 100));
		Facility nearAccess = FacilitiesUtils.wrapLink(link("nearAccess", 400));
		Facility farEgress = FacilitiesUtils.wrapLink(link("farEgress", 600));
		var finder = new MinimumTripDistanceAccessEgressFacilityFinder(
				candidatesDelegate(List.of(accessFacility, nearAccess), List.of(egressFacility, nearEgress, farEgress)),
				(request, access, egress) -> 400, network);

		// every remaining combination of access and egress is at least 400 m long
		assertThat(finder.findCandidates(request)).hasValueSatisfying(candidates -> {
			assertThat(candidates.access()).containsExactly(accessFacility);
			assertThat(candidates.egress()).containsExactly(egressFacility, farEgress);
		});
	}

	@Test
	void testStopCandidatesAreRefusedIfThePreferredTripIsTooShort() {
		Facility farEgress = FacilitiesUtils.wrapLink(link("farEgress", 600));
		var finder = new MinimumTripDistanceAccessEgressFacilityFinder(
				candidatesDelegate(List.of(accessFacility), List.of(egressFacility, farEgress)),
				(request, access, egress) -> 550, network);

		// consistent with findFacilities: the routed (preferred) trip is refused, so there are no candidates either
		assertThat(finder.findFacilities(request)).isEmpty();
		assertThat(finder.findCandidates(request)).isEmpty();
	}

	private AccessEgressFacilityFinder candidatesDelegate(List<Facility> access, List<Facility> egress) {
		return new AccessEgressFacilityFinder() {
			@Override
			public Optional<org.apache.commons.lang3.tuple.Pair<Facility, Facility>> findFacilities(
					RoutingRequest request) {
				return Optional.of(new ImmutablePair<>(access.getFirst(), egress.getFirst()));
			}

			@Override
			public Optional<AccessEgressFacilityCandidates> findCandidates(RoutingRequest request) {
				return Optional.of(new AccessEgressFacilityCandidates(access, egress));
			}
		};
	}

	private MinimumTripDistanceAccessEgressFacilityFinder finder(double minimumTripDistance) {
		return new MinimumTripDistanceAccessEgressFacilityFinder(delegate,
				(request, access, egress) -> minimumTripDistance, network);
	}

	private Link link(String id, double x) {
		Node fromNode = NetworkUtils.createAndAddNode(network, Id.createNodeId(id + "_from"), new Coord(x, 0));
		Node toNode = NetworkUtils.createAndAddNode(network, Id.createNodeId(id + "_to"), new Coord(x, 100));
		return NetworkUtils.createAndAddLink(network, Id.createLinkId(id), fromNode, toNode, 100, 10, 1000, 1);
	}
}
