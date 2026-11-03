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

import java.util.Optional;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
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
