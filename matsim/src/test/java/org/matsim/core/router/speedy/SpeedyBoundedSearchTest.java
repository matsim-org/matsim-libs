/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2026 by the members listed in the COPYING,        *
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

package org.matsim.core.router.speedy;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.ScoringConfigGroup;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeAndDisutility;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Tests for the bounded-search ({@code maxCost}) overload of
 * {@link LeastCostPathCalculator#calcLeastCostPath(Link, Link, double, org.matsim.api.core.v01.population.Person, org.matsim.vehicles.Vehicle, double)}
 * on the speedy implementations.
 *
 * <p>The contract under test:
 * <ul>
 *   <li>Cutoff above the optimal cost: returns the same path as the unbounded call.</li>
 *   <li>Cutoff below the optimal cost: returns {@code null}.</li>
 *   <li>Cutoff at exactly the optimal cost: returns the optimal path (cutoff is inclusive).</li>
 *   <li>{@code Double.POSITIVE_INFINITY}: equivalent to the unbounded call.</li>
 *   <li>Turn restrictions: cutoff behaviour is independent of turn-restriction expansion;
 *       optimal path through restrictions is found when cutoff is high enough, and
 *       {@code null} is returned when the cutoff is below that path's cost.</li>
 * </ul>
 */
public class SpeedyBoundedSearchTest {

	/**
	 * Builds a simple 4-node chain network:
	 * <pre>
	 *   A --(linkAB, 1000m)--> B --(linkBC, 1000m)--> C --(linkCD, 1000m)--> D
	 * </pre>
	 * All links have freespeed 10 m/s, so each link costs 100s of travel time
	 * under {@link FreespeedTravelTimeAndDisutility}. The optimal A->D cost is 300s.
	 * <p>
	 * Note that the link-based router queries from {@code linkAB.toNode = B} to
	 * {@code linkCD.fromNode = C}, exercising the path B->C of cost 100s.
	 */
	private static Network buildChainNetwork() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();

		Node a = NetworkUtils.createAndAddNode(network, Id.createNodeId("A"), new Coord(0, 0));
		Node b = NetworkUtils.createAndAddNode(network, Id.createNodeId("B"), new Coord(1000, 0));
		Node c = NetworkUtils.createAndAddNode(network, Id.createNodeId("C"), new Coord(2000, 0));
		Node d = NetworkUtils.createAndAddNode(network, Id.createNodeId("D"), new Coord(3000, 0));

		NetworkUtils.createAndAddLink(network, Id.createLinkId("AB"), a, b, 1000.0, 10.0, 1000.0, 1.0);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("BC"), b, c, 1000.0, 10.0, 1000.0, 1.0);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("CD"), c, d, 1000.0, 10.0, 1000.0, 1.0);

		return network;
	}

	private static SpeedyDijkstra buildDijkstra(Network network) {
		FreespeedTravelTimeAndDisutility td = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
		SpeedyGraph graph = SpeedyGraphBuilder.build(network);
		return new SpeedyDijkstra(graph, td, td);
	}

	private static SpeedyALT buildAlt(Network network) {
		FreespeedTravelTimeAndDisutility td = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
		SpeedyGraph graph = SpeedyGraphBuilder.build(network);
		SpeedyALTData altData = new SpeedyALTData(graph, 4, td, 4);
		return new SpeedyALT(altData, td, td);
	}

	// ------------------------------------------------------------------
	// SpeedyDijkstra
	// ------------------------------------------------------------------

	@Test
	void dijkstra_cutoffAboveOptimal_returnsSamePath() {
		Network network = buildChainNetwork();
		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkCD = network.getLinks().get(Id.createLinkId("CD"));

		SpeedyDijkstra router = buildDijkstra(network);
		Path unbounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null);
		Path bounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null, unbounded.travelCost + 1.0);

		assertNotNull(bounded, "cutoff above optimal must return a path");
		assertEquals(unbounded.travelCost, bounded.travelCost, 1e-9);
		assertEquals(unbounded.travelTime, bounded.travelTime, 1e-9);
		assertEquals(unbounded.links.size(), bounded.links.size());
		for (int i = 0; i < unbounded.links.size(); i++) {
			assertEquals(unbounded.links.get(i).getId(), bounded.links.get(i).getId(),
					"link " + i + " differs");
		}
	}

	@Test
	void dijkstra_cutoffBelowOptimal_returnsNull() {
		Network network = buildChainNetwork();
		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkCD = network.getLinks().get(Id.createLinkId("CD"));

		SpeedyDijkstra router = buildDijkstra(network);
		Path unbounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null);
		assertNotNull(unbounded);
		assertTrue(unbounded.travelCost > 0, "optimal cost must be > 0 for this test");

		Path bounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null, unbounded.travelCost - 1e-6);
		assertNull(bounded, "cutoff strictly below optimal must return null");
	}

	@Test
	void dijkstra_cutoffAtExactlyOptimal_returnsPath() {
		Network network = buildChainNetwork();
		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkCD = network.getLinks().get(Id.createLinkId("CD"));

		SpeedyDijkstra router = buildDijkstra(network);
		Path unbounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null);
		Path bounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null, unbounded.travelCost);
		assertNotNull(bounded, "cutoff == optimal must return the path (cutoff is inclusive)");
		assertEquals(unbounded.travelCost, bounded.travelCost, 1e-9);
	}

	@Test
	void dijkstra_positiveInfinityCutoff_equalsUnbounded() {
		Network network = buildChainNetwork();
		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkCD = network.getLinks().get(Id.createLinkId("CD"));

		SpeedyDijkstra router = buildDijkstra(network);
		Path unbounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null);
		Path bounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null, Double.POSITIVE_INFINITY);
		assertEquals(unbounded.travelCost, bounded.travelCost, 1e-9);
		assertEquals(unbounded.links.size(), bounded.links.size());
	}

	// ------------------------------------------------------------------
	// SpeedyALT
	// ------------------------------------------------------------------

	@Test
	void alt_cutoffAboveOptimal_returnsSamePath() {
		Network network = buildChainNetwork();
		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkCD = network.getLinks().get(Id.createLinkId("CD"));

		SpeedyALT router = buildAlt(network);
		Path unbounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null);
		Path bounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null, unbounded.travelCost + 1.0);

		assertNotNull(bounded);
		assertEquals(unbounded.travelCost, bounded.travelCost, 1e-9);
		assertEquals(unbounded.links.size(), bounded.links.size());
		for (int i = 0; i < unbounded.links.size(); i++) {
			assertEquals(unbounded.links.get(i).getId(), bounded.links.get(i).getId());
		}
	}

	@Test
	void alt_cutoffBelowOptimal_returnsNull() {
		Network network = buildChainNetwork();
		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkCD = network.getLinks().get(Id.createLinkId("CD"));

		SpeedyALT router = buildAlt(network);
		Path unbounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null);
		assertNotNull(unbounded);

		Path bounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null, unbounded.travelCost - 1e-6);
		assertNull(bounded);
	}

	@Test
	void alt_cutoffAtExactlyOptimal_returnsPath() {
		// ALT compares f = g + h against maxCost. Because the heuristic is admissible
		// (h <= remaining true cost), f at the target equals the true optimal cost, so
		// maxCost == optimalCost must still return the path (cutoff is inclusive).
		Network network = buildChainNetwork();
		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkCD = network.getLinks().get(Id.createLinkId("CD"));

		SpeedyALT router = buildAlt(network);
		Path unbounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null);
		Path bounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null, unbounded.travelCost);
		assertNotNull(bounded, "cutoff == optimal must return the path (cutoff is inclusive)");
		assertEquals(unbounded.travelCost, bounded.travelCost, 1e-9);
		assertEquals(unbounded.links.size(), bounded.links.size());
	}

	@Test
	void alt_positiveInfinityCutoff_equalsUnbounded() {
		Network network = buildChainNetwork();
		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkCD = network.getLinks().get(Id.createLinkId("CD"));

		SpeedyALT router = buildAlt(network);
		Path unbounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null);
		Path bounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null, Double.POSITIVE_INFINITY);
		assertEquals(unbounded.travelCost, bounded.travelCost, 1e-9);
		assertEquals(unbounded.links.size(), bounded.links.size());
	}

	// ------------------------------------------------------------------
	// Turn restrictions: cutoff is orthogonal to turn-restriction expansion.
	// We use the same network shape as AbstractLeastCostPathCalculatorTestWithTurnRestrictions
	// so we know the unique S->T path under the restrictions is S-1-2-3-4-5-T (6 links).
	// ------------------------------------------------------------------

	private static Network buildTurnRestrictionsNetwork() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();

		Node nodeS = NetworkUtils.createAndAddNode(network, Id.createNodeId("S"), new Coord(1, 2));
		Node node1 = NetworkUtils.createAndAddNode(network, Id.createNodeId("1"), new Coord(1, 1));
		Node node2 = NetworkUtils.createAndAddNode(network, Id.createNodeId("2"), new Coord(0, 1));
		Node node3 = NetworkUtils.createAndAddNode(network, Id.createNodeId("3"), new Coord(0, 0));
		Node node4 = NetworkUtils.createAndAddNode(network, Id.createNodeId("4"), new Coord(1, 0));
		Node node5 = NetworkUtils.createAndAddNode(network, Id.createNodeId("5"), new Coord(2, 0));
		Node nodeT = NetworkUtils.createAndAddNode(network, Id.createNodeId("T"), new Coord(2, 0));

		Link linkS1 = NetworkUtils.createAndAddLink(network, Id.createLinkId("S1"), nodeS, node1, 1, 1, 1, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("12"), node1, node2, 1, 1, 1, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("1T"), node1, nodeT, 1, 1, 1, 1);
		Link link23 = NetworkUtils.createAndAddLink(network, Id.createLinkId("23"), node2, node3, 1, 1, 1, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("34"), node3, node4, 1, 1, 1, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("4T"), node4, nodeT, 1, 1, 1, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("45"), node4, node5, 1, 1, 1, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("5T"), node5, nodeT, 1, 1, 1, 1);

		NetworkUtils.addDisallowedNextLinks(linkS1, TransportMode.car, Arrays.asList(Id.createLinkId("1T")));
		NetworkUtils.addDisallowedNextLinks(link23, TransportMode.car, Arrays.asList(Id.createLinkId("34"), Id.createLinkId("4T")));

		return network;
	}

	@Test
	void dijkstra_turnRestrictions_cutoffAboveOptimal_findsRoutedPath() {
		Network network = buildTurnRestrictionsNetwork();
		Link linkS1 = network.getLinks().get(Id.createLinkId("S1"));
		Link link5T = network.getLinks().get(Id.createLinkId("5T"));

		SpeedyDijkstra router = buildDijkstra(network);
		Path unbounded = router.calcLeastCostPath(linkS1, link5T, 0, null, null);
		assertNotNull(unbounded, "the turn-restriction-aware path must exist");
		Path bounded = router.calcLeastCostPath(linkS1, link5T, 0, null, null, unbounded.travelCost + 1.0);
		assertNotNull(bounded);
		assertEquals(unbounded.travelCost, bounded.travelCost, 1e-9);
		assertEquals(unbounded.links.size(), bounded.links.size());
	}

	@Test
	void dijkstra_turnRestrictions_cutoffBelowOptimal_returnsNull() {
		Network network = buildTurnRestrictionsNetwork();
		Link linkS1 = network.getLinks().get(Id.createLinkId("S1"));
		Link link5T = network.getLinks().get(Id.createLinkId("5T"));

		SpeedyDijkstra router = buildDijkstra(network);
		Path unbounded = router.calcLeastCostPath(linkS1, link5T, 0, null, null);
		assertNotNull(unbounded);

		Path bounded = router.calcLeastCostPath(linkS1, link5T, 0, null, null, unbounded.travelCost - 1e-6);
		assertNull(bounded, "cutoff strictly below the routed path's cost must return null");
	}

	@Test
	void alt_turnRestrictions_cutoffAboveOptimal_findsRoutedPath() {
		Network network = buildTurnRestrictionsNetwork();
		Link linkS1 = network.getLinks().get(Id.createLinkId("S1"));
		Link link5T = network.getLinks().get(Id.createLinkId("5T"));

		SpeedyALT router = buildAlt(network);
		Path unbounded = router.calcLeastCostPath(linkS1, link5T, 0, null, null);
		assertNotNull(unbounded);
		Path bounded = router.calcLeastCostPath(linkS1, link5T, 0, null, null, unbounded.travelCost + 1.0);
		assertNotNull(bounded);
		assertEquals(unbounded.travelCost, bounded.travelCost, 1e-9);
	}

	@Test
	void alt_turnRestrictions_cutoffBelowOptimal_returnsNull() {
		Network network = buildTurnRestrictionsNetwork();
		Link linkS1 = network.getLinks().get(Id.createLinkId("S1"));
		Link link5T = network.getLinks().get(Id.createLinkId("5T"));

		SpeedyALT router = buildAlt(network);
		Path unbounded = router.calcLeastCostPath(linkS1, link5T, 0, null, null);
		assertNotNull(unbounded);

		Path bounded = router.calcLeastCostPath(linkS1, link5T, 0, null, null, unbounded.travelCost - 1e-6);
		assertNull(bounded);
	}

	// ------------------------------------------------------------------
	// Disconnected target: the source can never reach the target.
	// This is the pt2matsim long-tail use case (a transit-route stop pair lands on a
	// disconnected component of the mode-specific subgraph). Bounded search must return
	// {@code null} for both routers, and it must do so quickly without exhausting the
	// full reachable component.
	// ------------------------------------------------------------------

	/**
	 * Builds two disconnected chain components.
	 * <pre>
	 *   A --(AB)--> B --(BC)--> C        (reachable component from A)
	 *   D --(DE)--> E --(EF)--> F        (separate component, unreachable from A)
	 * </pre>
	 */
	private static Network buildDisconnectedNetwork() {
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Network network = scenario.getNetwork();

		Node a = NetworkUtils.createAndAddNode(network, Id.createNodeId("A"), new Coord(0, 0));
		Node b = NetworkUtils.createAndAddNode(network, Id.createNodeId("B"), new Coord(1000, 0));
		Node c = NetworkUtils.createAndAddNode(network, Id.createNodeId("C"), new Coord(2000, 0));
		Node d = NetworkUtils.createAndAddNode(network, Id.createNodeId("D"), new Coord(5000, 0));
		Node e = NetworkUtils.createAndAddNode(network, Id.createNodeId("E"), new Coord(6000, 0));
		Node f = NetworkUtils.createAndAddNode(network, Id.createNodeId("F"), new Coord(7000, 0));

		NetworkUtils.createAndAddLink(network, Id.createLinkId("AB"), a, b, 1000.0, 10.0, 1000.0, 1.0);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("BC"), b, c, 1000.0, 10.0, 1000.0, 1.0);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("DE"), d, e, 1000.0, 10.0, 1000.0, 1.0);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("EF"), e, f, 1000.0, 10.0, 1000.0, 1.0);

		return network;
	}

	@Test
	void dijkstra_disconnectedTarget_returnsNullUnderCutoff() {
		Network network = buildDisconnectedNetwork();
		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkEF = network.getLinks().get(Id.createLinkId("EF"));

		SpeedyDijkstra router = buildDijkstra(network);
		Path bounded = router.calcLeastCostPath(linkAB, linkEF, 0, null, null, 1000.0);
		assertNull(bounded, "target on disconnected component must return null under a finite cutoff");
	}

	@Test
	void alt_disconnectedTarget_returnsNullUnderCutoff() {
		Network network = buildDisconnectedNetwork();
		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkEF = network.getLinks().get(Id.createLinkId("EF"));

		SpeedyALT router = buildAlt(network);
		// On the disconnected component, h(start) is +infinity (the triangle inequality with any
		// landmark reachable from t but not from start). The bounded cutoff must therefore fire on
		// the very first peek and return null without exploring the full reachable component.
		Path bounded = router.calcLeastCostPath(linkAB, linkEF, 0, null, null, 1000.0);
		assertNull(bounded, "target on disconnected component must return null under a finite cutoff");
	}
}
