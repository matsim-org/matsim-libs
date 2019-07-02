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

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;

import javax.inject.Inject;

import org.apache.log4j.Logger;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.Point;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.dvrp.router.DvrpRoutingNetworkProvider;
import org.matsim.core.utils.geometry.geotools.MGC;

import com.google.inject.Provider;
import com.google.inject.name.Named;

/**
 * @author jbischoff
 */
public class DrtZonalSystem {

	private final Map<Id<Link>, String> link2zone = new LinkedHashMap<>();
	private final Network network;
	private final Map<String, Geometry> zones;

	public DrtZonalSystem(Network network, double cellSize) {
		this.network = network;
		zones = DrtGridUtils.createGridFromNetwork(network, cellSize);

	}

	public DrtZonalSystem(Network network, Map<String, Geometry> zones) {
		this.network = network;
		this.zones = zones;

	}

	public Geometry getZone(String zone) {
		return zones.get(zone);
	}

	public String getZoneForLinkId(Id<Link> linkId) {
		if (this.link2zone.containsKey(linkId)) {
			return link2zone.get(linkId);
		}

		Point linkCoord = MGC.coord2Point(network.getLinks().get(linkId).getCoord());

		for (Entry<String, Geometry> e : zones.entrySet()) {
			if (e.getValue().intersects(linkCoord)) {
				//if a link Coord borders two or more cells, the allocation to a cell is random.
				// Seems hard to overcome, but most likely better than returning no zone at
				// all and mostly not too relevant in non-grid networks.
				// jb, june 2019
				link2zone.put(linkId, e.getKey());
				return e.getKey();
			}
		}
		link2zone.put(linkId, null);
		return null;

	}

	/**
	 * @return the zones
	 */
	public Map<String, Geometry> getZones() {
		return zones;
	}

	public Coord getZoneCentroid(String zoneId) {

		Geometry zone = zones.get(zoneId);
		if (zone == null) {
			Logger.getLogger(getClass()).error("Zone " + zoneId + " not found.");
			return null;
		}
		Coord c = MGC.point2Coord(zone.getCentroid());
		return c;
	}

	public static class DrtZonalSystemProvider implements Provider<DrtZonalSystem> {
		@Inject
		@Named(DvrpRoutingNetworkProvider.DVRP_ROUTING)
		private Network network;

		private final double cellSize;

		public DrtZonalSystemProvider(double cellSize) {
			this.cellSize = cellSize;
		}

		@Override
		public DrtZonalSystem get() {
			return new DrtZonalSystem(network, cellSize);
		}
	}
}
