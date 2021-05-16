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

import java.util.Map;

import org.junit.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.dvrp.router.TimeAsTravelDisutility;
import org.matsim.contrib.zone.Zone;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;

/**
 * @author Michal Maciejewski (michalm)
 */
public class TravelTimeMatricesTest {
	@Test
	public void test() {
		Network network = NetworkUtils.createNetwork();
		Node nodeA = NetworkUtils.createAndAddNode(network, Id.createNodeId("A"), new Coord(0, 0));
		Node nodeB = NetworkUtils.createAndAddNode(network, Id.createNodeId("B"), new Coord(150, 150));
		NetworkUtils.createAndAddLink(network, Id.createLinkId("AB"), nodeA, nodeB, 150, 15, 20, 1);
		NetworkUtils.createAndAddLink(network, Id.createLinkId("BA"), nodeB, nodeA, 300, 15, 40, 1);
		Zone zoneA = new Zone(Id.create("Zone_A", Zone.class), null);
		Zone zoneB = new Zone(Id.create("Zone_Z", Zone.class), null);

		var centralNodes = Map.of(zoneA, nodeA, zoneB, nodeB);
		var matrix = TravelTimeMatrices.calculateTravelTimeMatrix(network, centralNodes, 0, new FreeSpeedTravelTime(),
				new TimeAsTravelDisutility(new FreeSpeedTravelTime()), 1);

		assertThat(matrix.get(zoneA, zoneA)).isEqualTo(0);
		assertThat(matrix.get(zoneA, zoneB)).isEqualTo(10);
		assertThat(matrix.get(zoneB, zoneA)).isEqualTo(20);
		assertThat(matrix.get(zoneB, zoneB)).isEqualTo(0);
	}
}
