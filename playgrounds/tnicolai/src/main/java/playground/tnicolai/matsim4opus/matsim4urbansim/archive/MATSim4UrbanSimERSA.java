
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
package playground.tnicolai.matsim4opus.matsim4urbansim.archive;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.geotools.feature.Feature;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.matsim4opus.utils.io.Paths;
import org.matsim.core.controler.Controler;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.gis.Zone;
import playground.tnicolai.matsim4opus.gis.ZoneLayer;
import playground.tnicolai.matsim4opus.gis.io.FeatureSHP;
import playground.tnicolai.matsim4opus.matsim4urbansim.CellBasedAccessibilityControlerListener;
import playground.tnicolai.matsim4opus.utils.ProgressBar;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.CounterObject;
import playground.tnicolai.matsim4opus.utils.io.ReadFromUrbansimParcelModel;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Envelope;
import com.vividsolutions.jts.geom.Geometry;
import com.vividsolutions.jts.geom.GeometryFactory;
import com.vividsolutions.jts.geom.LinearRing;
import com.vividsolutions.jts.geom.Point;
import com.vividsolutions.jts.geom.Polygon;

/**
 * Manual:<ol>
 * 
 * <li>Download "opus_home.zip" file from svn repository:
 * https://svn.vsp.tu-berlin.de/repos/shared-svn/projects/SustainCity/MATSim_Data/accessibility_measure_input_files/opus_home.zip
 * <ul></ul>
 * 
 * <li>Extract opus_home.zip at a convenient place. The extracted file contains the following folder hierarchy:<ul>
 * 				<li> matsim_config folder contains the MATSim config
 * 				<li> opus_matsim/tmp folder contains UrbanSim input data
 * 				<li> data/network folder contains store network file 
 * 				<li> data/shapefile folder contains boundary shape file of the UrbanSim scenario
 * </ul>
 * 
 * <li>Relevant parts of MATSim config:<ul>
 * 				<li> set network file path in:<pre>
 *&lt;network&gt;
 *	&lt;inputFile&gt;
 *		enter full path to network file here
 *	&lt;/inputFile&gt;
 *&lt;/network&gt;
 *</pre>
 *
 * 				<li> set number of MATSim iterations in:<pre>
 * 					&lt;firstIteration&gt;
 *					0
 *					&lt;/firstIteration&gt;
 *					&lt;lastIteration&gt;
 *					9
 *					&lt;/lastIteration&gt;
 *</pre>
 *  
 *  			in this example MATSim performs 10 iterations
 *  
 * 				<li> set sampling rate for MATSim travelers in:<pre> 
 * 					&lt;samplingRate&gt;
 *					0.1
 *					&lt;/samplingRate&gt;
 *</pre>
 *
 *				<li> set full path to your extracted oupus_home folder in:<pre>
 *					&lt;opusHOME&gt;
 *					enter full path here
 *					&lt;/opusHOME&gt;
 *</pre>
 *
 * 				<li> set "tmp" directory path in:<pre>
 * 					&lt;tempDirectory&gt;
 *					relative path to "tmp" directory (should be "opus_matsim/tmp", see folder hierarchy above)
 *					&lt;/tempDirectory&gt;
 *</pre>
 *
 *</ul>
 * 
 * <li>MATSim4Urbansim description: This class calculates the "workplace" accessibility for different spatial resolutions. 
 * 								The study area is divided into squares (grid cells). For each square the accessibility computation is performed.
 * 								To speed up computation time small samples of travelers and jobs can be taken can be taken. 
 * 								Use the <samplingRate> in the config file to sample travelers, the job sample size must be passed when calling the class (see below).
 * <ul></ul>
 * <li>MATSim4UrbanSimERSA usage: Call MATSim4UrbanSimERSA with the following parameters
 * 
 * 							 path to MATSim configuration, path to shape file, edge length in meter (lower values lead to higher resolution), job sample size
 * 
 * 							 Example:
 * 							 /path/to/config.xml /path/to/shapefile.shp 1000 0.01
 * 							 
 * 							In this example the squares have a edge length of 1000x1000 meter, and MATSim considers only 1% of the workplaces
 * </ol>
 * 
 * Design comments:<ul>
 * 
 * <li> The shp file is currently necessary, but is not necessary in general.  Might make sense to write a version that functions
 * without.  kai, jun'11
 * <li> The accessibility computation is not optimally fast since it first computes the matrix and then sums up the results
 * into the accessibility.  This will/should be improved in future versions.  kai, jun'11
 * 
 * </ul>
 * 
 * Improvements part 1:<ul>
 * 
 * <li> Now the workplaces (destinations) are aggregated to their assigned parcel, which reduces the number of iterations in the accessibility computation dramatically.
 * The number of workplaces of a parcel are used as a weight in the accessibility measure. This also reduces time expansive look up in the network to determine nearest nodes.
 * <li> The ERSAControlerListener now uses separate LeastCostPathTrees with own cost calculators for travel time and distance...
 * <li> The travel distance computation is revised. The distance is now computed by the LeastCostPathTree (using a travel distance cost calculator). Before that distances
 * where computed separately, which took a huge amount of time.
 *  
 * </ul>
 * 
 * Improvements part 2:<ul>
 * 
 * <li> Now the workplaces (desinations) are aggregated directly to their nearest node in the network. This considerably reduces the number of destinations 
 *  
 * </ul>
 *
 * @author thomas
 *
 */
class MATSim4UrbanSimERSA extends MATSim4UrbanSim{
	
	// Logger
	private static final Logger logger = Logger.getLogger(MATSim4UrbanSimERSA.class);
	
	private String shapeFile = null;
	private double gridSizeInFeet  = -1;
	private double gridSizeInMeter = -1;
	private double jobSample = 1.;
	private double capacity = -1;
	
	/**
	 * constructor
	 * @param args
	 */
	public MATSim4UrbanSimERSA(String args[]){
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
			logger.info("The shape file path was set to " + shapeFile);
		}
		else{
			shapeFile = "/Users/thomas/Development/opus_home/data/psrc_parcel/shapefiles/boundary.shp";
			logger.warn("No path for the shape file was given. The path is set to " + shapeFile + "!!!");
		}
		
		if(!Paths.pathExsits(shapeFile))
			throw new RuntimeException("Given path to shape file does not exist: " + shapeFile);
		
		/** If the used input data comes from PSRC-PARCEL (URBANSIM) than the following spatial layer attributes will apply:
			Storage type of this layer: ESRI Shapefile
			Source for this layer: OPUS_HOME/data/psrc_parcel/shapefiles/zone.shp
			Geometry type of the features in this layer: Polygon
			The number of features in this layer: 938
			Editing capabilities of this layer: Add Features, Delete Features, Change Attribute Values, Add Attributes, Create Spatial Index, Fast Access to Features at ID, Change Geometries
			Extents:
			In layer spatial reference system units : xMin,yMin 1095791.50,-97422.8 : xMax,yMax 1622634.62,477503.72
			Layer Spatial Reference System:
			+proj=lcc +lat_1=47.5 +lat_2=48.73333333333333 +lat_0=47 +lon_0=-120.8333333333333 +x_0=500000.0000000001 +y_0=0 +ellps=GRS80 +datum=NAD83 +units=us-ft +no_defs
		 * @param args
		 */
	}
	
	/**
	 * Set the grid size for the starting points
	 * 
	 * @param args
	 */
	private void checkAndSetGridSize(String[] args) {
		
		gridSizeInMeter = 1000.; // default value, if nothing else is given (1000m)
		
		try{
			logger.warn("No conversion from meter to feed for the Zurich sceanrio!");
			if(args.length >= 3){
				gridSizeInMeter = Double.parseDouble( args[2].trim() );
				gridSizeInFeet = gridSizeInMeter; //* Constants.METER_IN_FEET_CONVERSION_FACTOR;
				// logger.info("The grid size was set to " + gridSizeInMeter + " meter (this approximately corresponds to " + gridSizeInFeet + " feet).");
			} else{
				gridSizeInFeet = gridSizeInMeter; //* Constants.METER_IN_FEET_CONVERSION_FACTOR;
				// logger.warn("No parameter for the grid size was given. The grid size is set to " + gridSizeInMeter + " meter (default setting)! This approximately corresponds to " + gridSizeInFeet + " feet.");
			}
		} catch (NumberFormatException nfe){
			nfe.printStackTrace();
			logger.error( "Please set a correct grid size (in meters). " + args[2] + " is not a valid value (double).");
		}
	}
	
	/**
	 * Set the jobSample for the starting points
	 * 
	 * @param args
	 */
	private void checkAndSetJobSample(String[] args) {
		try{
			if(args.length >= 4){
				jobSample = Double.parseDouble( args[3].trim() );
				logger.info("The jobSample was set to " + String.valueOf(jobSample) );
			} else{
				jobSample = 1.;
				logger.warn("No parameter for the job sample was given. The job sample is set to " + String.valueOf(jobSample) + " (default setting)!");
			}
		} catch(NumberFormatException nfe){
			nfe.printStackTrace();
			logger.error( "Please set a correct job sample . " + args[3] + " is not a valid value (double).");
		}
	}
	
	/**
	 * 
	 * @param args
	 */
	private void checkAndSetCUPUMNetworkModification(String[] args) {
		try{
			if(args.length >= 5){
				capacity = Double.parseDouble( args[4].trim() );
				logger.info("The capacity for ferry connection between Seattle CBD and Bainbridge Island was set to " + String.valueOf(capacity) );
			} else
				capacity = -1.;
		} catch(NumberFormatException nfe){
			nfe.printStackTrace();
			logger.error( "Please set a correct capacity for ferry connection between Seattle CBD and Bainbridge Island . " + args[4] + " is not a valid value (double).");
			capacity = -1;
		}
	}
	
	/**
	 * run simulation
	 * @param zones
	 * @throws IOException 
	 */
	@Override
	void runControler( ActivityFacilitiesImpl zones, ActivityFacilitiesImpl parcels, Map<Id,CounterObject> numberOfWorkplacesPerZone,
			ReadFromUrbansimParcelModel readFromUrbansim){
		
		Controler controler = new Controler(scenario);
		controler.setOverwriteFiles(true);	// sets, whether output files are overwritten
		controler.setCreateGraphs(false);	// sets, whether output Graphs are created
		
		CellBasedAccessibilityControlerListener myListener = null;
		
		try {
			myListener = initAndAddControlerListener(parcels, readFromUrbansim, controler);
//			modifyLinks( ((NetworkImpl)scenario.getNetwork()), capacity);
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		int cID = benchmark.addMeasure("Running Contoler");
		controler.run();
		benchmark.stoppMeasurement(cID);
		
		logger.info("Finished computations ...");
		logger.info("Running contoler took " + benchmark.getDurationInSeconds(cID) + " seconds.");
		
		// tnicolai: now writers integrated into ERSAControlerLitener
		// writeKMZFiles(myListener);
		// writeSpatialGridTables(myListener);
		
		// dumping benchmark results
		benchmark.dumpResults(Constants.MATSIM_4_OPUS_TEMP + "matsim4ersa_benchmark.txt");
	}

//	/**
//	 * @param myListener
//	 */
//	void writeSpatialGridTables(CellBasedAccessibilityControlerListener myListener) {
//		logger.info("Writing spatial grid tables ...");
//		SpatialGridTableWriter sgTableWriter = new SpatialGridTableWriter();
//		try {
//			int ttID = benchmark.addMeasure("Writing TravelTime SpatialGrid-Table" , Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_TRAVEL_TIME_ACCESSIBILITY + "_GridSize_" + gridSizeInMeter + Constants.FILE_TYPE_TXT, false);
//			sgTableWriter.write(myListener.getCarAccessibilityGrid(), Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_TRAVEL_TIME_ACCESSIBILITY + "_GridSize_" + gridSizeInMeter + Constants.FILE_TYPE_TXT);
//			benchmark.stoppMeasurement(ttID);
//			logger.info("Writing TravelTime SpatialGrid-Table took " + benchmark.getDurationInSeconds(ttID) + " seconds.");
//			
//			int tcID = benchmark.addMeasure("Writing TravelCostSpatialGrid-Table", Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_TRAVEL_COST_ACCESSIBILITY + "_GridSize_" + gridSizeInMeter + Constants.FILE_TYPE_TXT, false);
//			sgTableWriter.write(myListener.getCustomAccessibilityGrid(), Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_TRAVEL_COST_ACCESSIBILITY + "_GridSize_" + gridSizeInMeter + Constants.FILE_TYPE_TXT);
//			benchmark.stoppMeasurement(tcID);
//			logger.info("Writing TravelCost SpatialGrid-Table took " + benchmark.getDurationInSeconds(tcID) + " seconds.");
//			
//			int tdID = benchmark.addMeasure("Writing TravelDistanceSpatialGrid-Table", Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_TRAVEL_DISTANCE_ACCESSIBILITY + "_GridSize_" + gridSizeInMeter + Constants.FILE_TYPE_TXT, false);
//			sgTableWriter.write(myListener.getWalkAccessibilityGrid(), Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_TRAVEL_DISTANCE_ACCESSIBILITY + "_GridSize_" + gridSizeInMeter + Constants.FILE_TYPE_TXT);
//			benchmark.stoppMeasurement(tdID);
//			logger.info("Writing TravelDistance SpatialGrid-Table took " + benchmark.getDurationInSeconds(tdID) + " seconds.");
//			
//		} catch (IOException e) {
//			e.printStackTrace();
//		}
//		logger.info("Done with writing spatial grid tables ...");
//	}
//
//	/**
//	 * @param myListener
//	 */
//	void writeKMZFiles(CellBasedAccessibilityControlerListener myListener) {
//		logger.info("Writing Google Erath files ...");
//		
//		ZoneLayer<CounterObject> startZones = myListener.getMeasuringPoints();
//		
//		FeatureKMLWriter writer = new FeatureKMLWriter();
//		Set<Geometry> geometries = new HashSet<Geometry>();
//		
//		TObjectDoubleHashMap<Geometry> travelTimeValues = new TObjectDoubleHashMap<Geometry>();
//		TObjectDoubleHashMap<Geometry> travelCostValues = new TObjectDoubleHashMap<Geometry>();
//		TObjectDoubleHashMap<Geometry> travelDistanceValues = new TObjectDoubleHashMap<Geometry>();
//		
//		for(Zone<CounterObject> zone : startZones.getZones()) {
//			geometries.add(zone.getGeometry());
////			travelTimeValues.put(zone.getGeometry(), zone.getAttribute().getCarAccessibility());
////			travelCostValues.put(zone.getGeometry(), zone.getAttribute().getCustomAccessibility());
////			travelDistanceValues.put(zone.getGeometry(), zone.getAttribute().getWalkAccessibility());
//		}
//		
//		// writing travel time accessibility kmz file
//		int ttID = benchmark.addMeasure("Writing TravelTime KMZ-file", Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_TRAVEL_TIME_ACCESSIBILITY + "_GridSize_" + gridSizeInMeter + Constants.FILE_TYPE_KMZ, false);
//		writer.setColorizable(new MyColorizer(travelTimeValues));
//		writer.write(geometries, Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_TRAVEL_TIME_ACCESSIBILITY + "_GridSize_" + gridSizeInMeter + Constants.FILE_TYPE_KMZ);
//		benchmark.stoppMeasurement(ttID);
//		logger.info("Writing TravelTime KMZ-file took " + benchmark.getDurationInSeconds(ttID) + " seconds.");
//		
//		// writing travel cost accessibility kmz file
//		int tcID = benchmark.addMeasure("Writing TravelCost KMZ-file", Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_TRAVEL_COST_ACCESSIBILITY + "_GridSize_" + gridSizeInMeter + Constants.FILE_TYPE_KMZ, false);
//		writer.setColorizable(new MyColorizer(travelCostValues));
//		writer.write(geometries, Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_TRAVEL_COST_ACCESSIBILITY + "_GridSize_" + gridSizeInMeter + Constants.FILE_TYPE_KMZ);
//		benchmark.stoppMeasurement(tcID);
//		logger.info("Writing TravelCost KMZ-file took " + benchmark.getDurationInSeconds(tcID) + " seconds.");
//		
//		// writing travel distance accessibility kmz file
//		int tdID = benchmark.addMeasure("Writing TravelDistance KMZ-file", Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_TRAVEL_DISTANCE_ACCESSIBILITY + "_GridSize_" + gridSizeInMeter + Constants.FILE_TYPE_KMZ, false);
//		writer.setColorizable(new MyColorizer(travelDistanceValues));
//		writer.write(geometries, Constants.MATSIM_4_OPUS_TEMP + Constants.ERSA_TRAVEL_DISTANCE_ACCESSIBILITY + "_GridSize_" + gridSizeInMeter + Constants.FILE_TYPE_KMZ);
//		benchmark.stoppMeasurement(tdID);
//		logger.info("Writing TravelDistance KMZ-file took " + benchmark.getDurationInSeconds(tdID) + " seconds.");
//		
//		logger.info("Done with writing Google Erath files ...");
//	}

	/**
	 * @param parcels
	 * @param readFromUrbansim
	 * @param controler
	 * @throws IOException 
	 */
	private CellBasedAccessibilityControlerListener initAndAddControlerListener(ActivityFacilitiesImpl parcels,
			ReadFromUrbansimParcelModel readFromUrbansim, Controler controler) throws IOException {
		
		Geometry boundary = getBoundary(shapeFile);
		
		// set starting points to measure accessibility (KMZ and CSV output)
		int szID = benchmark.addMeasure("Creating Grid (startingZomes)");
		ZoneLayer<CounterObject> startZones = createGridLayerByGridSize(gridSizeInFeet, boundary);
		benchmark.stoppMeasurement(szID);
		logger.info("Creating Grid took " + benchmark.getDurationInSeconds(szID) + "seconds.");
		
		SpatialGrid<Double> travelTimeAccessibilityGrid = createSpatialGrid(boundary);
		SpatialGrid<Double> travelCostAccessibilityGrid = createSpatialGrid(boundary);
		SpatialGrid<Double> travelDistanceAccessibilityGrid = createSpatialGrid(boundary);
		
		// gather all workplaces
		int jmID = benchmark.addMeasure("Creating Destinations (jobObjectMap)");
		// JobClusterObject[] jobClusterArray = readFromUrbansim.readAndBuildJobsObject(parcels, jobSample); // tnicolai: old version
		AggregateObject2NearestNode[] jobClusterArray = readFromUrbansim.getAggregatedWorkplaces(parcels, jobSample, (NetworkImpl)scenario.getNetwork()); // this aggreagtes workplaces directly to their nearest node 
		benchmark.stoppMeasurement(jmID);
		logger.info("Creating job destinations (jobObjectMap) took " + benchmark.getDurationInSeconds(jmID) + "seconds.");
		
		CellBasedAccessibilityControlerListener myListener = new CellBasedAccessibilityControlerListener(startZones, 
																												   jobClusterArray,
																												   travelTimeAccessibilityGrid, 
																												   travelCostAccessibilityGrid, 
																												   travelDistanceAccessibilityGrid,
																												   CellBasedAccessibilityControlerListener.SHAPE_FILE,
																												   benchmark);
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
		
		return new SpatialGrid<Double>(xMin, yMin, xMax, yMax, gridSizeInFeet);
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
		// boundary.setSRID( Constants.SRID_WASHINGTON_NORTH );
		boundary.setSRID( Constants.SRID_SWITZERLAND );
		logger.warn("Using SRID of Switzerland: " + Constants.SRID_SWITZERLAND);
		logger.info("Done extracting boundary ...");
		
		return boundary;
	}
	
	/**
	 * 
	 * @param <T>
	 * @param gridSize
	 * @param boundary
	 * @return
	 */
	private static ZoneLayer<CounterObject> createGridLayerByGridSize(double gridSize, Geometry boundary) {
		
		logger.info("Setting statring points for accessibility measure ...");

		int skippedPoints = 0;
		int setPoints = 0;
		
		GeometryFactory factory = new GeometryFactory();
		
		Set<Zone<CounterObject>> zones = new HashSet<Zone<CounterObject>>();
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
					polygon.setSRID( Constants.SRID_WASHINGTON_NORTH ); 
					
					Zone<CounterObject> zone = new Zone<CounterObject>(polygon);
					zone.setAttribute( new CounterObject( setPoints ) );
					zones.add(zone);
					
					setPoints++;
				}
				else skippedPoints++;
			}
		}

		logger.info(setPoints + " starting points were set and " + skippedPoints + " points have been skipped (points were skipped when they lay outside the shape file boundary).");
		logger.info("Done with setting starting points!");
		
		ZoneLayer<CounterObject> layer = new ZoneLayer<CounterObject>(zones);
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
		
		MATSim4UrbanSimERSA m4uERSA = new MATSim4UrbanSimERSA(args);
		m4uERSA.runMATSim();
		
		endTime = System.currentTimeMillis();
		time = (endTime - startTime) / 60000;
		
		logger.info("Computation took " + time + " minutes. Computation done!");
	}

}

