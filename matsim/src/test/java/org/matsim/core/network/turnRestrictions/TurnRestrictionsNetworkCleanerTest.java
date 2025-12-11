package org.matsim.core.network.turnRestrictions;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

import com.google.common.base.Verify;

/**
 * @author hrewald
 */
class TurnRestrictionsNetworkCleanerTest {

	@Test
	void testNoChange() {

		Network network = createNetwork();
		addDisallowedNextLinks(network);
		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		TurnRestrictionsNetworkCleaner trc = new TurnRestrictionsNetworkCleaner();
		trc.run(network);

		// * --------------------------------------------------

		Network expectedNetwork = createNetwork();
		addDisallowedNextLinks(expectedNetwork);

		Assertions.assertTrue(NetworkUtils.compare(expectedNetwork, network));
		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));
	}

	@Test
	void testWithIsland() {

		Network network = createNetwork();
		addNetworkIsland(network);
		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		TurnRestrictionsNetworkCleaner trc = new TurnRestrictionsNetworkCleaner();
		trc.run(network);

		// * --------------------------------------------------

		Network expectedNetwork = createNetwork();

		Assertions.assertTrue(NetworkUtils.compare(expectedNetwork, network));
		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));

	}

	@Test
	void testNoExit() {

		Network network = createNetwork();
		addNoExit(network);
		Verify.verify(DisallowedNextLinksUtils.isValid(network));

		// * --------------------------------------------------

		TurnRestrictionsNetworkCleaner trc = new TurnRestrictionsNetworkCleaner();
		trc.run(network);

		// * --------------------------------------------------

		Network expectedNetwork = createNetwork();
		addNoExit(expectedNetwork);
		// some elements are removed as node 8 is not reachable anymore
		expectedNetwork.removeNode(Id.createNodeId("8"));
		expectedNetwork.removeLink(Id.createLinkId("78"));
		expectedNetwork.removeLink(Id.createLinkId("87"));
		Link l67 = expectedNetwork.getLinks().get(Id.createLinkId("67"));
		NetworkUtils.removeDisallowedNextLinks(l67);
		Verify.verify(DisallowedNextLinksUtils.isValid(expectedNetwork));

		Assertions.assertTrue(NetworkUtils.compare(expectedNetwork, network));
		Assertions.assertTrue(DisallowedNextLinksUtils.isValid(network));

	}

	// Helpers

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

		// * add some turn restrictions

		DisallowedNextLinks dnl01 = NetworkUtils.getOrCreateDisallowedNextLinks(l01);
		dnl01.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("12"), Id.createLinkId("23")));

		DisallowedNextLinks dnl34 = NetworkUtils.getOrCreateDisallowedNextLinks(l34);
		dnl34.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("41")));

		return network;
	}

	static void addDisallowedNextLinks(Network network) {
		Link l01 = network.getLinks().get(Id.createLinkId("01"));
		DisallowedNextLinks dnl01 = NetworkUtils.getOrCreateDisallowedNextLinks(l01);
		dnl01.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("12"), Id.createLinkId("23")));
	}

	static void addNetworkIsland(Network network) {

		Node n7 = NetworkUtils.createNode(Id.createNodeId("7"), new Coord(0, -7));
		Node n8 = NetworkUtils.createNode(Id.createNodeId("8"), new Coord(0, -8));

		network.addNode(n7);
		network.addNode(n8);

		Link l78 = NetworkUtils.createLink(Id.createLinkId("78"), n7, n8, network, 1, 1, 300, 1);
		Link l87 = NetworkUtils.createLink(Id.createLinkId("87"), n8, n7, network, 1, 1, 300, 1);
		l78.setAllowedModes(Set.of(TransportMode.car));
		l87.setAllowedModes(Set.of(TransportMode.car));

		network.addLink(l78);
		network.addLink(l87);
	}

	static void addNoExit(Network network) {

		Node n6 = network.getNodes().get(Id.createNodeId("6"));
		Node n7 = NetworkUtils.createNode(Id.createNodeId("7"), new Coord(0, -7));
		Node n8 = NetworkUtils.createNode(Id.createNodeId("8"), new Coord(0, -8));

		network.addNode(n7);
		network.addNode(n8);

		Link l67 = NetworkUtils.createLink(Id.createLinkId("67"), n6, n7, network, 1, 1, 300, 1);
		Link l76 = NetworkUtils.createLink(Id.createLinkId("76"), n7, n6, network, 1, 1, 300, 1);
		l67.setAllowedModes(Set.of(TransportMode.car));
		l76.setAllowedModes(Set.of(TransportMode.car));

		Link l78 = NetworkUtils.createLink(Id.createLinkId("78"), n7, n8, network, 1, 1, 300, 1);
		Link l87 = NetworkUtils.createLink(Id.createLinkId("87"), n8, n7, network, 1, 1, 300, 1);
		l78.setAllowedModes(Set.of(TransportMode.car));
		l87.setAllowedModes(Set.of(TransportMode.car));

		network.addLink(l67);
		network.addLink(l76);
		network.addLink(l78);
		network.addLink(l87);

		DisallowedNextLinks dnl67 = NetworkUtils.getOrCreateDisallowedNextLinks(l67);
		dnl67.addDisallowedLinkSequence(TransportMode.car, List.of(Id.createLinkId("78")));
	}
}
