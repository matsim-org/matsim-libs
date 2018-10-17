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
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;
import com.vividsolutions.jts.triangulate.VoronoiDiagramBuilder;

/**
 * @author dziemke
 */
public class VoronoiGeometryUtils {
	private final static Logger LOG = Logger.getLogger(VoronoiGeometryUtils.class);
	
	private static GeometryFactory geometryFactory = new GeometryFactory();
	
	
	static Map<Id<ActivityFacility>, Geometry> buildMeasurePointGeometryMap(ActivityFacilities measuringPoints, BoundingBox box) {
		LOG.warn("Started building measure-point-to-geometry map.");
		Map<Id<ActivityFacility>, Geometry> measurePointPolygons = new HashMap<>();
		
		Collection<Geometry> geometries = determineVoronoiShapes(measuringPoints, box);
		
		if (geometries.size() != measuringPoints.getFacilities().size()) {
			throw new RuntimeException("Number of Voronoi polygons and measure points must be equal.");
		}
		
		for (ActivityFacility measurePoint : measuringPoints.getFacilities().values()) {
			Id<ActivityFacility> measurePointId = measurePoint.getId();
			Point point = geometryFactory.createPoint(new Coordinate(measurePoint.getCoord().getX(), measurePoint.getCoord().getY()));
			
			boolean polygonFound = false;
			for (Geometry geometry : geometries) {
				if (geometry.contains(point)) {
					if (!measurePointPolygons.containsKey(measurePointId)) {
						measurePointPolygons.put(measurePointId, geometry);
						polygonFound = true;
					} else {
						throw new RuntimeException("Voronoi polygons can't contain more than one point.");
					}
				}
			}
			if (!polygonFound) {
				throw new RuntimeException("There must be one Voronoi polygons for each measure point.");
			}
		}
		LOG.warn("Finished building measure-point-to-geometry map.");
		return measurePointPolygons;
	}

	
	static Collection<Geometry> determineVoronoiShapes(ActivityFacilities measuringPoints, BoundingBox box) {
		LOG.warn("Started creating Voronoi shapes.");
		Collection<Coordinate> sites = new ArrayList<>();
		
		for (ActivityFacility measuringPoint : measuringPoints.getFacilities().values()) {
			Coordinate coordinate = new Coordinate(measuringPoint.getCoord().getX(), measuringPoint.getCoord().getY());
			sites.add(coordinate);
		}
		
		VoronoiDiagramBuilder voronoiDiagramBuilder = new VoronoiDiagramBuilder();
		voronoiDiagramBuilder.setSites(sites);		

		List<Polygon> polygons = voronoiDiagramBuilder.getSubdivision().getVoronoiCellPolygons(geometryFactory);
		
		Polygon boundingPolygon = createBoundingPolygon(box);
		Collection<Geometry> geometries = cutPolygonsByBoundary(polygons, boundingPolygon);
		
		LOG.warn("Finished creating Voronoi shapes.");
		return geometries;
	}

	
	static Polygon createBoundingPolygon(BoundingBox box) {
		Coordinate boundingBoxSW = new Coordinate(box.getXMin(), box.getYMin());
		Coordinate boundingBoxSE = new Coordinate(box.getXMax(), box.getYMin());
		Coordinate boundingBoxNE = new Coordinate(box.getXMax(), box.getYMax());
		Coordinate boundingBoxNW = new Coordinate(box.getXMin(), box.getYMax());
		Coordinate[] boundingBoxCoordinates = new Coordinate[]{boundingBoxSW, boundingBoxSE, boundingBoxNE, boundingBoxNW, boundingBoxSW};

		Polygon boundingPolygon = geometryFactory.createPolygon(boundingBoxCoordinates);
		return boundingPolygon;
	}
	
	
	static Collection<SimpleFeature> createFeaturesFromPolygons(Collection<Geometry> geometries) {
		Collection<SimpleFeature> features = new LinkedList<>();
	    		
	    SimpleFeatureTypeBuilder b = new SimpleFeatureTypeBuilder();
	    b.setName("polygon");
	    b.add("the_geom", Polygon.class);
	    b.add("id", Long.class);
	    SimpleFeatureType type = b.buildFeatureType();
	    SimpleFeatureBuilder fbuilder = new SimpleFeatureBuilder(type);

	    for (Geometry geometry : geometries) {
	        
	        Object[] values = new Object[]{geometry};
	        fbuilder.addAll(values);
	        SimpleFeature feature = fbuilder.buildFeature(null);
	        features.add(feature);
	    }
	    return features;
	}


	static Collection<Geometry> cutPolygonsByBoundary(Collection<Polygon> polygons, Polygon boundingPolygon) {
		Collection<Geometry> cutPolygons = new LinkedList<>();
		for (Polygon polygon : polygons) {
			// Need to determine convex hull of geometry to prevent it from staying self-intersecting
			Geometry hull = polygon.convexHull();
			Geometry intersection = hull.intersection(boundingPolygon);
			cutPolygons.add(intersection);
	    }
		return cutPolygons;
	}
}