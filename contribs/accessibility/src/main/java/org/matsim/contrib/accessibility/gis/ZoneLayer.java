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
package org.matsim.contrib.accessibility.gis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.geotools.api.referencing.FactoryException;
import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.api.referencing.operation.MathTransform;
import org.geotools.referencing.CRS;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.index.SpatialIndex;
import org.locationtech.jts.index.quadtree.Quadtree;

/**
 * Representation of a spatial index containing zones backed by a quadtree.
 * 
 * @author illenberger
 *
 */
public final class ZoneLayer<T> {

	private final SpatialIndex quadtree;
	
	private final Set<Zone<T>> zones;
	
	private CoordinateReferenceSystem crs;
	
	private int srid = -1;
	
	/**
	 * Creates a new zone layer containing the zones in <tt>zones</tt>.
	 * 
	 * @param zones a set of zones.
	 */
	public ZoneLayer(Set<Zone<T>> zones) {
		for(Zone<T> z : zones) {
			if(srid < 0) {
				srid = z.getGeometry().getSRID();
				crs = CRSUtils.getCRS(srid);
			} else {
				if(z.getGeometry().getSRID() != srid)
					throw new RuntimeException("Cannot build a spatial index with zones that have different coordinate reference systems.");
			}
		}
		
		this.zones = Collections.unmodifiableSet(zones);
		quadtree = new Quadtree();
		for(Zone<T> zone : zones) {
			quadtree.insert(zone.getGeometry().getEnvelopeInternal(), zone);
		}
	}

	/**
	 * Allows to manually overwrite the coordinate reference system (e.g. if the
	 * ZoneLayer is read from a shape file without crs information).
	 * 
	 * @param crs a coordinate reference system.
	 */
	public void overwriteCRS(CoordinateReferenceSystem crs) {
		this.crs = crs;
		this.srid = CRSUtils.getSRID(crs);
		for(Zone<?> zone : zones) {
			zone.getGeometry().setSRID(srid);
		}
	}

	@SuppressWarnings("unchecked")
	private List<Zone<T>> getZones(Point point) {
		if(point.getSRID() != srid)
			point = transformPoint(point);
		
		List<Zone<T>> result = quadtree.query(point.getEnvelopeInternal());
		List<Zone<T>> zones = new ArrayList<Zone<T>>(result.size());
		for(Zone<T> z : result) {
			if(z.getGeometry().contains(point))
				zones.add(z);
		}
		return zones;
	}
	
	private Point transformPoint(Point point) {
		CoordinateReferenceSystem sourceCRS = CRSUtils.getCRS(point.getSRID());
		CoordinateReferenceSystem targetCRS = crs;

		try {
			MathTransform transform = CRS.findMathTransform(sourceCRS, targetCRS);
			return CRSUtils.transformPoint(point, transform);
		} catch (FactoryException e) {
			e.printStackTrace();
			return null;
		}	
	}
	/**
	 * Returns the zone containing <tt>point</tt>. If multiple zones contain
	 * <tt>point</tt> one random zone is returned.
	 * 
	 * @param point a point geometry
	 * @return the zone containing <tt>point</tt>, or <tt>null</tt> if no zone contains <tt>point</tt>.
	 */
	public Zone<T> getZone(Point point) {
		List<Zone<T>> zones = getZones(point);
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
	public Set<Zone<T>> getZones() {
		return zones;
	}
}
