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
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.bicycle.BicycleUtils;
import org.matsim.contrib.sumo.SumoNetworkConverter;
import org.matsim.contrib.sumo.SumoNetworkHandler;
import org.matsim.core.network.NetworkUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Drives {@link SumoBicycleAttributes#process} against a netconvert-generated fixture.
 *
 * <p>The fixture is a closed ring of nine OSM ways (see {@code ring.osm}); the ring shape
 * matters because two of them get dropped and a chain would fall apart. Ways 1001 and
 * 2001 have identical SUMO attributes, so netconvert merged them into one edge — which is
 * what makes the multi-way cases testable at all.
 */
public class SumoBicycleAttributesTest {

	@RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils();

	/** Constant-slope elevation along x, so gradients are predictable without a DEM. */
	private static final LinkElevationProfile.ElevationSource SLOPE = c -> c.getX() * 0.01;

	private record Fixture(Network network, SumoNetworkHandler sumo, OsmWayTags tags) {
	}

	private Fixture read() throws Exception {
		Network network = NetworkUtils.createNetwork();
		SumoNetworkConverter converter = SumoNetworkConverter.newInstance(
				List.of(input("ring.net.xml")), Path.of(utils.getOutputDirectory(), "net.xml"),
				"EPSG:25832", "EPSG:25832")
			// the ring contains a footway; without this the converter would skip it
			.setKeepCyclableMinorWays(true);
		SumoNetworkHandler sumo = converter.convert(network);
		return new Fixture(network, sumo, OsmWayTags.read(input("ring.osm")));
	}

	private Path input(String file) {
		return Path.of(utils.getClassInputDirectory(), file);
	}

	private static SumoBicycleAttributes.Stats run(Fixture f,
												   LinkElevationProfile.ElevationSource elevation) {
		return SumoBicycleAttributes.process(f.network(), f.sumo(), f.tags(), elevation,
			SumoBicycleAttributes.Params.defaults());
	}

	private static Link link(Network network, String id) {
		return network.getLinks().get(Id.createLinkId(id));
	}

	private static String infra(Network network, String id) {
		return (String) link(network, id).getAttributes().getAttribute(BicycleUtils.BICYCLE_INFRA);
	}

	// ------------------------------------------------------------------------

	@Test
	void classifiesSingleWayLinksFromTheirOsmTags() throws Exception {

		Fixture f = read();
		run(f, null);

		// footway + bicycle=yes + foot=designated + segregated=no, in both directions
		assertEquals("FOOT_AND_CYCLEWAY_SHARED_ADJOINING_OR_ISOLATED", infra(f.network(), "8001"));
		assertEquals("FOOT_AND_CYCLEWAY_SHARED_ADJOINING_OR_ISOLATED", infra(f.network(), "-8001"));

		// living_street and primary carry no cycling infrastructure
		assertEquals("NONE", infra(f.network(), "9001"));
		assertEquals("NONE", infra(f.network(), "7001"));
	}

	@Test
	void marksMergedLinksWhoseWaysDisagree() throws Exception {

		Fixture f = read();
		SumoBicycleAttributes.Stats stats = run(f, null);

		// netconvert merged way 1001 (cycleway=lane) with way 2001 (no cycleway) into one
		// edge per direction. They classify differently, so the category must not pick one.
		assertEquals(2, stats.mixedMultiWay, "both directions of the merged edge");
		assertEquals("NEEDS_CLARIFICATION", infra(f.network(), "1001"));
		assertEquals("NEEDS_CLARIFICATION", infra(f.network(), "-2001"));

		// and the cause is recorded, because the classifier also emits NEEDS_CLARIFICATION
		// on its own - a bare highway=cycleway does, see link 5001 below
		assertEquals(Boolean.TRUE,
			link(f.network(), "1001").getAttributes().getAttribute(BicycleUtils.BICYCLE_INFRA_MIXED));
		assertEquals("NEEDS_CLARIFICATION", infra(f.network(), "5001"));
		assertNull(link(f.network(), "5001").getAttributes().getAttribute(BicycleUtils.BICYCLE_INFRA_MIXED),
			"a single-way link is never a merge artifact");
	}

	@Test
	void doesNotAdoptTagsTheMergedWaysDisagreeOn() throws Exception {

		Fixture f = read();
		SumoBicycleAttributes.Stats stats = run(f, null);

		Link merged = link(f.network(), "1001");

		// Both ways are surface=asphalt, so that one is safe to copy.
		assertEquals("asphalt", merged.getAttributes().getAttribute(BicycleUtils.OSM_PREFIX + "surface"));

		// cycleway=lane is on way 1001 only. netconvert wrote it onto the merged edge as a
		// param anyway - reading that param would claim a bike lane over twice the length.
		assertNull(merged.getAttributes().getAttribute(BicycleUtils.OSM_PREFIX + "cycleway"),
			"a tag only one of the merged ways carries must not be adopted");
		assertEquals("lane", f.sumo().getEdges().get("1001").getAttributes().get("cycleway"),
			"the SUMO edge does carry it - which is precisely why we do not read it from there");

		assertEquals(2, stats.tagsDroppedAsAmbiguous, "one per direction of the merged edge");
	}

	@Test
	void dropsWhatNetconvertLeavesRideable() throws Exception {

		Fixture f = read();
		SumoBicycleAttributes.Stats stats = run(f, null);

		// netconvert keeps both fully routable; cleanNetwork prunes them once emptied
		assertEquals(2, stats.droppedParkingAisle);
		assertEquals(2, stats.droppedRestrictedAccess);

		assertNull(link(f.network(), "4001"), "service=parking_aisle must be gone");
		assertNull(link(f.network(), "-4001"));
		assertNull(link(f.network(), "3001"), "access=private must be gone");
		assertNull(link(f.network(), "-3001"));

		// the footway in this fixture carries bicycle=yes, so the whitelist keeps it
		assertEquals(0, stats.droppedFootwayWithoutBike);
		assertNotNull(link(f.network(), "8001"));
	}

	@Test
	void keepsTheContraflowLinkAsBikeOnly() throws Exception {

		Fixture f = read();
		run(f, null);

		// way 6001 is oneway=yes + oneway:bicycle=no, so netconvert adds a reverse edge
		// that only allows bicycle. It has to survive and be classified like any other.
		Link contraflow = link(f.network(), "-6001");
		assertNotNull(contraflow, "the contraflow edge must survive");
		assertEquals(List.of(TransportMode.bike), List.copyOf(contraflow.getAllowedModes()));
		assertNotNull(infra(f.network(), "-6001"), "it must be classified, not skipped");

		// its direction comes from the leading minus, same as any other reverse link
		assertEquals(OsmWayDirection.REVERSE, SumoBicycleAttributes.directionOf(contraflow));
		assertEquals(OsmWayDirection.FORWARD, SumoBicycleAttributes.directionOf(link(f.network(), "6001")));
	}

	@Test
	void infersAsphaltOnBiggerRoadsWithoutSurfaceTag() throws Exception {

		Fixture f = read();
		run(f, null);

		// way 7001 is highway=primary with no surface tag; BicycleUtils.getSurface would
		// otherwise read nothing and the scoring would silently fall back to a comfort of 1
		assertEquals("asphalt",
			link(f.network(), "7001").getAttributes().getAttribute(BicycleUtils.OSM_PREFIX + "surface"));

		// living_street is not on the paved-by-default list, and tagged no surface either
		assertNull(link(f.network(), "9001").getAttributes().getAttribute(BicycleUtils.OSM_PREFIX + "surface"));
	}

	@Test
	void attachesElevationAlongTheSumoPolyline() throws Exception {

		Fixture f = read();
		SumoBicycleAttributes.Stats stats = run(f, SLOPE);

		assertTrue(stats.withElevation > 0);
		// the merged edge kept the node between the two ways as a shape point
		assertEquals(2, stats.linksWithTrueShape, "both directions of the merged edge");

		Link l = link(f.network(), "8001");
		assertNotNull(l.getAttributes().getAttribute(BicycleUtils.GRADIENT));
		assertNotNull(l.getAttributes().getAttribute(BicycleUtils.MAX_GRADIENT));
		assertNotNull(l.getAttributes().getAttribute(BicycleUtils.ELEVATION_GAIN));
		assertNotNull(l.getAttributes().getAttribute(BicycleUtils.ELEVATION_LOSS));

		// gradients are signed in the direction of travel
		double forward = (double) l.getAttributes().getAttribute(BicycleUtils.GRADIENT);
		double reverse = (double) link(f.network(), "-8001").getAttributes()
			.getAttribute(BicycleUtils.GRADIENT);
		assertEquals(forward, -reverse, 1e-9);

		// nodes get a Z so the simulation's own gradient calculation agrees
		assertTrue(f.network().getNodes().values().stream().allMatch(n -> n.getCoord().hasZ()));
	}

	@Test
	void runsWithoutAnElevationSource() throws Exception {

		Fixture f = read();
		SumoBicycleAttributes.Stats stats = run(f, null);

		assertEquals(0, stats.withElevation);
		assertTrue(f.network().getLinks().values().stream().allMatch(
			l -> l.getAttributes().getAttribute(BicycleUtils.GRADIENT) == null));
		assertFalse(f.network().getNodes().values().stream().anyMatch(n -> n.getCoord().hasZ()));
	}

	@Test
	void isIdempotent() throws Exception {

		Fixture f = read();
		run(f, SLOPE);

		String before = describe(f.network());
		SumoBicycleAttributes.Stats second = run(f, SLOPE);

		assertEquals(before, describe(f.network()), "a second run must not change anything");
		assertEquals(0, second.linksWithoutEdge);
	}

	@Test
	void refusesANetworkWithoutBikeLinks() throws Exception {

		Fixture f = read();
		f.network().getLinks().values().forEach(l -> l.setAllowedModes(java.util.Set.of(TransportMode.car)));

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> run(f, null));
		assertTrue(e.getMessage().contains("clean-network"),
			"the message should point at the likely cause");
	}

	// ------------------------------------------------------------------------
	// Companion files
	// ------------------------------------------------------------------------

	@Test
	void derivesCompanionNamesLikeNetworkFromSumo() {
		Path net = Path.of("in", "dresden.xml.gz");

		// the companions inherit the network's compression, exactly like network-from-sumo:
		// its .replace(".xml", suffix) leaves a trailing .gz in place
		assertEquals(Path.of("in", "dresden-linkGeometries.csv.gz"),
			SumoBicycleAttributes.companion(net, "-linkGeometries.csv"));
		assertEquals(Path.of("in", "dresden-ft.csv.gz"),
			SumoBicycleAttributes.companion(net, "-ft.csv"));

		// an uncompressed network gets uncompressed companions
		assertEquals(Path.of("in", "dresden-linkGeometries.csv"),
			SumoBicycleAttributes.companion(Path.of("in", "dresden.xml"), "-linkGeometries.csv"));
		assertEquals(Path.of("in", "dresden-ft.csv"),
			SumoBicycleAttributes.companion(Path.of("in", "dresden.xml"), "-ft.csv"));
	}

	/**
	 * An ".xml" further up the path must not be swapped — the suffix replacement applies
	 * to the file name, not to the whole path.
	 */
	@Test
	void leavesTheDirectoryAlone() {
		assertEquals(Path.of("nets.xml.d", "net-linkGeometries.csv.gz"),
			SumoBicycleAttributes.companion(Path.of("nets.xml.d", "net.xml.gz"), "-linkGeometries.csv"));
	}

	@Test
	void writesGeometriesForExactlyTheSurvivingLinks() throws Exception {

		Fixture f = read();
		// uncompressed output network, so the derived companion is plain CSV and readable below
		Path out = Path.of(utils.getOutputDirectory(), "annotated.xml");

		SumoBicycleAttributes.process(f.network(), f.sumo(), f.tags(), null,
			SumoBicycleAttributes.Params.defaults());
		SumoBicycleAttributes.writeGeometries(f.network(), f.sumo(),
			SumoBicycleAttributes.companion(out, "-linkGeometries.csv"));

		Path csv = out.resolveSibling("annotated-linkGeometries.csv");
		assertTrue(Files.exists(csv), "the geometry file must sit next to the network");

		List<String> lines = Files.readAllLines(csv);
		assertEquals("LinkId,Geometry", lines.get(0), "same header as network-from-sumo writes");
		assertEquals(f.network().getLinks().size(), lines.size() - 1,
			"one row per surviving link, no rows for the dropped ones");

		// the merged link keeps the geometry node between its two ways
		String merged = lines.stream().filter(l -> l.startsWith("1001,")).findFirst().orElseThrow();
		assertEquals(3, merged.split("\\),\\(").length, "two ways joined at one interior point");
	}

	// ------------------------------------------------------------------------
	// Motorised modes
	// ------------------------------------------------------------------------

	/**
	 * The motorised modes describe the same vehicles on the same roads, so a scenario
	 * wants them on one link set. SUMO derives truck separately and the cleaners only
	 * ever take car away, so without this they drift apart — which downstream is a
	 * routing failure for whichever mode kept a link car no longer has.
	 */
	@Test
	void givesTheMirroredModesExactlyTheCarLinks() {

		Network net = NetworkUtils.createNetwork();
		Node a = addNode(net, "a", 0, 0);
		Node b = addNode(net, "b", 100, 0);
		Node c = addNode(net, "c", 200, 0);

		// the four shapes this has to get right: car without the others, the others without
		// car but with something left, a bike-only link that must stay untouched, and a
		// link whose ONLY modes are mirrored ones - it must go, not linger as modes=""
		Link road = addLink(net, "road", a, b, Set.of(TransportMode.car));
		Link stale = addLink(net, "stale", b, a, Set.of(TransportMode.ride, TransportMode.bike));
		Link path = addLink(net, "path", a, b, Set.of(TransportMode.bike));
		addLink(net, "depot", b, c, Set.of(TransportMode.truck, "freight"));

		int changed = SumoBicycleAttributes.mirrorCarModes(net,
			Set.of(TransportMode.ride, TransportMode.truck, "freight"));

		assertEquals(3, changed, "road gains three modes, stale loses one, depot loses its last two");
		assertEquals(Set.of(TransportMode.car, TransportMode.ride, TransportMode.truck, "freight"),
			Set.copyOf(road.getAllowedModes()));
		assertEquals(Set.of(TransportMode.bike), Set.copyOf(stale.getAllowedModes()),
			"a motorised mode without car has nothing to be mirrored from");
		assertEquals(Set.of(TransportMode.bike), Set.copyOf(path.getAllowedModes()),
			"non-motorised modes are none of this method's business");

		assertNull(net.getLinks().get(Id.createLinkId("depot")),
			"a link whose last mode was mirrored away must not survive as a dead link");
		assertNull(net.getNodes().get(Id.createNodeId("c")),
			"its orphaned node goes with it");
	}

	private static Node addNode(Network net, String id, double x, double y) {
		Node n = net.getFactory().createNode(Id.createNodeId(id), new Coord(x, y));
		net.addNode(n);
		return n;
	}

	private static Link addLink(Network net, String id, Node from, Node to, Set<String> modes) {
		Link l = net.getFactory().createLink(Id.createLinkId(id), from, to);
		l.setAllowedModes(modes);
		net.addLink(l);
		return l;
	}

	/** Without the option the modes are left exactly as they came out of the converter. */
	@Test
	void leavesTheMotorisedModesAloneByDefault() throws Exception {

		Fixture f = read();
		run(f, null);

		assertTrue(f.network().getLinks().values().stream().noneMatch(l -> l.getAllowedModes().contains("freight")),
			"nothing may invent a freight mode on its own");
	}

	/**
	 * The merge can leave a stub hanging off nothing where a chain's interior node was
	 * the only tie to the rest of the network; the cleanup after it has to catch that,
	 * or agents end up stuck on an unreachable link.
	 */
	@Test
	void leavesNoStrandedLinksAfterTheMerge() throws Exception {

		Fixture f = read();
		SumoBicycleAttributes.process(f.network(), f.sumo(), f.tags(), null,
			SumoBicycleAttributes.Params.defaults().withSimplify());

		int linksBefore = f.network().getLinks().size();
		NetworkUtils.cleanNetwork(f.network(), Set.of(TransportMode.car, TransportMode.bike));

		assertEquals(linksBefore, f.network().getLinks().size(),
			"a second cleaning pass must find nothing left to remove");
	}

	// ------------------------------------------------------------------------
	// OSM tag stamping
	// ------------------------------------------------------------------------

	/**
	 * The classifier consults ~39 tag keys, but its verdict is on the link as
	 * {@code bicycle_infra} — carrying the inputs along too would be dead weight on
	 * every link of a city-sized network. By default only what something still reads
	 * survives, which is also exactly what the Supersonic path writes.
	 */
	@Test
	void keepsOnlyTheConsumedOsmTagsByDefault() throws Exception {

		Fixture f = read();
		run(f, null);

		Set<String> stamped = stampedOsmKeys(f.network());

		Set<String> unexpected = new TreeSet<>(stamped);
		unexpected.removeAll(BicycleOsmTags.KEPT_ON_LINKS);
		assertEquals(Set.of(), unexpected, "no tag outside the kept set may reach the links");
		assertTrue(stamped.contains(BicycleOsmTags.SURFACE),
			"the fixture tags surface, so stamping must still happen at all");
	}

	/** The escape hatch: everything the classifier saw, for working out why it decided as it did. */
	@Test
	void keepsEveryClassificationTagWithOsmTagsAll() throws Exception {

		Fixture withAll = read();
		SumoBicycleAttributes.process(withAll.network(), withAll.sumo(), withAll.tags(), null,
			SumoBicycleAttributes.Params.defaults()
				.withOsmTags(SumoBicycleAttributes.OsmTags.ALL));
		Set<String> stamped = stampedOsmKeys(withAll.network());

		assertTrue(stamped.contains(BicycleOsmTags.HIGHWAY),
			"highway is the classifier's main input and must be visible again");

		Fixture withMinimal = read();
		run(withMinimal, null);
		assertTrue(stamped.containsAll(stampedOsmKeys(withMinimal.network())),
			"ALL is a superset of the default, never a different set");
	}

	/** The unprefixed OSM keys stamped onto any link of the network. */
	private static Set<String> stampedOsmKeys(Network network) {
		return network.getLinks().values().stream()
			.flatMap(l -> l.getAttributes().getAsMap().keySet().stream())
			.filter(k -> k.startsWith(BicycleUtils.OSM_PREFIX))
			.map(k -> k.substring(BicycleUtils.OSM_PREFIX.length()))
			.collect(Collectors.toSet());
	}

	// ------------------------------------------------------------------------
	// Bicycle area
	// ------------------------------------------------------------------------

	/**
	 * With a marker configured, every link that reached the gate records which side it
	 * fell on, so downstream can filter on the area itself rather than inferring it from
	 * a missing category - which would also catch links left unclassified for other
	 * reasons. Without a marker the attribute stays absent: the whole network had the
	 * full treatment, and claiming an area would be a lie.
	 */
	@Test
	void recordsWhichSideOfTheBicycleAreaALinkIsOn() throws Exception {

		Fixture f = read();
		// way 8001 carries highway=footway; use that as the marker so the ring splits
		SumoBicycleAttributes.Params gated = new SumoBicycleAttributes.Params(
			"de", TransportMode.bike, BicycleLinkPolicy.AreaMarker.parse("highway=footway"),
			20.0, 3.0, false, Set.of(), SumoBicycleAttributes.OsmTags.MINIMAL, Set.of());
		SumoBicycleAttributes.Stats stats = SumoBicycleAttributes.process(
			f.network(), f.sumo(), f.tags(), null, gated);

		assertTrue(stats.outsideArea > 0, "the ring has ways outside a footway-only area");
		assertEquals(Boolean.TRUE, BicycleUtils.getBicycleArea(link(f.network(), "8001")),
			"the footway is inside");
		assertEquals(Boolean.FALSE, BicycleUtils.getBicycleArea(link(f.network(), "9001")),
			"the living_street is outside, and says so");
		assertNotNull(link(f.network(), "9001").getAllowedModes(),
			"outside links keep their modes");

		Fixture plain = read();
		run(plain, null);
		assertNull(BicycleUtils.getBicycleArea(link(plain.network(), "9001")),
			"no marker configured -> no area attribute at all");
	}

	// ------------------------------------------------------------------------
	// --drop-ways-without-infra
	// ------------------------------------------------------------------------

	/**
	 * The ring's way 9001 is a living_street and 8001 a footway with bicycle=yes; neither
	 * is a minor way type here, so listing them must change nothing. Way 5001 is a bare
	 * highway=cycleway (classifies as NEEDS_CLARIFICATION, not NONE) and proves the
	 * classification guard: even when its type is listed, a classified link survives.
	 */
	@Test
	void dropWaysWithoutInfra_sparesClassifiedAndUnlistedTypes() throws Exception {

		Fixture f = read();
		SumoBicycleAttributes.Params p = SumoBicycleAttributes.Params.defaults()
			.withDropWaysWithoutInfra(java.util.Set.of("cycleway", "track", "path"));
		SumoBicycleAttributes.Stats stats = SumoBicycleAttributes.process(
			f.network(), f.sumo(), f.tags(), null, p);

		assertEquals(0, stats.droppedMinorWayWithoutInfra,
			"the ring has no unclassified minor way, so nothing may be dropped");
		assertNotNull(link(f.network(), "5001"), "a classified cycleway survives its type being listed");
		assertNotNull(link(f.network(), "9001"), "living_street is not listed at all");
	}

	@Test
	void dropWaysWithoutInfra_isOffByDefault() throws Exception {

		Fixture f = read();
		SumoBicycleAttributes.Stats stats = run(f, null);
		assertEquals(0, stats.droppedMinorWayWithoutInfra);
		assertTrue(SumoBicycleAttributes.Params.defaults().dropWaysWithoutInfra().isEmpty());
	}

	// ------------------------------------------------------------------------
	// --simplify
	// ------------------------------------------------------------------------

	/**
	 * Two links per direction over a pass-through node, agreeing on every merge
	 * criterion. The ring fixture has no such pair — every way carries a different
	 * highway type by design — so the merge mechanics get a synthetic chain; the
	 * fixture still supplies the {@link SumoNetworkHandler} (whose edge ids simply
	 * never match, exercising the chord fallback for the constituents).
	 */
	private static Network mergeableChain() {
		Network net = NetworkUtils.createNetwork();
		Node n0 = net.getFactory().createNode(Id.createNodeId("n0"), new Coord(0, 0));
		Node n1 = net.getFactory().createNode(Id.createNodeId("n1"), new Coord(100, 0));
		Node n2 = net.getFactory().createNode(Id.createNodeId("n2"), new Coord(200, 0));
		net.addNode(n0);
		net.addNode(n1);
		net.addNode(n2);
		chainLink(net, "e1", n0, n1);
		chainLink(net, "e2", n1, n2);
		chainLink(net, "r2", n2, n1);
		chainLink(net, "r1", n1, n0);
		return net;
	}

	private static void chainLink(Network net, String id, Node from, Node to) {
		Link l = net.getFactory().createLink(Id.createLinkId(id), from, to);
		l.setLength(100);
		l.setFreespeed(13.89);
		l.setCapacity(600);
		l.setNumberOfLanes(1);
		l.setAllowedModes(java.util.Set.of(TransportMode.car, TransportMode.bike));
		l.getAttributes().putAttribute(BicycleUtils.BICYCLE_INFRA, "NONE");
		l.getAttributes().putAttribute(BicycleUtils.BICYCLE_AREA, true);
		l.getAttributes().putAttribute(NetworkUtils.TYPE, "highway.residential");
		l.getAttributes().putAttribute(BicycleUtils.OSM_PREFIX + "surface", "asphalt");
		l.getAttributes().putAttribute(NetworkUtils.ALLOWED_SPEED, 13.89);
		l.getAttributes().putAttribute("name", "Teststrasse");
		l.getAttributes().putAttribute("restricted_lanes", 1);
		net.addLink(l);
	}

	@Test
	void simplifyMergesAndCarriesShapesAndProvenance() throws Exception {

		Fixture f = read();
		Network net = mergeableChain();
		double kmBefore = net.getLinks().values().stream().mapToDouble(Link::getLength).sum();

		SumoBicycleAttributes.MergeCarry carry = new SumoBicycleAttributes.MergeCarry();
		int removed = SumoBicycleAttributes.simplifyAndTrack(net, f.sumo(), carry);

		assertEquals(2, removed, "one merge per direction");
		Link merged = link(net, "e1-e2");
		assertNotNull(merged, "the merged forward link");
		assertEquals(kmBefore,
			net.getLinks().values().stream().mapToDouble(Link::getLength).sum(), 1e-9,
			"merging must preserve the kilometres");
		assertEquals(600, merged.getCapacity(), 1e-9, "constituent capacity, neither halved nor doubled");

		// the vanished middle node survives as a support point of the carried shape...
		List<Coord> shape = carry.shapes.get(merged.getId());
		assertNotNull(shape, "the merge must carry a shape for the new link");
		assertEquals(3, shape.size());
		assertEquals(new Coord(100, 0), shape.get(1));

		// ...but not as an orphan node in the network
		assertNull(net.getNodes().get(Id.createNodeId("n1")),
			"the merged-through node must not linger as an orphan");

		// the feature row is inherited from the downstream constituent
		assertEquals(Id.createLinkId("e2"), carry.featureSource.get(merged.getId()));

		// equal-valued non-key attributes survive the merge
		assertEquals("Teststrasse", merged.getAttributes().getAttribute("name"));
		assertEquals(1, merged.getAttributes().getAttribute("restricted_lanes"));

		// the area flag is a match key, so it is re-imposed rather than lost
		assertEquals(Boolean.TRUE, BicycleUtils.getBicycleArea(merged));
	}

	@Test
	void simplifyFeedsElevationAndGeometryForMergedLinks() throws Exception {

		Fixture f = read();
		Network net = mergeableChain();
		SumoBicycleAttributes.MergeCarry carry = new SumoBicycleAttributes.MergeCarry();
		SumoBicycleAttributes.simplifyAndTrack(net, f.sumo(), carry);

		SumoBicycleAttributes.Stats stats = new SumoBicycleAttributes.Stats();
		SumoBicycleAttributes.attachElevation(net, f.sumo(), carry.shapes, SLOPE,
			SumoBicycleAttributes.Params.defaults(), stats);

		// SLOPE rises 0.01 per metre of x, so the 200 m merged link climbs 2 m
		Link merged = link(net, "e1-e2");
		assertNotNull(merged.getAttributes().getAttribute(BicycleUtils.GRADIENT),
			"a merged link must not lose its gradient");
		assertEquals(0.01, (double) merged.getAttributes().getAttribute(BicycleUtils.GRADIENT), 1e-3);
		assertTrue(stats.linksWithTrueShape >= 2, "merged links sample along the carried polyline");

		// and the geometry companion writes the concatenated course under the merged id
		Path out = Path.of(utils.getOutputDirectory(), "merged.xml");
		SumoBicycleAttributes.writeGeometries(net, f.sumo(), carry.shapes,
			SumoBicycleAttributes.companion(out, "-linkGeometries.csv"));
		List<String> lines = Files.readAllLines(out.resolveSibling("merged-linkGeometries.csv"));
		String row = lines.stream().filter(l -> l.startsWith("e1-e2,")).findFirst().orElseThrow();
		assertEquals(3, row.split("\\),\\(").length, "three support points, not a two-point chord");
	}

	@Test
	void simplifySynthesizesFeatureRowsForMergedLinks() throws Exception {

		Fixture f = read();
		Network net = mergeableChain();
		SumoBicycleAttributes.MergeCarry carry = new SumoBicycleAttributes.MergeCarry();
		SumoBicycleAttributes.simplifyAndTrack(net, f.sumo(), carry);

		Path in = Path.of(utils.getOutputDirectory(), "in-ft.csv");
		Files.write(in, List.of(
			"linkId,highway_type,speed,length,num_lanes,junction_type",
			"e1,residential,13.89,100.0,1,priority",
			"e2,residential,13.89,100.0,1,traffic_light",
			"r1,residential,13.89,100.0,1,priority",
			"r2,residential,13.89,100.0,1,dead_end"));
		Path out = Path.of(utils.getOutputDirectory(), "out-ft.csv");

		SumoBicycleAttributes.filterFeatures(net, carry.featureSource, in, out);

		List<String> lines = Files.readAllLines(out);
		assertEquals("linkId,highway_type,speed,length,num_lanes,junction_type", lines.get(0));
		String merged = lines.stream().filter(l -> l.startsWith("e1-e2,")).findFirst().orElseThrow();
		assertEquals("e1-e2,residential,13.89,200.00,1,traffic_light", merged,
			"row of the downstream constituent, id and length rewritten");
		assertTrue(lines.stream().noneMatch(l -> l.startsWith("e1,")),
			"the constituents' rows are gone with their links");
	}

	@Test
	void processWithSimplifyKeepsKilometresAndElevatesEverything() throws Exception {

		Fixture plain = read();
		run(plain, SLOPE);
		double kmPlain = plain.network().getLinks().values().stream().mapToDouble(Link::getLength).sum();

		Fixture f = read();
		SumoBicycleAttributes.MergeCarry carry = new SumoBicycleAttributes.MergeCarry();
		SumoBicycleAttributes.Stats stats = SumoBicycleAttributes.process(f.network(), f.sumo(), f.tags(),
			SLOPE, SumoBicycleAttributes.Params.defaults().withSimplify(), carry);

		double km = f.network().getLinks().values().stream().mapToDouble(Link::getLength).sum();
		assertEquals(kmPlain, km, 1e-6, "--simplify merges, it never filters");
		assertTrue(stats.mergedBySimplify >= 0);

		// every classified link has a gradient - merged ones included, because the
		// metrics are computed after the merge
		assertTrue(f.network().getLinks().values().stream()
			.filter(l -> l.getAttributes().getAttribute(BicycleUtils.BICYCLE_INFRA) != null)
			.allMatch(l -> l.getAttributes().getAttribute(BicycleUtils.GRADIENT) != null));
	}

	/** Everything the network holds, in a stable order, for comparing two runs. */
	private static String describe(Network network) {
		return network.getLinks().values().stream()
			.sorted(java.util.Comparator.comparing(l -> l.getId().toString()))
			.map(l -> l.getId() + "|" + new java.util.TreeSet<>(l.getAllowedModes())
				+ "|" + l.getCapacity() + "|" + l.getAttributes())
			.reduce("", (a, b) -> a + "\n" + b);
	}
}
