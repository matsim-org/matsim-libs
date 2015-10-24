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

import com.vividsolutions.jts.geom.*;
import org.matsim.contrib.socnetgen.sna.gis.Zone;
import org.matsim.contrib.socnetgen.sna.gis.ZoneLayer;
import org.matsim.contrib.socnetgen.sna.graph.analysis.VertexProperty;
import org.matsim.contrib.socnetgen.sna.graph.spatial.SpatialVertex;
import playground.johannes.socialnetworks.gis.PointUtils;
import playground.johannes.socialnetworks.graph.spatial.analysis.ZoneUtils;

import java.util.HashSet;
import java.util.Set;

/**
 * @author illenberger
 *
 */
public class VertexPropertyGrid {

	public static ZoneLayer<Double> createMeanGrid(Set<? extends SpatialVertex> vertices, VertexProperty property) {
		return createMeanGrid(vertices, property, 1000.0);
	}
	
	public static ZoneLayer<Double> createMeanGrid(Set<? extends SpatialVertex> vertices, VertexProperty property, double resolution) {
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
		
		ZoneLayer<Set<SpatialVertex>> vertexLayer = ZoneUtils.createGridLayer(resolution, polygon);
		
		ZoneUtils.fillZoneLayer(vertexLayer, (Set<SpatialVertex>)vertices);
		
		ZoneLayer<Double> valueLayer = ZoneUtils.createGridLayer(resolution, polygon);
		
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
