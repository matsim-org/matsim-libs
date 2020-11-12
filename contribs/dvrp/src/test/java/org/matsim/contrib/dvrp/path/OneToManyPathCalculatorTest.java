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
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.NoSuchElementException;

import org.junit.Before;
import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.path.OneToManyPathSearch.PathData;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

import com.google.common.collect.ImmutableList;

import ch.sbb.matsim.routing.graph.Graph;
import ch.sbb.matsim.routing.graph.LeastCostPathTree;

/**
 * @author Michal Maciejewski (michalm)
 */
public class OneToManyPathCalculatorTest {
	private final Network network = NetworkUtils.createNetwork();

	private final Node nodeA = createAndAddNode("A", new Coord(0, 0));
	private final Node nodeB = createAndAddNode("B", new Coord(150, 0));
	private final Node nodeC = createAndAddNode("C", new Coord(150, 150));

	private final Link linkAB = createAndAddLink("AB", nodeA, nodeB, 10 * 15, 15);
	private final Link linkBC = createAndAddLink("BC", nodeB, nodeC, 11 * 15, 15);
	private final Link linkAC = createAndAddLink("AC", nodeA, nodeC, 15 * 15, 15);

	private final Link linkBA = createAndAddLink("BA", nodeB, nodeA, 8 * 20, 20);
	private final Link linkCB = createAndAddLink("CB", nodeC, nodeB, 9 * 20, 20);
	private final Link linkCA = createAndAddLink("CA", nodeC, nodeA, 12 * 20, 20);

	private final IdMap<Node, Node> nodeMap = new IdMap<>(Node.class);

	private final LeastCostPathTree dijkstraTree = new LeastCostPathTree(new Graph(network), new FreeSpeedTravelTime(),
			new TimeAsTravelDisutility(new FreeSpeedTravelTime()));

	@Before
	public void init() {
		for (Node node : List.of(nodeA, nodeB, nodeC)) {
			nodeMap.put(node.getId(), node);
		}
	}

	@Test
	public void forward_fromNodeA_toNodeA() {
		//forward search starting from nodeA at time 0
		var pathCalculator = new OneToManyPathCalculator(nodeMap, dijkstraTree, true, linkBA, 0);

		//search until node A is reached (i.e. links AB and AC)
		pathCalculator.calculateDijkstraTree(List.of(linkAB, linkAC));

		assertThat(pathCalculator.createPath(nodeA)).isEqualToComparingFieldByField(
				new Path(List.of(nodeA), List.of(), 0, 0));

		assertThatThrownBy(() -> pathCalculator.createPath(nodeB)).isExactlyInstanceOf(NoSuchElementException.class)
				.hasMessage("Undefined time");

		assertThatThrownBy(() -> pathCalculator.createPath(nodeC)).isExactlyInstanceOf(NoSuchElementException.class)
				.hasMessage("Undefined time");
	}

	@Test
	public void forward_fromNodeA_toNodesBC() {
		//forward search starting from nodeA at time 0
		var pathCalculator = new OneToManyPathCalculator(nodeMap, dijkstraTree, true, linkBA, 0);

		//search until node B and C are visited
		pathCalculator.calculateDijkstraTree(List.of(linkBC, linkCB));

		assertThat(pathCalculator.createPath(nodeA)).isEqualToComparingFieldByField(
				new Path(List.of(nodeA), List.of(), 0, 0));

		assertThat(pathCalculator.createPath(nodeB)).isEqualToComparingFieldByField(
				new Path(List.of(nodeA, nodeB), List.of(linkAB), travelTime(linkAB), travelTime(linkAB)));

		assertThat(pathCalculator.createPath(nodeC)).isEqualToComparingFieldByField(
				new Path(List.of(nodeA, nodeC), List.of(linkAC), linkAC.getLength() / linkAC.getFreespeed(),
						linkAC.getLength() / linkAC.getFreespeed()));
	}

	@Test
	public void forward_fromLinkAB_toLinkAB() {
		LeastCostPathTree mockedTree = mock(LeastCostPathTree.class);
		//forward search starting from linkAB at time 0
		var pathCalculator = new OneToManyPathCalculator(nodeMap, mockedTree, true, linkAB, 0);

		//toLink == fromLink, so no search is done
		pathCalculator.calculateDijkstraTree(List.of(linkAB));
		verify(mockedTree, never()).calculate(anyInt(), anyDouble(), any(), any(), any());
	}

	@Test
	public void backward_fromNodeA_toNodeA() {
		//backward search starting from nodeA at time 0
		var pathCalculator = new OneToManyPathCalculator(nodeMap, dijkstraTree, false, linkAB, 0);

		//search until node A is reached (i.e. links BA and CA)
		pathCalculator.calculateDijkstraTree(List.of(linkBA, linkCA));

		assertThat(pathCalculator.createPath(nodeA)).isEqualToComparingFieldByField(
				new Path(List.of(nodeA), List.of(), 0, 0));

		assertThatThrownBy(() -> pathCalculator.createPath(nodeB)).isExactlyInstanceOf(NoSuchElementException.class)
				.hasMessage("Undefined time");

		assertThatThrownBy(() -> pathCalculator.createPath(nodeC)).isExactlyInstanceOf(NoSuchElementException.class)
				.hasMessage("Undefined time");
	}

	@Test
	public void backward_fromNodeA_toNodesBC() {
		//backward search starting from nodeA at time 0
		var pathCalculator = new OneToManyPathCalculator(nodeMap, dijkstraTree, false, linkAB, 0);

		//search until node B and C are visited
		pathCalculator.calculateDijkstraTree(List.of(linkBC, linkCB));

		assertThat(pathCalculator.createPath(nodeA)).isEqualToComparingFieldByField(
				new Path(List.of(nodeA), List.of(), 0, 0));

		assertThat(pathCalculator.createPath(nodeB)).isEqualToComparingFieldByField(
				new Path(List.of(nodeB, nodeA), List.of(linkBA), travelTime(linkBA), travelTime(linkBA)));

		assertThat(pathCalculator.createPath(nodeC)).isEqualToComparingFieldByField(
				new Path(List.of(nodeC, nodeA), List.of(linkCA), travelTime(linkCA), travelTime(linkCA)));
	}

	@Test
	public void backward_fromLinkAB_toLinkAB() {
		LeastCostPathTree mockedTree = mock(LeastCostPathTree.class);
		//backward search starting from linkAB at time 0
		var pathCalculator = new OneToManyPathCalculator(nodeMap, mockedTree, false, linkAB, 0);

		//toLink == fromLink, so no search is done
		pathCalculator.calculateDijkstraTree(List.of(linkAB));

		verify(mockedTree, never()).calculate(anyInt(), anyDouble(), any(), any(), any());
	}

	@Test
	public void pathData_forward() {
		//forward search starting from linkBA at time 0
		var pathCalculator = new OneToManyPathCalculator(nodeMap, dijkstraTree, true, linkBA, 0);

		pathCalculator.calculateDijkstraTree(List.of(linkBC));

		//path: BA -> AB -> BC
		assertPathData(pathCalculator.createPathDataLazily(linkBC),
				new Path(ImmutableList.of(nodeA, nodeB), ImmutableList.of(linkAB), travelTime(linkAB),
						travelTime(linkAB)), 1 + travelTime(linkBC));
	}

	@Test
	public void pathData_backward() {
		//forward search starting from linkBA at time 0
		var pathCalculator = new OneToManyPathCalculator(nodeMap, dijkstraTree, false, linkAB, 0);

		pathCalculator.calculateDijkstraTree(List.of(linkCB));

		//path: CB -> BA -> AB
		assertPathData(pathCalculator.createPathDataLazily(linkCB),
				new Path(ImmutableList.of(nodeB, nodeA), ImmutableList.of(linkBA), travelTime(linkBA),
						travelTime(linkBA)), 1 + travelTime(linkAB));
	}

	@Test
	public void pathData_fromLinkEqualsToLink() {
		//forward search starting from linkBA at time 0
		var pathCalculator = new OneToManyPathCalculator(nodeMap, dijkstraTree, true, linkAB, 0);

		pathCalculator.calculateDijkstraTree(List.of(linkAB));

		assertPathData(pathCalculator.createPathDataLazily(linkAB), new Path(null, null, 0, 0), 0);
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

	private double travelTime(Link link) {
		return link.getLength() / link.getFreespeed();
	}
}
