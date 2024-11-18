package org.matsim.core.router;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;
import org.xml.sax.SAXException;

import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;

public abstract class AbstractLeastCostPathCalculatorTestWithTurnRestrictions extends AbstractLeastCostPathCalculatorTest {
	@Test
	void testCalcLeastCostPath_TurnRestrictions() throws SAXException, ParserConfigurationException, IOException {
		Network network = createTurnRestrictionsTestNetwork();

		Node nodeS = network.getNodes().get(Id.create("S", Node.class));
		Node nodeT = network.getNodes().get(Id.create("T", Node.class));

		LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(network);
		Path path = routerAlgo.calcLeastCostPath(nodeS, nodeT, 8.0 * 3600, null, null);

		assertEquals(7, path.nodes.size(), "number of nodes wrong.");
		assertEquals(6, path.links.size(), "number of links wrong.");

		assertEquals(network.getNodes().get(Id.create("S", Node.class)), path.nodes.get(0));
		assertEquals(network.getNodes().get(Id.create("1", Node.class)), path.nodes.get(1));
		assertEquals(network.getNodes().get(Id.create("2", Node.class)), path.nodes.get(2));
		assertEquals(network.getNodes().get(Id.create("3", Node.class)), path.nodes.get(3));
		assertEquals(network.getNodes().get(Id.create("4", Node.class)), path.nodes.get(4));
		assertEquals(network.getNodes().get(Id.create("5", Node.class)), path.nodes.get(5));
		assertEquals(network.getNodes().get(Id.create("T", Node.class)), path.nodes.get(6));

		assertEquals(network.getLinks().get(Id.create("S1", Link.class)), path.links.get(0));
		assertEquals(network.getLinks().get(Id.create("12", Link.class)), path.links.get(1));
		assertEquals(network.getLinks().get(Id.create("23", Link.class)), path.links.get(2));
		assertEquals(network.getLinks().get(Id.create("34", Link.class)), path.links.get(3));
		assertEquals(network.getLinks().get(Id.create("45", Link.class)), path.links.get(4));
		assertEquals(network.getLinks().get(Id.create("5T", Link.class)), path.links.get(5));
	}

	@Test
	void testCalcLeastCostPath_TurnRestrictions_IntermediateNode() {
		Network network = createTurnRestrictionsTestNetwork();

		Node nodeS = network.getNodes().get(Id.create("S", Node.class));
		Node node3 = network.getNodes().get(Id.create("3", Node.class));

		LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(network);
		Path path = routerAlgo.calcLeastCostPath(nodeS, node3, 8.0 * 3600, null, null);
		Assertions.assertNotNull(path);
	}

	@Test
	void testCalcLeastCostPath_TurnRestrictions_simple() {
		Network network = createTurnRestrictionsTestNetwork();

		Node nodeS = network.getNodes().get(Id.create("S", Node.class));
		Node node1 = network.getNodes().get(Id.create("1", Node.class));

		LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(network);
		Path path = routerAlgo.calcLeastCostPath(nodeS, node1, 8.0 * 3600, null, null);
		Assertions.assertNotNull(path);
	}

	@Test
	void testCalcLeastCostPath_noTurnRestrictions_simple() {
		Network network = createTurnRestrictionsTestNetwork();

		Node nodeS = network.getNodes().get(Id.create("5", Node.class));
		Node node1 = network.getNodes().get(Id.create("T", Node.class));

		LeastCostPathCalculator routerAlgo = getLeastCostPathCalculator(network);
		Path path = routerAlgo.calcLeastCostPath(nodeS, node1, 8.0 * 3600, null, null);
		Assertions.assertNotNull(path);
	}

	//@formatter:off
    /**
     * Creates a test network where the shortest path is impossible due to turn restrictions
     *
     *           S
     *           |
     *          \/
     *      2<---1--->T
     *      |      /  /\
     *     \/    /    |
     *     3--->4--->5
     *
     *  Where S1 -> 1T  and 23 -> 34 -> 4T are forbidden.
     *
     * @return
     */
	//@formatter:on
	private Network createTurnRestrictionsTestNetwork() {
		Config config = utils.loadConfig((String) null);
		Scenario scenario = ScenarioUtils.createScenario(config);
		final Network network = scenario.getNetwork();

		final Node nodeS = NetworkUtils.createAndAddNode(network, Id.createNodeId("S"), new Coord(1, 2));
		final Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("1"), new Coord(1, 1));
		final Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("2"), new Coord(0, 1));
		final Node node3 = NetworkUtils.createAndAddNode(network, Id.createNodeId("3"), new Coord(0, 0));
		final Node node4 = NetworkUtils.createAndAddNode(network, Id.createNodeId("4"), new Coord(1, 0));
		final Node node5 = NetworkUtils.createAndAddNode(network, Id.createNodeId("5"), new Coord(2, 0));
		final Node nodeT = NetworkUtils.createAndAddNode(network, Id.createNodeId("T"), new Coord(2, 0));

		final Link linkS1 = NetworkUtils.createAndAddLink(network, Id.createLinkId("S1"), nodeS, node1, 1, 1, 1, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("12"), node1, node2, 1, 1, 1, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("1T"), node1, nodeT, 1, 1, 1, 1);
		final Link link23 = NetworkUtils.createAndAddLink(network, Id.createLinkId("23"), node2, node3, 1, 1, 1, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("34"), node3, node4, 1, 1, 1, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("4T"), node4, nodeT, 1, 1, 1, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("45"), node4, node5, 1, 1, 1, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("5T"), node5, nodeT, 1, 1, 1, 1);

		NetworkUtils.addDisallowedNextLinks(linkS1, TransportMode.car, Arrays.asList(Id.createLinkId("1T")));
		NetworkUtils.addDisallowedNextLinks(link23, TransportMode.car, Arrays.asList(Id.createLinkId("34"), Id.createLinkId("4T")));

		return network;
	}
}
