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

import com.google.common.base.Preconditions;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.GridZoneSystem;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneImpl;
import org.matsim.contrib.common.zones.ZoneSystem;
import org.matsim.core.network.NetworkUtils;

import java.util.*;

public class SquareGridZoneSystem implements GridZoneSystem {

	static final double EPSILON = 1;

	private final double cellSize;

	private double minX;
	private double minY;
	private double maxX;
	private double maxY;

	private final int cols;

    private final Zone[] internalZones;

	private final IdMap<Zone, Zone> zones = new IdMap<>(Zone.class);

	private final IdMap<Zone, List<Link>> zoneToLinksMap = new IdMap<>(Zone.class);
	private final Network network;


	public SquareGridZoneSystem(Network network, double cellSize) {
		Preconditions.checkArgument(!network.getNodes().isEmpty(), "Cannot create SquareGrid if no nodes");

		this.network = network;
		this.cellSize = cellSize;

		initBounds();

        int rows = (int) Math.ceil((maxY - minY) / cellSize);
		this.cols = (int)Math.ceil((maxX - minX) / cellSize);
		this.internalZones = new Zone[rows * cols];
	}

	@Override
	public Map<Id<Zone>, Zone> getZones() {
		return Collections.unmodifiableMap(zones);
	}

	@Override
	public Optional<Zone> getZoneForLinkId(Id<Link> linkId) {
		return getZoneForNodeId(network.getLinks().get(linkId).getToNode().getId());
	}

	@Override
	public Optional<Zone> getZoneForNodeId(Id<Node> nodeId) {
		return Optional.of(getOrCreateZone(network.getNodes().get(nodeId).getCoord()));
	}

	@Override
	public Optional<Zone> getZoneForCoord(Coord coord) {
		return Optional.of(getOrCreateZone(coord));
	}

	@Override
	public List<Link> getLinksForZoneId(Id<Zone> zone) {
		return zoneToLinksMap.get(zone);
	}

	private Zone getOrCreateZone(Coord coord) {
		int index = getIndex(coord);
		Zone zone = internalZones[index];
		if (zone == null) {
			double x0 = minX + cellSize / 2;
			double y0 = minY + cellSize / 2;
			int r = bin(coord.getY(), minY);
			int c = bin(coord.getX(), minX);
			Coord centroid = new Coord(c * cellSize + x0, r * cellSize + y0);
			zone = new ZoneImpl(Id.create(index, Zone.class), null, centroid, "square");
			internalZones[index] = zone;
			zones.put(zone.getId(), zone);

			for (Link link : network.getLinks().values()) {
				if(getIndex(link.getToNode().getCoord()) == index) {
					List<Link> links = zoneToLinksMap.computeIfAbsent(zone.getId(), zoneId -> new ArrayList<>());
					links.add(link);
				}
			}
		}
		return zone;
	}


	// This method's content has been copied from NetworkImpl
	private void initBounds() {
		double[] boundingBox = NetworkUtils.getBoundingBox(network.getNodes().values());

		minX = boundingBox[0] - EPSILON;
		minY = boundingBox[1] - EPSILON;
		maxX = boundingBox[2] + EPSILON;
		maxY = boundingBox[3] + EPSILON;
		// yy the above four lines are problematic if the coordinate values are much smaller than one. kai, oct'15
	}


	private int getIndex(Coord coord) {
		Preconditions.checkArgument(coord.getX() >= minX, "Coord.x less than minX");
		Preconditions.checkArgument(coord.getX() <= maxX, "Coord.x greater than maxX");
		Preconditions.checkArgument(coord.getY() >= minY, "Coord.y less than minY");
		Preconditions.checkArgument(coord.getY() <= maxY, "Coord.y greater than maxY");
		int r = bin(coord.getY(), minY);
		int c = bin(coord.getX(), minX);
		return r * cols + c;
	}

	private int bin(double coord, double minCoord) {
		return (int)((coord - minCoord) / cellSize);
	}
}
