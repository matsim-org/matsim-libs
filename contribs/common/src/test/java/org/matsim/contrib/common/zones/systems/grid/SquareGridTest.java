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

package org.matsim.contrib.common.zones.systems.grid;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneImpl;
import org.matsim.core.network.NetworkUtils;

import static org.assertj.core.api.Assertions.*;
import static org.matsim.contrib.common.zones.systems.grid.SquareGridZoneSystem.EPSILON;

/**
 * @author Michal Maciejewski (michalm)
 */
public class SquareGridTest {
	@Test
	void emptyNodes_fail() {
		assertThatThrownBy(() -> new SquareGridZoneSystem(NetworkUtils.createNetwork(), 100)).isExactlyInstanceOf(IllegalArgumentException.class)
				.hasMessage("Cannot create SquareGrid if no nodes");
	}

	@Test
	void outsideBoundaries_withinEpsilon_success() {
		Node node_0_0 = node(0, 0);
		Network network = NetworkUtils.createNetwork();
		network.addNode(node_0_0);
		SquareGridZoneSystem grid = new SquareGridZoneSystem(network, 100);
		assertThatCode(() -> grid.getZoneForCoord(new Coord(-EPSILON, EPSILON))).doesNotThrowAnyException();
	}

	@Test
	void outsideBoundaries_outsideEpsilon_fail() {
		Node node_0_0 = node(0, 0);
		Network network = NetworkUtils.createNetwork();
		network.addNode(node_0_0);
		SquareGridZoneSystem grid = new SquareGridZoneSystem(network, 100);

		assertThatThrownBy(() -> grid.getZoneForCoord(new Coord(-2 * EPSILON, 0))).isExactlyInstanceOf(
				IllegalArgumentException.class);
	}

	@Test
	void testGrid() {
		Node node_0_0 = node(0, 0);
		Node node_150_150 = node(150, 150);

		Network network = NetworkUtils.createNetwork();
		network.addNode(node_0_0);
		network.addNode(node_150_150);
		SquareGridZoneSystem grid = new SquareGridZoneSystem(network, 100);

		Coord coord0 = new Coord(100 - 2 * EPSILON, 100 - 2 * EPSILON);
		Zone zone0 = new ZoneImpl(Id.create(0, Zone.class), null, new Coord(49, 49), "square");
		assertThat(grid.getZoneForCoord(coord0).orElseThrow()).isEqualToComparingFieldByField(zone0);

		Coord coord1 = new Coord(100 - EPSILON, 100 - EPSILON);
		Zone zone1 = new ZoneImpl(Id.create(3, Zone.class), null, new Coord(149, 149), "square");
		assertThat(grid.getZoneForCoord(coord1).orElseThrow()).isEqualToComparingFieldByField(zone1);
	}

	private Node node(double x, double y) {
		return NetworkUtils.createNode(Id.createNodeId(x + "," + y), new Coord(x, y));
	}
}
