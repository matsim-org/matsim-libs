/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.dvrp.path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.speedy.LeastCostPathTree;
import org.matsim.core.router.speedy.SpeedyGraph;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import com.google.common.collect.ImmutableList;

/**
 * @author Michal Maciejewski (michalm)
 */
public class OneToManyPathCalculatorTest {
	private final Network network = NetworkUtils.createNetwork();

	private final Node nodeA = createAndAddNode("A", new Coord(0, 0));
	private final Node nodeB = createAndAddNode("B", new Coord(150, 0));
	private final Node nodeC = createAndAddNode("C", new Coord(300, 0));
	private final Node nodeD = createAndAddNode("D", new Coord(450, 0));
	private final Node nodeE = createAndAddNode("E", new Coord(600, 0));

	private final Link linkAB = createAndAddLink("AB", nodeA, nodeB, 10 * 15, 15);
	private final Link linkBC = createAndAddLink("BC", nodeB, nodeC, 10 * 15, 15);
	private final Link linkCD = createAndAddLink("CD", nodeC, nodeD, 10 * 15, 15);
	private final Link linkDE = createAndAddLink("DE", nodeD, nodeE, 10 * 15, 15);

	private final IdMap<Node, Node> nodeMap = new IdMap<>(Node.class);

	private final TravelTime travelTime = new FreeSpeedTravelTime();
	private final LeastCostPathTree dijkstraTree = new LeastCostPathTree(new SpeedyGraph(network), travelTime,
			new TimeAsTravelDisutility(travelTime));

	@BeforeEach
	public void init() {
		for (Node node : List.of(nodeA, nodeB, nodeC, nodeD, nodeE)) {
			nodeMap.put(node.getId(), node);
		}
	}

	@Test
	void forward_fromNodeB_toNodeB() {
		//forward search starting from nodeB at time 0
		var pathCalculator = new OneToManyPathCalculator(nodeMap, dijkstraTree, travelTime, true, linkAB, 0);

		//search until node B is reached
		pathCalculator.calculateDijkstraTree(List.of(linkBC));

		assertThat(pathCalculator.createPath(nodeB)).isEqualToComparingFieldByField(
				new Path(List.of(nodeB), List.of(), 0, 0));

		//no other nodes are visited
		nodeMap.values()
				.stream()
				.filter(node -> node != nodeB)
				.forEach(node -> assertThat(pathCalculator.createPath(node)).isNull());
	}

	@Test
	void forward_fromNodeB_toNodesBD() {
		//forward search starting from nodeB at time 0
		var pathCalculator = new OneToManyPathCalculator(nodeMap, dijkstraTree, travelTime, true, linkAB, 0);

		//search until nodes B and D are reached
		pathCalculator.calculateDijkstraTree(List.of(linkBC, linkDE));

		assertThat(pathCalculator.createPath(nodeB)).isEqualToComparingFieldByField(
				new Path(List.of(nodeB), List.of(), 0, 0));

		assertThat(pathCalculator.createPath(nodeD)).isEqualToComparingFieldByField(
				new Path(List.of(nodeB, nodeC, nodeD), List.of(linkBC, linkCD), 20, 20));
	}

	@Test
	void forward_fromNodeB_toNodesBD_maxTravelTime() {
		//forward search starting from nodeB at time 0
		var pathCalculator = new OneToManyPathCalculator(nodeMap, dijkstraTree, travelTime, true, linkAB, 0);

		//search until nodes B and D are reached with max travel time of 5
		pathCalculator.calculateDijkstraTree(List.of(linkBC, linkDE), 5);

		assertThat(pathCalculator.createPath(nodeB)).isEqualToComparingFieldByField(
				new Path(List.of(nodeB), List.of(), 0, 0));

		//nodeD beyond max time range
		assertThat(pathCalculator.createPath(nodeD)).isNull();
	}

	@Test
	void backward_fromNodeD_toNodeD() {
		//backward search starting from nodeD at time 0
		var pathCalculator = new OneToManyPathCalculator(nodeMap, dijkstraTree, travelTime, false, linkDE, 0);

		//search until node D is reached
		pathCalculator.calculateDijkstraTree(List.of(linkCD));

		assertThat(pathCalculator.createPath(nodeD)).isEqualToComparingFieldByField(
				new Path(List.of(nodeD), List.of(), 0, 0));

		//no other nodes are visited
		nodeMap.values()
				.stream()
				.filter(node -> node != nodeD)
				.forEach(node -> assertThat(pathCalculator.createPath(node)).isNull());
	}

	@Test
	void backward_fromNodeD_toNodesBD() {
		//backward search starting from nodeD at time 0
		var pathCalculator = new OneToManyPathCalculator(nodeMap, dijkstraTree, travelTime, false, linkDE, 0);

		//search until nodes B and D are reached
		pathCalculator.calculateDijkstraTree(List.of(linkAB, linkCD));

		assertThat(pathCalculator.createPath(nodeD)).isEqualToComparingFieldByField(
				new Path(List.of(nodeD), List.of(), 0, 0));

		assertThat(pathCalculator.createPath(nodeB)).isEqualToComparingFieldByField(
				new Path(List.of(nodeB, nodeC, nodeD), List.of(linkBC, linkCD), 20, 20));
	}

	@Test
	void backward_fromNodeD_toNodesBD_maxTravelTime() {
		//backward search starting from nodeD at time 0
		var pathCalculator = new OneToManyPathCalculator(nodeMap, dijkstraTree, travelTime, false, linkDE, 0);

		//search until nodes B and D are reached with max travel time of 5
		pathCalculator.calculateDijkstraTree(List.of(linkAB, linkCD), 5);

		assertThat(pathCalculator.createPath(nodeD)).isEqualToComparingFieldByField(
				new Path(List.of(nodeD), List.of(), 0, 0));

		//nodeB beyond max time range
		assertThat(pathCalculator.createPath(nodeB)).isNull();
	}

	@Test
	void equalFromLinkAndToLink() {
		LeastCostPathTree mockedTree = mock(LeastCostPathTree.class);

		for (boolean forward : List.of(true, false)) {
			var pathCalculator = new OneToManyPathCalculator(nodeMap, mockedTree, travelTime, forward, linkAB, 0);

			//toLink == fromLink, so no search is done
			pathCalculator.calculateDijkstraTree(List.of(linkAB));

			verify(mockedTree, never()).calculate(anyInt(), anyDouble(), any(), any(), any());
		}
	}

	@Test
	void pathData_forward() {
		//forward search starting from linkAB at time 0
		var pathCalculator = new OneToManyPathCalculator(nodeMap, dijkstraTree, travelTime, true, linkAB, 0);

		pathCalculator.calculateDijkstraTree(List.of(linkDE));

		//path: B -> C -> D
		assertPathData(pathCalculator.createPathDataLazily(linkDE),
				new Path(ImmutableList.of(nodeB, nodeC, nodeD), ImmutableList.of(linkBC, linkCD), 20, 20), 1 + 10);
	}

	@Test
	void pathData_backward() {
		//backward search starting from linkDE at time 0
		var pathCalculator = new OneToManyPathCalculator(nodeMap, dijkstraTree, travelTime, false, linkDE, 0);

		pathCalculator.calculateDijkstraTree(List.of(linkAB));

		//path: B -> C -> D
		assertPathData(pathCalculator.createPathDataLazily(linkAB),
				new Path(ImmutableList.of(nodeB, nodeC, nodeD), ImmutableList.of(linkBC, linkCD), 20, 20), 1 + 10);
	}

	@Test
	void pathData_fromLinkEqualsToLink() {
		for (boolean forward : List.of(true, false)) {
			var pathCalculator = new OneToManyPathCalculator(nodeMap, dijkstraTree, travelTime, forward, linkAB, 0);
			pathCalculator.calculateDijkstraTree(List.of(linkAB));
			assertThat(pathCalculator.createPathDataLazily(linkAB)).isEqualTo(PathData.EMPTY);
		}
	}

	@Test
	void pathData_nodeNotReached() {
		for (boolean forward : List.of(true, false)) {
			var pathCalculator = new OneToManyPathCalculator(nodeMap, dijkstraTree, travelTime, forward, linkAB, 0);
			pathCalculator.calculateDijkstraTree(List.of(linkBC));
			assertThat(pathCalculator.createPathDataLazily(linkDE)).isEqualTo(PathData.INFEASIBLE);
		}
	}

	private void assertPathData(PathData pathData, Path expectedPath, double firstAndLastLinkTT) {
		assertThat(pathData.getPath()).isEqualToComparingFieldByField(expectedPath);
		assertThat(pathData.getTravelTime()).isEqualTo(expectedPath.travelTime + firstAndLastLinkTT);
	}

	private Node createAndAddNode(String id, Coord coord) {
		return NetworkUtils.createAndAddNode(network, Id.createNodeId(id), coord);
	}

	private Link createAndAddLink(String id, Node fromNode, Node toNode, double length, double freespeed) {
		return NetworkUtils.createAndAddLink(network, Id.createLinkId(id), fromNode, toNode, length, freespeed,
				length / 7.5, 1);
	}
}
