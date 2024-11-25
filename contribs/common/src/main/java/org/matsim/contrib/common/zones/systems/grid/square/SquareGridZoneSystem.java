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

package org.matsim.contrib.common.zones.systems.grid.square;

import com.google.common.base.Preconditions;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.geom.prep.PreparedPolygon;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.IdMap;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.common.zones.GridZoneSystem;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.contrib.common.zones.ZoneImpl;
import org.matsim.core.network.NetworkUtils;
import org.matsim.core.utils.geometry.GeometryUtils;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class SquareGridZoneSystem implements GridZoneSystem {

	private final double cellSize;
	private final Predicate<Zone> zoneFilter;

	private double minX;
	private double minY;
	private double maxX;
	private double maxY;

	private final int cols;
	private final int rows;

    private final Zone[] internalZones;

	private final IdMap<Zone, Zone> zones = new IdMap<>(Zone.class);

	private final Map<Integer, List<Link>> index2Links;
	private final Network network;


	public SquareGridZoneSystem(Network network, double cellSize) {
		this(network, cellSize, true, z -> true);
	}
	public SquareGridZoneSystem(Network network, double cellSize, Predicate<Zone> zoneFilter) {
		this(network, cellSize, true, zoneFilter);
	}
	public SquareGridZoneSystem(Network network, double cellSize, boolean filterByNetwork, Predicate<Zone> zoneFilter) {
		this.zoneFilter = zoneFilter;
		this.network = network;
		this.cellSize = cellSize;

		Preconditions.checkArgument(!network.getNodes().isEmpty(), "Cannot create SquareGrid if no nodes");

		initBounds();

        this.rows = Math.max(1, (int) Math.ceil((maxY - minY) / cellSize));
		this.cols = Math.max(1, (int)Math.ceil((maxX - minX) / cellSize));
		this.internalZones = new Zone[rows * cols +1];
		this.index2Links = getIndexToLink(network);

		if(filterByNetwork) {
			network.getLinks().values().forEach(l -> getOrCreateZone(l.getToNode().getCoord()));
		} else {
			for (double lx = minX; lx < maxX; lx += cellSize) {
				for (double by = minY; by < maxY; by += cellSize) {
					Coord coord = new Coord(lx, by);
					getOrCreateZone(coord);
				}
			}
		}
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
		return getOrCreateZone(network.getNodes().get(nodeId).getCoord());
	}

	@Override
	public Optional<Zone> getZoneForCoord(Coord coord) {
		return getOrCreateZone(coord);
	}

	@Override
	public List<Link> getLinksForZoneId(Id<Zone> zone) {
		return this.index2Links.get(Integer.parseInt(zone.toString()));
	}

	private Optional<Zone> getOrCreateZone(Coord coord) {
		int index = getIndex(coord);
		Zone zone = internalZones[index];
		if (zone == null) {
			int r = bin(coord.getY(), minY);
			int c = bin(coord.getX(), minX);
			PreparedPolygon geometry = getGeometry(r, c);
			zone = new ZoneImpl(Id.create(index, Zone.class), geometry, "square");

			if(zoneFilter.test(zone)) {
				internalZones[index] = zone;
				zones.put(zone.getId(), zone);
			} else {
				return Optional.empty();
			}
		}
		return Optional.of(zone);
	}

	private Map<Integer, List<Link>> getIndexToLink(Network network) {
		return network.getLinks().values().stream()
			.collect(Collectors.groupingBy(link -> getIndex(link.getToNode().getCoord())));
	}

	private PreparedPolygon getGeometry(int r, int c) {
		List<Coord> coords = new ArrayList<>();
		coords.add(new Coord(minX + c * cellSize, minY + r * cellSize));
		coords.add(new Coord(minX + (c + 1) * cellSize, minY + r * cellSize));
		coords.add(new Coord(minX + (c + 1 ) * cellSize, minY + (r + 1) * cellSize));
		coords.add(new Coord(minX + c * cellSize, minY + (r + 1) * cellSize));
		Polygon polygon = GeometryUtils.createGeotoolsPolygon(coords);
		return new PreparedPolygon(polygon);
	}


	// This method's content has been copied from NetworkImpl
	private void initBounds() {
		double[] boundingBox = NetworkUtils.getBoundingBox(network.getNodes().values());
		minX = boundingBox[0];
		minY = boundingBox[1];
		maxX = boundingBox[2];
		maxY = boundingBox[3];
		// yy the above four lines are problematic if the coordinate values are much smaller than one. kai, oct'15
	}


	private int getIndex(Coord coord) {
		Preconditions.checkArgument(coord.getX() >= minX, "Coord.x less than minX");
		Preconditions.checkArgument(coord.getX() <= maxX, "Coord.x greater than maxX");
		Preconditions.checkArgument(coord.getY() >= minY, "Coord.y less than minY");
		Preconditions.checkArgument(coord.getY() <= maxY, "Coord.y greater than maxY");
		int r;
        int c;
        if(coord.getY() == maxY) {
			r = Math.max(0, rows - 1);
		} else {
        	r = bin(coord.getY(), minY);
		}

		if(coord.getX() == maxX) {
			c = Math.max(0, cols - 1);
		} else {
			c = bin(coord.getX(), minX);
		}
		return r * cols + c;
	}

	private int bin(double coord, double minCoord) {
		return (int)((coord - minCoord) / cellSize);
	}
}
