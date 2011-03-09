/* *********************************************************************** *
 * project: org.matsim.*
 * VertexPropertyGrid.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.johannes.socialnetworks.survey.ivt2009.analysis;

import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.gis.Zone;
import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.contrib.sna.graph.analysis.VertexProperty;
import org.matsim.contrib.sna.graph.spatial.SpatialVertex;

import playground.johannes.socialnetworks.gis.PointUtils;
import playground.johannes.socialnetworks.graph.spatial.analysis.ZoneUtils;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * @author illenberger
 *
 */
public class VertexPropertyGrid {

	public static ZoneLayer<Double> createMeanGrid(Set<? extends SpatialVertex> vertices, VertexProperty property) {
		Set<Point> points = new HashSet<Point>(vertices.size());
		for(SpatialVertex vertex : vertices)
			if(vertex.getPoint() != null)
				points.add(vertex.getPoint());
		
		Envelope env = PointUtils.envelope(points);
		
		Coordinate[] coords = new Coordinate[5];
		coords[0] = new Coordinate(env.getMinX(), env.getMinY());
		coords[1] = new Coordinate(env.getMaxX(), env.getMinY());
		coords[2] = new Coordinate(env.getMaxX(), env.getMaxY());
		coords[3] = new Coordinate(env.getMinX(), env.getMaxY());
		coords[4] = coords[0];
		
		GeometryFactory factory = new GeometryFactory();
		
		LinearRing linearRing = factory.createLinearRing(coords);
		Polygon polygon = factory.createPolygon(linearRing, null);
		polygon.setSRID(points.iterator().next().getSRID());
		
		ZoneLayer<Set<SpatialVertex>> vertexLayer = ZoneUtils.createGridLayer(1000.0, polygon);
		
		ZoneUtils.fillZoneLayer(vertexLayer, (Set<SpatialVertex>)vertices);
		
		ZoneLayer<Double> valueLayer = ZoneUtils.createGridLayer(1000.0, polygon);
		
		for(Zone<Set<SpatialVertex>> zone : vertexLayer.getZones()) {
			if(zone.getAttribute() != null) {
				double mean = property.statistics(zone.getAttribute()).getMean();
				Point p = zone.getGeometry().getCentroid();
				p.setSRID(polygon.getSRID());
				Zone<Double> valZone = valueLayer.getZone(p);
				valZone.setAttribute(mean);
			}
		}
		
		return valueLayer;
	}
}
