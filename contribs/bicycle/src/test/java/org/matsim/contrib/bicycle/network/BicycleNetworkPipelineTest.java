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

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
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
	// Tier 1 — the individual step methods in isolation
	// =========================================================================

	@Test
	void prefixOsmAttributes_movesOsmTagsAndLeavesOthers() {
		Network net = NetworkUtils.createNetwork();
		Link l = link(net, node(net, "a", 0, 0), node(net, "b", 100, 0),
			"highway.cycleway", "CYCLEWAY_LINK", "asphalt", 42L);
		l.getAttributes().putAttribute(BicycleOsmTags.BICYCLE, "yes");

		int moved = BicycleNetworkPipeline.prefixOsmAttributes(net);

		assertEquals(2, moved, "surface and bicycle are the two OSM tags present");
		assertEquals("asphalt", l.getAttributes().getAttribute("osm:surface"));
		assertEquals("yes", l.getAttributes().getAttribute("osm:bicycle"));
		assertNull(l.getAttributes().getAttribute("surface"));
		assertNull(l.getAttributes().getAttribute("bicycle"));
		// pipeline-internal attributes stay put
		assertEquals("highway.cycleway", l.getAttributes().getAttribute("type"));
		assertEquals(42L, l.getAttributes().getAttribute("origid"));
	}

	@Test
	void normalizeOrigIdType_convertsLongButLeavesStrings() {
		Network net = NetworkUtils.createNetwork();
		Node a = node(net, "a", 0, 0);
		Node b = node(net, "b", 100, 0);
		Node c = node(net, "c", 200, 0);
		Link longId = link(net, a, b, "highway.cycleway", "CYCLEWAY_LINK", "asphalt", 100L);
		Link stringId = link(net, b, c, "highway.cycleway", "CYCLEWAY_LINK", "asphalt", 200L);
		stringId.getAttributes().putAttribute("origid", "already-a-string");

		int converted = BicycleNetworkPipeline.normalizeOrigIdType(net);

		assertEquals(1, converted, "only the Long origid is converted");
		assertInstanceOf(String.class, longId.getAttributes().getAttribute("origid"));
		assertEquals("100", longId.getAttributes().getAttribute("origid"));
		assertEquals("already-a-string", stringId.getAttributes().getAttribute("origid"));
	}

	@Test
	void renameMode_renamesOnlyMatchingLinksAndCounts() {
		Network net = NetworkUtils.createNetwork();
		Node a = node(net, "a", 0, 0);
		Node b = node(net, "b", 100, 0);
		Link bike = link(net, a, b, "highway.cycleway", "CYCLEWAY_LINK", "asphalt", 1L);
		Link car = link(net, b, a, "highway.cycleway", "CYCLEWAY_LINK", "asphalt", 2L);
		car.setAllowedModes(Set.of("car"));

		int renamed = BicycleNetworkPipeline.renameMode(net, TransportMode.bike, "bicycle");

		assertEquals(1, renamed);
		assertEquals(Set.of("bicycle"), bike.getAllowedModes());
		assertEquals(Set.of("car"), car.getAllowedModes(), "non-bike links untouched");
	}

	@Test
	void renameMode_isNoOpWhenFromEqualsTo() {
		Network net = NetworkUtils.createNetwork();
		link(net, node(net, "a", 0, 0), node(net, "b", 100, 0),
			"highway.cycleway", "CYCLEWAY_LINK", "asphalt", 1L);

		assertEquals(0, BicycleNetworkPipeline.renameMode(net, TransportMode.bike, TransportMode.bike));
	}

	@Test
	void mergeOrigIds_dedupesAndPreservesOrder() {
		assertEquals("1-2", BicycleNetworkPipeline.mergeOrigIds("1", "2"));
		assertEquals("1-2-3", BicycleNetworkPipeline.mergeOrigIds("1-2", "2-3"), "shared id 2 kept once");
		assertEquals("1", BicycleNetworkPipeline.mergeOrigIds("1", "1"));
		assertEquals("1", BicycleNetworkPipeline.mergeOrigIds("1", null));
		assertEquals("2", BicycleNetworkPipeline.mergeOrigIds(null, "2"));
		assertNull(BicycleNetworkPipeline.mergeOrigIds(null, null));
	}


	// =========================================================================
	// Tier 1 — bicycle-aware simplification (where the recent bugfixes lived)
	// =========================================================================

	@Test
	void sameFreespeed_toleratesUlpButNotRealDifferences() {
		Network net = NetworkUtils.createNetwork();
		Node a = node(net, "a", 0, 0);
		Node b = node(net, "b", 100, 0);
		Node c = node(net, "c", 200, 0);
		Link l1 = simpLink(net, a, b, "CYCLEWAY_LINK", 8.0, 1000.0, 1.0);
		Link ulp = simpLink(net, b, c, "CYCLEWAY_LINK", 8.0 + 1e-12, 1000.0, 1.0);
		Link real = simpLink(net, a, c, "CYCLEWAY_LINK", 8.0 + 1.0, 1000.0, 1.0);

		assertTrue(BicycleNetworkPipeline.sameFreespeed(l1, ulp), "a 1e-12 difference counts as the same speed");
		assertFalse(BicycleNetworkPipeline.sameFreespeed(l1, real), "a whole m/s is a real difference");
	}

	@Test
	void baseCapacity_removesTheSub50mBoost() {
		Network net = NetworkUtils.createNetwork();
		Link shortLink = simpLink(net, node(net, "a", 0, 0), node(net, "b", 30, 0),
			"CYCLEWAY_LINK", 8.0, 1000.0, 1.0);
		Link longLink = simpLink(net, node(net, "c", 0, 100), node(net, "d", 100, 100),
			"CYCLEWAY_LINK", 8.0, 1000.0, 1.0);

		assertEquals(500.0, BicycleNetworkPipeline.baseCapacity(shortLink), 1e-9, "<50 m halves the boosted capacity");
		assertEquals(1000.0, BicycleNetworkPipeline.baseCapacity(longLink), 1e-9, ">=50 m keeps the capacity");
	}

	@Test
	void simplify_mergesTwoCollinearLinksWithIdenticalAttributes() {
		Network net = chain("CYCLEWAY_LINK", "CYCLEWAY_LINK");

		int removed = BicycleNetworkPipeline.simplifyUntilStable(net, false);

		assertEquals(1, removed);
		assertEquals(1, net.getLinks().size());
		Link merged = net.getLinks().values().iterator().next();
		assertEquals(200.0, merged.getLength(), 1e-9, "lengths are summed");
		assertEquals(Set.of(TransportMode.bike), merged.getAllowedModes(),
			"merged link keeps {bike}, not LinkImpl's default {car}");
	}

	@Test
	void simplify_doesNotMergeWhenBicycleInfraDiffers() {
		Network net = chain("CYCLEWAY_LINK", "SHARED_MOTOR_VEHICLE_LANE");

		assertEquals(0, BicycleNetworkPipeline.simplifyUntilStable(net, false));
		assertEquals(2, net.getLinks().size());
	}

	@Test
	void simplify_doesNotMergeWhenAllowedModesDiffer() {
		Network net = NetworkUtils.createNetwork();
		Node a = node(net, "a", 0, 0);
		Node b = node(net, "b", 100, 0);
		Node c = node(net, "c", 200, 0);
		simpLink(net, a, b, "CYCLEWAY_LINK", 8.0, 1000.0, 1.0);
		simpLink(net, b, c, "CYCLEWAY_LINK", 8.0, 1000.0, 1.0)
			.setAllowedModes(Set.of(TransportMode.bike, "car"));

		assertEquals(0, BicycleNetworkPipeline.simplifyUntilStable(net, false));
		assertEquals(2, net.getLinks().size());
	}

	@Test
	void simplify_doesNotMergeWhenLaneCountDiffers() {
		Network net = NetworkUtils.createNetwork();
		Node a = node(net, "a", 0, 0);
		Node b = node(net, "b", 100, 0);
		Node c = node(net, "c", 200, 0);
		simpLink(net, a, b, "CYCLEWAY_LINK", 8.0, 1000.0, 1.0);
		simpLink(net, b, c, "CYCLEWAY_LINK", 8.0, 1000.0, 2.0);

		assertEquals(0, BicycleNetworkPipeline.simplifyUntilStable(net, false));
		assertEquals(2, net.getLinks().size());
	}

	@Test
	void simplify_mergesDespiteUlpFreespeedDifference() {
		Network net = NetworkUtils.createNetwork();
		Node a = node(net, "a", 0, 0);
		Node b = node(net, "b", 100, 0);
		Node c = node(net, "c", 200, 0);
		simpLink(net, a, b, "CYCLEWAY_LINK", 8.0, 1000.0, 1.0);
		simpLink(net, b, c, "CYCLEWAY_LINK", 8.0 + 1e-12, 1000.0, 1.0);

		assertEquals(1, BicycleNetworkPipeline.simplifyUntilStable(net, false),
			"a 1e-12 freespeed difference must not block the merge");
		assertEquals(1, net.getLinks().size());
	}

	@Test
	void simplify_mergedLinkGetsUnboostedCapacity() {
		// two short (<50 m) links whose stored capacity carries the crossing boost;
		// the merged (>50 m) link must get the unboosted base capacity, not min(cap).
		Network net = NetworkUtils.createNetwork();
		Node a = node(net, "a", 0, 0);
		Node b = node(net, "b", 30, 0);
		Node c = node(net, "c", 60, 0);
		simpLink(net, a, b, "CYCLEWAY_LINK", 8.0, 1000.0, 1.0);
		simpLink(net, b, c, "CYCLEWAY_LINK", 8.0, 1000.0, 1.0);

		assertEquals(1, BicycleNetworkPipeline.simplifyUntilStable(net, false));
		Link merged = net.getLinks().values().iterator().next();
		assertEquals(60.0, merged.getLength(), 1e-9);
		assertEquals(500.0, merged.getCapacity(), 1e-9,
			"merged >50 m link keeps base capacity, not the sub-50 m boost");
	}

	@Test
	void simplify_collapsesAChainToASingleLink() {
		Network net = NetworkUtils.createNetwork();
		Node a = node(net, "a", 0, 0);
		Node b = node(net, "b", 100, 0);
		Node c = node(net, "c", 200, 0);
		Node d = node(net, "d", 300, 0);
		simpLink(net, a, b, "CYCLEWAY_LINK", 8.0, 1000.0, 1.0);
		simpLink(net, b, c, "CYCLEWAY_LINK", 8.0, 1000.0, 1.0);
		simpLink(net, c, d, "CYCLEWAY_LINK", 8.0, 1000.0, 1.0);

		int removed = BicycleNetworkPipeline.simplifyUntilStable(net, false);

		assertEquals(2, removed);
		assertEquals(1, net.getLinks().size());
		assertEquals(300.0, net.getLinks().values().iterator().next().getLength(), 1e-9);
	}


	// =========================================================================
	// Tier 1 — reversed-geometry repair
	// =========================================================================

	@Test
	void repairReversedGeometry_mirrorsBackwardsGeometry() {
		Network net = NetworkUtils.createNetwork();
		Node f = node(net, "F", 0, 0);
		Node t = node(net, "T", 100, 0);
		// listed backwards: first support point sits at the toNode, last at the fromNode
		Link l = linkWithGeom(net, f, t, "T,100,0 mid,50,0 F,0,0");

		int repaired = BicycleNetworkPipeline.repairReversedGeometry(net);

		assertEquals(1, repaired);
		assertEquals("F,0,0", firstGeomToken(l), "after mirroring, the fromNode point comes first");
	}

	@Test
	void repairReversedGeometry_leavesCorrectGeometryUntouched() {
		Network net = NetworkUtils.createNetwork();
		Node f = node(net, "F", 0, 0);
		Node t = node(net, "T", 100, 0);
		Link l = linkWithGeom(net, f, t, "F,0,0 mid,50,0 T,100,0");

		int repaired = BicycleNetworkPipeline.repairReversedGeometry(net);

		assertEquals(0, repaired);
		assertEquals("F,0,0", firstGeomToken(l), "already-correct geometry is left as is");
	}

	@Test
	void repairReversedGeometry_skipsMalformedTokensWithoutThrowing() {
		Network net = NetworkUtils.createNetwork();
		Node f = node(net, "F", 0, 0);
		Node t = node(net, "T", 100, 0);
		// first support point is non-numeric -> unparseable, must be skipped
		Link l = linkWithGeom(net, f, t, "F,east,north mid,50,0 T,100,0");

		int repaired = assertDoesNotThrow(() -> BicycleNetworkPipeline.repairReversedGeometry(net));

		assertEquals(0, repaired, "a malformed geometry is skipped, not repaired");
		assertEquals("F,east,north", firstGeomToken(l), "the malformed value is left untouched");
	}

	@Test
	void repairReversedGeometry_ignoresLinksWithoutStoredGeometry() {
		Network net = NetworkUtils.createNetwork();
		Node f = node(net, "F", 0, 0);
		Node t = node(net, "T", 100, 0);
		NetworkUtils.createAndAddLink(net, Id.createLinkId("F->T"), f, t, 100.0, 8.0, 1000.0, 1.0);

		assertEquals(0, BicycleNetworkPipeline.repairReversedGeometry(net));
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

	/** A one-way bike link with controllable link stats; length = |dx| on the x-axis. */
	private static Link simpLink(Network net, Node from, Node to, String infra,
								 double freespeed, double capacity, double lanes) {
		double length = Math.abs(to.getCoord().getX() - from.getCoord().getX());
		Link l = NetworkUtils.createAndAddLink(net,
			Id.createLinkId(from.getId() + "->" + to.getId()),
			from, to, length, freespeed, capacity, lanes);
		l.setAllowedModes(Set.of(TransportMode.bike));
		l.getAttributes().putAttribute("type", "highway.cycleway");
		l.getAttributes().putAttribute(BicycleNetworkPipeline.LINK_ATTR_BICYCLE_INFRA, infra);
		return l;
	}

	/** A one-way two-link chain a->b->c (100 m segments) with the given infra values. */
	private static Network chain(String infraAB, String infraBC) {
		Network net = NetworkUtils.createNetwork();
		Node a = node(net, "a", 0, 0);
		Node b = node(net, "b", 100, 0);
		Node c = node(net, "c", 200, 0);
		simpLink(net, a, b, infraAB, 8.0, 1000.0, 1.0);
		simpLink(net, b, c, infraBC, 8.0, 1000.0, 1.0);
		return net;
	}

	/** A link carrying a stored geometry ({@code "id,x,y id,x,y ..."}) in the origgeom attribute. */
	private static Link linkWithGeom(Network net, Node from, Node to, String geom) {
		Link l = NetworkUtils.createAndAddLink(net, Id.createLinkId(from.getId() + "->" + to.getId()),
			from, to, 100.0, 8.0, 1000.0, 1.0);
		l.getAttributes().putAttribute(NetworkUtils.ORIG_GEOM, geom);
		return l;
	}

	private static String firstGeomToken(Link link) {
		return link.getAttributes().getAttribute(NetworkUtils.ORIG_GEOM).toString().trim().split("\\s+")[0];
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
