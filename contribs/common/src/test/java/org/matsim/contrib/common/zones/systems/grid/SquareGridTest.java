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
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.impl.CoordinateArraySequence;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneImpl;
import org.matsim.contrib.common.zones.systems.grid.square.SquareGridZoneSystem;
import org.matsim.core.network.NetworkUtils;

import static org.assertj.core.api.Assertions.*;

/**
 * @author Michal Maciejewski (michalm)
 */
public class SquareGridTest {
	@Test
	void emptyNodes_fail() {
		assertThatThrownBy(() -> new SquareGridZoneSystem(NetworkUtils.createNetwork(), 100, zone -> true)).isExactlyInstanceOf(IllegalArgumentException.class)
				.hasMessage("Cannot create SquareGrid if no nodes");
	}

	@Test
	void outsideBoundaries_withinEpsilon_success() {
		Node node_0_0 = node(0, 0);
		Network network = NetworkUtils.createNetwork();
		network.addNode(node_0_0);
		SquareGridZoneSystem grid = new SquareGridZoneSystem(network, 100, zone -> true);
		assertThatCode(() -> grid.getZoneForCoord(new Coord(-0, 0))).doesNotThrowAnyException();
	}

	@Test
	void outsideBoundaries_outsideEpsilon_fail() {
		Node node_0_0 = node(0, 0);
		Network network = NetworkUtils.createNetwork();
		network.addNode(node_0_0);
		SquareGridZoneSystem grid = new SquareGridZoneSystem(network, 100, zone -> true);

		assertThatThrownBy(() -> grid.getZoneForCoord(new Coord(-2, 0))).isExactlyInstanceOf(
				IllegalArgumentException.class);
	}

	@Test
	void testGrid() {
		Node node_0_0 = node(0, 0);
		Node node_150_150 = node(150, 150);

		Network network = NetworkUtils.createNetwork();
		network.addNode(node_0_0);
		network.addNode(node_150_150);
		SquareGridZoneSystem grid = new SquareGridZoneSystem(network, 100, zone -> true);

		Coord coord0 = new Coord(100, 100);
		CoordinateSequence coordinateSequence = getCoordinateSequence();

		PreparedPolygon polygon = new PreparedPolygon(new GeometryFactory().createPolygon(coordinateSequence));
		Zone zone0 = new ZoneImpl(Id.create(3, Zone.class), polygon, new Coord(150, 150), "square");
		assertThat(grid.getZoneForCoord(coord0).orElseThrow()).isEqualToComparingFieldByFieldRecursively(zone0);


	}

	private static CoordinateSequence getCoordinateSequence() {
		CoordinateSequence coordinateSequence = new CoordinateArraySequence(5);

		coordinateSequence.setOrdinate(0,0, 100);
		coordinateSequence.setOrdinate(0,1, 100);

		coordinateSequence.setOrdinate(1,0, 200);
		coordinateSequence.setOrdinate(1,1, 100);

		coordinateSequence.setOrdinate(2,0, 200);
		coordinateSequence.setOrdinate(2,1, 200);

		coordinateSequence.setOrdinate(3,0, 100);
		coordinateSequence.setOrdinate(3,1, 200);

		coordinateSequence.setOrdinate(4,0, 100);
		coordinateSequence.setOrdinate(4,1, 100);
		return coordinateSequence;
	}

	private Node node(double x, double y) {
		return NetworkUtils.createNode(Id.createNodeId(x + "," + y), new Coord(x, y));
	}
}
