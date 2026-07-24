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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.contrib.bicycle.network.BicycleNetworkPipeline.Params;
import org.matsim.contrib.bicycle.network.LinkElevationProfile.ElevationSource;
import org.matsim.core.network.NetworkUtils;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for {@link BicycleNetworkPipeline#process}, the pure transformation seam
 * between reading the OSM file and writing the network. It performs no file I/O
 * and reads no CLI state, so the whole orchestration -- attribute prefixing,
 * origid normalization, isolated-component cleanup, bicycle-aware simplification,
 * service-link cleanup, mode rename and per-link elevation metrics -- runs on a
 * hand-built network with a synthetic {@link ElevationSource} in milliseconds.
 *
 * <p>The fixture mirrors what {@link org.matsim.contrib.osm.networkReader.OsmBicycleReader}
 * leaves behind: unprefixed OSM tag attributes ({@code surface}, {@code bicycle}),
 * {@code origid} stored as {@link Long}, a mergeable collinear chain, and a
 * service dead-end. Everything is bidirectional so it survives
 * {@link NetworkUtils#cleanNetwork} (which keeps only the strongly connected
 * bike component).
 *
 * <p>Node Z coordinates are deliberately <em>not</em> asserted: {@code process}
 * does not stamp them (that still happens in the reader callback), so a
 * hand-built network reaches the elevation step without node Z. The metrics are
 * sampled from the {@link ElevationSource} directly, which is what makes them
 * assertable here without a DEM.
 *
 * @author smetzler
 */
public class BicycleNetworkPipelineTest {

	private static final double EPS = 1e-9;

	/** The five elevation attribute keys every surviving link must carry. */
	private static final String[] ELEVATION_KEYS = {
		BicycleNetworkPipeline.LINK_ATTR_GRADIENT,
		BicycleNetworkPipeline.LINK_ATTR_MAX_GRADIENT,
		BicycleNetworkPipeline.LINK_ATTR_ELEVATION_GAIN,
		BicycleNetworkPipeline.LINK_ATTR_ELEVATION_LOSS,
		BicycleUtils.AVERAGE_ELEVATION
	};


	// =========================================================================
	// The whole orchestration on a reader-like network
	// =========================================================================

	@Test
	void processProducesAWellFormedBicycleNetwork() {
		// A bidirectional cycleway chain 1-2-3-4 on the x-axis (100 m segments),
		// plus a bidirectional service dead-end 4-5 branching off node 4.
		Network net = NetworkUtils.createNetwork();
		Node n1 = node(net, "1", 0, 0);
		Node n2 = node(net, "2", 100, 0);
		Node n3 = node(net, "3", 200, 0);
		Node n4 = node(net, "4", 300, 0);
		Node n5 = node(net, "5", 300, 100);

		// identical attributes on every chain link -> all mergeable
		link(net, n1, n2, "highway.cycleway", "CYCLEWAY_LINK", "asphalt", 100L);
		link(net, n2, n1, "highway.cycleway", "CYCLEWAY_LINK", "asphalt", 101L);
		link(net, n2, n3, "highway.cycleway", "CYCLEWAY_LINK", "asphalt", 102L);
		link(net, n3, n2, "highway.cycleway", "CYCLEWAY_LINK", "asphalt", 103L);
		link(net, n3, n4, "highway.cycleway", "CYCLEWAY_LINK", "asphalt", 104L);
		link(net, n4, n3, "highway.cycleway", "CYCLEWAY_LINK", "asphalt", 105L);

		// service dead-end: type differs, so it never merges into the chain
		link(net, n4, n5, "highway.service", "NONE", "paving_stones", 106L);
		link(net, n5, n4, "highway.service", "NONE", "paving_stones", 107L);

		// constant 2 % slope in +x direction
		ElevationSource elevation = c -> c.getX() * 0.02;

		BicycleNetworkPipeline.process(net, elevation, Params.defaults());

		// ---- service dead-end removed ---------------------------------------
		assertFalse(net.getNodes().containsKey(Id.createNodeId("5")),
			"service dead-end node should be removed");
		assertTrue(net.getLinks().values().stream()
				.noneMatch(l -> "highway.service".equals(l.getAttributes().getAttribute("type"))),
			"no service links should remain");

		// ---- mergeable chain collapsed to a single bidirectional edge -------
		assertEquals(2, net.getNodes().size(), "chain 1-2-3-4 collapses to just nodes 1 and 4");
		assertEquals(2, net.getLinks().size(), "one link per direction between node 1 and node 4");

		// ---- per-link invariants --------------------------------------------
		for (Link l : net.getLinks().values()) {
			// osm-prefixing: surface moved under "osm:", original key gone
			assertEquals("asphalt", l.getAttributes().getAttribute("osm:surface"),
				"surface should be moved under the osm: prefix");
			assertNull(l.getAttributes().getAttribute("surface"),
				"the unprefixed surface key should be gone");

			// origid normalized from Long to String (the ClassCastException regression)
			assertInstanceOf(String.class, l.getAttributes().getAttribute("origid"),
				"origid should be a String on every link");

			// every link carries all five elevation metrics
			for (String key : ELEVATION_KEYS) {
				assertNotNull(l.getAttributes().getAttribute(key),
					"missing elevation attribute '" + key + "'");
			}

			// mode untouched with the default --mode bike
			assertEquals(Set.of(TransportMode.bike), l.getAllowedModes());
		}

		// ---- gradient sign convention: +uphill, -downhill -------------------
		assertEquals(0.02, gradient(linkFrom(net, "1")), EPS,
			"uphill (node 1 -> node 4) should have a positive 2 % gradient");
		assertEquals(-0.02, gradient(linkFrom(net, "4")), EPS,
			"downhill (node 4 -> node 1) should have a negative 2 % gradient");
	}


	// =========================================================================
	// Mode rename leaves the osm:bicycle attribute alone
	// =========================================================================

	@Test
	void processRenamesAllowedModesButNotTheOsmBicycleAttribute() {
		Network net = NetworkUtils.createNetwork();
		Node a = node(net, "a", 0, 0);
		Node b = node(net, "b", 100, 0);
		Link ab = link(net, a, b, "highway.cycleway", "CYCLEWAY_LINK", "asphalt", 1L);
		Link ba = link(net, b, a, "highway.cycleway", "CYCLEWAY_LINK", "asphalt", 2L);
		// the OSM bicycle=* restriction value, which the reader stores as an attribute
		ab.getAttributes().putAttribute(BicycleOsmTags.BICYCLE, "yes");
		ba.getAttributes().putAttribute(BicycleOsmTags.BICYCLE, "yes");

		BicycleNetworkPipeline.process(net, c -> 0.0,
			new Params("bicycle", 10.0, 3.0, false));

		for (Link l : net.getLinks().values()) {
			assertEquals(Set.of("bicycle"), l.getAllowedModes(),
				"allowed mode should be renamed bike -> bicycle");
			assertEquals("yes", l.getAttributes().getAttribute("osm:bicycle"),
				"the osm:bicycle attribute must survive the mode rename");
			assertNull(l.getAttributes().getAttribute("bicycle"),
				"the unprefixed bicycle key should be gone");
		}
	}


	// =========================================================================
	// helpers
	// =========================================================================

	private static Node node(Network net, String id, double x, double y) {
		Node n = net.getFactory().createNode(Id.createNodeId(id), new Coord(x, y));
		net.addNode(n);
		return n;
	}

	/** A bike link mirroring what the reader writes: unprefixed tags, Long origid. */
	private static Link link(Network net, Node from, Node to, String type, String infra,
							 String surface, long origid) {
		double dx = to.getCoord().getX() - from.getCoord().getX();
		double dy = to.getCoord().getY() - from.getCoord().getY();
		Link l = NetworkUtils.createAndAddLink(net,
			Id.createLinkId(from.getId() + "->" + to.getId()),
			from, to, Math.hypot(dx, dy), 30 / 3.6, 1000.0, 1.0);
		l.setAllowedModes(Set.of(TransportMode.bike));
		l.getAttributes().putAttribute("type", type);
		l.getAttributes().putAttribute(BicycleOsmTags.SURFACE, surface);
		l.getAttributes().putAttribute(BicycleNetworkPipeline.LINK_ATTR_BICYCLE_INFRA, infra);
		l.getAttributes().putAttribute("origid", origid);   // Long, exactly as the reader writes it
		return l;
	}

	/** The (single, after collapse) link that starts at the given node id. */
	private static Link linkFrom(Network net, String fromNodeId) {
		return net.getLinks().values().stream()
			.filter(l -> l.getFromNode().getId().toString().equals(fromNodeId))
			.findFirst()
			.orElseThrow(() -> new AssertionError("no link starting at node " + fromNodeId));
	}

	private static double gradient(Link link) {
		return ((Number) link.getAttributes().getAttribute(BicycleNetworkPipeline.LINK_ATTR_GRADIENT))
			.doubleValue();
	}
}
