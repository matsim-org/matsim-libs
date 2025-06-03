package org.matsim.contrib.dvrp.path;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.speedy.LeastCostPathTree;
import org.matsim.core.router.speedy.SpeedyGraphBuilder;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

class LeastCostPathTreeTurnRestrictionsTest {

	Network network;

	@BeforeEach
	void init() {
		network = NetworkUtils.createNetwork();
		Node n0 = NetworkUtils.createNode(Id.createNodeId("0"), new Coord(0, 0));
		Node n1 = NetworkUtils.createNode(Id.createNodeId("1"), new Coord(0, 1));
		Node n2 = NetworkUtils.createNode(Id.createNodeId("2"), new Coord(0, 2));
		network.addNode(n0);
		network.addNode(n1);
		network.addNode(n2);

		Link l01 = NetworkUtils.createLink(Id.createLinkId("01"), n0, n1, network, 1, 1, 300, 1);
		Link l10 = NetworkUtils.createLink(Id.createLinkId("10"), n1, n0, network, 1, 1, 300, 1);
		Link l12 = NetworkUtils.createLink(Id.createLinkId("12"), n1, n2, network, 1, 1, 300, 1);
		Link l21 = NetworkUtils.createLink(Id.createLinkId("21"), n2, n1, network, 1, 1, 300, 1);
		List.of(l01, l10, l12, l21).forEach(network::addLink);

		for (Link link : network.getLinks().values()) {
			link.setAllowedModes(Set.of("car"));
		}
	}

	@Test
	void testNoTurnRestrictions() {

		// -------------

		TravelTime travelTime = new FreeSpeedTravelTime();
		LeastCostPathTree tree = new LeastCostPathTree(SpeedyGraphBuilder.build(network, "car"),
				travelTime, new TimeAsTravelDisutility(travelTime));

		// -------------

		tree.calculateBackwards(network.getLinks().get(Id.createLinkId("10")), 0, null, null);

		for (Node node : network.getNodes().values()) {
			System.out.println(tree.getCost(node.getId().index()));
		}

		Assertions.assertEquals(1., tree.getCost(Id.createNodeId("0").index()));
		Assertions.assertEquals(0., tree.getCost(Id.createNodeId("1").index()));
		Assertions.assertEquals(1., tree.getCost(Id.createNodeId("2").index()));
	}

	@Test
	void testWithOnlyColoredNodes() {

		NetworkUtils.getOrCreateDisallowedNextLinks(network.getLinks().get(Id.createLinkId("01")))
				.addDisallowedLinkSequence("car", List.of(Id.createLinkId("10"))); // no uturn at 1
		NetworkUtils.getOrCreateDisallowedNextLinks(network.getLinks().get(Id.createLinkId("21")))
				.addDisallowedLinkSequence("car", List.of(Id.createLinkId("12"))); // no uturn at 1

		// -------------

		TravelTime travelTime = new FreeSpeedTravelTime();
		LeastCostPathTree tree = new LeastCostPathTree(SpeedyGraphBuilder.build(network, "car"),
				travelTime, new TimeAsTravelDisutility(travelTime));

		// -------------

		tree.calculateBackwards(network.getLinks().get(Id.createLinkId("10")), 0, null, null);

		Assertions.assertEquals(1., tree.getCost(Id.createNodeId("0").index()));
		Assertions.assertEquals(0., tree.getCost(Id.createNodeId("1").index()));
		Assertions.assertEquals(1., tree.getCost(Id.createNodeId("2").index()));
	}
}
