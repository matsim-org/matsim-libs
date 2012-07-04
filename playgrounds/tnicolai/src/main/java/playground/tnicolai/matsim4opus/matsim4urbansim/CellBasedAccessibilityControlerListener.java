/* *********************************************************************** *
Ø * project: org.matsim.*
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
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.LeastCostPathTree;

import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.gis.GridUtils;
import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.gis.Zone;
import playground.tnicolai.matsim4opus.gis.ZoneLayer;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.FreeSpeedTravelTimeCostCalculator;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelWalkTimeCostCalculator;
import playground.tnicolai.matsim4opus.utils.helperObjects.AggregateObject2NearestNode;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.CounterObject;
import playground.tnicolai.matsim4opus.utils.io.writer.AnalysisCellBasedAccessibilityCSVWriterV2;
import playground.tnicolai.matsim4opus.utils.misc.ProgressBar;
import playground.tnicolai.matsim4opus.utils.network.NetworkUtil;

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
 *  improvements march'12
 *  - revised distance measure from centroid to network:
 *  	using orthogonal distance from centroid to nearest network link!
 *  - merged CellBasedAccessibilityNetworkControlerListener and CellBasedAccessibilityShapefileControlerListener
 *  - replaced "SpatialGrid<Double>" by "SpatialGrid" using double instead of Double-objects
 *  
 * TODO: implement configurable betas for different accessibility measures based on different costs
 * beta, betaTravelTimes, betaLnTravelTimes, betaPowerTravelTimes, betaTravelCosts, betaLnTravelCosts,
 * betaPowerTravelCosts, betaTravelDistance, betaLnTravelDistance, betaPowerTravelDistance
 * 
 * @author thomas
 * 
 */
public class CellBasedAccessibilityControlerListener implements ShutdownListener{

	private static final Logger log = Logger.getLogger(CellBasedAccessibilityControlerListener.class);
	public static final String SHAPE_FILE = "SF";
	public static final String NETWORK = "NW";
	
	private AggregateObject2NearestNode[] aggregatedOpportunities;
	private ZoneLayer<CounterObject> measuringPoints;
	
	private SpatialGrid congestedTravelTimeAccessibilityGrid;
	private SpatialGrid freespeedTravelTimeAccessibilityGrid;
	private SpatialGrid walkTravelTimeAccessibilityGrid;
	
	private double walkSpeedMeterPerMin = -1;
	
	private Benchmark benchmark;
	
	private String fileExtension;
	
	/**
	 * constructor
	 * 
	 * @param measuringPoints
	 * @param aggregatedOpportunities
	 * @param travelTimeAccessibilityGrid
	 * @param travelCostAccessibilityGrid
	 * @param travelDistanceAccessibilityGrid
	 * @param benchmark
	 */
	public CellBasedAccessibilityControlerListener(ZoneLayer<CounterObject> measuringPoints, 				// needed for google earth plots (not supported by now tnicolai feb'12)
			   									 AggregateObject2NearestNode[] aggregatedOpportunities, 	// destinations
			   									 SpatialGrid congestedTravelTimeAccessibilityGrid, 			// table for congested car travel times in accessibility computation
			   									 SpatialGrid freespeedTravelTimeAccessibilityGrid,			// table for free-speed car travel times in accessibility computation
			   									 SpatialGrid walkTravelTimeAccessibilityGrid, 				// table for walk travel times in accessibility computation
			   									 String extention,											// this string indicates if the study area (boundary) is given by shape-file or network
			   									 Benchmark benchmark){										// Benchmark tool
		assert ( measuringPoints != null );
		this.measuringPoints = measuringPoints;	
		assert ( aggregatedOpportunities != null );
		this.aggregatedOpportunities = aggregatedOpportunities;
		assert ( congestedTravelTimeAccessibilityGrid != null );
		this.congestedTravelTimeAccessibilityGrid = congestedTravelTimeAccessibilityGrid;
		assert ( freespeedTravelTimeAccessibilityGrid != null );
		this.freespeedTravelTimeAccessibilityGrid = freespeedTravelTimeAccessibilityGrid;
		assert ( walkTravelTimeAccessibilityGrid != null );
		this.walkTravelTimeAccessibilityGrid = walkTravelTimeAccessibilityGrid;
		assert( extention != null);
		this.fileExtension = extention;
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
		
		double walkSpeedMeterPerSec = sc.getConfig().plansCalcRoute().getWalkSpeed();
		this.walkSpeedMeterPerMin = walkSpeedMeterPerSec * 60.;
		
		TravelTime ttc = controler.getTravelTimeCalculator();
		// calculates the workplace accessibility based on congested travel times:
		// (travelTime(sec)*marginalCostOfTime)+(link.getLength()*marginalCostOfDistance) but marginalCostOfDistance = 0
		LeastCostPathTree lcptCongestedTravelTime = new LeastCostPathTree( ttc, new TravelTimeAndDistanceBasedTravelDisutility(ttc, controler.getConfig().planCalcScore()) );
		// calculates the workplace accessibility based on free-speed travel times:
		// link.getLength() * link.getFreespeed()
		LeastCostPathTree lcptFreespeedTravelTime = new LeastCostPathTree(ttc, new FreeSpeedTravelTimeCostCalculator());
		// calculates walk times in seconds as substitute for travel distances (tnicolai: changed from distance calculator to walk time feb'12)
		LeastCostPathTree lcptWalkTime = new LeastCostPathTree( ttc, new TravelWalkTimeCostCalculator( walkSpeedMeterPerSec ) );
		
		NetworkImpl network = (NetworkImpl) controler.getNetwork();
		double depatureTime = 8.*3600;	// tnicolai: make configurable
		
		double betaScale = sc.getConfig().planCalcScore().getBrainExpBeta(); // scale parameter. tnicolai: test different beta brains (e.g. 02, 1, 10 ...)
		double betaScalePreFactor = 1/betaScale;
		double betaCarHour = sc.getConfig().planCalcScore().getTraveling_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr();
		double betaCarMin = betaCarHour / 60.; // get utility per minute. this is done for urbansim that e.g. takes travel times in minutes (tnicolai feb'12)
		double betaWalkHour = sc.getConfig().planCalcScore().getTravelingWalk_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr();
		double betaWalkMin = betaWalkHour / 60.; // get utility per minute.

		try{
			AnalysisCellBasedAccessibilityCSVWriterV2 accCsvWriter = new AnalysisCellBasedAccessibilityCSVWriterV2(fileExtension);
			
			log.info("Computing and writing grid based accessibility measures with following settings:" );
			log.info("Departure time (in seconds): " + depatureTime);
			log.info("Beta car traveling utils/h: " + sc.getConfig().planCalcScore().getTraveling_utils_hr());
			log.info("Beta walk traveling utils/h: " + sc.getConfig().planCalcScore().getTravelingWalk_utils_hr());
			log.info("Beta performing utils/h: " + sc.getConfig().planCalcScore().getPerforming_utils_hr());
			log.info("Beta scale: " + betaScale);
			log.info("Beta car traveling per h: " + betaCarHour);
			log.info("Beta car traveling per min: " + betaCarMin);
			log.info("Beta walk traveling per h: " + betaWalkHour);
			log.info("Beta walk traveling per min: " + betaWalkMin);
			log.info("Walk speed (meter/min): " + this.walkSpeedMeterPerMin);
			
			Iterator<Zone<CounterObject>> measuringPointIterator = measuringPoints.getZones().iterator();
			log.info(measuringPoints.getZones().size() + " measuring points are now processing ...");
			
			ProgressBar bar = new ProgressBar( measuringPoints.getZones().size() );
		
			// iterates through all starting points (fromZone) and calculates their accessibility, e.g. to jobs
			while( measuringPointIterator.hasNext() ){
				
				bar.update();
				
				Zone<CounterObject> measurePoint = measuringPointIterator.next();
				
				Point point = measurePoint.getGeometry().getCentroid();
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
				
				// captures the distance (as walk time) between a zone centroid and its nearest node
				double walkTimeOffset_min = NetworkUtil.getDistance2Node(network.getNearestLink(coordFromZone), 
																		 point, 
																		 fromNode)  / this.walkSpeedMeterPerMin;
				// Possible offsets to calculate the gap between measuring (start) point and start node (fromNode)
				// Euclidean Distance (measuring point 2 nearest node):
				// double walkTimeOffset_min = NetworkUtil.getEuclideanDistanceAsWalkTimeInSeconds(coordFromZone, fromNode.getCoord()) / 60.;
				// Orthogonal Distance (measuring point 2 nearest link, does not include remaining distance between link intersection and nearest node)
				// LinkImpl nearestLink = network.getNearestLink( coordFromZone );
				// double walkTimeOffset_min = (nearestLink.calcDistance( coordFromZone ) / this.walkSpeedMeterPerMin); 
				// or use NetworkUtil.getOrthogonalDistance(link, point) instead!
				double congestedTravelTimesCarSum = 0.;
				double freespeedTravelTimesCarSum = 0.;
				double travelTimesWalkSum  	   	  = 0.; // substitute for travel distance

				// goes through all opportunities, e.g. jobs, (nearest network node) and calculate the accessibility
				for ( int i = 0; i < this.aggregatedOpportunities.length; i++ ) {
					
					// get stored network node (this is the nearest node next to an aggregated workplace)
					Node destinationNode = this.aggregatedOpportunities[i].getNearestNode();
					Id nodeID = destinationNode.getId();

					// using number of aggregated opportunities as weight for log sum measure
					int opportunityWeight = this.aggregatedOpportunities[i].getNumberOfObjects();

					double arrivalTime = lcptCongestedTravelTime.getTree().get( nodeID ).getTime();
					
					// congested car travel times in minutes
					double congestedTravelTime_min = (arrivalTime - depatureTime) / 60.;
					// freespeed car  travel times in minutes
					double freespeedTravelTime_min = lcptFreespeedTravelTime.getTree().get( nodeID ).getCost() / 60.;
					// walk travel times in minutes
					double walkTravelTime_min = lcptWalkTime.getTree().get( nodeID ).getCost() / 60.;
					
					// sum congested travel times
					congestedTravelTimesCarSum += Math.exp( betaScale * ((betaCarMin * congestedTravelTime_min) + (betaWalkMin * walkTimeOffset_min)) ) * opportunityWeight;
					// sum freespeed travel times
					freespeedTravelTimesCarSum += Math.exp( betaScale * ((betaCarMin * freespeedTravelTime_min) + (betaWalkMin * walkTimeOffset_min)) ) * opportunityWeight;
					// sum walk travel times (substitute for distances)
					travelTimesWalkSum 		   += Math.exp( betaScale * (betaWalkMin * (walkTravelTime_min + walkTimeOffset_min)) ) * opportunityWeight;
				}
				
				// get log sum 
				double congestedTravelTimesCarLogSum = betaScalePreFactor * Math.log( congestedTravelTimesCarSum );
				double freespeedTravelTimesCarLogSum = betaScalePreFactor * Math.log( freespeedTravelTimesCarSum );
				double travelTimesWalkLogSum 		 = betaScalePreFactor * Math.log( travelTimesWalkSum );
				
				// assign log sums to current measuring point and spatial grid
				setAccessibilityValues2MeasurePointAndSpatialGrid(measurePoint,
															   congestedTravelTimesCarLogSum, 
															   freespeedTravelTimesCarLogSum,
															   travelTimesWalkLogSum);
				
				// writing accessibility values (stored in starZone object) in csv format (for qgis) ...
				accCsvWriter.write(measurePoint, 
								   coordFromZone, 
								   fromNode, 
								   freespeedTravelTimesCarLogSum, 
								   congestedTravelTimesCarLogSum, 
								   0.,	// this should be bike accessibility tnicolai
								   travelTimesWalkLogSum);
			}
			System.out.println("");
			
			if( this.benchmark != null && benchmarkID > 0 ){
				this.benchmark.stoppMeasurement(benchmarkID);
				log.info("Accessibility computation with " + measuringPoints.getZones().size() + " starting points (origins) and " + this.aggregatedOpportunities.length + " destinations (workplaces) took " + this.benchmark.getDurationInSeconds(benchmarkID) + " seconds (" + this.benchmark.getDurationInSeconds(benchmarkID) / 60. + " minutes).");
			}
			accCsvWriter.close();
			dumpResults();
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	private void dumpResults() throws IOException{
		log.info("Writing files ...");
		// finish and close writing
		GridUtils.writeSpatialGridTable(
				congestedTravelTimeAccessibilityGrid,
				InternalConstants.MATSIM_4_OPUS_TEMP
						+ InternalConstants.CONGESTED_TRAVEL_TIME_ACCESSIBILITY
						+ "_cellsize_"
						+ congestedTravelTimeAccessibilityGrid.getResolution()
						+ fileExtension + InternalConstants.FILE_TYPE_TXT);
		GridUtils.writeSpatialGridTable(
				freespeedTravelTimeAccessibilityGrid,
				InternalConstants.MATSIM_4_OPUS_TEMP
						+ InternalConstants.FREESPEED_TRAVEL_TIME_ACCESSIBILITY
						+ "_cellsize_"
						+ freespeedTravelTimeAccessibilityGrid.getResolution()
						+ fileExtension + InternalConstants.FILE_TYPE_TXT);
		GridUtils.writeSpatialGridTable(
				walkTravelTimeAccessibilityGrid,
				InternalConstants.MATSIM_4_OPUS_TEMP
						+ InternalConstants.WALK_TRAVEL_TIME_ACCESSIBILITY
						+ "_cellsize_"
						+ walkTravelTimeAccessibilityGrid.getResolution()
						+ fileExtension + InternalConstants.FILE_TYPE_TXT);

		GridUtils.writeKMZFiles(
				measuringPoints, 
				congestedTravelTimeAccessibilityGrid, 
				InternalConstants.MATSIM_4_OPUS_TEMP
				+ InternalConstants.CONGESTED_TRAVEL_TIME_ACCESSIBILITY
				+ "_cellsize_"
				+ congestedTravelTimeAccessibilityGrid.getResolution()
				+ fileExtension + InternalConstants.FILE_TYPE_KMZ);
		GridUtils.writeKMZFiles(
				measuringPoints, 
				freespeedTravelTimeAccessibilityGrid, 
				InternalConstants.MATSIM_4_OPUS_TEMP
				+ InternalConstants.FREESPEED_TRAVEL_TIME_ACCESSIBILITY
				+ "_cellsize_"
				+ freespeedTravelTimeAccessibilityGrid.getResolution()
				+ fileExtension + InternalConstants.FILE_TYPE_KMZ);
		GridUtils.writeKMZFiles(
				measuringPoints, 
				walkTravelTimeAccessibilityGrid, 
				InternalConstants.MATSIM_4_OPUS_TEMP
				+ InternalConstants.WALK_TRAVEL_TIME_ACCESSIBILITY
				+ "_cellsize_"
				+ walkTravelTimeAccessibilityGrid.getResolution()
				+ fileExtension + InternalConstants.FILE_TYPE_KMZ);
		log.info("Writing files done!");
	}
	
	/**
	 * @param startZone
	 * @param congestedTravelTimesCarLogSum
	 * @param freespeedTravelTimesCarLogSum
	 * @param accessibilityTravelDistance
	 */
	private void setAccessibilityValues2MeasurePointAndSpatialGrid(Zone<CounterObject> startZone,
												 				double congestedTravelTimesCarLogSum, 
												 				double freespeedTravelTimesCarLogSum,
												 				double travelTimesWalkLogSum) {

		congestedTravelTimeAccessibilityGrid.setValue(congestedTravelTimesCarLogSum , startZone.getGeometry().getCentroid());
		freespeedTravelTimeAccessibilityGrid.setValue(freespeedTravelTimesCarLogSum , startZone.getGeometry().getCentroid());
		walkTravelTimeAccessibilityGrid.setValue(travelTimesWalkLogSum , startZone.getGeometry().getCentroid());
	}
}
