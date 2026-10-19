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

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for the bounded-search ({@code maxCost}) overload of
 * {@link LeastCostPathCalculator#calcLeastCostPath(Link, Link, double, org.matsim.api.core.v01.population.Person, org.matsim.vehicles.Vehicle, double)}
 * on the CH routers: {@link CHRouter} (static) and {@link CHRouterTimeDep} (time-dependent).
 *
 * <p>Contract under test (identical to {@link SpeedyBoundedSearchTest}):
 * <ul>
 *   <li>Cutoff above the optimal cost: returns the same path as the unbounded call.</li>
 *   <li>Cutoff below the optimal cost: returns {@code null}.</li>
 *   <li>{@code Double.POSITIVE_INFINITY}: equivalent to the unbounded call.</li>
 *   <li>Disconnected target: returns {@code null} under any finite cutoff.</li>
 * </ul>
 */
public class CHBoundedSearchTest {

	/** 4-node chain identical to {@link SpeedyBoundedSearchTest#buildChainNetwork()}. */
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

	/** Disconnected network: ABC component plus DEF island. */
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

	private static CHRouter buildChRouter(Network network) {
		FreespeedTravelTimeAndDisutility td = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
		SpeedyGraph graph = SpeedyGraphBuilder.buildWithSpatialOrdering(network);
		CHGraph chGraph = new CHBuilder(graph, td).build();
		new CHCustomizer().customize(chGraph, td);
		return new CHRouter(chGraph, td, td);
	}

	private static CHRouterTimeDep buildChRouterTimeDep(Network network) {
		FreespeedTravelTimeAndDisutility td = new FreespeedTravelTimeAndDisutility(new ScoringConfigGroup());
		SpeedyGraph graph = SpeedyGraphBuilder.buildWithSpatialOrdering(network);
		CHGraph chGraph = new CHBuilder(graph, td).build();
		new CHTTFCustomizer().customize(chGraph, td, td);
		return new CHRouterTimeDep(chGraph, td, td);
	}

	// ------------------------------------------------------------------
	// CHRouter (static)
	// ------------------------------------------------------------------

	@Test
	void chRouter_cutoffAboveOptimal_returnsSamePath() {
		Network network = buildChainNetwork();
		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkCD = network.getLinks().get(Id.createLinkId("CD"));

		CHRouter router = buildChRouter(network);
		Path unbounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null);
		Path bounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null, unbounded.travelCost + 1.0);

		assertNotNull(bounded);
		assertEquals(unbounded.travelCost, bounded.travelCost, 1e-9);
		assertEquals(unbounded.links.size(), bounded.links.size());
	}

	@Test
	void chRouter_cutoffBelowOptimal_returnsNull() {
		Network network = buildChainNetwork();
		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkCD = network.getLinks().get(Id.createLinkId("CD"));

		CHRouter router = buildChRouter(network);
		Path unbounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null);
		assertNotNull(unbounded);
		Path bounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null, unbounded.travelCost - 1e-6);
		assertNull(bounded);
	}

	@Test
	void chRouter_positiveInfinityCutoff_equalsUnbounded() {
		Network network = buildChainNetwork();
		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkCD = network.getLinks().get(Id.createLinkId("CD"));

		CHRouter router = buildChRouter(network);
		Path unbounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null);
		Path bounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null, Double.POSITIVE_INFINITY);
		assertEquals(unbounded.travelCost, bounded.travelCost, 1e-9);
	}

	@Test
	void chRouter_disconnectedTarget_returnsNullUnderCutoff() {
		Network network = buildDisconnectedNetwork();
		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkEF = network.getLinks().get(Id.createLinkId("EF"));

		CHRouter router = buildChRouter(network);
		Path bounded = router.calcLeastCostPath(linkAB, linkEF, 0, null, null, 1000.0);
		assertNull(bounded);
	}

	// ------------------------------------------------------------------
	// CHRouterTimeDep
	// ------------------------------------------------------------------

	@Test
	void chRouterTimeDep_cutoffAboveOptimal_returnsSamePath() {
		Network network = buildChainNetwork();
		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkCD = network.getLinks().get(Id.createLinkId("CD"));

		CHRouterTimeDep router = buildChRouterTimeDep(network);
		Path unbounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null);
		Path bounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null, unbounded.travelCost + 1.0);

		assertNotNull(bounded);
		assertEquals(unbounded.travelCost, bounded.travelCost, 1e-9);
		assertEquals(unbounded.links.size(), bounded.links.size());
	}

	@Test
	void chRouterTimeDep_cutoffBelowOptimal_returnsNull() {
		Network network = buildChainNetwork();
		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkCD = network.getLinks().get(Id.createLinkId("CD"));

		CHRouterTimeDep router = buildChRouterTimeDep(network);
		Path unbounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null);
		assertNotNull(unbounded);
		Path bounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null, unbounded.travelCost - 1e-6);
		assertNull(bounded);
	}

	@Test
	void chRouterTimeDep_positiveInfinityCutoff_equalsUnbounded() {
		Network network = buildChainNetwork();
		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkCD = network.getLinks().get(Id.createLinkId("CD"));

		CHRouterTimeDep router = buildChRouterTimeDep(network);
		Path unbounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null);
		Path bounded = router.calcLeastCostPath(linkAB, linkCD, 0, null, null, Double.POSITIVE_INFINITY);
		assertEquals(unbounded.travelCost, bounded.travelCost, 1e-9);
	}

	@Test
	void chRouterTimeDep_disconnectedTarget_returnsNullUnderCutoff() {
		Network network = buildDisconnectedNetwork();
		Link linkAB = network.getLinks().get(Id.createLinkId("AB"));
		Link linkEF = network.getLinks().get(Id.createLinkId("EF"));

		CHRouterTimeDep router = buildChRouterTimeDep(network);
		Path bounded = router.calcLeastCostPath(linkAB, linkEF, 0, null, null, 1000.0);
		assertNull(bounded);
	}
}
