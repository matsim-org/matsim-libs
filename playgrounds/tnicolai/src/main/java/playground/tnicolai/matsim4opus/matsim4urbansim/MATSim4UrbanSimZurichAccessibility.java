
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
package playground.tnicolai.matsim4opus.matsim4urbansim;

import gnu.trove.TObjectDoubleHashMap;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.gis.MyColorizer;
import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.gis.Zone;
import playground.tnicolai.matsim4opus.gis.ZoneLayer;
import playground.tnicolai.matsim4opus.gis.io.FeatureKMLWriter;
import playground.tnicolai.matsim4opus.gis.io.FeatureSHP;
import playground.tnicolai.matsim4opus.matsim4urbansim.MATSim4UrbanSim;
import playground.tnicolai.matsim4opus.scenario.ZurichUtilities;
import playground.tnicolai.matsim4opus.utils.ProgressBar;
import playground.tnicolai.matsim4opus.utils.helperObjects.JobClusterObject;
import playground.tnicolai.matsim4opus.utils.helperObjects.WorkplaceObject;
import playground.tnicolai.matsim4opus.utils.helperObjects.ZoneAccessibilityObject;
import playground.tnicolai.matsim4opus.utils.io.ReadFromUrbansimParcelModel;
import playground.tnicolai.matsim4opus.utils.io.writer.SpatialGridTableWriter;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;


class MATSim4UrbanSimZurichAccessibility extends MATSim4UrbanSim{
	
	// Logger
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimZurichAccessibility.class);
	
	private String shapeFile = null;
	private double gridSizeInMeter = -1;
	private double jobSample = 1.;
	
	/**
	 * constructor
	 * @param args
	 */
	public MATSim4UrbanSimZurichAccessibility(String args[]){
		super(args);
		// set the resolution, this is used for setting 
		// the starting points for accessibility measures
		checkAndSetShapeFile(args);
		checkAndSetGridSize(args);
		checkAndSetJobSample(args);
	}

	/**
	 * Set the shape file path in order to determine 
	 * the starting points for accessibility computation
	 * 
	 * @param args
	 */
	private void checkAndSetShapeFile(String[] args) {

		if( args.length >= 2 ){
			shapeFile = args[1].trim();
			log.info("The shape file path was set to " + shapeFile);
			if(!pathExsits(shapeFile))
				throw new RuntimeException("Given path to shape file does not exist: " + shapeFile);
		} else{
			log.error("Missing shape file!!!");
			System.exit(-1);
		}
	}
	
	/**
	 * Set the grid size for the starting points
	 * 
	 * @param args
	 */
	private void checkAndSetGridSize(String[] args) {
		
		if(args.length >= 3){
			gridSizeInMeter = Double.parseDouble( args[2].trim() );
			log.info("The resolution was set to " + String.valueOf(gridSizeInMeter) );
		} else{
			log.error("Missing resolution!!!");
			System.exit(-1);
		}

	}
	
	/**
	 * Set the jobSample for the starting points
	 * 
	 * @param args
	 */
	private void checkAndSetJobSample(String[] args) {

		if (args.length >= 4) {
			jobSample = Double.parseDouble(args[3].trim());
			log.info("The jobSample was set to " + String.valueOf(jobSample));
		} else {
			log.error("Missing jobSample!!!");
			System.exit(-1);
		}
	}
	
	/**
	 * This modifies the MATSim network according to the given
	 * test parameter in the MATSim config file (from UrbanSim)
	 */
	@Override
	void modifyNetwork(Network network){
		log.info("");
		log.info("Checking for network modifications ...");
		// check given test parameter for desired modifications
		String testParameter = scenario.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.TEST_PARAMETER_PARAM);
		if(testParameter.equals("")){
			log.info("No modifications to perform.");
			log.info("");
			return;
		}
		else{
			String scenarioArray[] = testParameter.split(",");
			ZurichUtilities.modifyNetwork(network, scenarioArray);
			log.info("Done modifying network.");
			log.info("");
		}
	}
	
	/**
	 * run simulation
	 * @param zones
	 * @throws IOException 
	 */
	@Override
	void runControler( ActivityFacilitiesImpl zones, ActivityFacilitiesImpl parcels, Map<Id,WorkplaceObject> numberOfWorkplacesPerZone,
			ReadFromUrbansimParcelModel readFromUrbansim){
		
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);	// sets, whether output files are overwritten
		controler.setCreateGraphs(false);	// sets, whether output Graphs are created
		
		ERSAControlerListener myListener = null;
		
		try {
			myListener = initAndAddControlerListener(parcels, readFromUrbansim, controler);
//			modifyLinks( ((NetworkImpl)scenario.getNetwork()), capacity);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		int cID = benchmark.addMeasure("Running Contoler");
		controler.run();
		benchmark.stoppMeasurement(cID);
		
		log.info("Finished computations ...");
		log.info("Running contoler took " + benchmark.getDurationInSeconds(cID) + " seconds.");
		
		writeKMZFiles(myListener);
		writeSpatialGridTables(myListener);
		
		// dumping benchmark results
		benchmark.dumpResults(Constants.MATSIM_4_OPUS_TEMP + "matsim4ersa_benchmark.txt");
	}

	/**
	 * @param myListener
	 */
	void writeSpatialGridTables(ERSAControlerListener myListener) {
		log.info("Writing spatial grid tables ...");
		SpatialGridTableWriter sgTableWriter = new SpatialGridTableWriter();
		try {
			sgTableWriter.write(myListener.getTravelTimeAccessibilityGrid(), Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_TRAVEL_TIME_ACCESSIBILITY + "_GridSize_" + gridSizeInMeter + Constants.FILE_TYPE_TXT);

			sgTableWriter.write(myListener.getTravelCostAccessibilityGrid(), Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_TRAVEL_COST_ACCESSIBILITY + "_GridSize_" + gridSizeInMeter + Constants.FILE_TYPE_TXT);

			sgTableWriter.write(myListener.getTravelDistanceAccessibilityGrid(), Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_TRAVEL_DISTANCE_ACCESSIBILITY + "_GridSize_" + gridSizeInMeter + Constants.FILE_TYPE_TXT);
			
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Done with writing spatial grid tables ...");
	}

	/**
	 * @param myListener
	 */
	void writeKMZFiles(ERSAControlerListener myListener) {
		log.info("Writing Google Erath files ...");
		
		ZoneLayer<ZoneAccessibilityObject> startZones = myListener.getStartZones();
		
		FeatureKMLWriter writer = new FeatureKMLWriter();
		Set<Geometry> geometries = new HashSet<Geometry>();
		
		TObjectDoubleHashMap<Geometry> travelTimeValues = new TObjectDoubleHashMap<Geometry>();
		TObjectDoubleHashMap<Geometry> travelCostValues = new TObjectDoubleHashMap<Geometry>();
		TObjectDoubleHashMap<Geometry> travelDistanceValues = new TObjectDoubleHashMap<Geometry>();
		
		for(Zone<ZoneAccessibilityObject> zone : startZones.getZones()) {
			geometries.add(zone.getGeometry());
			travelTimeValues.put(zone.getGeometry(), zone.getAttribute().getTravelTimeAccessibility());
			travelCostValues.put(zone.getGeometry(), zone.getAttribute().getTravelCostAccessibility());
			travelDistanceValues.put(zone.getGeometry(), zone.getAttribute().getTravelDistanceAccessibility());
		}
		
		// writing travel time accessibility kmz file
		writer.setColorizable(new MyColorizer(travelTimeValues));
		writer.write(geometries, Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_TRAVEL_TIME_ACCESSIBILITY + "_GridSize_" + gridSizeInMeter + Constants.FILE_TYPE_KMZ);
		
		// writing travel cost accessibility kmz file
		writer.setColorizable(new MyColorizer(travelCostValues));
		writer.write(geometries, Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_TRAVEL_COST_ACCESSIBILITY + "_GridSize_" + gridSizeInMeter + Constants.FILE_TYPE_KMZ);
		
		// writing travel distance accessibility kmz file
		writer.setColorizable(new MyColorizer(travelDistanceValues));
		writer.write(geometries, Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_TRAVEL_DISTANCE_ACCESSIBILITY + "_GridSize_" + gridSizeInMeter + Constants.FILE_TYPE_KMZ);
		
		log.info("Done with writing Google Erath files ...");
	}

	/**
	 * @param parcels
	 * @param readFromUrbansim
	 * @param controler
	 * @throws IOException 
	 */
	private ERSAControlerListener initAndAddControlerListener(ActivityFacilitiesImpl parcels,
			ReadFromUrbansimParcelModel readFromUrbansim, Controler controler) throws IOException {
		
		Geometry boundary = getBoundary(shapeFile);
		
		// set starting points to measure accessibility (KMZ and CSV output)
		int szID = benchmark.addMeasure("Creating Grid (startingZomes)");
		ZoneLayer<ZoneAccessibilityObject> startZones = createGridLayerByGridSize(gridSizeInMeter, boundary);
		benchmark.stoppMeasurement(szID);
		log.info("Creating Grid took " + benchmark.getDurationInSeconds(szID) + "seconds.");
		
		SpatialGrid<Double> travelTimeAccessibilityGrid = createSpatialGrid(boundary);
		SpatialGrid<Double> travelCostAccessibilityGrid = createSpatialGrid(boundary);
		SpatialGrid<Double> travelDistanceAccessibilityGrid = createSpatialGrid(boundary);
		
		// gather all workplaces
		int jmID = benchmark.addMeasure("Creating Destinations (jobObjectMap)");
		// JobClusterObject[] jobClusterArray = readFromUrbansim.readAndBuildJobsObject(parcels, jobSample); // tnicolai: old version
		JobClusterObject[] jobClusterArray = readFromUrbansim.getAggregatedWorkplaces(parcels, jobSample, (NetworkImpl)scenario.getNetwork()); // this aggreagtes workplaces directly to their nearest node 
		benchmark.stoppMeasurement(jmID);
		log.info("Creating job destinations (jobObjectMap) took " + benchmark.getDurationInSeconds(jmID) + "seconds.");
		
		ERSAControlerListener myListener = new ERSAControlerListener(startZones, jobClusterArray, 
				travelTimeAccessibilityGrid, travelCostAccessibilityGrid, travelDistanceAccessibilityGrid, benchmark);
		
		// The following lines register what should be done _after_ the iterations were run:
		controler.addControlerListener( myListener );
		
		return myListener;
	}

	/**
	 * @param boundary
	 */
	private SpatialGrid<Double> createSpatialGrid(Geometry boundary) {
		Envelope env = boundary.getEnvelopeInternal();
		double xMin = env.getMinX();
		double xMax = env.getMaxX();
		double yMin = env.getMinY();
		double yMax = env.getMaxY();
		
		return new SpatialGrid<Double>(xMin, yMin, xMax, yMax, gridSizeInMeter);
	}

	/**
	 * @param shpFile
	 * @return
	 * @throws IOException
	 */
	private Geometry getBoundary(String shpFile) throws IOException {
		// get boundaries of study area
		Set<Feature> featureSet = FeatureSHP.readFeatures(shpFile);
		log.info("Extracting boundary of the shape file ...");
		Geometry boundary = featureSet.iterator().next().getDefaultGeometry();
		// boundary.setSRID( Constants.SRID_WASHINGTON_NORTH );
		boundary.setSRID( Constants.SRID_SWITZERLAND );
		log.warn("Using SRID of Switzerland: " + Constants.SRID_SWITZERLAND);
		log.info("Done extracting boundary ...");
		
		return boundary;
	}
	
	/**
	 * 
	 * @param <T>
	 * @param gridSize
	 * @param boundary
	 * @return
	 */
	private static ZoneLayer<ZoneAccessibilityObject> createGridLayerByGridSize(double gridSize, Geometry boundary) {
		
		log.info("Setting statring points for accessibility measure ...");

		int skippedPoints = 0;
		int setPoints = 0;
		
		GeometryFactory factory = new GeometryFactory();
		
		Set<Zone<ZoneAccessibilityObject>> zones = new HashSet<Zone<ZoneAccessibilityObject>>();
		Envelope env = boundary.getEnvelopeInternal();
		
		ProgressBar bar = new ProgressBar( (env.getMaxX()-env.getMinX())/gridSize );
		
		// goes step by step from the min x and y coordinate to max x and y coordinate
		for(double x = env.getMinX(); x < env.getMaxX(); x += gridSize) {
			
			bar.update();
						
			for(double y = env.getMinY(); y < env.getMaxY(); y += gridSize) {
				Point point = factory.createPoint(new Coordinate(x, y));
				if(boundary.contains(point)) {
					
					Coordinate[] coords = new Coordinate[5];
					coords[0] = point.getCoordinate();
					coords[1] = new Coordinate(x, y + gridSize);
					coords[2] = new Coordinate(x + gridSize, y + gridSize);
					coords[3] = new Coordinate(x + gridSize, y);
					coords[4] = point.getCoordinate();
					// Linear Ring defines an artificial zone
					LinearRing linearRing = factory.createLinearRing(coords);
					Polygon polygon = factory.createPolygon(linearRing, null);
					polygon.setSRID( Constants.SRID_SWITZERLAND ); 
					
					Zone<ZoneAccessibilityObject> zone = new Zone<ZoneAccessibilityObject>(polygon);
					zone.setAttribute( new ZoneAccessibilityObject( setPoints ) );
					zones.add(zone);
					
					setPoints++;
				}
				else skippedPoints++;
			}
		}

		log.info("Having " + setPoints + " inside the shape file boundary (and " + skippedPoints + " outside).");
		log.info("Done with setting starting points!");
		
		ZoneLayer<ZoneAccessibilityObject> layer = new ZoneLayer<ZoneAccessibilityObject>(zones);
		return layer;
	}
	
	/**
	 * This is the program entry point
	 * 
	 * @param args
	 */
	public static void main(String[] args) {
		
		long startTime;
		long endTime;
		long time;
		
		startTime = System.currentTimeMillis();
		
		MATSim4UrbanSimZurichAccessibility m4uZurich = new MATSim4UrbanSimZurichAccessibility(args);
		m4uZurich.runMATSim();
		
		endTime = System.currentTimeMillis();
		time = (endTime - startTime) / 60000;
		
		log.info("Computation took " + time + " minutes. Computation done!");
	}

}

