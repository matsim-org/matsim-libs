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

package org.matsim.contrib.common.zones.systems.grid;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneSystem;

import javax.annotation.Nullable;
import java.util.*;

import static java.util.stream.Collectors.toMap;

public class SquareGridSystem implements ZoneSystem {
	private final SquareGrid grid;
	private final Map<Id<Zone>, Zone> zones;

	private final IdMap<Zone, List<Link>> zoneToLinksMap = new IdMap<>(Zone.class);
	private final Network network;


	public SquareGridSystem(Network network, double cellSize) {
		this.network = network;
		this.grid = new SquareGrid(network.getNodes().values(), cellSize);
		zones = network.getNodes().values().stream()
			.map(n -> grid.getOrCreateZone(n.getCoord()))
			.collect(toMap(Zone::getId, z -> z, (z1, z2) -> z1));
		for (Link link : network.getLinks().values()) {
			Id<Zone> zoneId = grid.getZone(link.getToNode().getCoord()).getId();
			List<Link> links = zoneToLinksMap.computeIfAbsent(zoneId, zoneId1 -> new ArrayList<>());
			links.add(link);
		}
	}

	@Override
	public Map<Id<Zone>, Zone> getZones() {
		return Collections.unmodifiableMap(zones);
	}

	@Nullable
	@Override
	public Zone getZoneForLink(Id<Link> link) {
		return grid.getZone(network.getLinks().get(link).getToNode().getCoord());
	}

	@Override
	public Zone getZoneForNode(Node node) {
		return grid.getZone(node.getCoord());
	}

	@Override
	public List<Link> getLinksForZone(Id<Zone> zone) {
		return zoneToLinksMap.get(zone);
	}
}
