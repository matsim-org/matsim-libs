/* *********************************************************************** *
 * project: org.matsim.*
 * TransitDijkstraTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.pt.router;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.pt.router.MultiNodeDijkstra.InitialNode;
import org.matsim.vehicles.Vehicle;

/**
 * @author mrieser
 */
public class MultiNodeDijkstraTest extends TestCase {

	public void testMultipleStarts() {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 2.0, 2.0);
		tc.setData(Id.create(5, Link.class), 1.0, 1.0);
		tc.setData(Id.create(6, Link.class), 3.0, 3.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(Id.create(1, Node.class)), new InitialNode(1.0, 1.0));
		fromNodes.put(f.network.getNodes().get(Id.create(2, Node.class)), new InitialNode(3.0, 3.0));
		fromNodes.put(f.network.getNodes().get(Id.create(3, Node.class)), new InitialNode(2.0, 2.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(Id.create(5, Node.class)), new InitialNode(0.0, 0.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("1", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("5", p.links.get(2).getId().toString());

		// change costs
		tc.setData(Id.create(1, Link.class), 2.0, 5.0);

		p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("2", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("5", p.links.get(2).getId().toString());

		// change costs again
		tc.setData(Id.create(1, Link.class), 2.0, 1.0);

		p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("1", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("5", p.links.get(2).getId().toString());
	}

	public void testMultipleEnds() {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 4.0, 4.0);
		tc.setData(Id.create(5, Link.class), 3.0, 3.0);
		tc.setData(Id.create(6, Link.class), 7.0, 7.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(Id.create(2, Node.class)), new InitialNode(0.0, 0.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(Id.create(4, Node.class)), new InitialNode(5.0, 5.0));
		toNodes.put(f.network.getNodes().get(Id.create(5, Node.class)), new InitialNode(4.0, 4.0));
		toNodes.put(f.network.getNodes().get(Id.create(6, Node.class)), new InitialNode(1.0, 1.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("2", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("5", p.links.get(2).getId().toString());

		// change costs
		tc.setData(Id.create(4, Link.class), 3.0, 1.0);

		p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("2", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("4", p.links.get(2).getId().toString());

		// change costs again
		tc.setData(Id.create(6, Link.class), 7.0, 3.0);

		p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("2", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("6", p.links.get(2).getId().toString());
	}

	public void testMultipleStartsAndEnds() {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 4.0, 4.0);
		tc.setData(Id.create(5, Link.class), 3.0, 3.0);
		tc.setData(Id.create(6, Link.class), 7.0, 7.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(Id.create(2, Node.class)), new InitialNode(4.0, 4.0));
		fromNodes.put(f.network.getNodes().get(Id.create(3, Node.class)), new InitialNode(3.0, 3.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(Id.create(4, Node.class)), new InitialNode(5.0, 5.0));
		toNodes.put(f.network.getNodes().get(Id.create(5, Node.class)), new InitialNode(4.0, 4.0));
		toNodes.put(f.network.getNodes().get(Id.create(6, Node.class)), new InitialNode(1.0, 1.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("2", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("5", p.links.get(2).getId().toString());

		// change costs
		tc.setData(Id.create(3, Link.class), 3.0, 1.0);
		tc.setData(Id.create(4, Link.class), 3.0, 1.0);

		p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("3", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("4", p.links.get(2).getId().toString());

		// change costs again
		tc.setData(Id.create(3, Link.class), 3.0, 4.0);
		tc.setData(Id.create(6, Link.class), 7.0, 3.0);

		p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("2", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("6", p.links.get(2).getId().toString());
	}

	/**
	 * Both nodes 1 and 4 are part of the start set. Even if the path from 1 to the
	 * target leads over node 4, it may be faster, due to the intial cost values.
	 * Test that the route does not cut at node 4 as the first node backwards from
	 * the start set.
	 */
	public void testStartViaFaster() {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 2.0, 2.0);
		tc.setData(Id.create(5, Link.class), 1.0, 1.0);
		tc.setData(Id.create(6, Link.class), 3.0, 3.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(Id.create(1, Node.class)), new InitialNode(1.0, 1.0));
		fromNodes.put(f.network.getNodes().get(Id.create(4, Node.class)), new InitialNode(4.0, 4.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(Id.create(5, Node.class)), new InitialNode(0.0, 0.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("1", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("5", p.links.get(2).getId().toString());
	}

	public void testEndViaFaster() {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 2.0, 2.0);
		tc.setData(Id.create(5, Link.class), 1.0, 1.0);
		tc.setData(Id.create(6, Link.class), 3.0, 3.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(Id.create(2, Node.class)), new InitialNode(1.0, 1.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(Id.create(8, Node.class)), new InitialNode(3.0, 3.0));
		toNodes.put(f.network.getNodes().get(Id.create(5, Node.class)), new InitialNode(1.0, 1.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("2", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("5", p.links.get(2).getId().toString());
	}

	public void testOnlyFromToSameNode() {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 2.0, 2.0);
		tc.setData(Id.create(5, Link.class), 1.0, 1.0);
		tc.setData(Id.create(6, Link.class), 3.0, 3.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(Id.create(2, Node.class)), new InitialNode(1.0, 1.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(Id.create(2, Node.class)), new InitialNode(3.0, 3.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		assertNotNull("no path found!", p);
		assertEquals(0, p.links.size());
		assertEquals(1, p.nodes.size());
		assertEquals("2", p.nodes.get(0).getId().toString());
	}

	/**
	 * Tests that a path is found if some links are in the set of start
	 * as well as in the set of end nodes and the path only containing
	 * of this node is the cheapest.
	 */
	public void testSameNodeInFromToSetCheapest() {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 2.0, 2.0);
		tc.setData(Id.create(5, Link.class), 1.0, 1.0);
		tc.setData(Id.create(6, Link.class), 3.0, 3.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(Id.create(2, Node.class)), new InitialNode(2.0, 2.0));
		fromNodes.put(f.network.getNodes().get(Id.create(4, Node.class)), new InitialNode(1.0, 1.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(Id.create(4, Node.class)), new InitialNode(1.0, 1.0));
		toNodes.put(f.network.getNodes().get(Id.create(6, Node.class)), new InitialNode(3.0, 3.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		assertNotNull("no path found!", p);
		assertEquals(0, p.links.size());
		assertEquals(1, p.nodes.size());
		assertEquals("4", p.nodes.get(0).getId().toString());
	}

	/**
	 * Tests that a path is found if some links are in the set of start
	 * as well as in the set of end nodes, but the path only containing
	 * of this node is the not the cheapest.
	 */
	public void testSameNodeInFromToSetNotCheapest() {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 2.0, 2.0);
		tc.setData(Id.create(5, Link.class), 1.0, 1.0);
		tc.setData(Id.create(6, Link.class), 3.0, 3.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(Id.create(2, Node.class)), new InitialNode(2.0, 2.0));
		fromNodes.put(f.network.getNodes().get(Id.create(4, Node.class)), new InitialNode(10.0, 10.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(Id.create(4, Node.class)), new InitialNode(8.0, 8.0));
		toNodes.put(f.network.getNodes().get(Id.create(6, Node.class)), new InitialNode(3.0, 3.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("2", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("6", p.links.get(2).getId().toString());
	}

	/**
	 * Tests that a route is found even if not all given end nodes are reachable
	 */
	public void testSomeEndNodesNotReachable() {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 2.0, 2.0);
		tc.setData(Id.create(5, Link.class), 1.0, 1.0);
		tc.setData(Id.create(6, Link.class), 3.0, 3.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(Id.create(2, Node.class)), new InitialNode(2.0, 2.0));
		fromNodes.put(f.network.getNodes().get(Id.create(1, Node.class)), new InitialNode(3.0, 3.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(Id.create(5, Node.class)), new InitialNode(1.0, 1.0));
		toNodes.put(f.network.getNodes().get(Id.create(3, Node.class)), new InitialNode(3.0, 3.0)); // cannot be reached!

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("2", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("5", p.links.get(2).getId().toString());
	}

	/**
	 * Tests that a route is found even if not all given start nodes lead to an end node
	 */
	public void testSomeStartNodesNotUseable() {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 2.0, 2.0);
		tc.setData(Id.create(5, Link.class), 1.0, 1.0);
		tc.setData(Id.create(6, Link.class), 3.0, 3.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(Id.create(2, Node.class)), new InitialNode(2.0, 2.0));
		fromNodes.put(f.network.getNodes().get(Id.create(4, Node.class)), new InitialNode(3.0, 3.0)); // cannot lead to 5 or 6
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(Id.create(5, Node.class)), new InitialNode(1.0, 1.0));
		toNodes.put(f.network.getNodes().get(Id.create(6, Node.class)), new InitialNode(3.0, 3.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("2", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("5", p.links.get(2).getId().toString());
	}

	public void testImpossibleRoute() {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 2.0, 2.0);
		tc.setData(Id.create(5, Link.class), 1.0, 1.0);
		tc.setData(Id.create(6, Link.class), 3.0, 3.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(Id.create(1, Node.class)), new InitialNode(1.0, 1.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(Id.create(2, Node.class)), new InitialNode(3.0, 3.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		assertNull("wow, impossible path found!", p);
	}

	/**
	 * Creates a simple network to be used in tests.
	 *
	 * <pre>
	 *   (1)                       (4)
	 *      \                     /
	 *       \_1               4_/
	 *        \                 /
	 *   (2)-2-(7)-----7-----(8)-5-(5)
	 *        /                 \
	 *       /_3               6_\
	 *      /                     \
	 *   (3)                       (6)
	 * </pre>
	 *
	 * @author mrieser
	 */
	/*package*/ static class Fixture {
		/*package*/ NetworkImpl network;

		public Fixture() {
			this.network = NetworkImpl.createNetwork();
			Node node1 = this.network.createAndAddNode(Id.create(1, Node.class), new Coord((double) 1000, (double) 0));
			Node node2 = this.network.createAndAddNode(Id.create(2, Node.class), new Coord((double) 500, (double) 0));
			Node node3 = this.network.createAndAddNode(Id.create(3, Node.class), new Coord((double) 0, (double) 0));
			Node node4 = this.network.createAndAddNode(Id.create(4, Node.class), new Coord((double) 1000, (double) 2000));
			Node node5 = this.network.createAndAddNode(Id.create(5, Node.class), new Coord((double) 500, (double) 2000));
			Node node6 = this.network.createAndAddNode(Id.create(6, Node.class), new Coord((double) 0, (double) 2000));
			Node node7 = this.network.createAndAddNode(Id.create(7, Node.class), new Coord((double) 500, (double) 500));
			Node node8 = this.network.createAndAddNode(Id.create(8, Node.class), new Coord((double) 500, (double) 1500));
			this.network.createAndAddLink(Id.create(1, Link.class), node1, node7, 1000.0, 10.0, 2000.0, 1);
			this.network.createAndAddLink(Id.create(2, Link.class), node2, node7, 1000.0, 10.0, 2000.0, 1);
			this.network.createAndAddLink(Id.create(3, Link.class), node3, node7, 1000.0, 10.0, 2000.0, 1);
			this.network.createAndAddLink(Id.create(4, Link.class), node8, node4, 1000.0, 10.0, 2000.0, 1);
			this.network.createAndAddLink(Id.create(5, Link.class), node8, node5, 1000.0, 10.0, 2000.0, 1);
			this.network.createAndAddLink(Id.create(6, Link.class), node8, node6, 1000.0, 10.0, 2000.0, 1);
			this.network.createAndAddLink(Id.create(7, Link.class), node7, node8, 1000.0, 10.0, 2000.0, 1);
		}
	}

	/*package*/ static class TestTimeCost implements TravelTime, TransitTravelDisutility {

		private final Map<Id<Link>, Double> travelTimes = new HashMap<>();
		private final Map<Id<Link>, Double> travelCosts = new HashMap<>();

		public void setData(final Id<Link> id, final double travelTime, final double travelCost) {
			this.travelTimes.put(id, Double.valueOf(travelTime));
			this.travelCosts.put(id, Double.valueOf(travelCost));
		}

		@Override
		public double getLinkTravelTime(final Link link, final double time, Person person, Vehicle vehicle) {
			return this.travelTimes.get(link.getId()).doubleValue();
		}

		@Override
		public double getLinkTravelDisutility(final Link link, final double time, final Person person, final Vehicle vehicle, final CustomDataManager dataManager) {
			return this.travelCosts.get(link.getId()).doubleValue();
		}

		@Override
		public double getTravelTime(Person person, Coord coord, Coord toCoord) {
			return 0;
		}

		@Override
		public double getTravelDisutility(Person person, Coord coord, Coord toCoord) {
			return 0;
		}
		
		
		
	}

}
