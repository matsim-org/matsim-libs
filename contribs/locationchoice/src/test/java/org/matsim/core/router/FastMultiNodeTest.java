/* *********************************************************************** *
 * project: org.matsim.*
 * FastMultiNodeTest.java
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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutilityFactory;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import java.util.ArrayList;
import java.util.List;

/**
 * Some more tests - but all of them assuming that there is only
 * a single from node as in the location choice use case.
 * For test using multiple from nodes look at MultiNodeDijkstraTest!
 *
 * @author cdobler
 */
public class FastMultiNodeTest {

	@Test
	void testFastMultiNodeDijkstra_OneToOne() {

		Config config = ConfigUtils.createConfig();
		config.routing().setRoutingRandomness( 0. );

		Scenario scenario = ScenarioUtils.createScenario(config);

		createNetwork(scenario);

		TravelTime travelTime = new FreeSpeedTravelTime();
                TravelDisutility travelDisutility = new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car,
                                config ).createTravelDisutility(travelTime );
		FastMultiNodeDijkstra dijkstra = (FastMultiNodeDijkstra) new FastMultiNodeDijkstraFactory().
				createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime);

		Node fromNode = scenario.getNetwork().getNodes().get(Id.create("n0", Node.class));
		Node toNode = scenario.getNetwork().getNodes().get(Id.create("n3", Node.class));

		/*
		 * test calcLeastCostPath method
		 */
		Path path = dijkstra.calcLeastCostPath(fromNode, toNode, 3600.0, null, null);

		Assertions.assertEquals(1.0, path.travelCost, 0.0);
		Assertions.assertEquals(300.0, path.travelTime, 0.0);

		Assertions.assertEquals(4, path.nodes.size());
		Assertions.assertEquals(Id.create("n0", Node.class), path.nodes.get(0).getId());
		Assertions.assertEquals(Id.create("n1", Node.class), path.nodes.get(1).getId());
		Assertions.assertEquals(Id.create("n2", Node.class), path.nodes.get(2).getId());
		Assertions.assertEquals(Id.create("n3", Node.class), path.nodes.get(3).getId());

		Assertions.assertEquals(3, path.links.size());
		Assertions.assertEquals(Id.create("l0", Link.class), path.links.get(0).getId());
		Assertions.assertEquals(Id.create("l1", Link.class), path.links.get(1).getId());
		Assertions.assertEquals(Id.create("l2", Link.class), path.links.get(2).getId());

		/*
		 * test constructPath method which uses data from the previous routing operation
		 */
		path = dijkstra.constructPath(fromNode, toNode, 3600.0);

		Assertions.assertEquals(1.0, path.travelCost, 0.0);
		Assertions.assertEquals(300.0, path.travelTime, 0.0);

		Assertions.assertEquals(4, path.nodes.size());
		Assertions.assertEquals(Id.create("n0", Node.class), path.nodes.get(0).getId());
		Assertions.assertEquals(Id.create("n1", Node.class), path.nodes.get(1).getId());
		Assertions.assertEquals(Id.create("n2", Node.class), path.nodes.get(2).getId());
		Assertions.assertEquals(Id.create("n3", Node.class), path.nodes.get(3).getId());

		Assertions.assertEquals(3, path.links.size());
		Assertions.assertEquals(Id.create("l0", Link.class), path.links.get(0).getId());
		Assertions.assertEquals(Id.create("l1", Link.class), path.links.get(1).getId());
		Assertions.assertEquals(Id.create("l2", Link.class), path.links.get(2).getId());
	}

	@Test
	void testFastMultiNodeDijkstra_OneToMany() {

		Config config = ConfigUtils.createConfig();
		config.routing().setRoutingRandomness( 0. );

		Scenario scenario = ScenarioUtils.createScenario(config);

		createNetwork(scenario);

		TravelTime travelTime = new FreeSpeedTravelTime();
                TravelDisutility travelDisutility = new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car,
                                config ).createTravelDisutility(travelTime );
		FastMultiNodeDijkstra dijkstra = (FastMultiNodeDijkstra) new FastMultiNodeDijkstraFactory(false).
				createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime);

		Node fromNode = scenario.getNetwork().getNodes().get(Id.create("n0", Node.class));
		Node toNode1 = scenario.getNetwork().getNodes().get(Id.create("n3", Node.class));
		Node toNode2 = scenario.getNetwork().getNodes().get(Id.create("n4", Node.class));
		Node toNode3 = scenario.getNetwork().getNodes().get(Id.create("n5", Node.class));
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		toNodes.add(new InitialNode(toNode1, 0.0, 0.0));
		toNodes.add(new InitialNode(toNode2, 0.0, 0.0));
		toNodes.add(new InitialNode(toNode3, 0.0, 0.0));
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		/*
		 * test calcLeastCostPath method
		 */
		Path path = dijkstra.calcLeastCostPath(fromNode, toNode, 3600.0, null, null);

		Assertions.assertEquals(1.0, path.travelCost, 0.0);
		Assertions.assertEquals(300.0, path.travelTime, 0.0);

		Assertions.assertEquals(4, path.nodes.size());
		Assertions.assertEquals(Id.create("n0", Node.class), path.nodes.get(0).getId());
		Assertions.assertEquals(Id.create("n1", Node.class), path.nodes.get(1).getId());
		Assertions.assertEquals(Id.create("n2", Node.class), path.nodes.get(2).getId());
		Assertions.assertEquals(Id.create("n3", Node.class), path.nodes.get(3).getId());

		Assertions.assertEquals(3, path.links.size());
		Assertions.assertEquals(Id.create("l0", Link.class), path.links.get(0).getId());
		Assertions.assertEquals(Id.create("l1", Link.class), path.links.get(1).getId());
		Assertions.assertEquals(Id.create("l2", Link.class), path.links.get(2).getId());

		/*
		 * test constructPath method which uses data from the previous routing operation - toNode1
		 */
		path = dijkstra.constructPath(fromNode, toNode1, 3600.0);

		Assertions.assertEquals(1.0, path.travelCost, 0.0);
		Assertions.assertEquals(300.0, path.travelTime, 0.0);

		Assertions.assertEquals(4, path.nodes.size());
		Assertions.assertEquals(Id.create("n0", Node.class), path.nodes.get(0).getId());
		Assertions.assertEquals(Id.create("n1", Node.class), path.nodes.get(1).getId());
		Assertions.assertEquals(Id.create("n2", Node.class), path.nodes.get(2).getId());
		Assertions.assertEquals(Id.create("n3", Node.class), path.nodes.get(3).getId());

		Assertions.assertEquals(3, path.links.size());
		Assertions.assertEquals(Id.create("l0", Link.class), path.links.get(0).getId());
		Assertions.assertEquals(Id.create("l1", Link.class), path.links.get(1).getId());
		Assertions.assertEquals(Id.create("l2", Link.class), path.links.get(2).getId());

		/*
		 * test constructPath method which uses data from the previous routing operation - toNode2
		 */
		path = dijkstra.constructPath(fromNode, toNode2, 3600.0);

		Assertions.assertEquals(1.333, path.travelCost, 0.001);
		Assertions.assertEquals(400.0, path.travelTime, 0.0);

		Assertions.assertEquals(4, path.nodes.size());
		Assertions.assertEquals(Id.create("n0", Node.class), path.nodes.get(0).getId());
		Assertions.assertEquals(Id.create("n1", Node.class), path.nodes.get(1).getId());
		Assertions.assertEquals(Id.create("n2", Node.class), path.nodes.get(2).getId());
		Assertions.assertEquals(Id.create("n4", Node.class), path.nodes.get(3).getId());

		Assertions.assertEquals(3, path.links.size());
		Assertions.assertEquals(Id.create("l0", Link.class), path.links.get(0).getId());
		Assertions.assertEquals(Id.create("l1", Link.class), path.links.get(1).getId());
		Assertions.assertEquals(Id.create("l3", Link.class), path.links.get(2).getId());

		/*
		 * test constructPath method which uses data from the previous routing operation - toNode3
		 */
		path = dijkstra.constructPath(fromNode, toNode3, 3600.0);

		Assertions.assertNull(path);
	}

	@Test
	void testFastMultiNodeDijkstra_OneToMany_SearchAllNodes() {

		Config config = ConfigUtils.createConfig();
		config.routing().setRoutingRandomness( 0. );

		Scenario scenario = ScenarioUtils.createScenario(config);

		createNetwork(scenario);

		TravelTime travelTime = new FreeSpeedTravelTime();
                TravelDisutility travelDisutility = new RandomizingTimeDistanceTravelDisutilityFactory( TransportMode.car,
                                config ).createTravelDisutility(travelTime );
		FastMultiNodeDijkstra dijkstra = (FastMultiNodeDijkstra) new FastMultiNodeDijkstraFactory(true).
				createPathCalculator(scenario.getNetwork(), travelDisutility, travelTime);

		Node fromNode = scenario.getNetwork().getNodes().get(Id.create("n0", Node.class));
		Node toNode1 = scenario.getNetwork().getNodes().get(Id.create("n3", Node.class));
		Node toNode2 = scenario.getNetwork().getNodes().get(Id.create("n4", Node.class));
		Node toNode3 = scenario.getNetwork().getNodes().get(Id.create("n5", Node.class));
		List<InitialNode> toNodes = new ArrayList<InitialNode>();
		toNodes.add(new InitialNode(toNode1, 0.0, 0.0));
		toNodes.add(new InitialNode(toNode2, 0.0, 0.0));
		toNodes.add(new InitialNode(toNode3, 0.0, 0.0));
		Node toNode = dijkstra.createImaginaryNode(toNodes);

		/*
		 * test calcLeastCostPath method
		 */
		Path path = dijkstra.calcLeastCostPath(fromNode, toNode, 3600.0, null, null);

		Assertions.assertEquals(1.0, path.travelCost, 0.0);
		Assertions.assertEquals(300.0, path.travelTime, 0.0);

		Assertions.assertEquals(4, path.nodes.size());
		Assertions.assertEquals(Id.create("n0", Node.class), path.nodes.get(0).getId());
		Assertions.assertEquals(Id.create("n1", Node.class), path.nodes.get(1).getId());
		Assertions.assertEquals(Id.create("n2", Node.class), path.nodes.get(2).getId());
		Assertions.assertEquals(Id.create("n3", Node.class), path.nodes.get(3).getId());

		Assertions.assertEquals(3, path.links.size());
		Assertions.assertEquals(Id.create("l0", Link.class), path.links.get(0).getId());
		Assertions.assertEquals(Id.create("l1", Link.class), path.links.get(1).getId());
		Assertions.assertEquals(Id.create("l2", Link.class), path.links.get(2).getId());

		/*
		 * test constructPath method which uses data from the previous routing operation - toNode1
		 */
		path = dijkstra.constructPath(fromNode, toNode1, 3600.0);

		Assertions.assertEquals(1.0, path.travelCost, 0.0);
		Assertions.assertEquals(300.0, path.travelTime, 0.0);

		Assertions.assertEquals(4, path.nodes.size());
		Assertions.assertEquals(Id.create("n0", Node.class), path.nodes.get(0).getId());
		Assertions.assertEquals(Id.create("n1", Node.class), path.nodes.get(1).getId());
		Assertions.assertEquals(Id.create("n2", Node.class), path.nodes.get(2).getId());
		Assertions.assertEquals(Id.create("n3", Node.class), path.nodes.get(3).getId());

		Assertions.assertEquals(3, path.links.size());
		Assertions.assertEquals(Id.create("l0", Link.class), path.links.get(0).getId());
		Assertions.assertEquals(Id.create("l1", Link.class), path.links.get(1).getId());
		Assertions.assertEquals(Id.create("l2", Link.class), path.links.get(2).getId());

		/*
		 * test constructPath method which uses data from the previous routing operation - toNode2
		 */
		path = dijkstra.constructPath(fromNode, toNode2, 3600.0);

		Assertions.assertEquals(1.333, path.travelCost, 0.001);
		Assertions.assertEquals(400.0, path.travelTime, 0.0);

		Assertions.assertEquals(4, path.nodes.size());
		Assertions.assertEquals(Id.create("n0", Node.class), path.nodes.get(0).getId());
		Assertions.assertEquals(Id.create("n1", Node.class), path.nodes.get(1).getId());
		Assertions.assertEquals(Id.create("n2", Node.class), path.nodes.get(2).getId());
		Assertions.assertEquals(Id.create("n4", Node.class), path.nodes.get(3).getId());

		Assertions.assertEquals(3, path.links.size());
		Assertions.assertEquals(Id.create("l0", Link.class), path.links.get(0).getId());
		Assertions.assertEquals(Id.create("l1", Link.class), path.links.get(1).getId());
		Assertions.assertEquals(Id.create("l3", Link.class), path.links.get(2).getId());

		/*
		 * test constructPath method which uses data from the previous routing operation - toNode3
		 */
		path = dijkstra.constructPath(fromNode, toNode3, 3600.0);

		Assertions.assertEquals(2.0, path.travelCost, 0.0);
		Assertions.assertEquals(600.0, path.travelTime, 0.0);

		Assertions.assertEquals(5, path.nodes.size());
		Assertions.assertEquals(Id.create("n0", Node.class), path.nodes.get(0).getId());
		Assertions.assertEquals(Id.create("n1", Node.class), path.nodes.get(1).getId());
		Assertions.assertEquals(Id.create("n2", Node.class), path.nodes.get(2).getId());
		Assertions.assertEquals(Id.create("n4", Node.class), path.nodes.get(3).getId());
		Assertions.assertEquals(Id.create("n5", Node.class), path.nodes.get(4).getId());

		Assertions.assertEquals(4, path.links.size());
		Assertions.assertEquals(Id.create("l0", Link.class), path.links.get(0).getId());
		Assertions.assertEquals(Id.create("l1", Link.class), path.links.get(1).getId());
		Assertions.assertEquals(Id.create("l3", Link.class), path.links.get(2).getId());
		Assertions.assertEquals(Id.create("l4", Link.class), path.links.get(3).getId());
	}

	/*
	 * Network structure
	 *
	 * n0---l0---n1---l1---n2---l2---n3
	 *                     |
	 *                     |
	 *                     l3
	 *                     |
	 *                     |
	 *                     n4
	 *                     |
	 *                     |
	 *                     l4
	 *                     |
	 *                     |
	 *                     n5
	 */
	private static void createNetwork( Scenario scenario ) {

		/*
		 * create nodes
		 */
		Node n0 = scenario.getNetwork().getFactory().createNode(Id.create("n0", Node.class), new Coord(0.0, 0.0));
		Node n1 = scenario.getNetwork().getFactory().createNode(Id.create("n1", Node.class), new Coord(1000.0, 0.0));
		Node n2 = scenario.getNetwork().getFactory().createNode(Id.create("n2", Node.class), new Coord(2000.0, 0.0));
		Node n3 = scenario.getNetwork().getFactory().createNode(Id.create("n3", Node.class), new Coord(3000.0, 0.0));
		double y1 = -2000.0;
		Node n4 = scenario.getNetwork().getFactory().createNode(Id.create("n4", Node.class), new Coord(2000.0, y1));
		double y = -4000.0;
		Node n5 = scenario.getNetwork().getFactory().createNode(Id.create("n5", Node.class), new Coord(2000.0, y));

		/*
		 * create links
		 */
		Link l0 = scenario.getNetwork().getFactory().createLink(Id.create("l0", Link.class), n0, n1);
		Link l1 = scenario.getNetwork().getFactory().createLink(Id.create("l1", Link.class), n1, n2);
		Link l2 = scenario.getNetwork().getFactory().createLink(Id.create("l2", Link.class), n2, n3);
		Link l3 = scenario.getNetwork().getFactory().createLink(Id.create("l3", Link.class), n2, n4);
		Link l4 = scenario.getNetwork().getFactory().createLink(Id.create("l4", Link.class), n4, n5);

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
