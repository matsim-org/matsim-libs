/* *********************************************************************** *
 * project: org.matsim.*
 * QuadTreeWrapper.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package org.matsim.contrib.sna.gis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.quadtree.Quadtree;

/**
 * Representation of a spatial index containing zones backed by a quadtree.
 * 
 * @author illenberger
 *
 */
public class ZoneLayer {

	private final SpatialIndex quadtree;
	
	private final Set<Zone> zones;
	
	/**
	 * Creates a new zone layer containing the zones in <tt>zones</tt>.
	 * 
	 * @param zones a set of zones.
	 */
	public ZoneLayer(Set<Zone> zones) {
		this.zones = Collections.unmodifiableSet(zones);
		quadtree = new Quadtree();
		for(Zone zone : zones) {
			quadtree.insert(zone.getGeometry().getEnvelopeInternal(), zone);
		}
	}

	@SuppressWarnings("unchecked")
	private List<Zone> getZones(Point point) {
		List<Zone> result = quadtree.query(point.getEnvelopeInternal());
		List<Zone> zones = new ArrayList<Zone>(result.size());
		for(Zone z : result) {
			if(z.getGeometry().contains(point))
				zones.add(z);
		}
		return zones;
	}
	
	/**
	 * Returns the zone containing <tt>point</tt>. If multiple zones contain
	 * <tt>point</tt> one random zone is returned.
	 * 
	 * @param point a point geometry
	 * @return the zone containing <tt>point</tt>, or <tt>null</tt> if no zone contains <tt>point</tt>.
	 */
	public Zone getZone(Point point) {
		List<Zone> zones = getZones(point);
		if(zones.isEmpty())
			return null;
		else
			return zones.get(0);
	}
	
	/**
	 * Returns a set of all zones.
	 * 
	 * @return a set of all zones.
	 */
	public Set<Zone> getZones() {
		return zones;
	}
}
