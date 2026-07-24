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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.osm.networkReader.SupersonicOsmNetworkReader.Direction;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.CoordUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
		new BicycleLinkPolicy(new BicycleInfraClassifier(), new TagCopy(List.of(), "osm:"));

	// =========================================================================
	// NPE regression: raw OSM tags rarely carry oneway / oneway:bicycle
	// =========================================================================

	@Test
	void cyclewayWithoutOnewayTags_doesNotThrow() {
		Link forward = link("1f");
		Link reverse = link("1r");
		Map<String, String> tags = tags("highway", "cycleway");

		// Neither direction may throw, and both keep bike (no oneway restriction).
		assertDoesNotThrow(() -> policy.apply(forward, tags, Direction.Forward));
		assertDoesNotThrow(() -> policy.apply(reverse, tags, Direction.Reverse));
		assertTrue(forward.getAllowedModes().contains(TransportMode.bike));
		assertTrue(reverse.getAllowedModes().contains(TransportMode.bike));

		// bicycle_infra is always written.
		assertNotNull(forward.getAttributes().getAttribute(BicycleNetworkPipeline.LINK_ATTR_BICYCLE_INFRA));
	}

	// =========================================================================
	// oneway:bicycle=yes -> bikes only in forward direction
	// =========================================================================

	@Test
	void onewayBicycleYes_forwardKeepsBike_reverseLosesBike_bikeReverseKilled() {
		Map<String, String> tags = tags("highway", "cycleway", "oneway:bicycle", "yes");

		Link forward = link("1f");
		Link reverse = link("1r");
		Link bikeReverse = link("1_bike-reverse");

		policy.apply(forward, tags, Direction.Forward);
		policy.apply(reverse, tags, Direction.Reverse);
		policy.apply(bikeReverse, tags, Direction.Reverse);

		assertTrue(forward.getAllowedModes().contains(TransportMode.bike), "forward keeps bike");
		assertFalse(reverse.getAllowedModes().contains(TransportMode.bike), "'r' reverse loses bike");
		assertTrue(bikeReverse.getAllowedModes().isEmpty(), "'_bike-reverse' is killed");
	}

	// =========================================================================
	// oneway=yes (car oneway) with / without an explicit bicycle exception
	// =========================================================================

	@Test
	void onewayYes_reverseLosesBike() {
		Map<String, String> tags = tags("highway", "cycleway", "oneway", "yes");
		Link reverse = link("1r");

		policy.apply(reverse, tags, Direction.Reverse);

		assertFalse(reverse.getAllowedModes().contains(TransportMode.bike));
	}

	@Test
	void onewayYesButBicycleNo_reverseKeepsBike() {
		// oneway=yes for cars, but oneway:bicycle=no -> bikes allowed both ways.
		Map<String, String> tags = tags("highway", "cycleway", "oneway", "yes", "oneway:bicycle", "no");
		Link reverse = link("1r");

		policy.apply(reverse, tags, Direction.Reverse);

		assertTrue(reverse.getAllowedModes().contains(TransportMode.bike));
	}

	// =========================================================================
	// footway / pedestrian whitelist + bicycle=no kill
	// =========================================================================

	@Test
	void footwayWithoutBicycle_isKilled() {
		Link link = link("1f");
		policy.apply(link, tags("highway", "footway"), Direction.Forward);
		assertTrue(link.getAllowedModes().isEmpty());
	}

	@Test
	void footwayWithBicycleYes_keepsBike() {
		Link link = link("1f");
		policy.apply(link, tags("highway", "footway", "bicycle", "yes"), Direction.Forward);
		assertTrue(link.getAllowedModes().contains(TransportMode.bike));
	}

	@Test
	void bicycleNo_isKilled() {
		Link link = link("1f");
		policy.apply(link, tags("highway", "cycleway", "bicycle", "no"), Direction.Forward);
		assertTrue(link.getAllowedModes().isEmpty());
	}

	// =========================================================================
	// access=no/private/customer kill
	// =========================================================================

	@Test
	void accessNo_isKilled() {
		Link link = link("1f");
		policy.apply(link, tags("highway", "service", "access", "no"), Direction.Forward);
		assertTrue(link.getAllowedModes().isEmpty());
	}

	@Test
	void accessPrivate_isKilled() {
		Link link = link("1f");
		policy.apply(link, tags("highway", "service", "access", "private"), Direction.Forward);
		assertTrue(link.getAllowedModes().isEmpty());
	}

	@Test
	void accessCustomer_isKilled() {
		Link link = link("1f");
		policy.apply(link, tags("highway", "service", "access", "customer"), Direction.Forward);
		assertTrue(link.getAllowedModes().isEmpty());
	}

	@Test
	void accessYes_keepsBike() {
		// access=yes (and other unrestricted values) must not kill the link.
		Link link = link("1f");
		policy.apply(link, tags("highway", "service", "access", "yes"), Direction.Forward);
		assertTrue(link.getAllowedModes().contains(TransportMode.bike));
	}

	@Test
	void accessPrivateButBicycleYes_keepsBike() {
		// OSM: the bicycle-specific tag overrides the general access restriction.
		Link link = link("1f");
		policy.apply(link, tags("highway", "service", "access", "private", "bicycle", "yes"), Direction.Forward);
		assertTrue(link.getAllowedModes().contains(TransportMode.bike));
	}

	@Test
	void accessNoButBicycleDesignated_keepsBike() {
		Link link = link("1f");
		policy.apply(link, tags("highway", "service", "access", "no", "bicycle", "designated"), Direction.Forward);
		assertTrue(link.getAllowedModes().contains(TransportMode.bike));
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
