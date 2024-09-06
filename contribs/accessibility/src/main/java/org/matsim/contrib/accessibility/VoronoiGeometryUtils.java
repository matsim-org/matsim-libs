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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.geotools.api.feature.simple.SimpleFeature;
import org.geotools.api.feature.simple.SimpleFeatureType;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.feature.simple.SimpleFeatureTypeBuilder;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Polygon;
import org.locationtech.jts.triangulate.VoronoiDiagramBuilder;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.matrixbasedptrouter.utils.BoundingBox;
import org.matsim.core.utils.geometry.CoordUtils;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.facilities.ActivityFacility;

/**
 * @author dziemke
 */
class VoronoiGeometryUtils {
	private final static Logger LOG = LogManager.getLogger(VoronoiGeometryUtils.class);
	
	private static GeometryFactory geometryFactory = new GeometryFactory();
	
	public static Map<Id<ActivityFacility>, Geometry> buildMeasurePointGeometryMap(ActivityFacilities measuringPoints, BoundingBox boundingBox, int tileSize_m) {
		LOG.warn("Started building measure-point-to-geometry map.");
		if (boundingBox == null) {
			throw new IllegalArgumentException("Bounding box must be specified.");
		}
		Map<Id<ActivityFacility>, Geometry> measurePointPolygons = new HashMap<>();
		
		Collection<Geometry> geometries = determineVoronoiShapes(measuringPoints, boundingBox);
		
		if (geometries.size() != measuringPoints.getFacilities().size()) {
			throw new RuntimeException("Number of Voronoi polygons and measure points must be equal.");
		}
		
		for (ActivityFacility measurePoint : measuringPoints.getFacilities().values()) {
			Id<ActivityFacility> measurePointId = measurePoint.getId();
			Coord coord = measurePoint.getCoord();
			
			boolean polygonFound = false;
			for (Geometry geometry : geometries) {
				if (geometry.covers(geometryFactory.createPoint(CoordUtils.createGeotoolsCoordinate(coord)))) {
					if (!measurePointPolygons.containsKey(measurePointId)) {
						measurePointPolygons.put(measurePointId, geometry);
						polygonFound = true;
					} else {
						throw new RuntimeException("Voronoi polygons can't contain more than one point.");
					}
				}
			}
			if (!polygonFound) {
				throw new RuntimeException("No polygon found for measure point " + measurePointId + " with coord = " + coord + ".");
			}
		}		
		Map<Id<ActivityFacility>, Geometry> revisedMeasurePointPolygons = reduceEdgePolygons(measuringPoints, tileSize_m, measurePointPolygons);
		
		LOG.warn("Finished building measure-point-to-geometry map.");
		return revisedMeasurePointPolygons;
	}

	private static Map<Id<ActivityFacility>, Geometry> reduceEdgePolygons(ActivityFacilities measuringPoints,
			int tileSize_m, Map<Id<ActivityFacility>, Geometry> measurePointPolygons) {
		Map<Id<ActivityFacility>, Geometry> measurePointPolygons2 = new HashMap<>();
		for (Id<ActivityFacility> measurePointId : measurePointPolygons.keySet()) {
			Coord measuringPointsCoord = measuringPoints.getFacilities().get(measurePointId).getCoord();
			Polygon polygon = (Polygon) measurePointPolygons.get(measurePointId);

			Coordinate[] coordinates = polygon.getCoordinates();
			Coordinate[] revisedCoordinates = new Coordinate[coordinates.length];
			
			int i = 0;
			for (Coordinate coordinate : coordinates) {
				double xCoordCenter = measuringPointsCoord.getX();
				double xCoord;
				if (coordinate.x < xCoordCenter - tileSize_m/2.) {
					xCoord = xCoordCenter - tileSize_m/2.;
				} else if (coordinate.x > xCoordCenter + tileSize_m/2.) {
					xCoord = xCoordCenter + tileSize_m/2.;
				} else {
					xCoord = coordinate.x;
				}

				double yCoordCenter = measuringPointsCoord.getY();
				double yCoord;
				if (coordinate.y < yCoordCenter - tileSize_m/2.) {
					yCoord = yCoordCenter - tileSize_m/2.;
				} else if (coordinate.y > yCoordCenter + tileSize_m/2.) {
					yCoord = yCoordCenter + tileSize_m/2.;
				} else {
					yCoord = coordinate.y;
				}

				Coordinate revisedCoordinate = new Coordinate(xCoord, yCoord);
				revisedCoordinates[i] = revisedCoordinate;
				i++;
			}
			Geometry geometry = geometryFactory.createPolygon(revisedCoordinates);
			measurePointPolygons2.put(measurePointId, geometry.convexHull());
		}
		return measurePointPolygons2;
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
	    	fbuilder.add(geometry);
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
