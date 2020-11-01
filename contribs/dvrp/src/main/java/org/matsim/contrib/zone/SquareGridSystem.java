/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package org.matsim.contrib.zone;

import static java.util.stream.Collectors.toMap;

import java.util.Collection;
import java.util.Collections;
import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Node;

public class SquareGridSystem implements ZonalSystem {
	private final SquareGrid grid;
	private final Map<Id<Zone>, Zone> zones;

	public SquareGridSystem(Collection<? extends Node> nodes, double cellSize) {
		this.grid = new SquareGrid(nodes, cellSize);
		zones = nodes.stream()
				.map(n -> grid.getOrCreateZone(n.getCoord()))
				.collect(toMap(Zone::getId, z -> z, (z1, z2) -> z1));
	}

	@Override
	public Map<Id<Zone>, Zone> getZones() {
		return Collections.unmodifiableMap(zones);
	}

	@Override
	public Zone getZone(Node node) {
		return grid.getZone(node.getCoord());
	}
}
