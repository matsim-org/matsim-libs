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

package org.matsim.contrib.zone;

import static org.assertj.core.api.Assertions.*;
import static org.matsim.contrib.zone.SquareGrid.EPSILON;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.NetworkUtils;

/**
 * @author Michal Maciejewski (michalm)
 */
public class SquareGridTest {
	@Test
	void emptyNodes_fail() {
		assertThatThrownBy(() -> new SquareGrid(List.of(), 100)).isExactlyInstanceOf(IllegalArgumentException.class)
				.hasMessage("Cannot create SquareGrid if no nodes");
	}

	@Test
	void outsideBoundaries_withinEpsilon_success() {
		Node node_0_0 = node(0, 0);
		SquareGrid grid = new SquareGrid(List.of(node_0_0), 100);
		assertThatCode(() -> grid.getZone(new Coord(-EPSILON, EPSILON))).doesNotThrowAnyException();
	}

	@Test
	void outsideBoundaries_outsideEpsilon_fail() {
		Node node_0_0 = node(0, 0);
		SquareGrid grid = new SquareGrid(List.of(node_0_0), 100);
		assertThatThrownBy(() -> grid.getZone(new Coord(-2 * EPSILON, 0))).isExactlyInstanceOf(
				IllegalArgumentException.class);
	}

	@Test
	void testLazyZoneCreation() {
		Node node_0_0 = node(0, 0);
		SquareGrid grid = new SquareGrid(List.of(node_0_0), 100);

		Coord coord = new Coord(0, 0);
		Zone zone = new Zone(Id.create(0, Zone.class), "square", new Coord(49, 49));
		assertThat(grid.getZone(coord)).isNull();
		assertThat(grid.getOrCreateZone(coord)).isEqualToComparingFieldByField(zone);
		assertThat(grid.getZone(coord)).isEqualToComparingFieldByField(zone);
	}

	@Test
	void testGrid() {
		Node node_0_0 = node(0, 0);
		Node node_150_150 = node(150, 150);
		SquareGrid grid = new SquareGrid(List.of(node_0_0, node_150_150), 100);

		Coord coord0 = new Coord(100 - 2 * EPSILON, 100 - 2 * EPSILON);
		Zone zone0 = new Zone(Id.create(0, Zone.class), "square", new Coord(49, 49));
		assertThat(grid.getOrCreateZone(coord0)).isEqualToComparingFieldByField(zone0);

		Coord coord1 = new Coord(100 - EPSILON, 100 - EPSILON);
		Zone zone1 = new Zone(Id.create(3, Zone.class), "square", new Coord(149, 149));
		assertThat(grid.getOrCreateZone(coord1)).isEqualToComparingFieldByField(zone1);
	}

	private Node node(double x, double y) {
		return NetworkUtils.createNode(Id.createNodeId(x + "," + y), new Coord(x, y));
	}
}
