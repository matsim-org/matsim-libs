package org.matsim.contrib.msp.traveltimematrix;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.network.NetworkUtils;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests the road classes: which class a link lands in, and that the classes come out in a reproducible
 * order - the order is part of the stored data, so it may not depend on the iteration order of a map.
 */
class RoadClassificationTest {

	@Test
	void singleClassPutsEveryLinkInTheSameClass() {

		Network network = TestNetworks.corridor();
		RoadClassification classification = RoadClassification.single();

		assertEquals(List.of(RoadClassification.ALL), classification.getRoadClasses());
		assertTrue(classification.isSingleClass());

		for (Link link : network.getLinks().values()) {
			assertEquals(0, classification.indexOf(link));
			assertEquals(RoadClassification.ALL, classification.classOf(link));
		}
	}

	@Test
	void theDefaultGroupsPutRampsWithTheRoadTheyServe() {

		Network network = TestNetworks.corridor();
		RoadClassification classification = RoadClassification.byDefaultGroups(network);

		assertEquals("motorway", classification.classOf(network.getLinks().get(linkId("2-3"))));
		assertEquals("residential", classification.classOf(network.getLinks().get(linkId("1-2"))));

		// a ramp, a trunk road and the trunk's ramp are all motorway-like
		Link ramp = TestNetworks.link(network, "ramp", network.getNodes().get(nodeId("n1")),
			network.getNodes().get(nodeId("n3")), 30, "motorway_link");
		assertEquals("motorway", classification.classOf(ramp));

		NetworkUtils.setType(ramp, "trunk");
		assertEquals("motorway", classification.classOf(ramp));

		NetworkUtils.setType(ramp, "trunk_link");
		assertEquals("motorway", classification.classOf(ramp));
	}

	@Test
	void aTypeWithoutAGroupIsOther() {

		Network network = TestNetworks.corridor();
		RoadClassification classification = RoadClassification.byDefaultGroups(network);

		Link track = TestNetworks.link(network, "track", network.getNodes().get(nodeId("n1")),
			network.getNodes().get(nodeId("n3")), 5, "track");
		assertEquals(RoadClassification.OTHER, classification.classOf(track));

		// a link with no type attribute at all reads 'unclassified', which the default groups do have
		Link noType = NetworkUtils.createAndAddLink(network, linkId("no-type"), network.getNodes().get(nodeId("n3")),
			network.getNodes().get(nodeId("n1")), 1000, 10, 1800, 1);
		assertEquals("residential", classification.classOf(noType));
	}

	@Test
	void theClassesAreAlphabeticalAndEndWithOther() {

		RoadClassification classification = RoadClassification.byDefaultGroups(TestNetworks.corridor());

		List<String> classes = classification.getRoadClasses();
		assertEquals(List.of("motorway", "primary", "residential", "secondary", "tertiary", RoadClassification.OTHER),
			classes);
		// the fallback class has to be a class of its own, or an unknown type would land in a real one
		assertEquals(RoadClassification.OTHER, classes.get(classes.size() - 1));
	}

	@Test
	void byHighwayTypeOnlyHasTheTypesOfTheNetwork() {

		RoadClassification classification = RoadClassification.byHighwayType(TestNetworks.corridor());

		assertEquals(List.of("motorway", "residential", RoadClassification.OTHER),
			classification.getRoadClasses());
	}

	@Test
	void aTypeInTwoGroupsIsRefused() {

		Network network = TestNetworks.corridor();
		Map<String, Set<String>> overlapping = Map.of(
			"fast", Set.of("motorway", "primary"),
			"slow", Set.of("primary", "residential"));

		IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
			() -> RoadClassification.byGroups(network, overlapping));
		assertTrue(e.getMessage().contains("primary"), e.getMessage());
	}

	@Test
	void theClassesOfAProfileAlwaysIncludeTheNetworkWideOne() {

		RoadClassification classification = RoadClassification.byDefaultGroups(TestNetworks.corridor());

		assertTrue(classification.getRoadClassesWithAll().contains(RoadClassification.ALL));
		assertEquals(classification.size() + 1, classification.getRoadClassesWithAll().size());
	}

	private static org.matsim.api.core.v01.Id<Link> linkId(String id) {
		return org.matsim.api.core.v01.Id.createLinkId(id);
	}

	private static org.matsim.api.core.v01.Id<org.matsim.api.core.v01.network.Node> nodeId(String id) {
		return org.matsim.api.core.v01.Id.createNodeId(id);
	}
}
