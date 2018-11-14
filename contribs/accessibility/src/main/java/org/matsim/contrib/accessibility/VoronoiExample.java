/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.contrib.accessibility;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.utils.gis.ShapeFileWriter;
import org.opengis.feature.simple.SimpleFeature;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

/**
 * @author dziemke
 */
public class VoronoiExample {
		
	public static void main(String[] args) {
		GeometryFactory geometryFactory = new GeometryFactory();
		
		Collection<Coordinate> sites = new ArrayList<>();
		sites.add(new Coordinate(70, 70));
		sites.add(new Coordinate(50, 150));
		sites.add(new Coordinate(150, 50));
		sites.add(new Coordinate(150, 150));
		sites.add(new Coordinate(250, 50));
		sites.add(new Coordinate(250, 150));
		sites.add(new Coordinate(350, 50));
		sites.add(new Coordinate(370, 170));
		
		VoronoiDiagramBuilder voronoiDiagramBuilder = new VoronoiDiagramBuilder();
		voronoiDiagramBuilder.setSites(sites);		
				
		List<Polygon> polygons = voronoiDiagramBuilder.getSubdivision().getVoronoiCellPolygons(geometryFactory);
		
		BoundingBox boundingBox = BoundingBox.createBoundingBox(0, 0, 400, 200);
		Polygon boundingPolygon = VoronoiGeometryUtils.createBoundingPolygon(boundingBox);
		Collection<Geometry> cutGeometries = VoronoiGeometryUtils.cutPolygonsByBoundary(polygons, boundingPolygon);
		Collection<SimpleFeature> features = VoronoiGeometryUtils.createFeaturesFromPolygons(cutGeometries);
	    ShapeFileWriter.writeGeometries(features, "/Users/dominik/voronoi_test.shp");
	}
}