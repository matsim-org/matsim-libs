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

package playground.tnicolai.matsim4opus.gis;

import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.core.api.experimental.facilities.ActivityFacility;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.utils.gis.ShapeFileReader;
import org.opengis.feature.simple.SimpleFeature;

import playground.tnicolai.matsim4opus.gis.io.FeatureKMLWriter;
import playground.tnicolai.matsim4opus.utils.io.writer.SpatialGridTableWriter;
import playground.tnicolai.matsim4opus.utils.misc.ProgressBar;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

public class GridUtils {
	
	// logger
	private static final Logger log = Logger.getLogger(GridUtils.class);
	
	/**
	 * 
	 * @param shapeFile
	 * @return Geometry determines the scenario boundary for the accessibility measure
	 */
	public static Geometry getBoundary(String shapeFile){
		// get boundaries of study area
		Collection<SimpleFeature> featureSet = ShapeFileReader.getAllFeatures(shapeFile);
		log.info("Extracting boundary of the shape file ...");
		Geometry boundary = (Geometry) featureSet.iterator().next().getDefaultGeometry();
		// boundary.setSRID( srid ); // tnicolai: this is not needed to match the grid layer with locations / facilities from UrbanSim
		// log.warn("Using SRID: " + srid);
		log.info("Done extracting boundary ...");
		
		return boundary;
	}
	
	/**
	 * creates measuring points for accessibility computation
	 * 
	 * @param <T>
	 * @param gridSize
	 * @param boundary
	 * @return
	 */
	public static ZoneLayer<Id> createGridLayerByGridSizeByShapeFile(double gridSize, Geometry boundary) {
		
		log.info("Setting statring points for accessibility measure ...");

		int skippedPoints = 0;
		int setPoints = 0;
		
		GeometryFactory factory = new GeometryFactory();
		
		Set<Zone<Id>> zones = new HashSet<Zone<Id>>();
		Envelope env = boundary.getEnvelopeInternal();
		
		ProgressBar bar = new ProgressBar( (env.getMaxX()-env.getMinX())/gridSize );
		
		// goes step by step from the min x and y coordinate to max x and y coordinate
		for(double x = env.getMinX(); x < env.getMaxX(); x += gridSize) {
			
			bar.update();
						
			for(double y = env.getMinY(); y < env.getMaxY(); y += gridSize) {
				
				// check first if cell centroid is within study area
				double center_X = x + (gridSize/2);
				double center_Y = y + (gridSize/2);
				Point centroid = factory.createPoint(new Coordinate(center_X, center_Y));
				
				if(boundary.contains(centroid)) {
					Point point = factory.createPoint(new Coordinate(x, y));
					
					Coordinate[] coords = new Coordinate[5];
					coords[0] = point.getCoordinate();
					coords[1] = new Coordinate(x, y + gridSize);			// upper left
					coords[2] = new Coordinate(x + gridSize, y + gridSize);	// upper right
					coords[3] = new Coordinate(x + gridSize, y);			// lower right
					coords[4] = point.getCoordinate();						// lower left
					
					// Linear Ring defines an artificial zone
					LinearRing linearRing = factory.createLinearRing(coords);
					Polygon polygon = factory.createPolygon(linearRing, null);
					// polygon.setSRID( srid ); // tnicolai: this is not needed to match the grid layer with locations / facilities from UrbanSim
					
					Zone<Id> zone = new Zone<Id>(polygon);
					zone.setAttribute( new IdImpl( setPoints ) );
					zones.add(zone);
					
					setPoints++;
				}
				else skippedPoints++;
			}
		}

		log.info("Having " + setPoints + " inside the shape file boundary (and " + skippedPoints + " outside).");
		log.info("Done with setting starting points!");
		
		ZoneLayer<Id> layer = new ZoneLayer<Id>(zones);
		return layer;
	}
	
	/**
	 * creates measuring points for accessibility computation
	 * 
	 * @param <T>
	 * @param gridSize
	 * @param boundingBox defines an area within the network file, this area will later be processed when calculating accessibilities.
	 * @param boundary
	 * @return
	 */
	public static ZoneLayer<Id> createGridLayerByGridSizeByNetwork(double gridSize, double [] boundingBox) {
		
		log.info("Setting statring points for accessibility measure ...");

		int skippedPoints = 0;
		int setPoints = 0;
		
		GeometryFactory factory = new GeometryFactory();
		
		Set<Zone<Id>> zones = new HashSet<Zone<Id>>();

		double xmin = boundingBox[0];
		double ymin = boundingBox[1];
		double xmax = boundingBox[2];
		double ymax = boundingBox[3];
		
		
		ProgressBar bar = new ProgressBar( (xmax-xmin)/gridSize );
		
		// goes step by step from the min x and y coordinate to max x and y coordinate
		for(double x = xmin; x <xmax; x += gridSize) {
			
			bar.update();
						
			for(double y = ymin; y < ymax; y += gridSize) {
				
				// check first if cell centroid is within study area
				double center_X = x + (gridSize/2);
				double center_Y = y + (gridSize/2);
				
				// check if x, y is within network boundary
				if (center_X <= xmax && center_X >= xmin && 
					center_Y <= ymax && center_Y >= ymin) {
				
					Point point = factory.createPoint(new Coordinate(x, y));
					
					Coordinate[] coords = new Coordinate[5];
					coords[0] = point.getCoordinate();
					coords[1] = new Coordinate(x, y + gridSize);
					coords[2] = new Coordinate(x + gridSize, y + gridSize);
					coords[3] = new Coordinate(x + gridSize, y);
					coords[4] = point.getCoordinate();
					// Linear Ring defines an artificial zone
					LinearRing linearRing = factory.createLinearRing(coords);
					Polygon polygon = factory.createPolygon(linearRing, null);
					// polygon.setSRID( srid ); // tnicolai: this is not needed to match the grid layer with locations / facilities from UrbanSim
					
					Zone<Id> zone = new Zone<Id>(polygon);
					zone.setAttribute( new IdImpl( setPoints ) );
					zones.add(zone);
					
					setPoints++;
				}
				else skippedPoints++;
			}
		}

		log.info("Having " + setPoints + " inside the shape file boundary (and " + skippedPoints + " outside).");
		log.info("Done with setting starting points!");
		
		ZoneLayer<Id> layer = new ZoneLayer<Id>(zones);
		return layer;
	}
	
	/**
	 * Converting zones (or zone centroids) of type ActivityFacilitiesImpl into a ZoneLayer
	 * 
	 * @param facility ActivityFacilitiesImpl
	 * @return ZoneLayer<Id>
	 */
	public static ZoneLayer<Id> convertActivityFacilities2ZoneLayer(ActivityFacilitiesImpl facility){
		
		int setPoints = 0;
		
		GeometryFactory factory = new GeometryFactory();
		
		Set<Zone<Id>> zones = new HashSet<Zone<Id>>();

		Iterator<? extends ActivityFacility> facilityIterator =  facility.getFacilities().values().iterator();
		
		while(facilityIterator.hasNext()){
			
			ActivityFacility af = facilityIterator.next();

			Coord coord = af.getCoord();
			// Point defines the artificial zone centroid
			Point point = factory.createPoint(new Coordinate(coord.getX(), coord.getY()));
			// point.setSRID(srid); // tnicolai: this is not needed to match the grid layer with locations / facilities from UrbanSim
			
			Zone<Id> zone = new Zone<Id>(point);
			zone.setAttribute( af.getId() );
			zones.add(zone);
			
			setPoints++;
		}
		
		log.info("Having " + setPoints + " 'ActivityFacilitiesImpl' items converted into 'ZoneLayer' format");
		log.info("Done with conversion!");
		
		ZoneLayer<Id> layer = new ZoneLayer<Id>(zones);

		if(!checkConversion(facility, layer))
			log.error("Conversion error: Either not all items are converted or coordinates are wrong!");
		
		return layer;
	}
	
	/**
	 * returns a spatial grid for a given geometry (e.g. shape file) with a given grid size
	 * 
	 * @param gridSize side length of the grid
	 * @param boundary a boundary, e.g. from a shape file
	 * @return SpatialGrid storing accessibility values
	 */
	public static SpatialGrid createSpatialGridByShapeBoundary(double gridSize, Geometry boundary) {
		Envelope env = boundary.getEnvelopeInternal();
		double xMin = env.getMinX();
		double xMax = env.getMaxX();
		double yMin = env.getMinY();
		double yMax = env.getMaxY();
		
		return new SpatialGrid(xMin, yMin, xMax, yMax, gridSize);
	}

	/**
	 * stores measured accessibilities in a file
	 * 
	 * @param grid SpatialGrid containing measured accessibilities
	 * @param fileName output file
	 */
	public static void writeSpatialGridTable(SpatialGrid grid, String fileName){
		
		log.info("Writing spatial grid table " + fileName + " ...");
		SpatialGridTableWriter sgTableWriter = new SpatialGridTableWriter();
		try{
			sgTableWriter.write(grid, fileName);
			log.info("... done!");
		}catch(IOException e){
			e.printStackTrace();
		}
	}

	/**
	 * stores measured accessibilities as a kmz file for google earth
	 * 
	 * @param measuringPoints cell centroid (projected coordinate using a srid; spatial reference id)
	 * @param grid side length of the grid
	 * @param fileName output file
	 */
	public static void writeKMZFiles(ZoneLayer<Id> measuringPoints, SpatialGrid grid, String fileName) {
		log.info("Writing Google Erath file " + fileName + " ...");
		
		FeatureKMLWriter writer = new FeatureKMLWriter();
		Set<Geometry> geometries = new HashSet<Geometry>();
		
		TObjectDoubleHashMap<Geometry> kmzData = new TObjectDoubleHashMap<Geometry>();
		
		for(Zone<Id> zone : measuringPoints.getZones()) {
			Geometry geometry = zone.getGeometry();
			geometries.add(geometry);
			kmzData.put( geometry, grid.getValue(geometry.getCentroid()) );
		}
		
		// writing travel time accessibility kmz file
		writer.setColorizable(new MyColorizer(kmzData));
		writer.write(geometries, fileName);
		log.info("... done!");
	}
	
	/**
	 * consistency checker for conversion from 
	 * ActivityFacilitiesImpl into ZoneLayer<Id> structure
	 * 
	 * @param facility ActivityFacilitiesImpl
	 * @param layer ZoneLayer<Id>
	 * @return true if no inconsistency found, else false
	 */
	private static boolean checkConversion(ActivityFacilitiesImpl facility, ZoneLayer<Id> layer){
		
		boolean isEqual = true;

		Iterator<Zone<Id>> layerIterator = layer.getZones().iterator();
		
		while(layerIterator.hasNext()){
			
			Zone<Id> zone = layerIterator.next();
			Point centroid = zone.getGeometry().getCentroid();
			
			ActivityFacility af = facility.getFacilities().get(zone.getAttribute());
			if(af == null){
				isEqual = false;
				break;
			}
			
			if(af.getCoord().getX() == centroid.getX() && 
			   af.getCoord().getY() == centroid.getY())
				continue;
			else{
				isEqual = false;
				break;
			}
		}
		return isEqual;
	}
}
