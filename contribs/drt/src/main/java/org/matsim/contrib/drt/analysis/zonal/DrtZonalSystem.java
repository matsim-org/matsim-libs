/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.drt.analysis.zonal;

import static java.util.stream.Collectors.toMap;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.utils.geometry.geotools.MGC;

/**
 * @author jbischoff
 * @author Michal Maciejewski (michalm)
 */
public class DrtZonalSystem {
	public static DrtZonalSystem createFromPreparedGeometries(Network network,
			Map<String, PreparedGeometry> geometries) {
		return new DrtZonalSystem(network, geometries.entrySet()
				.stream()
				.collect(toMap(Entry::getKey, e -> new DrtZone(e.getKey(), e.getValue()))));
	}

	public static DrtZonalSystem createFromGeometries(Network network, Map<String, Geometry> geometries) {
		return new DrtZonalSystem(network, geometries.entrySet()
				.stream()
				.collect(toMap(Entry::getKey, e -> new DrtZone(e.getKey(), e.getValue()))));
	}

	private static final DrtZone NO_ZONE = new DrtZone(null, null, null, null);

	private final Map<Id<Link>, DrtZone> link2zone = new HashMap<>();
	private final Network network;
	private final Map<String, DrtZone> zones;

	public DrtZonalSystem(Network network, Map<String, DrtZone> zones) {
		this.network = network;
		this.zones = zones;
	}

	public DrtZone getZoneForLinkId(Id<Link> linkId) {
		DrtZone zone = link2zone.get(linkId);
		if (zone != null) {
			return zone == NO_ZONE ? null : zone;
		}

		Point linkCoord = MGC.coord2Point(network.getLinks().get(linkId).getCoord());
		for (DrtZone z : zones.values()) {
			if (intersects(z, linkCoord)) {
				//if a link Coord borders two or more cells, the allocation to a cell is random.
				// Seems hard to overcome, but most likely better than returning no zone at
				// all and mostly not too relevant in non-grid networks.
				// jb, june 2019
				link2zone.put(linkId, z);
				return z;
			}
		}

		link2zone.put(linkId, NO_ZONE);
		return null;
	}

	private boolean intersects(DrtZone zone, Point point) {
		PreparedGeometry preparedGeometry = zone.getPreparedGeometry();
		return preparedGeometry != null ? preparedGeometry.intersects(point) : zone.getGeometry().intersects(point);
	}

	/**
	 * @return the zones
	 */
	public Map<String, DrtZone> getZones() {
		return zones;
	}
}
