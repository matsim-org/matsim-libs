/* *********************************************************************** *
 * project: org.matsim.*
 * ERSAControlerListener.java
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

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.LeastCostPathTree;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.gis.EuclideanDistance;
import playground.tnicolai.matsim4opus.gis.GridUtils;
import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.gis.Zone;
import playground.tnicolai.matsim4opus.gis.ZoneLayer;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.FreeSpeedTravelTimeCostCalculator;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelWalkTimeCostCalculator;
import playground.tnicolai.matsim4opus.utils.ProgressBar;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.ClusterObject;
import playground.tnicolai.matsim4opus.utils.helperObjects.ZoneAccessibilityObject;

import com.vividsolutions.jts.geom.Point;

/**
 * improvements sep'11:
 * 
 * Code improvements since last version (deadline ersa paper):
 * - Aggregated Workplaces: 
 * 	Workplaces with same parcel_id are aggregated to a weighted job (see JobClusterObject)
 * 	This means much less iteration cycles
 * - Less time consuming look-ups: 
 * 	All workplaces are assigned to their nearest node in an pre-proscess step
 * 	(see addNearestNodeToJobClusterArray) instead to do nearest node look-ups in each 
 * 	iteration cycle
 * - Distance based accessibility added: 
 * 	like the travel time accessibility computation now also distances are computed
 * 	with LeastCostPathTree (tnicolai feb'12 distances are replaced by walking times 
 *  which is also linear and corresponds to distances)
 * 
 * improvements jan'12:
 * 
 * - Better readability:
 * 	Removed unused methods such as "addNearestNodeToJobClusterArray" (this is done while gathering/processing
 * 	workplaces). Also all results are now dumped directly from this class. Before, the SpatialGrid 
 * 	tables were transfered to another class to dump out the results. This also improves readability
 * - Workplace data dump:
 * 	Dumping out the used workplace data was simplified, since the simulation now already uses aggregated data.
 * 	Corresponding subroutines aggregating the data are not needed any more (see dumpWorkplaceData()).
 * 	But coordinates of the origin workplaces could not dumped out, this is now done in ReadFromUrbansimParcelModel
 *  during processing the UrbnAism job data
 *  
 *  improvements feb'12
 *  - distance between square centroid and nearest node on road network is considered in the accessibility computation
 *  as walk time of the euclidian distance between both (centroid and nearest node). This walk time is added as an offset 
 *  to each measured travel times
 *  - using walk travel times instead of travel distances. This is because of the betas that are utils/time unit. The walk time
 *  corresponds to distances since this is also linear.
 * 
 * TODO: implement configurable betas for different accessibility measures based on different costs
 * beta, betaTravelTimes, betaLnTravelTimes, betaPowerTravelTimes, betaTravelCosts, betaLnTravelCosts,
 * betaPowerTravelCosts, betaTravelDistance, betaLnTravelDistance, betaPowerTravelDistance
 * 
 * @author thomas
 * 
 */
public class ERSAControlerListener implements ShutdownListener{

	private static final Logger log = Logger.getLogger(ERSAControlerListener.class);
	
	private ClusterObject[] aggregatedWorkplaces;
	private ZoneLayer<ZoneAccessibilityObject> startZones;
	
	private SpatialGrid<Double> congestedTravelTimeAccessibilityGrid;
	private SpatialGrid<Double> freespeedTravelTimeAccessibilityGrid;
	private SpatialGrid<Double> walkTravelTimeAccessibilityGrid;
	
	private Benchmark benchmark;
	
	private int csvID = -1;
	
	/**
	 * constructor
	 * @param jobClusterMap
	 */
	ERSAControlerListener(ZoneLayer<ZoneAccessibilityObject> startZones, ClusterObject[] aggregatedWorkplaces, 
			SpatialGrid<Double> travelTimeAccessibilityGrid, SpatialGrid<Double> travelCostAccessibilityGrid, 
			SpatialGrid<Double> travelDistanceAccessibilityGrid, Benchmark benchmark){
		
		assert ( startZones != null );
		this.startZones	= startZones;	
		assert ( aggregatedWorkplaces != null );
		this.aggregatedWorkplaces 	= aggregatedWorkplaces;
		assert ( travelTimeAccessibilityGrid != null );
		this.congestedTravelTimeAccessibilityGrid = travelTimeAccessibilityGrid;
		assert ( travelCostAccessibilityGrid != null );
		this.freespeedTravelTimeAccessibilityGrid = travelCostAccessibilityGrid;
		assert ( travelDistanceAccessibilityGrid != null );
		this.walkTravelTimeAccessibilityGrid = travelDistanceAccessibilityGrid;
		assert( benchmark != null );
		this.benchmark = benchmark;
	}
	
	/**
	 * calculating accessibility indicators
	 */
	@Override
	public void notifyShutdown(ShutdownEvent event){
		log.info("Entering notifyShutdown ..." );
		
		int benchmarkID = this.benchmark.addMeasure("1-Point accessibility computation");
		
		// get the controller and scenario
		Controler controler = event.getControler();
		Scenario sc = controler.getScenario();
		// init spannig tree in order to calculate travel times and travel costs
		TravelTime ttc = controler.getTravelTimeCalculator();
		// calculates the workplace accessibility based on congested travel times:
		// (travelTime(sec)*marginalCostOfTime)+(link.getLength()*marginalCostOfDistance) but marginalCostOfDistance = 0
		LeastCostPathTree lcptCongestedTravelTime = new LeastCostPathTree( ttc, new TravelTimeDistanceCostCalculator(ttc, controler.getConfig().planCalcScore()) );
		// calculates the workplace accessibility based on freespeed travel times:
		// link.getLength() * link.getFreespeed()
		LeastCostPathTree lcptFreespeedTravelTime = new LeastCostPathTree(ttc, new FreeSpeedTravelTimeCostCalculator());
		// calculates walk times in seconds as substitute for travel distances (tnicolai: changed from distance calculator to walk time feb'12)
		LeastCostPathTree lcptWalkTime = new LeastCostPathTree( ttc, new TravelWalkTimeCostCalculator() );
		
		NetworkImpl network = (NetworkImpl) controler.getNetwork();
		double depatureTime = 8.*3600;	// tnicolai: make configurable
		
		double betaBrain = sc.getConfig().planCalcScore().getBrainExpBeta(); // scale parameter. tnicolai: test different beta brains (e.g. 02, 1, 10 ...)
		double betaCarHour = betaBrain * (sc.getConfig().planCalcScore().getTraveling_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr());
		double betaCarMin = betaCarHour / 60.; // get utility per minute. this is done for urbansim that e.g. takes travel times in minutes (tnicolai feb'12)
		double betaWalkHour = betaBrain * (sc.getConfig().planCalcScore().getTravelingWalk_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr());
		double betaWalkMin = betaWalkHour / 60.; // get utility per minute.

		try{
			BufferedWriter accessibilityIndicatorWriter = initCSVWriter( sc );
			
			log.info("Computing and writing grid based accessibility measures with following settings:" );
			log.info("Depature time (in seconds): " + depatureTime);
			log.info("Beta car traveling utils/h: " + sc.getConfig().planCalcScore().getTraveling_utils_hr());
			log.info("Beta walk traveling utils/h: " + sc.getConfig().planCalcScore().getTravelingWalk_utils_hr());
			log.info("Beta performing utils/h: " + sc.getConfig().planCalcScore().getPerforming_utils_hr());
			log.info("Beta brain (scale factor): " + betaBrain);
			log.info("Beta car traveling per h: " + betaCarHour);
			log.info("Beta car traveling per min: " + betaCarMin);
			log.info("Beta walk traveling per h: " + betaWalkHour);
			log.info("Beta walk traveling per min: " + betaWalkMin);
			
			Iterator<Zone<ZoneAccessibilityObject>> startZoneIterator = startZones.getZones().iterator();
			log.info(startZones.getZones().size() + " measurement points are now processing ...");
			
			ProgressBar bar = new ProgressBar( startZones.getZones().size() );
		
			// iterates through all starting points (fromZone) and calculates their workplace accessibility
			while( startZoneIterator.hasNext() ){
				
				bar.update();
				
				Zone<ZoneAccessibilityObject> startZone = startZoneIterator.next();
				
				Point point = startZone.getGeometry().getCentroid();
				// get coordinate from origin (start point)
				Coord coordFromZone = new CoordImpl( point.getX(), point.getY());
				assert( coordFromZone!=null );
				// determine nearest network node
				Node fromNode = network.getNearestNode(coordFromZone);
				assert( fromNode != null );
				// run dijkstra on network
				lcptCongestedTravelTime.calculate(network, fromNode, depatureTime);
				lcptFreespeedTravelTime.calculate(network, fromNode, depatureTime);			
				lcptWalkTime.calculate(network, fromNode, depatureTime);	
				
				// from here: accessibility computation for current starting point ("fromNode")
				
				// captures the eulidean distance between a square centroid and its nearest node
				double walkTimeOffset_min = EuclideanDistance.getEuclideanDistanceAsWalkTimeInSeconds(coordFromZone, fromNode.getCoord()) / 60.;
				double congestedTravelTimesCarSum = 0.;
				double freespeedTravelTimesCarSum = 0.;
				double travelTimesWalkSum  	   	  = 0.; // substitute for travel distance

				// go through all jobs (nearest network node) and calculate workplace accessibility
				for ( int i = 0; i < this.aggregatedWorkplaces.length; i++ ) {
					
					// get stored network node (this is the nearest node next to an aggregated workplace)
					Node destinationNode = this.aggregatedWorkplaces[i].getNearestNode();
					Id nodeID = destinationNode.getId();

					// using number of aggregated workplaces as weight for log sum measure
					int jobWeight = this.aggregatedWorkplaces[i].getNumberOfObjects();

					double arrivalTime = lcptCongestedTravelTime.getTree().get( nodeID ).getTime();
					
					// congested car travel times in minutes
					double congestedTravelTime_min = (arrivalTime - depatureTime) / 60.;
					// freespeed car  travel times in minutes
					double freespeedTravelTime_min = lcptFreespeedTravelTime.getTree().get( nodeID ).getCost() / 60.;
					// walk travel times in minutes
					double walkTravelTime_min = lcptWalkTime.getTree().get( nodeID ).getCost() / 60.;
					
					// sum congested travel times
					congestedTravelTimesCarSum += Math.exp( betaCarMin * (congestedTravelTime_min + walkTimeOffset_min) ) * jobWeight;
					// sum freespeed travel times
					freespeedTravelTimesCarSum += Math.exp( betaCarMin * (freespeedTravelTime_min + walkTimeOffset_min) ) * jobWeight;
					// sum walk travel times (substitute for distances)
					travelTimesWalkSum 		   += Math.exp( betaWalkMin * (walkTravelTime_min + walkTimeOffset_min) ) * jobWeight;
				}
				
				// assign accessibilities sums to current starZone object. 
				setAccessibilityValue2StartZone(startZone,
												congestedTravelTimesCarSum, 
												freespeedTravelTimesCarSum,
												travelTimesWalkSum);
				
				// accessibility values stored in current starZone object are now used in spatial grid
				setAccessiblityValue2SpatialGrid(startZone);
				
				// writing accessibility values (stored in starZone object) in csv format ...
				dumpCSVData(accessibilityIndicatorWriter, startZone, coordFromZone);
			}
			System.out.println("");
			
			if( this.benchmark != null && benchmarkID > 0 ){
				this.benchmark.stoppMeasurement(benchmarkID);
				log.info("Accessibility computation with " + startZones.getZones().size() + " starting points (origins) and " + this.aggregatedWorkplaces.length + " destinations (workplaces) took " + this.benchmark.getDurationInSeconds(benchmarkID) + " seconds (" + this.benchmark.getDurationInSeconds(benchmarkID) / 60. + " minutes).");
			}
			
			dumpResults(accessibilityIndicatorWriter);
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void dumpResults(BufferedWriter accessibilityIndicatorWriter) throws IOException{
		log.info("Writing files ...");
		// finish and close writing
		closeCSVFile(accessibilityIndicatorWriter);
		dumpWorkplaceData();
		GridUtils.writeSpatialGridTables(this);
		GridUtils.writeKMZFiles(this);
		log.info("Writing files done!");
	}

	/**
	 * @throws IOException
	 */
	private void dumpWorkplaceData() throws IOException {
		
		log.info("Dumping workplace information as csv ...");

		BufferedWriter bwAggregatedWP = IOUtils.getBufferedWriter( Constants.MATSIM_4_OPUS_TEMP + "aggregated_workplaces.csv" );
//		BufferedWriter bwAggregatedWP = IOUtils.getBufferedWriter( Constants.MATSIM_4_OPUS_TEMP + "aggregated_workplaces.csv" );
		
		log.info("Dumping workplace data used for this simulation: " + Constants.MATSIM_4_OPUS_TEMP + "aggregated_workplaces.csv");
		
		// dumping out data from jobClusterArray (containing aggregated workplace data)
		bwAggregatedWP.write("zone_ID,num_of_jobs,x_nearest_node_coord,y_nearest_node_coord,nearest_node_ID");
		bwAggregatedWP.newLine();
		for(int i = 0; i < aggregatedWorkplaces.length; i++){
			bwAggregatedWP.write(aggregatedWorkplaces[i].getZoneID() + "," + 
						   	   aggregatedWorkplaces[i].getNumberOfObjects() + "," +
					 		   aggregatedWorkplaces[i].getCoordinate().getX() + "," +
					 		   aggregatedWorkplaces[i].getCoordinate().getY() + "," +
					 		   aggregatedWorkplaces[i].getNearestNode().getId());
			bwAggregatedWP.newLine();
		}
		bwAggregatedWP.flush();
		bwAggregatedWP.close();
		
		log.info("... done!");
		
//		log.info("Aggregating workplaces to their nearest network node and dumping results as csv: " + Constants.MATSIM_4_OPUS_TEMP + "aggregated_workplaces.csv");
//		
//		// aggregate number of workplaces to their nearest network node and dump out the results
//		Map<Id,WorkplaceObject> aggregatedWorkplacesHashMap = new HashMap<Id,WorkplaceObject>();
//		Map<Id,Node> nearestNode = new HashMap<Id, Node>();
//		for(int i = 0; i < aggregatedWorkplaces.length; i++){
//			Id nodeID = aggregatedWorkplaces[i].getNearestNode().getId();
//			if(aggregatedWorkplacesHashMap.containsKey( nodeID )){
//				WorkplaceObject numWorkplaces = aggregatedWorkplacesHashMap.get( nodeID );
//				numWorkplaces.counter += aggregatedWorkplaces[i].getNumberOfJobs();
//			}
//			else{
//				WorkplaceObject numWorkplaces = new WorkplaceObject();
//				numWorkplaces.counter = aggregatedWorkplaces[i].getNumberOfJobs();
//				aggregatedWorkplacesHashMap.put(nodeID, numWorkplaces);
//				nearestNode.put( nodeID, aggregatedWorkplaces[i].getNearestNode());
//			}
//		}
//		bwAggregatedWP.write("nearest_node_ID,nearest_node_x_coord,nearest_node_y_coord,job_count");
//		bwAggregatedWP.newLine();
//		Iterator<Node> nodeIterator = nearestNode.values().iterator();
//		while(nodeIterator.hasNext()){
//			Node node = nodeIterator.next();
//			long count = 0L; 
//			if(aggregatedWorkplacesHashMap.get( node.getId() ) != null){
//				count = aggregatedWorkplacesHashMap.get( node.getId() ).counter;
//			}
//			bwAggregatedWP.write(node.getId() + "," +
//								 node.getCoord().getX() + "," +
//								 node.getCoord().getY() + "," +
//								 count);
//			bwAggregatedWP.newLine();
//		}
//		bwAggregatedWP.flush();
//		bwAggregatedWP.close();
//		
//		log.info("... done!");
	}

	/**
	 * Sets the accessibility values for each spatial grid (travel times, travel costs and travel distance)
	 * 
	 * @param startZone
	 */
	private void setAccessiblityValue2SpatialGrid(Zone<ZoneAccessibilityObject> startZone) {
		
		congestedTravelTimeAccessibilityGrid.setValue(startZone.getAttribute().getCongestedTravelTimeAccessibility() , startZone.getGeometry().getCentroid());
		freespeedTravelTimeAccessibilityGrid.setValue(startZone.getAttribute().getFreespeedTravelTimeAccessibility() , startZone.getGeometry().getCentroid());
		walkTravelTimeAccessibilityGrid.setValue(startZone.getAttribute().getWalkTravelTimeAccessibility() , startZone.getGeometry().getCentroid());
	}
	
	/**
	 * @param sc
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private BufferedWriter initCSVWriter(Scenario sc)
			throws FileNotFoundException, IOException {
		String filename = sc.getConfig().getParam(Constants.MATSIM_4_URBANSIM_PARAM, Constants.MATSIM_4_OPUS_TEMP_PARAM) + "accessibility_indicators_ersa.csv";
		csvID = benchmark.addMeasure("Writing CSV File (Accessibility Measures)", filename, false);
		
		BufferedWriter accessibilityIndicatorWriter = IOUtils.getBufferedWriter( filename );
		// create header
		accessibilityIndicatorWriter.write( Constants.ERSA_ZONE_ID + "," +
											Constants.ERSA_X_COORDNIATE + "," +
											Constants.ERSA_Y_COORDINATE + "," + 
											Constants.ERSA_CONGESTED_TRAVEL_TIME_ACCESSIBILITY + "," +
											Constants.ERSA_FREESPEED_TRAVEL_TIME_ACCESSIBILITY + "," + 
											Constants.ERSA_WALK_TRAVEL_TIME_ACCESSIBILITY);
		accessibilityIndicatorWriter.newLine();
		return accessibilityIndicatorWriter;
	}	
	
	/**
	 * @param startZone
	 * @param accessibilityTravelTimes
	 * @param accessibilityTravelCosts
	 * @param accessibilityTravelDistance
	 */
	private void setAccessibilityValue2StartZone(Zone<ZoneAccessibilityObject> startZone,
												 double accessibilityTravelTimes, 
												 double accessibilityTravelCosts,
												 double accessibilityTravelDistance) {
		
		double tt = Math.log( accessibilityTravelTimes );
		double tc = Math.log( accessibilityTravelCosts );
		double td =  Math.log( accessibilityTravelDistance);
		
		startZone.getAttribute().setCongestedTravelTimeAccessibility( tt < 0.0 ? 0.0 : tt );
		startZone.getAttribute().setFreespeedTravelTimeAccessibility( tc < 0.0 ? 0.0 : tc );
		startZone.getAttribute().setWalkTravelTimeAccessibility(td < 0.0 ? 0.0 : td );
	}

	/**
	 * @param accessibilityIndicatorWriter
	 * @param startZone
	 * @param coordFromZone
	 * @throws IOException
	 */
	private void dumpCSVData(BufferedWriter accessibilityIndicatorWriter,
			Zone<ZoneAccessibilityObject> startZone, Coord coordFromZone) throws IOException {
		
		// dumping results into output file
		accessibilityIndicatorWriter.write( startZone.getAttribute().getZoneID() + "," +
											coordFromZone.getX() + "," +
											coordFromZone.getY() + "," +
											startZone.getAttribute().getCongestedTravelTimeAccessibility() + "," +
											startZone.getAttribute().getFreespeedTravelTimeAccessibility() + "," +
											startZone.getAttribute().getWalkTravelTimeAccessibility() );
		accessibilityIndicatorWriter.newLine();
	}

	/**
	 * finish and close accessibility writer
	 * 
	 * @param accessibilityIndicatorWriter
	 * @throws IOException
	 */
	private void closeCSVFile(BufferedWriter accessibilityIndicatorWriter) throws IOException {
		//		accessibilityIndicatorWriter.newLine();
		//		accessibilityIndicatorWriter.write("used parameters");
		//		accessibilityIndicatorWriter.newLine();
		//		accessibilityIndicatorWriter.write("beta," + beta);
		accessibilityIndicatorWriter.flush();
		accessibilityIndicatorWriter.close();
		
		benchmark.stoppMeasurement(csvID);
		log.info("Done with writing CSV-File. This took " + benchmark.getDurationInSeconds(csvID) + " seconds.");
	}
	
	// getter methods
	
	public ZoneLayer<ZoneAccessibilityObject> getStartZones(){
		return startZones;
	}
	public ClusterObject[] getJobObjectMap(){
		return aggregatedWorkplaces;
	}
	public SpatialGrid<Double> getCongestedTravelTimeAccessibilityGrid(){
		return congestedTravelTimeAccessibilityGrid;
	}
	public SpatialGrid<Double> getFreespeedTravelTimeAccessibilityGrid(){
		return freespeedTravelTimeAccessibilityGrid;
	}
	public SpatialGrid<Double> getWalkTravelTimeAccessibilityGrid(){
		return walkTravelTimeAccessibilityGrid;
	}
}
