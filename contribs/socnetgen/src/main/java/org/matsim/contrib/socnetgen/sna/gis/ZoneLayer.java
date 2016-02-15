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
package org.matsim.contrib.socnetgen.sna.gis;

import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.index.SpatialIndex;
import com.vividsolutions.jts.index.quadtree.Quadtree;
import org.geotools.referencing.CRS;
import org.matsim.contrib.common.gis.CRSUtils;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.opengis.referencing.operation.MathTransform;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Representation of a spatial index containing zones backed by a quadtree.
 * 
 * @author illenberger
 *
 */
public class ZoneLayer<T> {

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
//		quadtree = new STRtree();
		for(Zone<T> zone : zones) {
			quadtree.insert(zone.getGeometry().getEnvelopeInternal(), zone);
		}
	}

	public CoordinateReferenceSystem getCRS() {
		return crs;
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
//			zone.getFeature().getDefaultGeometry().setSRID(srid);
			zone.getGeometry().setSRID(srid);
		}
	}

	@SuppressWarnings("unchecked")
	private List<Zone<T>> getZones(Point point) {
		if(point.getSRID() != srid)
			point = transformPoint(point);
		
		Envelope env = point.getEnvelopeInternal();
		List<Zone<T>> result = quadtree.query(env);
		List<Zone<T>> zones = new ArrayList<Zone<T>>(result.size());
		for(Zone<T> z : result) {
			if(z.getGeometry().getEnvelopeInternal().contains(point.getCoordinate())) {
				if(z.getPreparedGeometry().contains(point)) {
					zones.add(z);
					break; // assumes that zones do not overlap!
				}
			}
			
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
		else if(zones.size() == 1) {
			return zones.get(0);
		} else {
			/*
			 * FIXME
			 * Apparently this makes huge problems. Need to find a more elegant solution.
			 */
			System.err.println("Overlapping zones!");
			Geometry geo0 = zones.get(0).getGeometry();
			Geometry geo1 = zones.get(1).getGeometry();
			if(geo0.contains(geo1)) {
				return zones.get(1);
			} else {
				return zones.get(0);
			}
		}
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
