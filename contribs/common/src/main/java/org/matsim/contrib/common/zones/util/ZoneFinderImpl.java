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

package org.matsim.contrib.common.zones.util;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.quadtree.Quadtree;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.common.zones.Zone;
import org.matsim.core.utils.geometry.geotools.MGC;

public class ZoneFinderImpl implements ZoneFinder {
	private final SpatialIndex quadTree = new Quadtree();
	private final double expansionDistance;

	public ZoneFinderImpl(Map<Id<Zone>, Zone> zones, double expansionDistance) {
		this.expansionDistance = expansionDistance;

		for (Zone z : zones.values()) {
			quadTree.insert(z.getPreparedGeometry().getGeometry().getEnvelopeInternal(), z);
		}
	}
	public ZoneFinderImpl(Map<Id<Zone>, Zone> zones) {
		this.expansionDistance = Double.MIN_VALUE;

		for (Zone z : zones.values()) {
			quadTree.insert(z.getPreparedGeometry().getGeometry().getEnvelopeInternal(), z);
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public Optional<Zone> findZone(Coord coord) {
		Point point = MGC.coord2Point(coord);
		Envelope env = point.getEnvelopeInternal();

		Zone zone = getSmallestZoneContainingPoint(quadTree.query(env), point);
		if (zone != null) {
			return Optional.of(zone);
		}

		if (expansionDistance > 0) {
			env.expandBy(expansionDistance);
			zone = getNearestZone(quadTree.query(env), point);
		}

		return Optional.ofNullable(zone);
	}

	private Zone getSmallestZoneContainingPoint(List<Zone> zones, Point point) {
		if (zones.size() == 1) {// almost 100% cases
			return zones.get(0);
		}

		double minArea = Double.MAX_VALUE;
		Zone smallestZone = null;

		for (Zone z : zones) {
			if (z.getPreparedGeometry().contains(point)) {
				double area = z.getPreparedGeometry().getGeometry().getArea();
				if (area < minArea) {
					minArea = area;
					smallestZone = z;
				}
			}
		}

		return smallestZone;
	}

	private Zone getNearestZone(List<Zone> zones, Point point) {
		if (zones.size() == 1) {
			return zones.get(0);
		}

		double minDistance = Double.MAX_VALUE;
		Zone nearestZone = null;

		for (Zone z : zones) {
			double distance = z.getPreparedGeometry().getGeometry().distance(point);
			if (distance <= expansionDistance) {
				if (distance < minDistance) {
					minDistance = distance;
					nearestZone = z;
				}
			}
		}

		return nearestZone;
	}

}
