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
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.TravelTimeAndDistanceBasedTravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.utils.LeastCostPathTree;

import playground.tnicolai.matsim4opus.gis.GridUtils;
import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.gis.Zone;
import playground.tnicolai.matsim4opus.gis.ZoneLayer;
import playground.tnicolai.matsim4opus.interfaces.controlerinterface.AccessibilityControlerInterface;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.FreeSpeedTravelTimeCostCalculator;
import playground.tnicolai.matsim4opus.matsim4urbansim.costcalculators.TravelWalkTimeCostCalculator;
import playground.tnicolai.matsim4opus.utils.ProgressBar;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.ClusterObject;
import playground.tnicolai.matsim4opus.utils.helperObjects.ZoneAccessibilityObject;
import playground.tnicolai.matsim4opus.utils.io.writer.CellBasedAccessibilityCSVWriter;

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
 * 
 * TODO: implement configurable betas for different accessibility measures based on different costs
 * beta, betaTravelTimes, betaLnTravelTimes, betaPowerTravelTimes, betaTravelCosts, betaLnTravelCosts,
 * betaPowerTravelCosts, betaTravelDistance, betaLnTravelDistance, betaPowerTravelDistance
 * 
 * @author thomas
 * 
 */
public class CellBasedAccessibilityNetworkControlerListener implements ShutdownListener, AccessibilityControlerInterface{

	private static final Logger log = Logger.getLogger(CellBasedAccessibilityNetworkControlerListener.class);
	
	private ClusterObject[] aggregatedWorkplaces;
	private ZoneLayer<ZoneAccessibilityObject> startZones;
	
	private SpatialGrid<Double> congestedTravelTimeAccessibilityGrid;
	private SpatialGrid<Double> freespeedTravelTimeAccessibilityGrid;
	private SpatialGrid<Double> walkTravelTimeAccessibilityGrid;
	
	private double walkSpeedMeterPerMin = -1;
	
	private Benchmark benchmark;
	
	private String fileExtension = "networkBoundary";
	
	/**
	 * constructor
	 * 
	 * @param startZones
	 * @param aggregatedWorkplaces
	 * @param travelTimeAccessibilityGrid
	 * @param travelCostAccessibilityGrid
	 * @param travelDistanceAccessibilityGrid
	 * @param benchmark
	 */
	CellBasedAccessibilityNetworkControlerListener(ZoneLayer<ZoneAccessibilityObject> startZones, 				// needed for google earth plots (not supported by now tnicolai feb'12)
												   ClusterObject[] aggregatedWorkplaces, 						// destinations
												   SpatialGrid<Double> congestedTravelTimeAccessibilityGrid, 	// table for congested car travel times in accessibility computation
												   SpatialGrid<Double> freespeedTravelTimeAccessibilityGrid,	// table for freespeed car travel times in accessibility computation
												   SpatialGrid<Double> walkTravelTimeAccessibilityGrid, 		// table for walk travel times in accessibility computation
												   Benchmark benchmark){										// Benchmark tool
		assert ( startZones != null );
		this.startZones	= startZones;	
		assert ( aggregatedWorkplaces != null );
		this.aggregatedWorkplaces 	= aggregatedWorkplaces;
		assert ( congestedTravelTimeAccessibilityGrid != null );
		this.congestedTravelTimeAccessibilityGrid = congestedTravelTimeAccessibilityGrid;
		assert ( freespeedTravelTimeAccessibilityGrid != null );
		this.freespeedTravelTimeAccessibilityGrid = freespeedTravelTimeAccessibilityGrid;
		assert ( walkTravelTimeAccessibilityGrid != null );
		this.walkTravelTimeAccessibilityGrid = walkTravelTimeAccessibilityGrid;
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
		// calculates the workplace accessibility based on freespeed travel times:
		// link.getLength() * link.getFreespeed()
		LeastCostPathTree lcptFreespeedTravelTime = new LeastCostPathTree(ttc, new FreeSpeedTravelTimeCostCalculator());
		// calculates walk times in seconds as substitute for travel distances (tnicolai: changed from distance calculator to walk time feb'12)
		LeastCostPathTree lcptWalkTime = new LeastCostPathTree( ttc, new TravelWalkTimeCostCalculator( walkSpeedMeterPerSec ) );
		
		NetworkImpl network = (NetworkImpl) controler.getNetwork();
		double depatureTime = 8.*3600;	// tnicolai: make configurable
		
		double betaBrain = sc.getConfig().planCalcScore().getBrainExpBeta(); // scale parameter. tnicolai: test different beta brains (e.g. 02, 1, 10 ...)
		double betaBrainPerMinPreFactor = 1/(betaBrain / 60.);
		double betaCarHour = betaBrain * (sc.getConfig().planCalcScore().getTraveling_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr());
		double betaCarMin = betaCarHour / 60.; // get utility per minute. this is done for urbansim that e.g. takes travel times in minutes (tnicolai feb'12)
		double betaWalkHour = betaBrain * (sc.getConfig().planCalcScore().getTravelingWalk_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr());
		double betaWalkMin = betaWalkHour / 60.; // get utility per minute.

		try{
			CellBasedAccessibilityCSVWriter accCsvWriter = new CellBasedAccessibilityCSVWriter(fileExtension);
			
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
			log.info("Walk speed (meter/min): " + this.walkSpeedMeterPerMin);
			
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
				
				// captures the euclidean distance between a square centroid and its nearest node
				LinkImpl nearestLink = network.getNearestLink( coordFromZone );
				double distCentroid2Link = nearestLink.calcDistance(coordFromZone);
				double walkTimeOffset_min = (distCentroid2Link / this.walkSpeedMeterPerMin); 
//				double walkTimeOffset_min = EuclideanDistance.getEuclideanDistanceAsWalkTimeInSeconds(coordFromZone, fromNode.getCoord()) / 60.;
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
					congestedTravelTimesCarSum += Math.exp( (betaCarMin * congestedTravelTime_min) + (betaWalkMin * walkTimeOffset_min) ) * jobWeight;
					// sum freespeed travel times
					freespeedTravelTimesCarSum += Math.exp( (betaCarMin * freespeedTravelTime_min) + (betaWalkMin * walkTimeOffset_min) ) * jobWeight;
					// sum walk travel times (substitute for distances)
					travelTimesWalkSum 		   += Math.exp( betaWalkMin * (walkTravelTime_min + walkTimeOffset_min) ) * jobWeight;
				}
				
				// get log sum 
				double congestedTravelTimesCarLogSum = betaBrainPerMinPreFactor * Math.log( congestedTravelTimesCarSum );
				double freespeedTravelTimesCarLogSum = betaBrainPerMinPreFactor * Math.log( freespeedTravelTimesCarSum );
				double travelTimesWalkLogSum 		 = betaBrainPerMinPreFactor * Math.log( travelTimesWalkSum );
				
				// assign log sums to current starZone object and spatial grid
				setAccessibilityValues2StartZoneAndSpatialGrid(startZone,
															   congestedTravelTimesCarLogSum, 
															   freespeedTravelTimesCarLogSum,
															   travelTimesWalkLogSum);
				
				// writing accessibility values (stored in starZone object) in csv format ...
				accCsvWriter.write(startZone, 
						   coordFromZone, 
						   fromNode, 
						   congestedTravelTimesCarLogSum, 
						   freespeedTravelTimesCarLogSum, 
						   travelTimesWalkLogSum);
			}
			System.out.println("");
			
			if( this.benchmark != null && benchmarkID > 0 ){
				this.benchmark.stoppMeasurement(benchmarkID);
				log.info("Accessibility computation with " + startZones.getZones().size() + " starting points (origins) and " + this.aggregatedWorkplaces.length + " destinations (workplaces) took " + this.benchmark.getDurationInSeconds(benchmarkID) + " seconds (" + this.benchmark.getDurationInSeconds(benchmarkID) / 60. + " minutes).");
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
		GridUtils.writeSpatialGridTables(this, "UsingNetworkBoundary");
		GridUtils.writeKMZFiles(this, "UsingNetworkBoundary");
		log.info("Writing files done!");
	}
	
	/**
	 * @param startZone
	 * @param congestedTravelTimesCarLogSum
	 * @param freespeedTravelTimesCarLogSum
	 * @param accessibilityTravelDistance
	 */
	private void setAccessibilityValues2StartZoneAndSpatialGrid(Zone<ZoneAccessibilityObject> startZone,
												 				double congestedTravelTimesCarLogSum, 
												 				double freespeedTravelTimesCarLogSum,
												 				double travelTimesWalkLogSum) {

		startZone.getAttribute().setCongestedTravelTimeAccessibility( congestedTravelTimesCarLogSum );
		startZone.getAttribute().setFreespeedTravelTimeAccessibility( freespeedTravelTimesCarLogSum );
		startZone.getAttribute().setWalkTravelTimeAccessibility(travelTimesWalkLogSum );
		
		congestedTravelTimeAccessibilityGrid.setValue(congestedTravelTimesCarLogSum , startZone.getGeometry().getCentroid());
		freespeedTravelTimeAccessibilityGrid.setValue(freespeedTravelTimesCarLogSum , startZone.getGeometry().getCentroid());
		walkTravelTimeAccessibilityGrid.setValue(travelTimesWalkLogSum , startZone.getGeometry().getCentroid());
	}
	
	// getter methods (this implements AccessibilityControlerInterface)
	
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
