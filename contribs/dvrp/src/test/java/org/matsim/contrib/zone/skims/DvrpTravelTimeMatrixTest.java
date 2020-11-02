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

package org.matsim.contrib.zone.skims;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

/**
 * @author Michal Maciejewski (michalm)
 */
public class DvrpTravelTimeMatrixTest {

	private final Network network = NetworkUtils.createNetwork();
	private final Node nodeA = NetworkUtils.createAndAddNode(network, Id.createNodeId("A"), new Coord(0, 0));
	private final Node nodeB = NetworkUtils.createAndAddNode(network, Id.createNodeId("B"), new Coord(150, 150));
	private final DvrpTravelTimeMatrix matrix;

	public DvrpTravelTimeMatrixTest() {
		NetworkUtils.createAndAddLink(network, Id.createLinkId("AB"), nodeA, nodeB, 150, 15, 20, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("BA"), nodeB, nodeA, 300, 15, 40, 1);
		DvrpTravelTimeMatrixParams params = new DvrpTravelTimeMatrixParams();
		params.setCellSize(100);
		matrix = new DvrpTravelTimeMatrix(network, params, 1);
	}

	@Test
	public void test_centralNodes() {
		assertThat(matrix.getFreeSpeedTravelTime(nodeA, nodeA)).isEqualTo(0);
		assertThat(matrix.getFreeSpeedTravelTime(nodeA, nodeB)).isEqualTo(10);
		assertThat(matrix.getFreeSpeedTravelTime(nodeB, nodeA)).isEqualTo(20);
		assertThat(matrix.getFreeSpeedTravelTime(nodeB, nodeB)).isEqualTo(0);
	}

	@Test
	public void test_nonCentralNodes() {
		Node nodeC = NetworkUtils.createAndAddNode(network, Id.createNodeId("C"), new Coord(10, 10));
		Node nodeD = NetworkUtils.createAndAddNode(network, Id.createNodeId("D"), new Coord(140, 140));

		assertThat(matrix.getFreeSpeedTravelTime(nodeC, nodeC)).isEqualTo(0);
		assertThat(matrix.getFreeSpeedTravelTime(nodeC, nodeD)).isEqualTo(10);
		assertThat(matrix.getFreeSpeedTravelTime(nodeD, nodeC)).isEqualTo(20);
		assertThat(matrix.getFreeSpeedTravelTime(nodeD, nodeD)).isEqualTo(0);
	}
}
