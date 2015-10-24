/* *********************************************************************** *
 * project: org.matsim.*
 * ZoneUtils.java
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
package playground.johannes.socialnetworks.graph.spatial.analysis;

import com.vividsolutions.jts.geom.*;
import playground.johannes.sna.gis.Zone;
import playground.johannes.sna.gis.ZoneLayer;
import playground.johannes.sna.graph.spatial.SpatialVertex;

import java.util.HashSet;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class ZoneUtils {

	public static <T> ZoneLayer<T> createGridLayer(double resolution, Geometry boundary) {
		GeometryFactory factory = new GeometryFactory();
		Set<Zone<T>> zones = new HashSet<Zone<T>>();
		Envelope env = boundary.getEnvelopeInternal();
		
		for(double x = env.getMinX(); x < env.getMaxX(); x += resolution) {
			for(double y = env.getMinY(); y < env.getMaxY(); y += resolution) {
		
				Point point = factory.createPoint(new Coordinate(x, y));
				
				if(boundary.contains(point)) {
					
					Coordinate[] coords = new Coordinate[5];
					coords[0] = point.getCoordinate();
					coords[1] = new Coordinate(x, y + resolution);
					coords[2] = new Coordinate(x + resolution, y + resolution);
					coords[3] = new Coordinate(x + resolution, y);
					coords[4] = point.getCoordinate();
					
					LinearRing linearRing = factory.createLinearRing(coords);
					Polygon polygon = factory.createPolygon(linearRing, null);
					polygon.setSRID(boundary.getSRID());
					
					Zone<T> zone = new Zone<T>(polygon);
					
					zones.add(zone);
				}
			}
		}
		
		ZoneLayer<T> layer = new ZoneLayer<T>(zones);
		
		return layer;
	}
	
	public static <V extends SpatialVertex> ZoneLayer<Set<V>> fillZoneLayer(ZoneLayer<Set<V>> layer, Set<V> vertices) {
		for(V v : vertices) {
			Zone<Set<V>> zone = layer.getZone(v.getPoint());
			if(zone != null) {
				Set<V> set = zone.getAttribute();
				if(set == null) {
					set = new HashSet<V>();
					zone.setAttribute(set);
				}
				set.add(v);
			}
		}
		
		return layer;
	}
}
