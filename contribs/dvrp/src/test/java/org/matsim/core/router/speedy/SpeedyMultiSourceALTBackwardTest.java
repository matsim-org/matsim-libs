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
 * A copy of SpeedyMultiSourceALTForwardTest (however the network is inverted and the computed paths as well)
 *
 * @author Michal Maciejewski (michalm)
 */
public class SpeedyMultiSourceALTBackwardTest {
	private final Network network = NetworkUtils.createNetwork();

	// single-direction links (from right to left, e.g. B->A, but not A->B)
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

	private final Link linkBA = createAndAddLink("BA", nodeB, nodeA, 10 * 15, 15);
	private final Link linkCA = createAndAddLink("CA", nodeC, nodeA, 10 * 15, 15);
	private final Link linkDA = createAndAddLink("DA", nodeD, nodeA, 10 * 15, 15);

	private final Link linkDB = createAndAddLink("DB", nodeD, nodeB, 10 * 15, 15);

	private final Link linkDC = createAndAddLink("DC", nodeD, nodeC, 10 * 15, 15);
	private final Link linkEC = createAndAddLink("EC", nodeE, nodeC, 10 * 15, 15);

	private final Link linkED = createAndAddLink("ED", nodeE, nodeD, 10 * 15, 15);

	private final Link linkAE = createAndAddLink("AE", nodeA, nodeE, 10 * 15, 15);

	private final TravelTime travelTime = new FreeSpeedTravelTime();
	private final TravelDisutility travelDisutility = new TimeAsTravelDisutility(travelTime);
	private final SpeedyGraph speedyGraph = new SpeedyGraph(network);
	private final SpeedyALTData landmarks = new SpeedyALTData(speedyGraph, 3, travelDisutility);
	private final SpeedyMultiSourceALT multiSourceALT = new SpeedyMultiSourceALT(landmarks, travelTime,
			travelDisutility);

	@Test
	void testOneSource_backward() {
		var startNode = new StartNode(nodeB, 9999, 7777);
		var path = multiSourceALT.calcLeastCostPath(List.of(startNode), nodeA, null, null, true);
		assertThat(path.nodes).containsExactly(nodeA, nodeE, nodeD, nodeB);
		assertThat(path.links).containsExactly(linkAE, linkED, linkDB);
		assertThat(path.travelTime).isEqualTo(10 + 10 + 10);
		assertThat(path.travelCost).isEqualTo(9999 + 10 + 10 + 10);
	}

	@Test
	void testManySources_backward_sameStartCost() {
		var startNodeB = new StartNode(nodeB, 9999, 7777);
		var startNodeC = new StartNode(nodeC, 9999, 7777);
		var path = multiSourceALT.calcLeastCostPath(List.of(startNodeB, startNodeC), nodeA, null, null, true);
		assertThat(path.nodes).containsExactly(nodeA, nodeE, nodeC);
		assertThat(path.links).containsExactly(linkAE, linkEC);
		assertThat(path.travelTime).isEqualTo(10 + 10);
		assertThat(path.travelCost).isEqualTo(9999 + 10 + 10);
	}

	@Test
	void testManySources_backward_selectFartherNodeWithLowerCost() {
		var startNodeB = new StartNode(nodeB, 100, 7777);
		var startNodeC = new StartNode(nodeC, 111, 1111);
		var path = multiSourceALT.calcLeastCostPath(List.of(startNodeB, startNodeC), nodeA, null, null, true);
		assertThat(path.nodes).containsExactly(nodeA, nodeE, nodeD, nodeB);
		assertThat(path.links).containsExactly(linkAE, linkED, linkDB);
		assertThat(path.travelTime).isEqualTo(10 + 10 + 10);
		assertThat(path.travelCost).isEqualTo(100 + 10 + 10 + 10);
	}

	@Test
	void testManySources_backward_selectNearestNodeWithHigherCost() {
		var startNodeB = new StartNode(nodeB, 100, 7777);
		var startNodeC = new StartNode(nodeC, 109, 1111);
		var path = multiSourceALT.calcLeastCostPath(List.of(startNodeB, startNodeC), nodeA, null, null, true);
		assertThat(path.nodes).containsExactly(nodeA, nodeE, nodeC);
		assertThat(path.links).containsExactly(linkAE, linkEC);
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
