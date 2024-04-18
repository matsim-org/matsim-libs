/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2021 by the members listed in the COPYING,        *
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

package org.matsim.core.router.speedy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.router.speedy.SpeedyMultiSourceALT.StartNode;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

/**
 * @author Michal Maciejewski (michalm)
 */
public class SpeedyMultiSourceALTForwardTest {
	private final Network network = NetworkUtils.createNetwork();

	// single-direction links (from left to right, e.g. A->B, but not B->A)
	//
	//      B
	//    /  \
	//  A --- D
	//   \  /  \
	//    C --- E --- (A)

	private final Node nodeA = createAndAddNode("A", new Coord(0, 0));
	private final Node nodeB = createAndAddNode("B", new Coord(150, 150));
	private final Node nodeC = createAndAddNode("C", new Coord(150, -150));
	private final Node nodeD = createAndAddNode("D", new Coord(300, 0));
	private final Node nodeE = createAndAddNode("E", new Coord(450, -150));

	private final Link linkAB = createAndAddLink("AB", nodeA, nodeB, 10 * 15, 15);
	private final Link linkAC = createAndAddLink("AC", nodeA, nodeC, 10 * 15, 15);
	private final Link linkAD = createAndAddLink("AD", nodeA, nodeD, 10 * 15, 15);

	private final Link linkBD = createAndAddLink("BD", nodeB, nodeD, 10 * 15, 15);

	private final Link linkCD = createAndAddLink("CD", nodeC, nodeD, 10 * 15, 15);
	private final Link linkCE = createAndAddLink("CE", nodeC, nodeE, 10 * 15, 15);

	private final Link linkDE = createAndAddLink("DE", nodeD, nodeE, 10 * 15, 15);

	private final Link linkEA = createAndAddLink("EA", nodeE, nodeA, 10 * 15, 15);

	private final TravelTime travelTime = new FreeSpeedTravelTime();
	private final TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);
	private final SpeedyGraph speedyGraph = SpeedyGraphBuilder.build(network);
	private final SpeedyALTData landmarks = new SpeedyALTData(speedyGraph, 3, travelDisutility);
	private final SpeedyMultiSourceALT multiSourceALT = new SpeedyMultiSourceALT(landmarks, travelTime,
			travelDisutility);

	@Test
	void testOneSource_forward() {
		var startNode = new StartNode(nodeB, 9999, 7777);
		var path = multiSourceALT.calcLeastCostPath(List.of(startNode), nodeA, null, null, false);
		assertThat(path.nodes).containsExactly(nodeB, nodeD, nodeE, nodeA);
		assertThat(path.links).containsExactly(linkBD, linkDE, linkEA);
		assertThat(path.travelTime).isEqualTo(10 + 10 + 10);
		assertThat(path.travelCost).isEqualTo(9999 + 10 + 10 + 10);
	}

	@Test
	void testManySources_forward_sameStartCost() {
		var startNodeB = new StartNode(nodeB, 9999, 7777);
		var startNodeC = new StartNode(nodeC, 9999, 7777);
		var path = multiSourceALT.calcLeastCostPath(List.of(startNodeB, startNodeC), nodeA, null, null, false);
		assertThat(path.nodes).containsExactly(nodeC, nodeE, nodeA);
		assertThat(path.links).containsExactly(linkCE, linkEA);
		assertThat(path.travelTime).isEqualTo(10 + 10);
		assertThat(path.travelCost).isEqualTo(9999 + 10 + 10);
	}

	@Test
	void testManySources_forward_selectFartherNodeWithLowerCost() {
		var startNodeB = new StartNode(nodeB, 100, 7777);
		var startNodeC = new StartNode(nodeC, 111, 1111);
		var path = multiSourceALT.calcLeastCostPath(List.of(startNodeB, startNodeC), nodeA, null, null, false);
		assertThat(path.nodes).containsExactly(nodeB, nodeD, nodeE, nodeA);
		assertThat(path.links).containsExactly(linkBD, linkDE, linkEA);
		assertThat(path.travelTime).isEqualTo(10 + 10 + 10);
		assertThat(path.travelCost).isEqualTo(100 + 10 + 10 + 10);
	}

	@Test
	void testManySources_forward_selectNearestNodeWithHigherCost() {
		var startNodeB = new StartNode(nodeB, 100, 7777);
		var startNodeC = new StartNode(nodeC, 109, 1111);
		var path = multiSourceALT.calcLeastCostPath(List.of(startNodeB, startNodeC), nodeA, null, null, false);
		assertThat(path.nodes).containsExactly(nodeC, nodeE, nodeA);
		assertThat(path.links).containsExactly(linkCE, linkEA);
		assertThat(path.travelTime).isEqualTo(10 + 10);
		assertThat(path.travelCost).isEqualTo(109 + 10 + 10);
	}

	private Node createAndAddNode(String id, Coord coord) {
		return NetworkUtils.createAndAddNode(network, Id.createNodeId(id), coord);
	}

	private Link createAndAddLink(String id, Node fromNode, Node toNode, double length, double freespeed) {
		return NetworkUtils.createAndAddLink(network, Id.createLinkId(id), fromNode, toNode, length, freespeed,
				length / 7.5, 1);
	}
}
