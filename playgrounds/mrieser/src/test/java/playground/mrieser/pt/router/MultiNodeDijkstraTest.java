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

package playground.mrieser.pt.router;

import java.util.HashMap;
import java.util.Map;

import junit.framework.TestCase;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.mrieser.pt.router.MultiNodeDijkstra;
import playground.mrieser.pt.router.MultiNodeDijkstra.InitialNode;

/**
 * @author mrieser
 */
public class MultiNodeDijkstraTest extends TestCase {

	public void testMultipleStarts() {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 2.0, 2.0);
		tc.setData(new IdImpl(5), 1.0, 1.0);
		tc.setData(new IdImpl(6), 3.0, 3.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(new IdImpl(1)), new InitialNode(null, 1.0, 1.0));
		fromNodes.put(f.network.getNodes().get(new IdImpl(2)), new InitialNode(null, 3.0, 3.0));
		fromNodes.put(f.network.getNodes().get(new IdImpl(3)), new InitialNode(null, 2.0, 2.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(new IdImpl(5)), new InitialNode(null, 0.0, 0.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("1", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("5", p.links.get(2).getId().toString());

		// change costs
		tc.setData(new IdImpl(1), 2.0, 5.0);

		p = dijkstra.calcLeastCostPath(fromNodes, toNodes);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("2", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("5", p.links.get(2).getId().toString());

		// change costs again
		tc.setData(new IdImpl(1), 2.0, 1.0);

		p = dijkstra.calcLeastCostPath(fromNodes, toNodes);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("1", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("5", p.links.get(2).getId().toString());
	}

	public void testMultipleEnds() {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 4.0, 4.0);
		tc.setData(new IdImpl(5), 3.0, 3.0);
		tc.setData(new IdImpl(6), 7.0, 7.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(new IdImpl(2)), new InitialNode(f.network.getNodes().get(new IdImpl(2)), 0.0, 0.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(new IdImpl(4)), new InitialNode(f.network.getNodes().get(new IdImpl(4)), 5.0, 5.0));
		toNodes.put(f.network.getNodes().get(new IdImpl(5)), new InitialNode(f.network.getNodes().get(new IdImpl(5)), 4.0, 4.0));
		toNodes.put(f.network.getNodes().get(new IdImpl(6)), new InitialNode(f.network.getNodes().get(new IdImpl(6)), 1.0, 1.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("2", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("5", p.links.get(2).getId().toString());

		// change costs
		tc.setData(new IdImpl(4), 3.0, 1.0);

		p = dijkstra.calcLeastCostPath(fromNodes, toNodes);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("2", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("4", p.links.get(2).getId().toString());

		// change costs again
		tc.setData(new IdImpl(6), 7.0, 3.0);

		p = dijkstra.calcLeastCostPath(fromNodes, toNodes);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("2", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("6", p.links.get(2).getId().toString());
	}

	public void testMultipleStartsAndEnds() {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 4.0, 4.0);
		tc.setData(new IdImpl(5), 3.0, 3.0);
		tc.setData(new IdImpl(6), 7.0, 7.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(new IdImpl(2)), new InitialNode(f.network.getNodes().get(new IdImpl(2)), 4.0, 4.0));
		fromNodes.put(f.network.getNodes().get(new IdImpl(3)), new InitialNode(f.network.getNodes().get(new IdImpl(3)), 3.0, 3.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(new IdImpl(4)), new InitialNode(f.network.getNodes().get(new IdImpl(4)), 5.0, 5.0));
		toNodes.put(f.network.getNodes().get(new IdImpl(5)), new InitialNode(f.network.getNodes().get(new IdImpl(5)), 4.0, 4.0));
		toNodes.put(f.network.getNodes().get(new IdImpl(6)), new InitialNode(f.network.getNodes().get(new IdImpl(6)), 1.0, 1.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("2", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("5", p.links.get(2).getId().toString());

		// change costs
		tc.setData(new IdImpl(3), 3.0, 1.0);
		tc.setData(new IdImpl(4), 3.0, 1.0);

		p = dijkstra.calcLeastCostPath(fromNodes, toNodes);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("3", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("4", p.links.get(2).getId().toString());

		// change costs again
		tc.setData(new IdImpl(3), 3.0, 4.0);
		tc.setData(new IdImpl(6), 7.0, 3.0);

		p = dijkstra.calcLeastCostPath(fromNodes, toNodes);
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
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 2.0, 2.0);
		tc.setData(new IdImpl(5), 1.0, 1.0);
		tc.setData(new IdImpl(6), 3.0, 3.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(new IdImpl(1)), new InitialNode(f.network.getNodes().get(new IdImpl(1)), 1.0, 1.0));
		fromNodes.put(f.network.getNodes().get(new IdImpl(4)), new InitialNode(f.network.getNodes().get(new IdImpl(4)), 4.0, 4.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(new IdImpl(5)), new InitialNode(f.network.getNodes().get(new IdImpl(5)), 0.0, 0.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("1", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("5", p.links.get(2).getId().toString());
	}

	public void testEndViaFaster() {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 2.0, 2.0);
		tc.setData(new IdImpl(5), 1.0, 1.0);
		tc.setData(new IdImpl(6), 3.0, 3.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(new IdImpl(2)), new InitialNode(f.network.getNodes().get(new IdImpl(2)), 1.0, 1.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(new IdImpl(8)), new InitialNode(f.network.getNodes().get(new IdImpl(8)), 3.0, 3.0));
		toNodes.put(f.network.getNodes().get(new IdImpl(5)), new InitialNode(f.network.getNodes().get(new IdImpl(5)), 1.0, 1.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("2", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("5", p.links.get(2).getId().toString());
	}

	public void testOnlyFromToSameNode() {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 2.0, 2.0);
		tc.setData(new IdImpl(5), 1.0, 1.0);
		tc.setData(new IdImpl(6), 3.0, 3.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(new IdImpl(2)), new InitialNode(f.network.getNodes().get(new IdImpl(2)), 1.0, 1.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(new IdImpl(2)), new InitialNode(f.network.getNodes().get(new IdImpl(2)), 3.0, 3.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes);
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
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 2.0, 2.0);
		tc.setData(new IdImpl(5), 1.0, 1.0);
		tc.setData(new IdImpl(6), 3.0, 3.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(new IdImpl(2)), new InitialNode(f.network.getNodes().get(new IdImpl(2)), 2.0, 2.0));
		fromNodes.put(f.network.getNodes().get(new IdImpl(4)), new InitialNode(f.network.getNodes().get(new IdImpl(4)), 1.0, 1.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(new IdImpl(4)), new InitialNode(f.network.getNodes().get(new IdImpl(4)), 1.0, 1.0));
		toNodes.put(f.network.getNodes().get(new IdImpl(6)), new InitialNode(f.network.getNodes().get(new IdImpl(6)), 3.0, 3.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes);
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
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 2.0, 2.0);
		tc.setData(new IdImpl(5), 1.0, 1.0);
		tc.setData(new IdImpl(6), 3.0, 3.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(new IdImpl(2)), new InitialNode(f.network.getNodes().get(new IdImpl(2)), 2.0, 2.0));
		fromNodes.put(f.network.getNodes().get(new IdImpl(4)), new InitialNode(f.network.getNodes().get(new IdImpl(4)), 10.0, 10.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(new IdImpl(4)), new InitialNode(f.network.getNodes().get(new IdImpl(4)), 8.0, 8.0));
		toNodes.put(f.network.getNodes().get(new IdImpl(6)), new InitialNode(f.network.getNodes().get(new IdImpl(6)), 3.0, 3.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes);
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
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 2.0, 2.0);
		tc.setData(new IdImpl(5), 1.0, 1.0);
		tc.setData(new IdImpl(6), 3.0, 3.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(new IdImpl(2)), new InitialNode(f.network.getNodes().get(new IdImpl(2)), 2.0, 2.0));
		fromNodes.put(f.network.getNodes().get(new IdImpl(1)), new InitialNode(f.network.getNodes().get(new IdImpl(1)), 3.0, 3.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(new IdImpl(5)), new InitialNode(f.network.getNodes().get(new IdImpl(5)), 1.0, 1.0));
		toNodes.put(f.network.getNodes().get(new IdImpl(3)), new InitialNode(f.network.getNodes().get(new IdImpl(3)), 3.0, 3.0)); // cannot be reached!

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes);
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
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 2.0, 2.0);
		tc.setData(new IdImpl(5), 1.0, 1.0);
		tc.setData(new IdImpl(6), 3.0, 3.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(new IdImpl(2)), new InitialNode(f.network.getNodes().get(new IdImpl(2)), 2.0, 2.0));
		fromNodes.put(f.network.getNodes().get(new IdImpl(4)), new InitialNode(f.network.getNodes().get(new IdImpl(4)), 3.0, 3.0)); // cannot lead to 5 or 6
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(new IdImpl(5)), new InitialNode(f.network.getNodes().get(new IdImpl(5)), 1.0, 1.0));
		toNodes.put(f.network.getNodes().get(new IdImpl(6)), new InitialNode(f.network.getNodes().get(new IdImpl(6)), 3.0, 3.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes);
		assertNotNull("no path found!", p);
		assertEquals(3, p.links.size());
		assertEquals("2", p.links.get(0).getId().toString());
		assertEquals("7", p.links.get(1).getId().toString());
		assertEquals("5", p.links.get(2).getId().toString());
	}

	public void testImpossibleRoute() {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(new IdImpl(1), 2.0, 2.0);
		tc.setData(new IdImpl(2), 1.0, 1.0);
		tc.setData(new IdImpl(3), 3.0, 3.0);
		tc.setData(new IdImpl(4), 2.0, 2.0);
		tc.setData(new IdImpl(5), 1.0, 1.0);
		tc.setData(new IdImpl(6), 3.0, 3.0);
		tc.setData(new IdImpl(7), 4.0, 4.0);
		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
		fromNodes.put(f.network.getNodes().get(new IdImpl(1)), new InitialNode(f.network.getNodes().get(new IdImpl(1)), 1.0, 1.0));
		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
		toNodes.put(f.network.getNodes().get(new IdImpl(2)), new InitialNode(f.network.getNodes().get(new IdImpl(2)), 3.0, 3.0));

		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes);
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
		/*package*/ NetworkLayer network;

		public Fixture() {
			this.network = new NetworkLayer();
			Node node1 = this.network.createAndAddNode(new IdImpl(1), new CoordImpl(1000,    0));
			Node node2 = this.network.createAndAddNode(new IdImpl(2), new CoordImpl( 500,    0));
			Node node3 = this.network.createAndAddNode(new IdImpl(3), new CoordImpl(   0,    0));
			Node node4 = this.network.createAndAddNode(new IdImpl(4), new CoordImpl(1000, 2000));
			Node node5 = this.network.createAndAddNode(new IdImpl(5), new CoordImpl( 500, 2000));
			Node node6 = this.network.createAndAddNode(new IdImpl(6), new CoordImpl(   0, 2000));
			Node node7 = this.network.createAndAddNode(new IdImpl(7), new CoordImpl( 500,  500));
			Node node8 = this.network.createAndAddNode(new IdImpl(8), new CoordImpl( 500, 1500));
			this.network.createAndAddLink(new IdImpl(1), node1, node7, 1000.0, 10.0, 2000.0, 1);
			this.network.createAndAddLink(new IdImpl(2), node2, node7, 1000.0, 10.0, 2000.0, 1);
			this.network.createAndAddLink(new IdImpl(3), node3, node7, 1000.0, 10.0, 2000.0, 1);
			this.network.createAndAddLink(new IdImpl(4), node8, node4, 1000.0, 10.0, 2000.0, 1);
			this.network.createAndAddLink(new IdImpl(5), node8, node5, 1000.0, 10.0, 2000.0, 1);
			this.network.createAndAddLink(new IdImpl(6), node8, node6, 1000.0, 10.0, 2000.0, 1);
			this.network.createAndAddLink(new IdImpl(7), node7, node8, 1000.0, 10.0, 2000.0, 1);
		}
	}

	/*package*/ static class TestTimeCost implements TravelTime, TravelCost {

		private final Map<Id, Double> travelTimes = new HashMap<Id, Double>();
		private final Map<Id, Double> travelCosts = new HashMap<Id, Double>();

		public void setData(final Id id, final double travelTime, final double travelCost) {
			this.travelTimes.put(id, Double.valueOf(travelTime));
			this.travelCosts.put(id, Double.valueOf(travelCost));
		}

		public double getLinkTravelTime(final Link link, final double time) {
			return this.travelTimes.get(link.getId()).doubleValue();
		}

		public double getLinkTravelCost(final Link link, final double time) {
			return this.travelCosts.get(link.getId()).doubleValue();
		}

	}

}
