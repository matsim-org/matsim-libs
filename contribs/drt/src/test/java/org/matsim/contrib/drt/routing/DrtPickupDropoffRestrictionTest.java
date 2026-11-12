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

package org.matsim.contrib.drt.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.matsim.contrib.drt.passenger.DrtPickupDropoffRestrictionRequestValidator.PICKUP_DROPOFF_RESTRICTION_CAUSE;

import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.junit.jupiter.api.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneImpl;
import org.matsim.contrib.drt.passenger.DrtPickupDropoffRestrictionRequestValidator;
import org.matsim.contrib.drt.passenger.DrtRequest;
import org.matsim.contrib.drt.run.DrtPickupDropoffRestrictionParams.RequiredInArea;
import org.matsim.contrib.dvrp.optimizer.Request;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.DefaultRoutingRequest;
import org.matsim.facilities.FacilitiesUtils;

class DrtPickupDropoffRestrictionTest {

	private final Network network = NetworkUtils.createNetwork();

	// the area is the square [0, 1000] x [0, 1000]; only the toNode of a link counts
	private final Link inside = link("inside", new Coord(-500, 500), new Coord(500, 500));
	private final Link outside = link("outside", new Coord(500, 500), new Coord(2000, 500));
	private final Link otherOutside = link("otherOutside", new Coord(500, 500), new Coord(-2000, 500));

	@Test
	void testTheToNodeDecidesWhetherALinkIsInTheArea() {
		var restriction = restriction(RequiredInArea.pickupOrDropoff);
		assertThat(restriction.isInArea(inside)).isTrue();
		// the fromNode of 'outside' lies in the area, its toNode does not
		assertThat(restriction.isInArea(outside)).isFalse();
	}

	@Test
	void testPickup() {
		var restriction = restriction(RequiredInArea.pickup);
		assertThat(restriction.isAllowed(inside, outside)).isTrue();
		assertThat(restriction.isAllowed(outside, inside)).isFalse();
		assertThat(restriction.isAllowed(inside, inside)).isTrue();
		assertThat(restriction.isAllowed(outside, otherOutside)).isFalse();
	}

	@Test
	void testDropoff() {
		var restriction = restriction(RequiredInArea.dropoff);
		assertThat(restriction.isAllowed(inside, outside)).isFalse();
		assertThat(restriction.isAllowed(outside, inside)).isTrue();
		assertThat(restriction.isAllowed(inside, inside)).isTrue();
		assertThat(restriction.isAllowed(outside, otherOutside)).isFalse();
	}

	@Test
	void testPickupOrDropoff() {
		var restriction = restriction(RequiredInArea.pickupOrDropoff);
		assertThat(restriction.isAllowed(inside, outside)).isTrue();
		assertThat(restriction.isAllowed(outside, inside)).isTrue();
		assertThat(restriction.isAllowed(inside, inside)).isTrue();
		assertThat(restriction.isAllowed(outside, otherOutside)).isFalse();
	}

	@Test
	void testPickupAndDropoff() {
		var restriction = restriction(RequiredInArea.pickupAndDropoff);
		assertThat(restriction.isAllowed(inside, outside)).isFalse();
		assertThat(restriction.isAllowed(outside, inside)).isFalse();
		assertThat(restriction.isAllowed(inside, inside)).isTrue();
		assertThat(restriction.isAllowed(outside, otherOutside)).isFalse();
	}

	@Test
	void testFinderRefusesTripsViolatingTheRestriction() {
		var restriction = restriction(RequiredInArea.pickup);
		assertThat(findFacilities(restriction, inside, outside)).isPresent();
		assertThat(findFacilities(restriction, outside, inside)).isEmpty();
	}

	@Test
	void testFinderKeepsNothingFoundByTheDelegate() {
		var finder = new PickupDropoffRestrictionAccessEgressFacilityFinder(request -> Optional.empty(),
				restriction(RequiredInArea.pickupOrDropoff), network);
		var facility = FacilitiesUtils.wrapLink(inside);
		assertThat(finder.findFacilities(DefaultRoutingRequest.withoutAttributes(facility, facility, 0, null))).isEmpty();
	}

	@Test
	void testValidatorDecidesLikeTheFinder() {
		var validator = new DrtPickupDropoffRestrictionRequestValidator(restriction(RequiredInArea.dropoff));
		assertThat(validator.validateRequest(request(outside, inside))).isEmpty();
		assertThat(validator.validateRequest(request(inside, outside))).containsExactly(PICKUP_DROPOFF_RESTRICTION_CAUSE);
	}

	@Test
	void testRebalancingZonesAreRestrictedOnlyIfAllPickupsLieInTheArea() {
		Zone insideZone = zone("insideZone", 100, 900);
		Zone overlappingZone = zone("overlappingZone", 900, 1100);
		Zone outsideZone = zone("outsideZone", 2000, 3000);

		for (RequiredInArea requiredInArea : List.of(RequiredInArea.pickup, RequiredInArea.pickupAndDropoff)) {
			var filter = restriction(requiredInArea).pickupZoneFilter();
			assertThat(filter.test(insideZone)).isTrue();
			assertThat(filter.test(overlappingZone)).isTrue();
			assertThat(filter.test(outsideZone)).isFalse();
		}
		for (RequiredInArea requiredInArea : List.of(RequiredInArea.dropoff, RequiredInArea.pickupOrDropoff)) {
			assertThat(restriction(requiredInArea).pickupZoneFilter().test(outsideZone)).isTrue();
		}
	}

	private static Zone zone(String id, double min, double max) {
		var square = new GeometryFactory().createPolygon(new Coordinate[] { new Coordinate(min, min),
				new Coordinate(max, min), new Coordinate(max, max), new Coordinate(min, max), new Coordinate(min, min) });
		return new ZoneImpl(Id.create(id, Zone.class), new PreparedPolygon(square), null);
	}

	private Optional<?> findFacilities(DrtPickupDropoffRestriction restriction, Link pickup, Link dropoff) {
		var pickupFacility = FacilitiesUtils.wrapLink(pickup);
		var dropoffFacility = FacilitiesUtils.wrapLink(dropoff);
		var finder = new PickupDropoffRestrictionAccessEgressFacilityFinder(
				request -> Optional.of(new ImmutablePair<>(pickupFacility, dropoffFacility)), restriction, network);
		return finder.findFacilities(DefaultRoutingRequest.withoutAttributes(pickupFacility, dropoffFacility, 0, null));
	}

	private static DrtPickupDropoffRestriction restriction(RequiredInArea requiredInArea) {
		var square = new GeometryFactory().createPolygon(new Coordinate[] { new Coordinate(0, 0), new Coordinate(1000, 0),
				new Coordinate(1000, 1000), new Coordinate(0, 1000), new Coordinate(0, 0) });
		return new DrtPickupDropoffRestriction(List.of(PreparedGeometryFactory.prepare(square)), requiredInArea);
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

	private Link link(String id, Coord from, Coord to) {
		Node fromNode = NetworkUtils.createAndAddNode(network, Id.createNodeId(id + "_from"), from);
		Node toNode = NetworkUtils.createAndAddNode(network, Id.createNodeId(id + "_to"), to);
		return NetworkUtils.createAndAddLink(network, Id.createLinkId(id), fromNode, toNode, 100, 10, 1000, 1);
	}
}
