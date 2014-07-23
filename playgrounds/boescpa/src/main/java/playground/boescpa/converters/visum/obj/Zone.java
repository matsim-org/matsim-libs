/*
 * *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package playground.boescpa.converters.visum.obj;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import org.opengis.feature.simple.SimpleFeature;

import java.util.Collection;

/**
 * Provides a representation of zones.
 *
 * @author boescpa
 */
public class Zone {
	
	private static final GeometryFactory factory = new GeometryFactory();
	
	private final SimpleFeature features;
	private final Geometry geometry;
	private final Long zoneId;
	private final String name;
	public Long getZoneId() {
		return zoneId;
	}
	public String getName() {
		return name;
	}

	public Zone(SimpleFeature features) {
		this.features = features;
		this.geometry = (Geometry) features.getDefaultGeometry();
		this.name = (String) features.getAttribute("NAME");
		Number id = (Number) features.getAttribute("NO");
		if (id instanceof Long) {
			this.zoneId = (Long) id;
		}
		else if (id instanceof Integer) {
			this.zoneId = (id).longValue();
		}
		else {
			this.zoneId = null;
		}
	}

	public Zone(Geometry geometry, Long zoneId, String name) {
		this.features = null;
		this.geometry = geometry;
		this.name = name;
		this.zoneId = zoneId;
	}

	public boolean isWithinZone(double xCoord, double yCoord) {
		Point point = factory.createPoint(new Coordinate(xCoord,yCoord));
		return geometry.contains(point);
	}

	public double getDistToCentroid(double XCoord, double YCoord) {
		Point point = factory.createPoint(new Coordinate(XCoord, YCoord));
		return point.distance(this.geometry);
	}
	
	public static Zone mergeZones(Collection<Zone> zones) {
		Geometry mergedGeometry = null;
		
		for (Zone zone : zones) {
			if (mergedGeometry == null) {
				mergedGeometry = (Geometry) ((Geometry) zone.features.getDefaultGeometry()).clone();
			}
			else {
				mergedGeometry = mergedGeometry.union((Geometry) zone.features.getDefaultGeometry());
			}
		}
		
		return new Zone(mergedGeometry,(long) 0,"mergedZone");
	}
	
}
