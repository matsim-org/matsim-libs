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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.core.router.RoutingRequest;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.collections.QuadTrees;
import org.matsim.facilities.Facility;

/**
 * @author nkuehnel / MOIA
 */
public class ClosestAccessEgressFacilityFinderTest {

	private final Network network = createNetwork();

	private final Facility stopA = facility("linkAtA", new Coord(0, 0));
	private final Facility stopB = facility("linkAtB", new Coord(1000, 0));

	private final Facility fromFacility = facility("linkAtA", new Coord(100, 0));
	private final Facility toFacility = facility("linkAtB", new Coord(900, 0));

	@Test
	void testBothStopsWithinConstantMaxDistance() {
		var finder = new ClosestAccessEgressFacilityFinder(200, network, quadTree());

		Optional<Pair<Facility, Facility>> facilities = finder.findFacilities(request(0, null));

		assertThat(facilities).isPresent();
		assertThat(facilities.get().getLeft()).isSameAs(stopA);
		assertThat(facilities.get().getRight()).isSameAs(stopB);
	}

	@Test
	void testAccessStopTooFarAway() {
		// 100 m to the access stop, 100 m to the egress stop
		var finder = new ClosestAccessEgressFacilityFinder(99, network, quadTree());
		assertThat(finder.findFacilities(request(0, null))).isEmpty();
	}

	@Test
	void testEgressStopTooFarAway() {
		// 100 m to the access stop, 300 m to the egress stop
		var finder = new ClosestAccessEgressFacilityFinder(200, network, quadTree());
		RoutingRequest request = DefaultRoutingRequest.withoutAttributes(fromFacility,
				facility("linkAtB", new Coord(700, 0)), 0, null);
		assertThat(finder.findFacilities(request)).isEmpty();
	}

	@Test
	void testUnboundedMaxDistance() {
		var finder = new ClosestAccessEgressFacilityFinder(Double.MAX_VALUE, network, quadTree());
		RoutingRequest request = DefaultRoutingRequest.withoutAttributes(facility("linkAtA", new Coord(-100_000, 0)),
				toFacility, 0, null);
		assertThat(finder.findFacilities(request)).isPresent();
	}

	@Test
	void testMaxAccessEgressDistanceIsAskedWithTheFoundStopsAndTheRequest() {
		RoutingRequest request = request(8 * 3600, null);
		var recorded = new Object() {
			RoutingRequest seenRequest;
			Facility seenAccessFacility;
			Facility seenEgressFacility;
		};

		var finder = new ClosestAccessEgressFacilityFinder((r, accessFacility, egressFacility) -> {
			recorded.seenRequest = r;
			recorded.seenAccessFacility = accessFacility;
			recorded.seenEgressFacility = egressFacility;
			return 200;
		}, 0, network, quadTree());

		assertThat(finder.findFacilities(request)).isPresent();
		assertThat(recorded.seenRequest).isSameAs(request);
		assertThat(recorded.seenAccessFacility).isSameAs(stopA);
		assertThat(recorded.seenEgressFacility).isSameAs(stopB);
	}

	/**
	 * This is the regression test for the maxWalkDistance defect: the maximum distance may differ per trip (e.g. per
	 * person, as here, or per time of day), so it must be evaluated per call and not once at construction time.
	 */
	@Test
	void testMaxAccessEgressDistanceMayDifferPerPerson() {
		Person personWithLongWalk = PopulationUtils.getFactory().createPerson(Id.createPersonId("long"));
		Person personWithShortWalk = PopulationUtils.getFactory().createPerson(Id.createPersonId("short"));

		var finder = new ClosestAccessEgressFacilityFinder(
				(r, accessFacility, egressFacility) -> r.getPerson() == personWithLongWalk ? 200 : 50, 200, network,
				quadTree());

		assertThat(finder.findFacilities(request(0, personWithLongWalk))).isPresent();
		assertThat(finder.findFacilities(request(0, personWithShortWalk))).isEmpty();
	}

	@Test
	void testFacilityWithoutCoordIsResolvedViaNetwork() {
		// linkAtA and linkAtB are centred on the two stops, so the distances are 0
		var finder = new ClosestAccessEgressFacilityFinder(0, network, quadTree());

		Optional<Pair<Facility, Facility>> facilities = finder.findFacilities(
				DefaultRoutingRequest.withoutAttributes(facility("linkAtA", null), facility("linkAtB", null), 0, null));

		assertThat(facilities).isPresent();
		assertThat(facilities.get().getLeft()).isSameAs(stopA);
		assertThat(facilities.get().getRight()).isSameAs(stopB);
	}

	@Test
	void testFindClosestStopUsesTheDefaultMaxDistance() {
		var finder = new ClosestAccessEgressFacilityFinder((r, accessFacility, egressFacility) -> 0, 200, network,
				quadTree());

		assertThat(finder.findClosestStop(fromFacility)).isSameAs(stopA);
		assertThat(finder.findClosestStop(facility("linkAtA", new Coord(500, 0)))).isNull();
		assertThat(finder.findClosestStop(facility("linkAtA", new Coord(500, 0)), 600)).isNotNull();
	}

	private RoutingRequest request(double departureTime, Person person) {
		return DefaultRoutingRequest.withoutAttributes(fromFacility, toFacility, departureTime, person);
	}

	private QuadTree<Facility> quadTree() {
		return QuadTrees.createQuadTree(List.of(stopA, stopB));
	}

	private static Facility facility(String linkId, Coord coord) {
		return new Facility() {
			@Override
			public Coord getCoord() {
				return coord;
			}

			@Override
			public Id<Link> getLinkId() {
				return Id.createLinkId(linkId);
			}

			@Override
			public Map<String, Object> getCustomAttributes() {
				return Map.of();
			}
		};
	}

	private static Network createNetwork() {
		Network network = NetworkUtils.createNetwork();
		// the links are centred on (0, 0) and (1000, 0), i.e. on the two stops, because Link.getCoord() interpolates
		// between the from and the to node
		Node a1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("a1"), new Coord(-10, 0));
		Node a2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("a2"), new Coord(10, 0));
		Node b1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("b1"), new Coord(990, 0));
		Node b2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("b2"), new Coord(1010, 0));
		NetworkUtils.createAndAddLink(network, Id.createLinkId("linkAtA"), a1, a2, 20, 10, 1000, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("linkAtB"), b1, b2, 20, 10, 1000, 1);
		return network;
	}
}
