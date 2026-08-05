/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.network;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link BicycleLinkPolicy}, run against the raw OSM tag maps the
 * reader hands to the {@code setAfterLinkCreated} callback — i.e. maps that
 * do <em>not</em> carry default {@code oneway} / {@code oneway:bicycle} values.
 *
 * <p>The bulk of these pin down the oneway handling, which is where a
 * null-unsafe {@code tags.get(...).equals(...)} previously threw a
 * {@link NullPointerException} for the common case of a cycleway without any
 * oneway tagging (see {@link #cyclewayWithoutOnewayTags_doesNotThrow}).
 *
 * @author smetzler
 */
public class BicycleLinkPolicyTest {

	private final BicycleLinkPolicy policy =
		new BicycleLinkPolicy(new BicycleInfraClassifier(), new TagCopier(List.of(), "osm:"));

	// =========================================================================
	// NPE regression: raw OSM tags rarely carry oneway / oneway:bicycle
	// =========================================================================

	@Test
	void cyclewayWithoutOnewayTags_doesNotThrow() {
		Link forward = link("1f");
		Link reverse = link("1r");
		Map<String, String> tags = tags("highway", "cycleway");

		// Neither direction may throw, and both keep bike (no oneway restriction).
		assertDoesNotThrow(() -> policy.apply(forward, tags, OsmWayDirection.FORWARD));
		assertDoesNotThrow(() -> policy.apply(reverse, tags, OsmWayDirection.REVERSE));
		assertTrue(forward.getAllowedModes().contains(TransportMode.bike));
		assertTrue(reverse.getAllowedModes().contains(TransportMode.bike));

		// bicycle_infra is always written.
		assertNotNull(forward.getAttributes().getAttribute(BicycleUtils.BICYCLE_INFRA));
	}

	// =========================================================================
	// oneway:bicycle=yes -> bikes only in forward direction
	// =========================================================================

	@Test
	void onewayBicycleYes_forwardKeepsBike_reverseLosesBike_bikeReverseDropped() {
		Map<String, String> tags = tags("highway", "cycleway", "oneway:bicycle", "yes");

		Link forward = link("1f");
		Link reverse = link("1r");
		Link bikeReverse = link("1_bike-reverse");

		policy.apply(forward, tags, OsmWayDirection.FORWARD);
		policy.apply(reverse, tags, OsmWayDirection.REVERSE);
		policy.apply(bikeReverse, tags, OsmWayDirection.REVERSE);

		assertTrue(forward.getAllowedModes().contains(TransportMode.bike), "forward keeps bike");
		assertFalse(reverse.getAllowedModes().contains(TransportMode.bike), "'r' reverse loses bike");
		assertTrue(bikeReverse.getAllowedModes().isEmpty(), "'_bike-reverse' is dropped");
	}

	// =========================================================================
	// oneway=yes (car oneway) with / without an explicit bicycle exception
	// =========================================================================

	@Test
	void onewayYes_reverseLosesBike() {
		Map<String, String> tags = tags("highway", "cycleway", "oneway", "yes");
		Link reverse = link("1r");

		policy.apply(reverse, tags, OsmWayDirection.REVERSE);

		assertFalse(reverse.getAllowedModes().contains(TransportMode.bike));
	}

	@Test
	void onewayYesButBicycleNo_reverseKeepsBike() {
		// oneway=yes for cars, but oneway:bicycle=no -> bikes allowed both ways.
		Map<String, String> tags = tags("highway", "cycleway", "oneway", "yes", "oneway:bicycle", "no");
		Link reverse = link("1r");

		policy.apply(reverse, tags, OsmWayDirection.REVERSE);

		assertTrue(reverse.getAllowedModes().contains(TransportMode.bike));
	}

	// =========================================================================
	// footway / pedestrian whitelist + bicycle=no handling
	// =========================================================================

	@Test
	void footwayWithoutBicycle_isDropped() {
		Link link = link("1f");
		policy.apply(link, tags("highway", "footway"), OsmWayDirection.FORWARD);
		assertTrue(link.getAllowedModes().isEmpty());
	}

	@Test
	void footwayWithBicycleYes_keepsBike() {
		Link link = link("1f");
		policy.apply(link, tags("highway", "footway", "bicycle", "yes"), OsmWayDirection.FORWARD);
		assertTrue(link.getAllowedModes().contains(TransportMode.bike));
	}

	@Test
	void bicycleNo_onBikeOnlyLink_leavesNoModes() {
		// A cycleway is bike-only, so dropping bike leaves it empty -> removed downstream.
		Link link = link("1f");
		policy.apply(link, tags("highway", "cycleway", "bicycle", "no"), OsmWayDirection.FORWARD);
		assertTrue(link.getAllowedModes().isEmpty());
	}

	@Test
	void bicycleNo_onCarLink_keepsCarDropsBike() {
		// highway=primary + bicycle=no: bikes are forbidden, but the road stays
		// open to cars. The link must survive as a car-only link, not be dropped.
		Link link = link("1f");
		link.setAllowedModes(Set.of(TransportMode.car, TransportMode.bike));
		policy.apply(link, tags("highway", "primary", "bicycle", "no"), OsmWayDirection.FORWARD);
		assertFalse(link.getAllowedModes().contains(TransportMode.bike), "bike is dropped");
		assertTrue(link.getAllowedModes().contains(TransportMode.car), "car is kept");
	}

	// =========================================================================
	// service=parking_aisle drop
	// =========================================================================

	@Test
	void serviceParkingAisle_isDropped() {
		Link link = link("1f");
		policy.apply(link, tags("highway", "service", "service", "parking_aisle"), OsmWayDirection.FORWARD);
		assertTrue(link.getAllowedModes().isEmpty());
	}

	@Test
	void serviceDriveway_isNotDroppedByParkingRule() {
		// Only parking_aisle is dropped; other service subtypes stay cyclable.
		Link link = link("1f");
		policy.apply(link, tags("highway", "service", "service", "driveway"), OsmWayDirection.FORWARD);
		assertTrue(link.getAllowedModes().contains(TransportMode.bike));
	}

	// =========================================================================
	// access=no/private/customer drop
	// =========================================================================

	@Test
	void accessNo_isDropped() {
		Link link = link("1f");
		policy.apply(link, tags("highway", "service", "access", "no"), OsmWayDirection.FORWARD);
		assertTrue(link.getAllowedModes().isEmpty());
	}

	@Test
	void accessPrivate_isDropped() {
		Link link = link("1f");
		policy.apply(link, tags("highway", "service", "access", "private"), OsmWayDirection.FORWARD);
		assertTrue(link.getAllowedModes().isEmpty());
	}

	@Test
	void accessCustomer_isDropped() {
		Link link = link("1f");
		policy.apply(link, tags("highway", "service", "access", "customer"), OsmWayDirection.FORWARD);
		assertTrue(link.getAllowedModes().isEmpty());
	}

	/**
	 * Every value in {@code ACCESS_RESTRICTED} drops the link, not just no/private.
	 * {@code permissive} and {@code permit} are deliberately included although OSM reads
	 * them as "allowed until revoked": they mark private ground, and routing cyclists
	 * across a supermarket yard produces shortcuts nobody rides.
	 */
	@ParameterizedTest
	@ValueSource(strings = {"no", "private", "customer", "customers", "emergency", "permissive", "permit"})
	void restrictedAccessValues_areDropped(String access) {
		Link link = link("1f");
		policy.apply(link, tags("highway", "service", "access", access), OsmWayDirection.FORWARD);
		assertTrue(link.getAllowedModes().isEmpty(), "access=" + access + " must drop the link");
	}

	/** ...but a bicycle-specific permission still wins over every one of them. */
	@ParameterizedTest
	@ValueSource(strings = {"no", "private", "customer", "customers", "emergency", "permissive", "permit"})
	void restrictedAccessValues_yieldToBicycleDesignated(String access) {
		Link link = link("1f");
		policy.apply(link, tags("highway", "service", "access", access, "bicycle", "designated"),
			OsmWayDirection.FORWARD);
		assertTrue(link.getAllowedModes().contains(TransportMode.bike),
			"access=" + access + " + bicycle=designated stays cyclable");
	}

	/** Values outside the set are not restrictions and must leave the link alone. */
	@ParameterizedTest
	@ValueSource(strings = {"yes", "destination", "agricultural", "forestry"})
	void unrestrictedAccessValues_keepBike(String access) {
		Link link = link("1f");
		policy.apply(link, tags("highway", "service", "access", access), OsmWayDirection.FORWARD);
		assertTrue(link.getAllowedModes().contains(TransportMode.bike),
			"access=" + access + " is no reason to drop the link");
	}

	@Test
	void accessYes_keepsBike() {
		// access=yes (and other unrestricted values) must not drop the link.
		Link link = link("1f");
		policy.apply(link, tags("highway", "service", "access", "yes"), OsmWayDirection.FORWARD);
		assertTrue(link.getAllowedModes().contains(TransportMode.bike));
	}

	@Test
	void accessPrivateButBicycleYes_keepsBike() {
		// OSM: the bicycle-specific tag overrides the general access restriction.
		Link link = link("1f");
		policy.apply(link, tags("highway", "service", "access", "private", "bicycle", "yes"), OsmWayDirection.FORWARD);
		assertTrue(link.getAllowedModes().contains(TransportMode.bike));
	}

	@Test
	void accessNoButBicycleDesignated_keepsBike() {
		Link link = link("1f");
		policy.apply(link, tags("highway", "service", "access", "no", "bicycle", "designated"), OsmWayDirection.FORWARD);
		assertTrue(link.getAllowedModes().contains(TransportMode.bike));
	}

	// =========================================================================
	// --bike-area-marker gating
	// =========================================================================

	private static final BicycleLinkPolicy GATED = new BicycleLinkPolicy(
		new BicycleInfraClassifier(), new TagCopier(List.of(), "osm:"),
		BicycleLinkPolicy.AreaMarker.parse("city_center=yes"));

	@Test
	void areaMarker_parsesKeyOnly() {
		BicycleLinkPolicy.AreaMarker m = BicycleLinkPolicy.AreaMarker.parse("city_center");
		assertEquals("city_center", m.key());
		assertNull(m.value());
		assertTrue(m.matches(tags("city_center", "whatever")), "any value matches a key-only marker");
		assertFalse(m.matches(tags("highway", "primary")));
	}

	@Test
	void areaMarker_parsesKeyValue() {
		BicycleLinkPolicy.AreaMarker m = BicycleLinkPolicy.AreaMarker.parse("city_center=yes");
		assertEquals("city_center", m.key());
		assertEquals("yes", m.value());
		assertTrue(m.matches(tags("city_center", "yes")));
		assertFalse(m.matches(tags("city_center", "no")), "a different value does not match");
	}

	@Test
	void markedWay_getsFullBicycleTreatment() {
		Link link = link("1f");   // {bike}
		GATED.apply(link, tags("highway", "cycleway", "city_center", "yes"), OsmWayDirection.FORWARD);

		assertTrue(link.getAllowedModes().contains(TransportMode.bike), "marked way keeps bike");
		assertNotNull(link.getAttributes().getAttribute(BicycleUtils.BICYCLE_INFRA),
			"marked way gets bicycle_infra");
		assertEquals(Boolean.TRUE, BicycleUtils.getBicycleArea(link), "and is flagged as inside");
	}

	/**
	 * Which side of the area a link fell on is recorded on both sides, so downstream can
	 * filter on the area itself rather than inferring it from a missing category - which
	 * would also catch links that stayed unclassified for entirely different reasons.
	 */
	@Test
	void bicycleArea_isRecordedOnBothSides() {
		Link inside = link("1f");
		GATED.apply(inside, tags("highway", "residential", "city_center", "yes"), OsmWayDirection.FORWARD);
		assertEquals(Boolean.TRUE, BicycleUtils.getBicycleArea(inside));

		Link outside = link("2f");
		GATED.apply(outside, tags("highway", "residential"), OsmWayDirection.FORWARD);
		assertEquals(Boolean.FALSE, BicycleUtils.getBicycleArea(outside));

		// no marker configured -> the whole network had the full treatment, so the
		// attribute stays absent rather than claiming an area that does not exist
		Link ungated = link("3f");
		new BicycleLinkPolicy(new BicycleInfraClassifier(), new TagCopier(List.of(), "osm:"), null)
			.apply(ungated, tags("highway", "residential"), OsmWayDirection.FORWARD);
		assertNull(BicycleUtils.getBicycleArea(ungated));
	}

	@Test
	void unmarkedWay_keepsModesButStripsBicycleDetail() {
		Link link = link("1f");
		link.setAllowedModes(Set.of(TransportMode.car, TransportMode.bike));
		// mimic the bike attributes the reader stamps before the policy runs
		link.getAttributes().putAttribute("surface", "asphalt");
		link.getAttributes().putAttribute("smoothness", "good");
		link.getAttributes().putAttribute("cycleway", "lane");
		link.getAttributes().putAttribute("bicycle", "yes");

		GATED.apply(link, tags("highway", "secondary"), OsmWayDirection.FORWARD);   // no marker tag

		assertEquals(Set.of(TransportMode.car, TransportMode.bike), link.getAllowedModes(),
			"outside the area the reader's modes are kept -- bike stays open");
		assertNull(link.getAttributes().getAttribute("surface"));
		assertNull(link.getAttributes().getAttribute("smoothness"));
		assertNull(link.getAttributes().getAttribute("cycleway"));
		assertNull(link.getAttributes().getAttribute("bicycle"));
		assertNull(link.getAttributes().getAttribute(BicycleUtils.BICYCLE_INFRA),
			"no bicycle_infra outside the area");
	}

	@Test
	void unmarkedBikeOnlyWay_keepsBikeButNoDetail() {
		// A cycleway outside the area keeps its bike mode (still rideable) but gets
		// no bicycle_infra -- and therefore no elevation metrics later.
		Link link = link("1f");   // {bike}
		GATED.apply(link, tags("highway", "cycleway"), OsmWayDirection.FORWARD);
		assertEquals(Set.of(TransportMode.bike), link.getAllowedModes());
		assertNull(link.getAttributes().getAttribute(BicycleUtils.BICYCLE_INFRA));
	}

	// =========================================================================
	// helpers
	// =========================================================================

	/** A fresh bike link with the given id, mirroring what the reader produces. */
	private static Link link(String id) {
		Network net = NetworkUtils.createNetwork();
		Node from = NetworkUtils.createNode(Id.createNodeId(id + "_from"), CoordUtils.createCoord(0, 0));
		Node to = NetworkUtils.createNode(Id.createNodeId(id + "_to"), CoordUtils.createCoord(100, 0));
		Link link = NetworkUtils.createLink(Id.createLinkId(id), from, to, net, 100.0, 30 / 3.6, 1000.0, 1.0);
		link.setAllowedModes(Set.of(TransportMode.bike));
		return link;
	}

	private static Map<String, String> tags(String... kv) {
		Map<String, String> m = new HashMap<>();
		for (int i = 0; i < kv.length; i += 2) {
			m.put(kv[i], kv[i + 1]);
		}
		return m;
	}
}
