/* *********************************************************************** *
 * project: org.matsim.*
 * MultiNodeDijkstraTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.core.router;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.Person;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.vehicles.Vehicle;

/**
 * Use the same test cases as for the PT MultiNodeDijkstra.
 * 
 * @author cdobler
 */
public class MultiNodeDijkstraTest {

	private final static Logger log = LogManager.getLogger(MultiNodeDijkstraTest.class);

	private MultiNodeDijkstra makeMultiNodeDikstra(Network network, TravelDisutility travelDisutility, TravelTime travelTime,
			boolean fastRouter) {
		if (fastRouter) {
			return (MultiNodeDijkstra) new FastMultiNodeDijkstraFactory().createPathCalculator(network, travelDisutility, travelTime);
		} else return (MultiNodeDijkstra) new MultiNodeDijkstraFactory().createPathCalculator(network, travelDisutility, travelTime);
	}

	@Test
	void testMultipleStarts() {
		testMultipleStarts(true);
		testMultipleStarts(false);
	}
	
	public void testMultipleStarts(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 2.0, 2.0);
		tc.setData(Id.create(5, Link.class), 1.0, 1.0);
		tc.setData(Id.create(6, Link.class), 3.0, 3.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(1, Node.class)), 1.0, 1.0));
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(2, Node.class)), 3.0, 3.0));
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(3, Node.class)), 2.0, 2.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(5, Node.class)), 0.0, 0.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);

		Assertions.assertNotNull(p, "no path found!");
		Assertions.assertEquals(3, p.links.size());
		Assertions.assertEquals("1", p.links.get(0).getId().toString());
		Assertions.assertEquals("7", p.links.get(1).getId().toString());
		Assertions.assertEquals("5", p.links.get(2).getId().toString());

		// change costs
		tc.setData(Id.create(1, Link.class), 2.0, 5.0);

		p = createPath(dijkstra, fromNode, toNode);
		Assertions.assertNotNull(p, "no path found!");
		Assertions.assertEquals(3, p.links.size());
		Assertions.assertEquals("2", p.links.get(0).getId().toString());
		Assertions.assertEquals("7", p.links.get(1).getId().toString());
		Assertions.assertEquals("5", p.links.get(2).getId().toString());

		// change costs again
		tc.setData(Id.create(1, Link.class), 2.0, 1.0);

		p = createPath(dijkstra, fromNode, toNode);
		Assertions.assertNotNull(p, "no path found!");
		Assertions.assertEquals(3, p.links.size());
		Assertions.assertEquals("1", p.links.get(0).getId().toString());
		Assertions.assertEquals("7", p.links.get(1).getId().toString());
		Assertions.assertEquals("5", p.links.get(2).getId().toString());
	}

	@Test
	void testMultipleEnds() {
		testMultipleEnds(true);
		testMultipleEnds(false);
	}
	
	public void testMultipleEnds(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 4.0, 4.0);
		tc.setData(Id.create(5, Link.class), 3.0, 3.0);
		tc.setData(Id.create(6, Link.class), 7.0, 7.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(2, Node.class)), 0.0, 0.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(4, Node.class)), 5.0, 5.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(5, Node.class)), 4.0, 4.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(6, Node.class)), 1.0, 1.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
		Assertions.assertNotNull(p, "no path found!");
		Assertions.assertEquals(3, p.links.size());
		Assertions.assertEquals("2", p.links.get(0).getId().toString());
		Assertions.assertEquals("7", p.links.get(1).getId().toString());
		Assertions.assertEquals("5", p.links.get(2).getId().toString());

		// change costs
		tc.setData(Id.create(4, Link.class), 3.0, 1.0);

		p = createPath(dijkstra, fromNode, toNode);
		Assertions.assertNotNull(p, "no path found!");
		Assertions.assertEquals(3, p.links.size());
		Assertions.assertEquals("2", p.links.get(0).getId().toString());
		Assertions.assertEquals("7", p.links.get(1).getId().toString());
		Assertions.assertEquals("4", p.links.get(2).getId().toString());

		// change costs again
		tc.setData(Id.create(6, Link.class), 7.0, 3.0);

		p = createPath(dijkstra, fromNode, toNode);
		Assertions.assertNotNull(p, "no path found!");
		Assertions.assertEquals(3, p.links.size());
		Assertions.assertEquals("2", p.links.get(0).getId().toString());
		Assertions.assertEquals("7", p.links.get(1).getId().toString());
		Assertions.assertEquals("6", p.links.get(2).getId().toString());
	}

	@Test
	void testMultipleStartsAndEnds() {
		testMultipleStartsAndEnds(true);
		testMultipleStartsAndEnds(false);
	}
	
	public void testMultipleStartsAndEnds(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 4.0, 4.0);
		tc.setData(Id.create(5, Link.class), 3.0, 3.0);
		tc.setData(Id.create(6, Link.class), 7.0, 7.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(2, Node.class)), 4.0, 4.0));
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(3, Node.class)), 3.0, 3.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(4, Node.class)), 5.0, 5.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(5, Node.class)), 4.0, 4.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(6, Node.class)), 1.0, 1.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);

		Assertions.assertNotNull(p, "no path found!");
		Assertions.assertEquals(3, p.links.size());
		Assertions.assertEquals("2", p.links.get(0).getId().toString());
		Assertions.assertEquals("7", p.links.get(1).getId().toString());
		Assertions.assertEquals("5", p.links.get(2).getId().toString());

		// change costs
		tc.setData(Id.create(3, Link.class), 3.0, 1.0);
		tc.setData(Id.create(4, Link.class), 3.0, 1.0);

		p = createPath(dijkstra, fromNode, toNode);
		Assertions.assertNotNull(p, "no path found!");
		Assertions.assertEquals(3, p.links.size());
		Assertions.assertEquals("3", p.links.get(0).getId().toString());
		Assertions.assertEquals("7", p.links.get(1).getId().toString());
		Assertions.assertEquals("4", p.links.get(2).getId().toString());

		// change costs again
		tc.setData(Id.create(3, Link.class), 3.0, 4.0);
		tc.setData(Id.create(6, Link.class), 7.0, 3.0);

		p = createPath(dijkstra, fromNode, toNode);
		Assertions.assertNotNull(p, "no path found!");
		Assertions.assertEquals(3, p.links.size());
		Assertions.assertEquals("2", p.links.get(0).getId().toString());
		Assertions.assertEquals("7", p.links.get(1).getId().toString());
		Assertions.assertEquals("6", p.links.get(2).getId().toString());
	}

	@Test
	void testStartViaFaster() {
		testStartViaFaster(true);
		testStartViaFaster(false);
	}
	
	/**
	 * Both nodes 1 and 4 are part of the start set. Even if the path from 1 to the
	 * target leads over node 4, it may be faster, due to the intial cost values.
	 * Test that the route does not cut at node 4 as the first node backwards from
	 * the start set.
	 */
	public void testStartViaFaster(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 2.0, 2.0);
		tc.setData(Id.create(5, Link.class), 1.0, 1.0);
		tc.setData(Id.create(6, Link.class), 3.0, 3.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(1, Node.class)), 1.0, 1.0));
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(4, Node.class)), 4.0, 4.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(5, Node.class)), 0.0, 0.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
		Assertions.assertNotNull(p, "no path found!");
		Assertions.assertEquals(3, p.links.size());
		Assertions.assertEquals("1", p.links.get(0).getId().toString());
		Assertions.assertEquals("7", p.links.get(1).getId().toString());
		Assertions.assertEquals("5", p.links.get(2).getId().toString());
	}

	@Test
	void testEndViaFaster() {
		testEndViaFaster(true);
		testEndViaFaster(false);
	}
	
	public void testEndViaFaster(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 2.0, 2.0);
		tc.setData(Id.create(5, Link.class), 1.0, 1.0);
		tc.setData(Id.create(6, Link.class), 3.0, 3.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(2, Node.class)), 1.0, 1.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(8, Node.class)), 3.0, 3.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(5, Node.class)), 1.0, 1.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
//		MultiNodeDijkstra dijkstra = new MultiNodeDijkstra(f.network, tc, tc);
//		Map<Node, InitialNode> fromNodes = new HashMap<Node, InitialNode>();
//		fromNodes.put(f.network.getNodes().get(Id.create(2)), new InitialNode(1.0, 1.0));
//		Map<Node, InitialNode> toNodes = new HashMap<Node, InitialNode>();
//		toNodes.put(f.network.getNodes().get(Id.create(8)), new InitialNode(3.0, 3.0));
//		toNodes.put(f.network.getNodes().get(Id.create(5)), new InitialNode(1.0, 1.0));
//
//		Path p = dijkstra.calcLeastCostPath(fromNodes, toNodes, null);
		Assertions.assertNotNull(p, "no path found!");
		Assertions.assertEquals(3, p.links.size());
		Assertions.assertEquals("2", p.links.get(0).getId().toString());
		Assertions.assertEquals("7", p.links.get(1).getId().toString());
		Assertions.assertEquals("5", p.links.get(2).getId().toString());
	}

	@Test
	void testOnlyFromToSameNode() {
		testOnlyFromToSameNode(true);
		testOnlyFromToSameNode(false);
	}
	
	public void testOnlyFromToSameNode(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 2.0, 2.0);
		tc.setData(Id.create(5, Link.class), 1.0, 1.0);
		tc.setData(Id.create(6, Link.class), 3.0, 3.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(2, Node.class)), 1.0, 1.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(2, Node.class)), 3.0, 3.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
		Assertions.assertNotNull(p, "no path found!");
		Assertions.assertEquals(0, p.links.size());
		Assertions.assertEquals(1, p.nodes.size());
		Assertions.assertEquals("2", p.nodes.get(0).getId().toString());
	}

	@Test
	void testSameNodeInFromToSetCheapest() {
		testSameNodeInFromToSetCheapest(true);
		testSameNodeInFromToSetCheapest(false);
	}
	
	/**
	 * Tests that a path is found if some links are in the set of start
	 * as well as in the set of end nodes and the path only containing
	 * of this node is the cheapest.
	 */
	public void testSameNodeInFromToSetCheapest(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 2.0, 2.0);
		tc.setData(Id.create(5, Link.class), 1.0, 1.0);
		tc.setData(Id.create(6, Link.class), 3.0, 3.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(2, Node.class)), 2.0, 2.0));
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(4, Node.class)), 1.0, 1.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(4, Node.class)), 1.0, 1.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(6, Node.class)), 3.0, 3.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
		Assertions.assertNotNull(p, "no path found!");
		Assertions.assertEquals(0, p.links.size());
		Assertions.assertEquals(1, p.nodes.size());
		Assertions.assertEquals("4", p.nodes.get(0).getId().toString());
	}

	@Test
	void testSameNodeInFromToSetNotCheapest() {
		testSameNodeInFromToSetNotCheapest(true);
		testSameNodeInFromToSetNotCheapest(false);
	}
	
	/**
	 * Tests that a path is found if some links are in the set of start
	 * as well as in the set of end nodes, but the path only containing
	 * of this node is the not the cheapest.
	 */
	public void testSameNodeInFromToSetNotCheapest(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 2.0, 2.0);
		tc.setData(Id.create(5, Link.class), 1.0, 1.0);
		tc.setData(Id.create(6, Link.class), 3.0, 3.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(2, Node.class)), 2.0, 2.0));
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(4, Node.class)), 10.0, 10.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(4, Node.class)), 8.0, 8.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(6, Node.class)), 3.0, 3.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
		Assertions.assertNotNull(p, "no path found!");
		Assertions.assertEquals(3, p.links.size());
		Assertions.assertEquals("2", p.links.get(0).getId().toString());
		Assertions.assertEquals("7", p.links.get(1).getId().toString());
		Assertions.assertEquals("6", p.links.get(2).getId().toString());
	}

	@Test
	void testSomeEndNodesNotReachable() {
		testSomeEndNodesNotReachable(true);
		testSomeEndNodesNotReachable(false);
	}
	
	/**
	 * Tests that a route is found even if not all given end nodes are reachable
	 */
	public void testSomeEndNodesNotReachable(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 2.0, 2.0);
		tc.setData(Id.create(5, Link.class), 1.0, 1.0);
		tc.setData(Id.create(6, Link.class), 3.0, 3.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(2, Node.class)), 2.0, 2.0));
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(1, Node.class)), 3.0, 3.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(5, Node.class)), 1.0, 1.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(3, Node.class)), 3.0, 3.0)); // cannot be reached!
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
		Assertions.assertNotNull(p, "no path found!");
		Assertions.assertEquals(3, p.links.size());
		Assertions.assertEquals("2", p.links.get(0).getId().toString());
		Assertions.assertEquals("7", p.links.get(1).getId().toString());
		Assertions.assertEquals("5", p.links.get(2).getId().toString());
	}

	@Test
	void testSomeStartNodesNotUseable() {
		testSomeStartNodesNotUseable(true);
		testSomeStartNodesNotUseable(false);
	}
	
	/**
	 * Tests that a route is found even if not all given start nodes lead to an end node
	 */
	public void testSomeStartNodesNotUseable(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 2.0, 2.0);
		tc.setData(Id.create(5, Link.class), 1.0, 1.0);
		tc.setData(Id.create(6, Link.class), 3.0, 3.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(2, Node.class)), 2.0, 2.0));
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(4, Node.class)), 3.0, 3.0)); // cannot lead to 5 or 6
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(5, Node.class)), 1.0, 1.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(3, Node.class)), 3.0, 3.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
		Assertions.assertNotNull(p, "no path found!");
		Assertions.assertEquals(3, p.links.size());
		Assertions.assertEquals("2", p.links.get(0).getId().toString());
		Assertions.assertEquals("7", p.links.get(1).getId().toString());
		Assertions.assertEquals("5", p.links.get(2).getId().toString());
	}

	@Test
	void testImpossibleRoute() {
		testImpossibleRoute(true);
		testImpossibleRoute(false);
	}
	
	public void testImpossibleRoute(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 2.0, 2.0);
		tc.setData(Id.create(2, Link.class), 1.0, 1.0);
		tc.setData(Id.create(3, Link.class), 3.0, 3.0);
		tc.setData(Id.create(4, Link.class), 2.0, 2.0);
		tc.setData(Id.create(5, Link.class), 1.0, 1.0);
		tc.setData(Id.create(6, Link.class), 3.0, 3.0);
		tc.setData(Id.create(7, Link.class), 4.0, 4.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(1, Node.class)), 1.0, 1.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(2, Node.class)), 3.0, 3.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
		Assertions.assertNull(p, "wow, impossible path found!");
	}

	/*
	 * Ensure that the initial time and cost values are not taken into
	 * account in the path.
	 */
	@Test
	void testInitialValuesCorrection() {
		testInitialValuesCorrection(true);
		testInitialValuesCorrection(false);
	}
	
	public void testInitialValuesCorrection(boolean fastRouter) {
		Fixture f = new Fixture();
		TestTimeCost tc = new TestTimeCost();
		tc.setData(Id.create(1, Link.class), 100.0, 200.0);
		tc.setData(Id.create(2, Link.class), 100.0, 200.0);
		tc.setData(Id.create(3, Link.class), 100.0, 200.0);
		tc.setData(Id.create(4, Link.class), 100.0, 200.0);
		tc.setData(Id.create(5, Link.class), 100.0, 200.0);
		tc.setData(Id.create(6, Link.class), 100.0, 200.0);
		tc.setData(Id.create(7, Link.class), 100.0, 200.0);
		
		MultiNodeDijkstra dijkstra = makeMultiNodeDikstra(f.network, tc, tc, fastRouter);
		List<InitialNode> fromNodes = new ArrayList<InitialNode>();
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		
		fromNodes.add(new InitialNode(f.network.getNodes().get(Id.create(1, Node.class)), 10000.0, 10000.0));
		toNodes.add(new InitialNode(f.network.getNodes().get(Id.create(6, Node.class)), 20000.0, 20000.0));
		
		Node fromNode = dijkstra.createImaginaryNode(fromNodes);
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		Path p = createPath(dijkstra, fromNode, toNode);
		
		Assertions.assertNotNull(p);
		Assertions.assertEquals(300.0, p.travelTime, 0.0);
		Assertions.assertEquals(600.0, p.travelCost, 0.0);
	}
	
	/*package*/ static Path createPath(Dijkstra dijsktra, Node fromNode, Node toNode) {
		Path path = dijsktra.calcLeastCostPath(fromNode, toNode, 0., null, null);
		
		if (path == null) return path;
		
		for(Node node : path.nodes) log.info("\t\t" + node.getId());
		for(Link link : path.links) log.info("\t\t" + link.getId());
		log.info(path.travelCost);
		log.info(path.travelTime);
		
		return path;
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
		/*package*/ Network network;

		public Fixture() {
			this.network = (Network) NetworkUtils.createNetwork();
			Node node1 = NetworkUtils.createAndAddNode(this.network, Id.create(1, Node.class), new Coord((double) 1000, (double) 0));
			Node node2 = NetworkUtils.createAndAddNode(this.network, Id.create(2, Node.class), new Coord((double) 500, (double) 0));
			Node node3 = NetworkUtils.createAndAddNode(this.network, Id.create(3, Node.class), new Coord((double) 0, (double) 0));
			Node node4 = NetworkUtils.createAndAddNode(this.network, Id.create(4, Node.class), new Coord((double) 1000, (double) 2000));
			Node node5 = NetworkUtils.createAndAddNode(this.network, Id.create(5, Node.class), new Coord((double) 500, (double) 2000));
			Node node6 = NetworkUtils.createAndAddNode(this.network, Id.create(6, Node.class), new Coord((double) 0, (double) 2000));
			Node node7 = NetworkUtils.createAndAddNode(this.network, Id.create(7, Node.class), new Coord((double) 500, (double) 500));
			Node node8 = NetworkUtils.createAndAddNode(this.network, Id.create(8, Node.class), new Coord((double) 500, (double) 1500));
			final Node fromNode = node1;
			final Node toNode = node7;
			NetworkUtils.createAndAddLink(this.network,Id.create(1, Link.class), fromNode, toNode, 1000.0, 10.0, 2000.0, (double) 1 );
			final Node fromNode1 = node2;
			final Node toNode1 = node7;
			NetworkUtils.createAndAddLink(this.network,Id.create(2, Link.class), fromNode1, toNode1, 1000.0, 10.0, 2000.0, (double) 1 );
			final Node fromNode2 = node3;
			final Node toNode2 = node7;
			NetworkUtils.createAndAddLink(this.network,Id.create(3, Link.class), fromNode2, toNode2, 1000.0, 10.0, 2000.0, (double) 1 );
			final Node fromNode3 = node8;
			final Node toNode3 = node4;
			NetworkUtils.createAndAddLink(this.network,Id.create(4, Link.class), fromNode3, toNode3, 1000.0, 10.0, 2000.0, (double) 1 );
			final Node fromNode4 = node8;
			final Node toNode4 = node5;
			NetworkUtils.createAndAddLink(this.network,Id.create(5, Link.class), fromNode4, toNode4, 1000.0, 10.0, 2000.0, (double) 1 );
			final Node fromNode5 = node8;
			final Node toNode5 = node6;
			NetworkUtils.createAndAddLink(this.network,Id.create(6, Link.class), fromNode5, toNode5, 1000.0, 10.0, 2000.0, (double) 1 );
			final Node fromNode6 = node7;
			final Node toNode6 = node8;
			NetworkUtils.createAndAddLink(this.network,Id.create(7, Link.class), fromNode6, toNode6, 1000.0, 10.0, 2000.0, (double) 1 );
		}
	}

	/*package*/ static class TestTimeCost implements TravelTime, TravelDisutility {

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
		public double getLinkTravelDisutility(Link link, double time, Person person, Vehicle vehicle) {
			return this.travelCosts.get(link.getId()).doubleValue();
		}

		@Override
		public double getLinkMinimumTravelDisutility(Link link) {
			return 0;
		}

	}
}
