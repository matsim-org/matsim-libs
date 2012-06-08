/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package playground.tnicolai.matsim4opus.alpha;

import java.io.IOException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.gis.CRSUtils;
import playground.tnicolai.matsim4opus.gis.io.FeatureSHP;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.Point;

import playground.johannes.socialnetworks.gis.DistanceCalculator;
import playground.johannes.socialnetworks.gis.DistanceCalculatorFactory;
import playground.tnicolai.matsim4opus.gis.io.FeatureSHP;
import playground.tnicolai.matsim4opus.constants.InternalConstants;

public class TestDistance {
	
	// Logger
	private static final Logger logger = Logger.getLogger(TestDistance.class);
	
	private String shapeFile = null;
	private int gridSize = -1;
	
	public TestDistance(){
		shapeFile = "/Users/thomas/Development/opus_home/data/seattle_parcel/shapefiles/boundary.shp";
		gridSize = 15000;
	}
	
	
	public void runTest(){
		
		Geometry boundary;
		try {
			
			boundary = getBoundary(shapeFile);
			testDistance(gridSize, boundary);
			
		} catch (IOException e) {
			e.printStackTrace();
		} 
	}
	

	/**
	 * @param psrcSHPFile
	 * @return
	 * @throws IOException
	 */
	private Geometry getBoundary(String psrcSHPFile) throws IOException {
		// get boundaries of study area
		Set<Feature> featureSet = FeatureSHP.readFeatures(psrcSHPFile);
		logger.info("Extracting boundary of the shape file ...");
		Geometry boundary = featureSet.iterator().next().getDefaultGeometry();
		boundary.setSRID( InternalConstants.SRID_WASHINGTON_NORTH ); // tnicolai: check if this is the correct id
		logger.info("Done extracting boundary ...");
		
		return boundary;
	}
	
	
	private void testDistance(double gridSize, Geometry boundary){
		
		GeometryFactory factory = new GeometryFactory();
		Envelope env = boundary.getEnvelopeInternal();
		
		int factorySrid = factory.getSRID();
		
		// goes step by step from the min x and y coordinate to max x and y coordinate
		for(double x = env.getMinX(); x < env.getMaxX(); x += gridSize) {
			
			for(double y = env.getMinY(); y < env.getMaxY(); y += gridSize) {
				Point point = factory.createPoint(new Coordinate(x, y));
				if(boundary.contains(point)) {
					
					Point testPoint1 = factory.createPoint(new Coordinate(x, y + gridSize));
					Point testPoint2 = factory.createPoint(new Coordinate(x + gridSize, y + gridSize));
					Point testPoint3 = factory.createPoint(new Coordinate(x + gridSize, y));
					
					int srid = boundary.getSRID();
					CoordinateReferenceSystem crs = CRSUtils.getCRS(srid);
					
					DistanceCalculator distanceCalculator = DistanceCalculatorFactory.createDistanceCalculator(crs);
					double d1 = distanceCalculator.distance(point, testPoint1);
					double d2 = distanceCalculator.distance(point, testPoint2);
					double d3 = distanceCalculator.distance(point, testPoint3);
				}
			}
			
		}
	}
	

	
	/**
	 * starting point
	 */
	public static void main(String args[]){
		
		TestDistance testDistance = new TestDistance();
		testDistance.runTest();
	}

}
