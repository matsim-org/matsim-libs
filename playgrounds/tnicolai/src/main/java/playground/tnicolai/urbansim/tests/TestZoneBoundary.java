/* *********************************************************************** *
 * project: org.matsim.*
 * TestZoneBoundary.java
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

/**
 * 
 */
package playground.tnicolai.urbansim.tests;

import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.matsim.contrib.sna.gis.CRSUtils;
import org.matsim.contrib.sna.gis.Zone;
import org.matsim.contrib.sna.gis.ZoneLayer;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.johannes.socialnetworks.gis.io.ZoneLayerSHP;

/**
 * @author thomas
 *
 */
public class TestZoneBoundary {

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		
		int resolution = 100;
		ZoneLayer<Double> startZones = null;
		String psrcSHPFile = "/Users/thomas/Development/opus_home/data/psrc_parcel/shapefiles/boundary.shp";
		
		String swissMunicipalityZones = "/Users/thomas/Documents/SVN_Studies/tnicolai/ersa/data/zones/G1G08.shp";
		String swissCountryZone = "/Users/thomas/Documents/SVN_Studies/tnicolai/ersa/data/zones/G1L08.shp";
		
		
		try {
//			Geometry swissExample1 = FeatureSHP.readFeatures(psrcSHPFile).iterator().next().getDefaultGeometry();
//			Geometry swissExample2 = FeatureSHP.readFeatures(psrcSHPFile).iterator().next().getDefaultGeometry();
//			
//			double maxX1 = swissExample1.getEnvelopeInternal().getMaxX();
//			double minX1 = swissExample1.getEnvelopeInternal().getMinX();
//			double maxY1 = swissExample1.getEnvelopeInternal().getMaxY();
//			double minY1 = swissExample1.getEnvelopeInternal().getMinY();
//			double maxX2 = swissExample2.getEnvelopeInternal().getMaxX();
//			double minX2 = swissExample2.getEnvelopeInternal().getMinX();
//			double maxY2 = swissExample2.getEnvelopeInternal().getMaxY();
//			double minY2 = swissExample2.getEnvelopeInternal().getMinY();
			
			Geometry boundary = FeatureSHP.readFeatures(psrcSHPFile).iterator().next().getDefaultGeometry();
			Envelope env = boundary.getEnvelopeInternal();
			System.out.println("X_MIN: " + env.getMinX() + " , X_MAX: " + env.getMaxX() + " , Y_MIN: " + env.getMinY() + " , Y_MAX: " + env.getMaxY());
			int srid = boundary.getSRID();
			double area = boundary.getArea();
			Coordinate[] cordinate = boundary.getCoordinates();

			
			startZones= ZoneLayerSHP.read(psrcSHPFile);	
			startZones.overwriteCRS(CRSUtils.getCRS(21781));
			startZones = createGridLayer(resolution, boundary);
			
			
			int i = 0;
			for(Zone zone: startZones.getZones()){
				System.out.println(zone.getAttribute());
				i++;
			}
			System.out.println("number of zones = " + i);
			
		} catch (IOException e) {
			e.printStackTrace();
		}

	}
	
	public static <T> ZoneLayer<T> createGridLayer(double resolution, Geometry boundary) {
		GeometryFactory factory = new GeometryFactory();
		Set<Zone<T>> zones = new HashSet<Zone<T>>();
		Envelope env = boundary.getEnvelopeInternal();
		
		int skippedPoints = 0;
		int setPoints = 0;
		
		System.out.println("X_MIN: " + env.getMinX() + " , X_MAX: " + env.getMaxX() + " , Y_MIN: " + env.getMinY() + " , Y_MAX: " + env.getMaxY());
		
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
					polygon.setSRID(21781);
					Zone<T> zone = new Zone<T>(polygon);
					zones.add(zone);
					
					setPoints++;
				}
				else skippedPoints++;
			}
		}
		
		System.out.println(setPoints + " were set and " + skippedPoints + " have been skipped.");
		
		ZoneLayer<T> layer = new ZoneLayer<T>(zones);
		return layer;
	}

}

