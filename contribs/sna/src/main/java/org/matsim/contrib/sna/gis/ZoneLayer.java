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

import java.util.Collections;
import java.util.List;
import java.util.Set;


import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.quadtree.Quadtree;

/**
 * Representation of a spatial index containing zones backed by a quadtree.
 * 
 * @author illenberger
 *
 */
public class ZoneLayer {

	private final Quadtree quadtree;
	
	private final Set<Zone> zones;
	
	/**
	 * Creates a new zone layer containing the zones in <tt>zones</tt>.
	 * 
	 * @param zones a set of zones.
	 */
	public ZoneLayer(Set<Zone> zones) {
		this.zones = Collections.unmodifiableSet(zones);
		quadtree = new Quadtree();
	}
	
	/**
	 * Returns a list of zones that intersects <tt>env</tt>.
	 * 
	 * @param env a search envelope.
	 * @return a list of zones.
	 * @see {@link Quadtree#query(Envelope)}
	 */
	@SuppressWarnings("unchecked")
	public List<Zone> getZones(Envelope env) {
		return quadtree.query(env);
	}

	/**
	 * Returns the zone containing <tt>point</tt>. If multiple zones contain
	 * <tt>point</tt> one random zone is returned.
	 * 
	 * @param point a point geometry
	 * @return the zone containing <tt>point</tt>, or <tt>null</tt> if no zone contains <tt>point</tt>.
	 */
	public Zone getZone(Point point) {
		List<Zone> zones = getZones(point.getEnvelopeInternal());
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
