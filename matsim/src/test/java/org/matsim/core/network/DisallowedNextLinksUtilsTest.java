package org.matsim.core.network;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.BiPredicate;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.algorithms.NetworkSimplifier;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinks;
import org.matsim.core.network.turnRestrictions.DisallowedNextLinksUtils;

import com.google.common.base.Verify;

class DisallowedNextLinksUtilsTest {

	@Test
	void testEquals1() {
		Network n = DisallowedNextLinksTest.createNetwork();
		Network n2 = DisallowedNextLinksTest.createNetwork();

		Assertions.assertTrue(NetworkUtils.compare(n, n2));
	}

	@Test
	void testEquals2() {
		Network n1 = DisallowedNextLinksTest.createNetwork();
		{
			Link l1 = n1.getLinks().get(Id.createLinkId("1"));
			DisallowedNextLinks dnl0 = NetworkUtils.getOrCreateDisallowedNextLinks(l1);
			dnl0.addDisallowedLinkSequence("car", List.of(l1.getId(), Id.createLinkId("2")));
		}

		Network n2 = DisallowedNextLinksTest.createNetwork();
		{
			Link l1 = n2.getLinks().get(Id.createLinkId("1"));
			DisallowedNextLinks dnl0 = NetworkUtils.getOrCreateDisallowedNextLinks(l1);
			dnl0.addDisallowedLinkSequence("car", List.of(l1.getId(), Id.createLinkId("2")));
		}

		Assertions.assertTrue(NetworkUtils.compare(n1, n2));
	}

	@Test
	void testNotEquals() {
		Network n = DisallowedNextLinksTest.createNetwork();
		Network n2 = DisallowedNextLinksTest.createNetwork();
		Link l1 = n2.getLinks().get(Id.createLinkId("1"));
		DisallowedNextLinks dnl0 = NetworkUtils.getOrCreateDisallowedNextLinks(l1);
		dnl0.addDisallowedLinkSequence("car", List.of(l1.getId(), Id.createLinkId("2")));

		Assertions.assertFalse(NetworkUtils.compare(n, n2));
	}

	@Test
	void testNoDisallowedNextLinks() {
		Network n = DisallowedNextLinksTest.createNetwork();
		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(n));
	}

	@Test
	void testIsNotValid1() {
		Network n = DisallowedNextLinksTest.createNetwork();
		Map<Id<Link>, ? extends Link> links = n.getLinks();
		Link l1 = links.get(Id.createLinkId("1"));
		Link l3 = links.get(Id.createLinkId("3"));

		DisallowedNextLinks dnl = NetworkUtils.getOrCreateDisallowedNextLinks(l1);
		dnl.addDisallowedLinkSequence("car", List.of(l1.getId(), l3.getId()));
		dnl.addDisallowedLinkSequence("bike", List.of(l1.getId()));

		Assertions.assertFalse(DisallowedNextLinksUtils.isValid(n));
	}

	@Test
	void testIsNotValid2() {
		Network n = DisallowedNextLinksTest.createNetwork();
		Map<Id<Link>, ? extends Link> links = n.getLinks();
		Link l1 = links.get(Id.createLinkId("1"));
		Link l3 = links.get(Id.createLinkId("3"));

		DisallowedNextLinks dnl = NetworkUtils.getOrCreateDisallowedNextLinks(l1);
		dnl.addDisallowedLinkSequence("car", List.of(l1.getId(), l3.getId()));
		dnl.addDisallowedLinkSequence("bike", List.of(Id.createLinkId("a")));

		Assertions.assertFalse(DisallowedNextLinksUtils.isValid(n));
	}

	@Test
	void testIsValid() {
		Network n = DisallowedNextLinksTest.createNetwork();
		Map<Id<Link>, ? extends Link> links = n.getLinks();
		Link l1 = links.get(Id.createLinkId("1"));
		Link l3 = links.get(Id.createLinkId("3"));
		Link l5 = links.get(Id.createLinkId("5"));
		NetworkUtils.addAllowedMode(l1, "bike");
		NetworkUtils.addAllowedMode(l3, "bike");

		DisallowedNextLinks dnl = NetworkUtils.getOrCreateDisallowedNextLinks(l1);
		dnl.addDisallowedLinkSequence("car", List.of(l3.getId(), l5.getId()));
		dnl.addDisallowedLinkSequence("bike", List.of(l3.getId()));

		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(n));
	}

	@Test
	void testClean() {

		Network network = createNetwork();

		Link l01 = network.getLinks().get(Id.createLinkId("01"));
		DisallowedNextLinks dnl01 = NetworkUtils.getOrCreateDisallowedNextLinks(l01);
		dnl01.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("12"), Id.createLinkId("23")));
		dnl01.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("14")));

		network.removeLink(Id.createLinkId("23"));

		Assertions.assertNotNull(NetworkUtils.getDisallowedNextLinks(l01));
		Assertions.assertFalse(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		DisallowedNextLinksUtils.clean(network);

		// * --------------------------------------------------

		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));

		DisallowedNextLinks dnl = NetworkUtils
				.getOrCreateDisallowedNextLinks(network.getLinks().get(Id.createLinkId("01")));
		Assertions.assertEquals(List.of(List.of(Id.createLinkId("14"))),
				dnl.getDisallowedLinkSequences(TransportMode.car));

	}

	@Test
	void testCleanCompletely() {

		Network network = createNetwork();

		Link l01 = network.getLinks().get(Id.createLinkId("01"));
		DisallowedNextLinks dnl01 = NetworkUtils.getOrCreateDisallowedNextLinks(l01);
		dnl01.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("12"), Id.createLinkId("23")));

		network.removeLink(Id.createLinkId("23"));

		Assertions.assertFalse(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		DisallowedNextLinksUtils.clean(network);

		// * --------------------------------------------------

		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));

		Assertions.assertTrue(network.getLinks().values().stream()
				.map(NetworkUtils::getDisallowedNextLinks)
				.allMatch(Objects::isNull));

	}

	@Test
	void testCleanWrongMode() {

		Network network = createNetwork();

		Link l01 = network.getLinks().get(Id.createLinkId("01"));
		DisallowedNextLinks dnl01 = NetworkUtils.getOrCreateDisallowedNextLinks(l01);
		dnl01.addDisallowedLinkSequence(TransportMode.bike, List.of(Id.createLinkId("12"), Id.createLinkId("23")));

		Assertions.assertFalse(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		DisallowedNextLinksUtils.clean(network);

		// * --------------------------------------------------

		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));

		Assertions.assertTrue(network.getLinks().values().stream()
				.map(NetworkUtils::getDisallowedNextLinks)
				.allMatch(Objects::isNull));

	}

	@Test
	void testCleanWrongModeOnNextLink() {

		Network network = createNetwork();

		Link l01 = network.getLinks().get(Id.createLinkId("01"));
		DisallowedNextLinks dnl01 = NetworkUtils.getOrCreateDisallowedNextLinks(l01);
		dnl01.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("12"), Id.createLinkId("23")));

		Link l12 = network.getLinks().get(Id.createLinkId("12"));
		l12.setAllowedModes(Set.of(TransportMode.bike));

		Assertions.assertFalse(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		DisallowedNextLinksUtils.clean(network);

		// * --------------------------------------------------

		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));

		Assertions.assertTrue(network.getLinks().values().stream()
				.map(NetworkUtils::getDisallowedNextLinks)
				.allMatch(Objects::isNull));

	}

	@Test
	void testMergeLastNextLinkWithFollowing() {

		Network network = createNetwork();

		Link l01 = network.getLinks().get(Id.createLinkId("01"));
		DisallowedNextLinks dnl01 = NetworkUtils.getOrCreateDisallowedNextLinks(l01);
		dnl01.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("12")));
		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		// Simplify
		NetworkSimplifier.createNetworkSimplifier(network).run(network);

		// * --------------------------------------------------

		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));

		Assertions.assertEquals(List.of(List.of(Id.createLinkId("12-23-34"))),
				NetworkUtils.getDisallowedNextLinks(l01).getDisallowedLinkSequences(TransportMode.car));

	}

	@Test
	void testMergeLastNextLinkWithFollowingAndOverlap() {

		Network network = createNetwork();

		Link l01 = network.getLinks().get(Id.createLinkId("01"));
		DisallowedNextLinks dnl01 = NetworkUtils.getOrCreateDisallowedNextLinks(l01);
		dnl01.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("12")));
		dnl01.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("12"), Id.createLinkId("23")));

		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		// Simplify
		NetworkSimplifier.createNetworkSimplifier(network).run(network);

		// * --------------------------------------------------

		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));

		Assertions.assertEquals(List.of(
				// List.of(Id.createLinkId("12-23")), // is removed, because subset of next
				List.of(Id.createLinkId("12-23-34"))),
				NetworkUtils.getDisallowedNextLinks(l01).getDisallowedLinkSequences(TransportMode.car));

	}

	@Test
	void testMergeLastNextLinkWithFollowingAndOverlap2() {

		Network network = createNetwork();

		Link l01 = network.getLinks().get(Id.createLinkId("01"));
		DisallowedNextLinks dnl01 = NetworkUtils.getOrCreateDisallowedNextLinks(l01);
		dnl01.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("12"), Id.createLinkId("23")));

		Link l12 = network.getLinks().get(Id.createLinkId("12"));
		DisallowedNextLinks dnl12 = NetworkUtils.getOrCreateDisallowedNextLinks(l12);
		dnl12.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("23"), Id.createLinkId("34")));

		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		// Simplify
		NetworkSimplifier.createNetworkSimplifier(network).run(network);

		// * --------------------------------------------------

		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));

		// this could be optimized a bit more, but it's fine for now
		Assertions.assertEquals(List.of(List.of(Id.createLinkId("12"), Id.createLinkId("23"))),
				NetworkUtils.getDisallowedNextLinks(l01).getDisallowedLinkSequences(TransportMode.car));
		Assertions.assertEquals(List.of(List.of(Id.createLinkId("23"), Id.createLinkId("34"))),
				NetworkUtils.getDisallowedNextLinks(l12).getDisallowedLinkSequences(TransportMode.car));

	}

	@Test
	void testOverlappingDisallowedLinks() {

		Network network = createNetwork();

		Link l01 = network.getLinks().get(Id.createLinkId("01"));
		DisallowedNextLinks dnl01 = NetworkUtils.getOrCreateDisallowedNextLinks(l01);
		dnl01.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("12"), Id.createLinkId("23")));

		Link l23 = network.getLinks().get(Id.createLinkId("23"));
		DisallowedNextLinks dnl23 = NetworkUtils.getOrCreateDisallowedNextLinks(l23);
		dnl23.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("34"), Id.createLinkId("41")));

		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		// Simplify
		NetworkSimplifier.createNetworkSimplifier(network).run(network);

		// * --------------------------------------------------

		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));

		Assertions.assertEquals(List.of(List.of(Id.createLinkId("12"), Id.createLinkId("23"))),
				NetworkUtils.getDisallowedNextLinks(l01).getDisallowedLinkSequences(TransportMode.car));

		Assertions.assertEquals(List.of(List.of(Id.createLinkId("34"), Id.createLinkId("41"))),
				NetworkUtils.getDisallowedNextLinks(l23).getDisallowedLinkSequences(TransportMode.car));

	}

	@Test
	void testLoopLinksInDnl() {

		Network network = createNetworkWithLoop();
		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		DisallowedNextLinksUtils.clean(network);

		// * --------------------------------------------------

		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));

		Link l11 = network.getLinks().get(Id.createLinkId("11"));
		Assertions.assertNull(NetworkUtils.getDisallowedNextLinks(l11));

		Link l01 = network.getLinks().get(Id.createLinkId("01"));
		Assertions.assertEquals(List.of(List.of(Id.createLinkId("12"))),
				NetworkUtils.getDisallowedNextLinks(l01).getDisallowedLinkSequences(TransportMode.car));

	}

	// Helpers

	static Network createNetworkWithLoop() {
		Network network = createNetwork();

		// add loop link
		Node n1 = network.getNodes().get(Id.createNodeId("1"));
		Link l11 = NetworkUtils.createLink(Id.createLinkId("11"), n1, n1, network, 1, 1, 300, 1);
		l11.setAllowedModes(Set.of(TransportMode.car));
		network.addLink(l11);

		// add DNL on loop link
		DisallowedNextLinks dnl11 = NetworkUtils.getOrCreateDisallowedNextLinks(l11);
		dnl11.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("12")));

		// add DNL over loop link
		Link l01 = network.getLinks().get(Id.createLinkId("01"));
		DisallowedNextLinks dnl01 = NetworkUtils.getOrCreateDisallowedNextLinks(l01);
		dnl01.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("11"), Id.createLinkId("12")));

		return network;
	}
	static Network createNetwork() {
		Network network = NetworkUtils.createNetwork();

		Node n0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0, -0));
		Node n1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0, -1));
		Node n2 = NetworkUtils.createNode(Id.createNodeId("2"), new Coord(-1, -2));
		Node n3 = NetworkUtils.createNode(Id.createNodeId("3"), new Coord(-1, -3));
		Node n4 = NetworkUtils.createNode(Id.createNodeId("4"), new Coord(0, -4));
		Node n5 = NetworkUtils.createNode(Id.createNodeId("5"), new Coord(1, -1));
		Node n6 = NetworkUtils.createNode(Id.createNodeId("6"), new Coord(0, -6));

		network.addNode(n0);
		network.addNode(n1);
		network.addNode(n2);
		network.addNode(n3);
		network.addNode(n4);
		network.addNode(n5);
		network.addNode(n6);

		// * n0
		// l01 l10 (0110)
		// * n1 - l15 l51 (1551) - n5
		// l12 l21 (1221) \
		// * n2
		// l23 l32 (2332) | l14 l41 (1441)
		// * n3
		// l34 l43 (3443) /
		// * n4
		// l46 l64 (4664)
		// * n6

		Link l01 = NetworkUtils.createLink(Id.createLinkId("01"), n0, n1, network, 1, 1, 300, 1);
		Link l10 = NetworkUtils.createLink(Id.createLinkId("10"), n1, n0, network, 1, 1, 300, 1);

		Link l12 = NetworkUtils.createLink(Id.createLinkId("12"), n1, n2, network, 1, 1, 300, 1);
		Link l21 = NetworkUtils.createLink(Id.createLinkId("21"), n2, n1, network, 1, 1, 300, 1);

		Link l14 = NetworkUtils.createLink(Id.createLinkId("14"), n1, n4, network, 1, 1, 300, 1);
		Link l41 = NetworkUtils.createLink(Id.createLinkId("41"), n4, n1, network, 1, 1, 300, 1);

		Link l23 = NetworkUtils.createLink(Id.createLinkId("23"), n2, n3, network, 1, 1, 300, 1);
		Link l32 = NetworkUtils.createLink(Id.createLinkId("32"), n3, n2, network, 1, 1, 300, 1);

		Link l34 = NetworkUtils.createLink(Id.createLinkId("34"), n3, n4, network, 1, 1, 300, 1);
		Link l43 = NetworkUtils.createLink(Id.createLinkId("43"), n4, n3, network, 1, 1, 300, 1);

		Link l15 = NetworkUtils.createLink(Id.createLinkId("15"), n1, n5, network, 1, 1, 300, 1);
		Link l51 = NetworkUtils.createLink(Id.createLinkId("51"), n5, n1, network, 1, 1, 300, 1);

		Link l46 = NetworkUtils.createLink(Id.createLinkId("46"), n4, n6, network, 1, 1, 300, 1);
		Link l64 = NetworkUtils.createLink(Id.createLinkId("64"), n6, n4, network, 1, 1, 300, 1);

		network.addLink(l01);
		network.addLink(l10);
		network.addLink(l12);
		network.addLink(l21);
		network.addLink(l14);
		network.addLink(l41);
		network.addLink(l23);
		network.addLink(l32);
		network.addLink(l34);
		network.addLink(l43);
		network.addLink(l15);
		network.addLink(l51);
		network.addLink(l46);
		network.addLink(l64);

		for (Link link : network.getLinks().values()) {
			link.setAllowedModes(Set.of(TransportMode.car));
		}

		return network;
	}
}
