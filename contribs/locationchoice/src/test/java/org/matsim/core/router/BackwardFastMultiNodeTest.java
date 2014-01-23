/* *********************************************************************** *
 * project: org.matsim.*
 * BackwardFastMultiNodeTest.java
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
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.BackwardsFastMultiNodeDijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

/**
 * @author cdobler
 */
public class BackwardFastMultiNodeTest {
	
	@Test
	public void testBackwardsFastMultiNodeDijkstra_OneToOne() {
		runTestBackwardsFastMultiNodeDijkstra_OneToOne(true);
		runTestBackwardsFastMultiNodeDijkstra_OneToOne(false);
	}
	
	private void runTestBackwardsFastMultiNodeDijkstra_OneToOne(boolean searchAllEndNodes) {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		createNetwork(scenario);

		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = new TravelTimeAndDistanceBasedTravelDisutility(travelTime, config.planCalcScore());
		BackwardFastMultiNodeDijkstra dijkstra = (BackwardFastMultiNodeDijkstra) new BackwardsFastMultiNodeDijkstraFactory(searchAllEndNodes).
				createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime);
		
		Node fromNode = scenario.getNetwork().getNodes().get(scenario.createId("n3"));
		Node toNode = scenario.getNetwork().getNodes().get(scenario.createId("n0"));
		
		/*
		 * test calcLeastCostPath method
		 */
		Path path = dijkstra.calcLeastCostPath(fromNode, toNode, 3600.0, null, null);
		
		Assert.assertEquals(1.0, path.travelCost, 0.0);
		Assert.assertEquals(300.0, path.travelTime, 0.0);
		
		Assert.assertEquals(4, path.nodes.size());
		Assert.assertEquals(scenario.createId("n0"), path.nodes.get(0).getId());
		Assert.assertEquals(scenario.createId("n1"), path.nodes.get(1).getId());
		Assert.assertEquals(scenario.createId("n2"), path.nodes.get(2).getId());
		Assert.assertEquals(scenario.createId("n3"), path.nodes.get(3).getId());
		
		Assert.assertEquals(3, path.links.size());
		Assert.assertEquals(scenario.createId("l0"), path.links.get(0).getId());
		Assert.assertEquals(scenario.createId("l1"), path.links.get(1).getId());
		Assert.assertEquals(scenario.createId("l2"), path.links.get(2).getId());
		
		/*
		 * test constructPath method which uses data from the previous routing operation
		 */
		path = dijkstra.constructPath(fromNode, toNode, 3600.0);
		
		Assert.assertEquals(1.0, path.travelCost, 0.0);
		Assert.assertEquals(300.0, path.travelTime, 0.0);
		
		Assert.assertEquals(4, path.nodes.size());
		Assert.assertEquals(scenario.createId("n0"), path.nodes.get(0).getId());
		Assert.assertEquals(scenario.createId("n1"), path.nodes.get(1).getId());
		Assert.assertEquals(scenario.createId("n2"), path.nodes.get(2).getId());
		Assert.assertEquals(scenario.createId("n3"), path.nodes.get(3).getId());
		
		Assert.assertEquals(3, path.links.size());
		Assert.assertEquals(scenario.createId("l0"), path.links.get(0).getId());
		Assert.assertEquals(scenario.createId("l1"), path.links.get(1).getId());
		Assert.assertEquals(scenario.createId("l2"), path.links.get(2).getId());
	}
	
	/*
	 * Search only cheapest to node. n5 should not be found.
	 */
	@Test
	public void testBackwardsFastMultiNodeDijkstra_OneToMany() {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		createNetwork(scenario);

		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = new TravelTimeAndDistanceBasedTravelDisutility(travelTime, config.planCalcScore());
		BackwardFastMultiNodeDijkstra dijkstra = (BackwardFastMultiNodeDijkstra) new BackwardsFastMultiNodeDijkstraFactory(false).
				createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime);
		
		Node fromNode = scenario.getNetwork().getNodes().get(scenario.createId("n3"));
		Node toNode1 = scenario.getNetwork().getNodes().get(scenario.createId("n0"));
		Node toNode2 = scenario.getNetwork().getNodes().get(scenario.createId("n4"));
		Node toNode3 = scenario.getNetwork().getNodes().get(scenario.createId("n5"));
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		toNodes.add(new InitialNode(toNode1, 0.0, 0.0));
		toNodes.add(new InitialNode(toNode2, 0.0, 0.0));
		toNodes.add(new InitialNode(toNode3, 0.0, 0.0));
		Node toNode = dijkstra.createImaginaryNode(toNodes);
		
		/*
		 * test calcLeastCostPath method
		 */
		Path path = dijkstra.calcLeastCostPath(fromNode, toNode, 3600.0, null, null);
		
		Assert.assertEquals(1.0, path.travelCost, 0.0);
		Assert.assertEquals(300.0, path.travelTime, 0.0);
		
		Assert.assertEquals(4, path.nodes.size());
		Assert.assertEquals(scenario.createId("n0"), path.nodes.get(0).getId());
		Assert.assertEquals(scenario.createId("n1"), path.nodes.get(1).getId());
		Assert.assertEquals(scenario.createId("n2"), path.nodes.get(2).getId());
		Assert.assertEquals(scenario.createId("n3"), path.nodes.get(3).getId());
		
		Assert.assertEquals(3, path.links.size());
		Assert.assertEquals(scenario.createId("l0"), path.links.get(0).getId());
		Assert.assertEquals(scenario.createId("l1"), path.links.get(1).getId());
		Assert.assertEquals(scenario.createId("l2"), path.links.get(2).getId());
		
		/*
		 * test constructPath method which uses data from the previous routing operation - toNode1
		 */
		path = dijkstra.constructPath(fromNode, toNode1, 3600.0);
		
		Assert.assertEquals(1.0, path.travelCost, 0.0);
		Assert.assertEquals(300.0, path.travelTime, 0.0);
		
		Assert.assertEquals(4, path.nodes.size());
		Assert.assertEquals(scenario.createId("n0"), path.nodes.get(0).getId());
		Assert.assertEquals(scenario.createId("n1"), path.nodes.get(1).getId());
		Assert.assertEquals(scenario.createId("n2"), path.nodes.get(2).getId());
		Assert.assertEquals(scenario.createId("n3"), path.nodes.get(3).getId());
		
		Assert.assertEquals(3, path.links.size());
		Assert.assertEquals(scenario.createId("l0"), path.links.get(0).getId());
		Assert.assertEquals(scenario.createId("l1"), path.links.get(1).getId());
		Assert.assertEquals(scenario.createId("l2"), path.links.get(2).getId());
		
		/*
		 * test constructPath method which uses data from the previous routing operation - toNode2
		 */
		path = dijkstra.constructPath(fromNode, toNode2, 3600.0);
		
		Assert.assertEquals(1.333, path.travelCost, 0.001);
		Assert.assertEquals(400.0, path.travelTime, 0.0);
		
		Assert.assertEquals(4, path.nodes.size());
		Assert.assertEquals(scenario.createId("n4"), path.nodes.get(0).getId());
		Assert.assertEquals(scenario.createId("n1"), path.nodes.get(1).getId());
		Assert.assertEquals(scenario.createId("n2"), path.nodes.get(2).getId());
		Assert.assertEquals(scenario.createId("n3"), path.nodes.get(3).getId());
		
		Assert.assertEquals(3, path.links.size());
		Assert.assertEquals(scenario.createId("l3"), path.links.get(0).getId());
		Assert.assertEquals(scenario.createId("l1"), path.links.get(1).getId());
		Assert.assertEquals(scenario.createId("l2"), path.links.get(2).getId());
		
		/*
		 * test constructPath method which uses data from the previous routing operation - toNode3
		 */
		path = dijkstra.constructPath(fromNode, toNode3, 3600.0);
		
		Assert.assertNull(null);
	}
	
	@Test
	public void testBackwardsFastMultiNodeDijkstra_OneToMany_SearchAllNodes() {
		
		Config config = ConfigUtils.createConfig();
		Scenario scenario = ScenarioUtils.createScenario(config);
		
		createNetwork(scenario);

		TravelTime travelTime = new FreeSpeedTravelTime();
		TravelDisutility travelDisutility = new TravelTimeAndDistanceBasedTravelDisutility(travelTime, config.planCalcScore());
		BackwardFastMultiNodeDijkstra dijkstra = (BackwardFastMultiNodeDijkstra) new BackwardsFastMultiNodeDijkstraFactory(true).
				createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime);
		
		Node fromNode = scenario.getNetwork().getNodes().get(scenario.createId("n3"));
		Node toNode1 = scenario.getNetwork().getNodes().get(scenario.createId("n0"));
		Node toNode2 = scenario.getNetwork().getNodes().get(scenario.createId("n4"));
		Node toNode3 = scenario.getNetwork().getNodes().get(scenario.createId("n5"));
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		toNodes.add(new InitialNode(toNode1, 0.0, 0.0));
		toNodes.add(new InitialNode(toNode2, 0.0, 0.0));
		toNodes.add(new InitialNode(toNode3, 0.0, 0.0));
		Node toNode = dijkstra.createImaginaryNode(toNodes);
		
		/*
		 * test calcLeastCostPath method
		 */
		Path path = dijkstra.calcLeastCostPath(fromNode, toNode, 3600.0, null, null);
		
		Assert.assertEquals(1.0, path.travelCost, 0.0);
		Assert.assertEquals(300.0, path.travelTime, 0.0);
		
		Assert.assertEquals(4, path.nodes.size());
		Assert.assertEquals(scenario.createId("n0"), path.nodes.get(0).getId());
		Assert.assertEquals(scenario.createId("n1"), path.nodes.get(1).getId());
		Assert.assertEquals(scenario.createId("n2"), path.nodes.get(2).getId());
		Assert.assertEquals(scenario.createId("n3"), path.nodes.get(3).getId());
		
		Assert.assertEquals(3, path.links.size());
		Assert.assertEquals(scenario.createId("l0"), path.links.get(0).getId());
		Assert.assertEquals(scenario.createId("l1"), path.links.get(1).getId());
		Assert.assertEquals(scenario.createId("l2"), path.links.get(2).getId());
		
		/*
		 * test constructPath method which uses data from the previous routing operation - toNode1
		 */
		path = dijkstra.constructPath(fromNode, toNode1, 3600.0);
		
		Assert.assertEquals(1.0, path.travelCost, 0.0);
		Assert.assertEquals(300.0, path.travelTime, 0.0);
		
		Assert.assertEquals(4, path.nodes.size());
		Assert.assertEquals(scenario.createId("n0"), path.nodes.get(0).getId());
		Assert.assertEquals(scenario.createId("n1"), path.nodes.get(1).getId());
		Assert.assertEquals(scenario.createId("n2"), path.nodes.get(2).getId());
		Assert.assertEquals(scenario.createId("n3"), path.nodes.get(3).getId());
		
		Assert.assertEquals(3, path.links.size());
		Assert.assertEquals(scenario.createId("l0"), path.links.get(0).getId());
		Assert.assertEquals(scenario.createId("l1"), path.links.get(1).getId());
		Assert.assertEquals(scenario.createId("l2"), path.links.get(2).getId());
		
		/*
		 * test constructPath method which uses data from the previous routing operation - toNode2
		 */
		path = dijkstra.constructPath(fromNode, toNode2, 3600.0);
		
		Assert.assertEquals(1.333, path.travelCost, 0.001);
		Assert.assertEquals(400.0, path.travelTime, 0.0);
		
		Assert.assertEquals(4, path.nodes.size());
		Assert.assertEquals(scenario.createId("n4"), path.nodes.get(0).getId());
		Assert.assertEquals(scenario.createId("n1"), path.nodes.get(1).getId());
		Assert.assertEquals(scenario.createId("n2"), path.nodes.get(2).getId());
		Assert.assertEquals(scenario.createId("n3"), path.nodes.get(3).getId());
		
		Assert.assertEquals(3, path.links.size());
		Assert.assertEquals(scenario.createId("l3"), path.links.get(0).getId());
		Assert.assertEquals(scenario.createId("l1"), path.links.get(1).getId());
		Assert.assertEquals(scenario.createId("l2"), path.links.get(2).getId());
		
		/*
		 * test constructPath method which uses data from the previous routing operation - toNode3
		 */
		path = dijkstra.constructPath(fromNode, toNode3, 3600.0);
		
		Assert.assertEquals(2.0, path.travelCost, 0.0);
		Assert.assertEquals(600.0, path.travelTime, 0.0);
		
		Assert.assertEquals(5, path.nodes.size());
		Assert.assertEquals(scenario.createId("n5"), path.nodes.get(0).getId());
		Assert.assertEquals(scenario.createId("n4"), path.nodes.get(1).getId());
		Assert.assertEquals(scenario.createId("n1"), path.nodes.get(2).getId());
		Assert.assertEquals(scenario.createId("n2"), path.nodes.get(3).getId());
		Assert.assertEquals(scenario.createId("n3"), path.nodes.get(4).getId());
		
		Assert.assertEquals(4, path.links.size());
		Assert.assertEquals(scenario.createId("l4"), path.links.get(0).getId());
		Assert.assertEquals(scenario.createId("l3"), path.links.get(1).getId());
		Assert.assertEquals(scenario.createId("l1"), path.links.get(2).getId());
		Assert.assertEquals(scenario.createId("l2"), path.links.get(3).getId());
	}
		
	/*
	 * Network structure
	 * 
	 * n0---l0---n1---l1---n2---l2---n3
	 *           |
	 *           |
	 *           l3
	 *           |
	 *           |
	 *           n4
	 *           |
	 *           |
	 *           l4
	 *           |
	 *           |
	 *           n5
	 */
	private void createNetwork(Scenario scenario) {

		/*
		 * create nodes
		 */
		Node n0 = scenario.getNetwork().getFactory().createNode(scenario.createId("n0"), scenario.createCoord(   0.0,     0.0));
		Node n1 = scenario.getNetwork().getFactory().createNode(scenario.createId("n1"), scenario.createCoord(1000.0,     0.0));
		Node n2 = scenario.getNetwork().getFactory().createNode(scenario.createId("n2"), scenario.createCoord(2000.0,     0.0));
		Node n3 = scenario.getNetwork().getFactory().createNode(scenario.createId("n3"), scenario.createCoord(3000.0,     0.0));
		Node n4 = scenario.getNetwork().getFactory().createNode(scenario.createId("n4"), scenario.createCoord(1000.0, -2000.0));
		Node n5 = scenario.getNetwork().getFactory().createNode(scenario.createId("n5"), scenario.createCoord(1000.0, -4000.0));
		
		/*
		 * create links
		 */
		Link l0 = scenario.getNetwork().getFactory().createLink(scenario.createId("l0"), n0, n1);
		Link l1 = scenario.getNetwork().getFactory().createLink(scenario.createId("l1"), n1, n2);
		Link l2 = scenario.getNetwork().getFactory().createLink(scenario.createId("l2"), n2, n3);
		Link l3 = scenario.getNetwork().getFactory().createLink(scenario.createId("l3"), n4, n1);
		Link l4 = scenario.getNetwork().getFactory().createLink(scenario.createId("l4"), n5, n4);
				
		/*
		 * set link parameter
		 */
		l0.setLength(1000.0);
		l1.setLength(1000.0);
		l2.setLength(1000.0);
		l3.setLength(2000.0);
		l4.setLength(2000.0);
		
		l0.setFreespeed(10.0);
		l1.setFreespeed(10.0);
		l2.setFreespeed(10.0);
		l3.setFreespeed(10.0);
		l4.setFreespeed(10.0);
	
		/*
		 * add nodes to network
		 */
		scenario.getNetwork().addNode(n0);
		scenario.getNetwork().addNode(n1);
		scenario.getNetwork().addNode(n2);
		scenario.getNetwork().addNode(n3);
		scenario.getNetwork().addNode(n4);
		scenario.getNetwork().addNode(n5);
		
		/*
		 * add links to network
		 */
		scenario.getNetwork().addLink(l0);
		scenario.getNetwork().addLink(l1);
		scenario.getNetwork().addLink(l2);
		scenario.getNetwork().addLink(l3);
		scenario.getNetwork().addLink(l4);
	}
}