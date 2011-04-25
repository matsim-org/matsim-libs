/* *********************************************************************** *
 * project: org.matsim.*
 * MATSim4UrbanSimERSA.java
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
package playground.tnicolai.urbansim.ersa;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.sna.gis.Zone;
import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

import playground.johannes.socialnetworks.gis.io.FeatureSHP;
import playground.tnicolai.urbansim.MATSim4Urbansim;
import playground.tnicolai.urbansim.utils.helperObjects.JobsObject;
import playground.tnicolai.urbansim.utils.helperObjects.WorkplaceObject;
import playground.tnicolai.urbansim.utils.io.ReadFromUrbansimParcelModel;

/**
 * @author thomas
 *
 */
public class MATSim4UrbanSimERSA extends MATSim4Urbansim{
	
	// Logger
	private static final Logger logger = Logger.getLogger(MATSim4UrbanSimERSA.class);
	
	public MATSim4UrbanSimERSA(String args[]){
		super(args);
	}
	
	/**
	 * run simulation
	 * @param zones
	 * @throws IOException 
	 */
	@Override
	protected void runControler( ActivityFacilitiesImpl zones, Map<Id,WorkplaceObject> numberOfWorkplacesPerZone, ActivityFacilitiesImpl parcels, 
			ReadFromUrbansimParcelModel readFromUrbansim){
		
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);	// sets, whether output files are overwritten
		controler.setCreateGraphs(false);	// sets, whether output Graphs are created
		
		
		try {
			initAndAddControlerListener(parcels, readFromUrbansim, controler);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		runControler(controler);
		
		System.out.println("Finished!!!");
	}

	/**
	 * @param controler
	 */
	private void runControler(Controler controler) {
		long startTime;
		long endTime;
		long time;
		
		startTime = System.currentTimeMillis();
		// run the iterations, including the post-processing:
		controler.run();
		endTime = System.currentTimeMillis();
		time = (endTime - startTime) / 1000;
		logger.info("Running MATSim controler took " + time +" seconds.");
	}

	/**
	 * @param parcels
	 * @param readFromUrbansim
	 * @param controler
	 * @throws IOException 
	 */
	private void initAndAddControlerListener(ActivityFacilitiesImpl parcels,
			ReadFromUrbansimParcelModel readFromUrbansim, Controler controler) throws IOException {
		
		// set starting points to measure accessibility
		ZoneLayer<Integer> startZones = getStratZones();
		
		// gather all workplaces
		Map<Id, JobsObject> jobObjectMap = getJobMap(parcels, readFromUrbansim);
		
		// The following lines register what should be done _after_ the iterations were run:
		controler.addControlerListener( new ERSAControlerListener(startZones,jobObjectMap) );
	}

	/**
	 * @param parcels
	 * @param readFromUrbansim
	 * @return
	 */
	private Map<Id, JobsObject> getJobMap(ActivityFacilitiesImpl parcels,
			ReadFromUrbansimParcelModel readFromUrbansim) {
		long startTime;
		long endTime;
		long time;
		startTime = System.currentTimeMillis();
		Map<Id, JobsObject> jobObjectMap = readFromUrbansim.readDisaggregatedJobs(parcels);
		endTime = System.currentTimeMillis();
		time = (endTime - startTime) / 1000;
		logger.info("Creating job map took " + time + "seconds.");
		return jobObjectMap;
	}

	/**
	 * @return
	 * @throws IOException
	 */
	private ZoneLayer<Integer> getStratZones() throws IOException {
		long startTime;
		long endTime;
		long time;
		int resolution = 50000;
		String psrcSHPFile = "/Users/thomas/Development/opus_home/data/psrc_parcel/shapefiles/boundary.shp";
		startTime = System.currentTimeMillis();
		ZoneLayer<Integer> startZones = setMeasurePoints(resolution, psrcSHPFile);
		endTime = System.currentTimeMillis();
		time = (endTime - startTime) / 1000;
		logger.info("Creating start points took " + time + "seconds with a resolution of " + resolution + ".");
		return startZones;
	}

	/**
	 * Sets measurement points (for accessibility measurements) in the study area
	 * @throws IOException
	 */
	private ZoneLayer<Integer> setMeasurePoints(int resolution, String psrcSHPFile) throws IOException{
		
		// get boundaries of study area
		Geometry boundary = FeatureSHP.readFeatures(psrcSHPFile).iterator().next().getDefaultGeometry();
//		boundary.setSRID(21781); // tnicolai: this is the srid for switzerland, set right srid instead ..
		
		return createGridLayerByResolution(resolution, boundary);
	}
	
	/**
	 * 
	 * @param <T>
	 * @param resolution
	 * @param boundary
	 * @return
	 */
	private static ZoneLayer<Integer> createGridLayerByResolution(double resolution, Geometry boundary) {
		
		logger.info("Setting statring points for accessibility measure ...");

		int skippedPoints = 0;
		int setPoints = 0;
		
		GeometryFactory factory = new GeometryFactory();
		Set<Zone<Integer>> zones = new HashSet<Zone<Integer>>();
		Envelope env = boundary.getEnvelopeInternal();
		
//		System.out.println("X_MIN: " + env.getMinX() + " , X_MAX: " + env.getMaxX() + " , Y_MIN: " + env.getMinY() + " , Y_MAX: " + env.getMaxY());
		
		// Progress bar
		System.out.println("|--------------------------------------------------------------------------------------------------|") ;
		long cnt = 0; 
		long percentDone = 0;
		long total = (long) (env.getMaxX() - env.getMinX());
		
		// goes step by step from the min x and y coordinate to max x and y coordinate
		for(double x = env.getMinX(); x < env.getMaxX(); x += resolution) {
			
			// progress bar
			int progres = (int) (100.*cnt/(total/resolution));
			while ( progres >= percentDone ) {
				percentDone++ ; 
				System.out.print('|') ;
			}
			cnt++;
			
			for(double y = env.getMinY(); y < env.getMaxY(); y += resolution) {
				Point point = factory.createPoint(new Coordinate(x, y));
				if(boundary.contains(point)) {
					
					Coordinate[] coords = new Coordinate[5];
					coords[0] = point.getCoordinate();
					coords[1] = new Coordinate(x, y + resolution);
					coords[2] = new Coordinate(x + resolution, y + resolution);
					coords[3] = new Coordinate(x + resolution, y);
					coords[4] = point.getCoordinate();
					// Linear Ring defines an artificial zone
					LinearRing linearRing = factory.createLinearRing(coords);
					Polygon polygon = factory.createPolygon(linearRing, null);
//					polygon.setSRID(21781); // tnicolai: this is the srid for switzerland, set right srid instead ..
					Zone<Integer> zone = new Zone<Integer>(polygon);
					zone.setAttribute( new Integer(setPoints) );
					zones.add(zone);
					
					setPoints++;
				}
				else skippedPoints++;
			}
		}
		
		System.out.println("|\r\n");
		logger.info(setPoints + " starting points were set and " + skippedPoints + " points have been skipped (because they lay outside the shape file boundary).");
		logger.info("Finished setting starting points!");
		
		ZoneLayer<Integer> layer = new ZoneLayer<Integer>(zones);
		return layer;
	}
	
	/**
	 * 
	 * @param <T>
	 * @param resolution
	 * @param boundary
	 * @return
	 */
	private static <Integer> ZoneLayer<Integer> createGridLayerByFixedNumberOfStartingPoints(double numberfStartingPoints, Geometry boundary) {
		
		logger.info("Setting statring points for accessibility measure ...");
		
		int skippedPoints = 0;
		int setPoints = 0;
		
//		GeometryFactory factory = new GeometryFactory();
		Set<Zone<Integer>> zones = new HashSet<Zone<Integer>>();
//		Envelope env = boundary.getEnvelopeInternal();
//		for(double x = env.getMinX(); x < env.getMaxX(); x += resolution) {
//			for(double y = env.getMinY(); y < env.getMaxY(); y += resolution) {
//				Point point = factory.createPoint(new Coordinate(x, y));
//				if(boundary.contains(point)) {
//					Coordinate[] coords = new Coordinate[5];
//					coords[0] = point.getCoordinate();
//					coords[1] = new Coordinate(x, y + resolution);
//					coords[2] = new Coordinate(x + resolution, y + resolution);
//					coords[3] = new Coordinate(x + resolution, y);
//					coords[4] = point.getCoordinate();
//					
//					LinearRing linearRing = factory.createLinearRing(coords);
//					Polygon polygon = factory.createPolygon(linearRing, null);
//					polygon.setSRID(21781);
//					Zone<T> zone = new Zone<T>(polygon);
//					zones.add(zone);
//					
//					setPoints++;
//				}
//				else skippedPoints++;
//			}
//		}
		
		logger.info(setPoints + " were set and " + skippedPoints + " have been skipped.");
		logger.info("Finished setting starting points!");
		
		ZoneLayer<Integer> layer = new ZoneLayer<Integer>(zones);
		return layer;
	}
	
	/**
	 * This is the program entry point
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		MATSim4UrbanSimERSA m4uERSA = new MATSim4UrbanSimERSA(args);
		m4uERSA.runMATSim();
//		if(m4uERSA.computationMeasures != null)
//			MeasurementObject.wirteLogfile();
//		else throw new RuntimeException("The object measuring comuting times is not initialized...");
	}

}

