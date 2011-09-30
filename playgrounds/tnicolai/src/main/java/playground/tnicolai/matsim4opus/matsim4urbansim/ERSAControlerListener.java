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
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.contrib.sna.gis.Zone;
import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.gis.SpatialGrid;
import playground.tnicolai.matsim4opus.utils.ProgressBar;
import playground.tnicolai.matsim4opus.utils.helperObjects.Benchmark;
import playground.tnicolai.matsim4opus.utils.helperObjects.JobClusterObject;
import playground.tnicolai.matsim4opus.utils.helperObjects.WorkplaceObject;
import playground.tnicolai.matsim4opus.utils.helperObjects.ZoneAccessibilityObject;
import playground.toronto.ttimematrix.LeastCostPathTree;

import com.vividsolutions.jts.geom.Point;

/**
 * @author thomas
 *
 */
public class ERSAControlerListener implements ShutdownListener{
	
	/**
	 * Code improvements since last version (deadline ersa paper):
	 * - aggregated workplaces: workplaces with same parcel_id are aggregated to a weighted job (see JobClusterObject)
	 * 							This means much less iteration cycles
	 * - less time consuming look-ups: all workplaces are assigned to their nearest node in an pre-proscess step
	 * 							(see addNearestNodeToJobClusterArray) instead to do nearest node look-ups in each 
	 * 							iteration cycle
	 * - distance based accessibility: like the travel time accessibility computation now also distances are computed
	 * 							with LeastCostPathTree -> CHECK WITH KAI IF TRAVELDISTANCECOSTCALULATOR IS OK!
	 * tnicolai: sep'11
	 */
	
	private static final Logger log = Logger.getLogger(ERSAControlerListener.class);
	
	private JobClusterObject[] jobClusterArray;
	private ZoneLayer<ZoneAccessibilityObject> startZones;
	
	private SpatialGrid<Double> travelTimeAccessibilityGrid;
	private SpatialGrid<Double> travelCostAccessibilityGrid;
	private SpatialGrid<Double> travelDistanceAccessibilityGrid;
	
	private Benchmark benchmark;
	
	private int csvID = -1;
	
	/**
	 * constructor
	 * @param jobClusterMap
	 */
	ERSAControlerListener(ZoneLayer<ZoneAccessibilityObject> startZones, JobClusterObject[] jobClusterArray, 
			SpatialGrid<Double> travelTimeAccessibilityGrid, SpatialGrid<Double> travelCostAccessibilityGrid, 
			SpatialGrid<Double> travelDistanceAccessibilityGrid, Benchmark benchmark){
		
		assert ( startZones != null );
		this.startZones			= startZones;	
		assert ( jobClusterArray != null );
		this.jobClusterArray 	= jobClusterArray;
		assert ( travelTimeAccessibilityGrid != null );
		this.travelTimeAccessibilityGrid = travelTimeAccessibilityGrid;
		assert ( travelCostAccessibilityGrid != null );
		this.travelCostAccessibilityGrid = travelCostAccessibilityGrid;
		assert ( travelDistanceAccessibilityGrid != null );
		this.travelDistanceAccessibilityGrid = travelDistanceAccessibilityGrid;
		this.benchmark 			= benchmark;
	}
	
	/**
	 * calculating accessibility indicators
	 */
	public void notifyShutdown(ShutdownEvent event){
		log.info("Entering notifyShutdown ..." );
		
		int benchmarkID = -1;
		if(this.benchmark != null)
			benchmarkID = this.benchmark.addMeasure("1-Point accessibility computation");
		
		// get the controller and scenario
		Controler controler = event.getControler();
		Scenario sc = controler.getScenario();
		// init spannig tree in order to calculate travel times and travel costs
		TravelTime ttc = controler.getTravelTimeCalculator();
		// this calculates the workplace accessibility travel times
		LeastCostPathTree lcptTravelTime = new LeastCostPathTree( ttc, new TravelTimeDistanceCostCalculator(ttc, controler.getConfig().planCalcScore()) );
		// this calculates the workplace accessibility distances
		LeastCostPathTree lcptDistance = new LeastCostPathTree( ttc, new TravelDistanceCostCalculator() ); // tnicolai: this is experimental, check with Kai, sep'2011
		
		NetworkImpl network = controler.getNetwork();
		double depatureTime = 8.*3600;	// tnicolai: make configurable
		lcptTravelTime.setDepartureTime(depatureTime);
		lcptDistance.setDepartureTime(depatureTime);
		
		double beta_per_hr = sc.getConfig().planCalcScore().getTraveling_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr();
		double beta_per_min = beta_per_hr / 60.; // get utility per minute
		
		try{
			BufferedWriter accessibilityIndicatorWriter = initCSVWriter( sc );
			
			log.info("Computing and writing accessibility measures ..." );
			Iterator<Zone<ZoneAccessibilityObject>> startZoneIterator = startZones.getZones().iterator();
			log.info(startZones.getZones().size() + " measurement points are now processing ...");
			
			addNearestNodeToJobClusterArray( network );
			
			ProgressBar bar = new ProgressBar( startZones.getZones().size() );
		
			// iterates through all starting points (fromZone) and calculates their workplace accessibility
			while( startZoneIterator.hasNext() ){
				
				bar.update();
				
				Zone<ZoneAccessibilityObject> startZone = startZoneIterator.next();
				
				Point point = startZone.getGeometry().getCentroid();
				// get coordinate from origin (start point)
				Coord coordFromZone = new CoordImpl( point.getX(), point.getY());
				assert( coordFromZone!=null );
				Node fromNode = network.getNearestNode(coordFromZone);
				assert( fromNode != null );
				// starting Dijkstra
				lcptTravelTime.setOrigin( fromNode );	// setting starting point
				lcptTravelTime.run( network );			// run dijkstra on network
				lcptDistance.setOrigin( fromNode );
				lcptDistance.run( network );	
				
				// from here: accessibility computation for current starting point ("fromNode")
				
				double accessibilityTravelTimes = 0.;
				double accessibilityTravelTimeCosts = 0.;
				double accessibilityTravelDistanceCosts = 0.;

				// go through all jobs (nearest network node) and calculate workplace accessibility
				for ( int i = 0; i < this.jobClusterArray.length; i++ ) {
					
					Node destinationNode = this.jobClusterArray[i].getNearestNode();
					Id nodeID = destinationNode.getId();
					int jobCounter = this.jobClusterArray[i].getNumberOfJobs();

					double arrivalTime = lcptTravelTime.getTree().get( nodeID ).getTime();
					
					// travel times in minutes
					double travelTime_min = (arrivalTime - depatureTime) / 60.;
					// travel costs in utils
					double travelCosts = lcptTravelTime.getTree().get( nodeID ).getCost();
					// travel distance by car in meter
					double travelDistance_meter = lcptDistance.getTree().get( nodeID ).getCost();
					
					// sum travel times
					accessibilityTravelTimes += Math.exp( beta_per_min * travelTime_min ) * jobCounter;
					// sum travel costs
					accessibilityTravelTimeCosts += Math.exp( beta_per_min * travelCosts ) * jobCounter; // tnicolai: find another beta for travel costs
					// sum travel distances
					accessibilityTravelDistanceCosts += Math.exp( beta_per_min * travelDistance_meter ) * jobCounter; // tnicolai: find another beta for travel distance
				}
				
				setAccessibilityValue2StartZone(startZone,
						accessibilityTravelTimes, accessibilityTravelTimeCosts,
						accessibilityTravelDistanceCosts);
				
				// sets the accessibility values for each spatial grid (travel times, travel costs and travel distance)
				setAccessiblityValue2SpatialGrid(startZone);
				
				// dumping results into csv file
				dumpCSVData(accessibilityIndicatorWriter, startZone, coordFromZone);
			}
			System.out.println("");
			// finish and close writing
			closeCSVFile(accessibilityIndicatorWriter);
			
			dumpWorkplaceData();
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		if( this.benchmark != null && benchmarkID > 0 ){
			this.benchmark.stoppMeasurement(benchmarkID);
			log.info("Accessibility computation with " + startZones.getZones().size() + " starting points (origins) and " + this.jobClusterArray.length + " destinations (workplaces) took " + this.benchmark.getDurationInSeconds(benchmarkID) + " seconds (" + this.benchmark.getDurationInSeconds(benchmarkID) / 60. + " minutes).");
		}
	}

	/**
	 * @throws IOException
	 */
	private void dumpWorkplaceData() throws IOException {
		
		log.info("Dumping workplace information as csv ...");

		BufferedWriter bwWeightedWP = IOUtils.getBufferedWriter( Constants.MATSIM_4_OPUS_TEMP + "weighted_workplaces.csv" );
		BufferedWriter bwAggregatedWP = IOUtils.getBufferedWriter( Constants.MATSIM_4_OPUS_TEMP + "aggregated_workplaces.csv" );
		
		log.info("Dumping workplace data used for this simulation: " + Constants.MATSIM_4_OPUS_TEMP + "weighted_workplaces.csv");
		
		// dumping out data from jobClusterArray
		bwWeightedWP.write("zone_ID,x_coord,y_coord,job_count,nearest_node_ID,nearest_node_x_coord,nearest_node_y_coord");
		bwWeightedWP.newLine();
		for(int i = 0; i < jobClusterArray.length; i++){
			bwWeightedWP.write(jobClusterArray[i].getZoneID() + "," + 
					 jobClusterArray[i].getCoordinate().getX() + "," +
					 jobClusterArray[i].getCoordinate().getY() + "," +
					 jobClusterArray[i].getNumberOfJobs() + "," +
					 jobClusterArray[i].getNearestNode().getId()  + "," +
					 jobClusterArray[i].getNearestNode().getCoord().getX()  + "," +
					 jobClusterArray[i].getNearestNode().getCoord().getX());
			bwWeightedWP.newLine();
		}
		bwWeightedWP.flush();
		bwWeightedWP.close();
		
		log.info("... done!");
		
		log.info("Aggregating workplaces to their nearest network node and dumping results as csv: " + Constants.MATSIM_4_OPUS_TEMP + "aggregated_workplaces.csv");
		
		// aggregate number of workplaces to their nearest network node and dump out the results
		Map<Id,WorkplaceObject> aggregatedWorkplaces = new HashMap<Id,WorkplaceObject>();
		Map<Id,Node> nearestNode = new HashMap<Id, Node>();
		for(int i = 0; i < jobClusterArray.length; i++){
			Id nodeID = jobClusterArray[i].getNearestNode().getId();
			if(aggregatedWorkplaces.containsKey( nodeID )){
				WorkplaceObject numWorkplaces = aggregatedWorkplaces.get( nodeID );
				numWorkplaces.counter += jobClusterArray[i].getNumberOfJobs();
			}
			else{
				WorkplaceObject numWorkplaces = new WorkplaceObject();
				numWorkplaces.counter = jobClusterArray[i].getNumberOfJobs();
				aggregatedWorkplaces.put(nodeID, numWorkplaces);
				nearestNode.put( nodeID, jobClusterArray[i].getNearestNode());
			}
		}
		bwAggregatedWP.write("nearest_node_ID,nearest_node_x_coord,nearest_node_y_coord,job_count");
		bwAggregatedWP.newLine();
		Iterator<Node> nodeIterator = nearestNode.values().iterator();
		while(nodeIterator.hasNext()){
			Node node = nodeIterator.next();
			long count = 0L; 
			if(aggregatedWorkplaces.get( node.getId() ) != null){
				count = aggregatedWorkplaces.get( node.getId() ).counter;
			}
			bwAggregatedWP.write(node.getId() + "," +
								 node.getCoord().getX() + "," +
								 node.getCoord().getY() + "," +
								 count);
			bwAggregatedWP.newLine();
		}
		bwAggregatedWP.flush();
		bwAggregatedWP.close();
		
		log.info("... done!");
	}

	/**
	 * This runs through the jobClusterArray and determines and assigns
	 * the nearest Node for each (aggregated) job.
	 * Expensive queries like "getNearestNode" are performed only 
	 * once in this pre-processing in order to save computing time
	 * @param network
	 */
	private void addNearestNodeToJobClusterArray(NetworkImpl network) {
		
		log.info("Determining nearest Nodes ...");
		for(int i = 0; i < this.jobClusterArray.length; i++){
			
			assert ( jobClusterArray[ i ].getCoordinate() != null );
			Node nearestNode = network.getNearestNode( jobClusterArray[ i ].getCoordinate() );
			assert ( nearestNode != null );
			// add nearest node to job object
			jobClusterArray[ i ].addNearestNode( nearestNode );
		}
		log.info("... done!");
	}

	/**
	 * @param startZone
	 * @param accessibilityTravelTimes
	 * @param accessibilityTravelCosts
	 * @param accessibilityTravelDistance
	 */
	private void setAccessibilityValue2StartZone(Zone<ZoneAccessibilityObject> startZone,
			double accessibilityTravelTimes, double accessibilityTravelCosts,
			double accessibilityTravelDistance) {
		
		double tt = Math.log( accessibilityTravelTimes );
		double tc = Math.log( accessibilityTravelCosts );
		double td =  Math.log( accessibilityTravelDistance);
		
		startZone.getAttribute().setTravelTimeAccessibility( tt < 0.0 ? 0.0 : tt );
		startZone.getAttribute().setTravelCostAccessibility( tc < 0.0 ? 0.0 : tc );
		startZone.getAttribute().setTravelDistanceAccessibility(td < 0.0 ? 0.0 : td );
	}

	/**
	 * Sets the accessibility values for each spatial grid (travel times, travel costs and travel distance)
	 * 
	 * @param startZone
	 */
	private void setAccessiblityValue2SpatialGrid(Zone<ZoneAccessibilityObject> startZone) {
		
		travelTimeAccessibilityGrid.setValue(startZone.getAttribute().getTravelTimeAccessibility() , startZone.getGeometry().getCentroid());
		travelCostAccessibilityGrid.setValue(startZone.getAttribute().getTravelCostAccessibility() , startZone.getGeometry().getCentroid());
		travelDistanceAccessibilityGrid.setValue(startZone.getAttribute().getTravelDistanceAccessibility() , startZone.getGeometry().getCentroid());
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
											startZone.getAttribute().getTravelTimeAccessibility() + "," +
											startZone.getAttribute().getTravelCostAccessibility() + "," +
											startZone.getAttribute().getTravelDistanceAccessibility() );
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
											Constants.ERSA_TRAVEL_TIME_ACCESSIBILITY + "," +
											Constants.ERSA_TRAVEL_COST_ACCESSIBILITY + "," + 
											Constants.ERSA_TRAVEL_DISTANCE_ACCESSIBILITY);
		accessibilityIndicatorWriter.newLine();
		return accessibilityIndicatorWriter;
	}	
	
	// getter methods
	
	ZoneLayer<ZoneAccessibilityObject> getStartZones(){
		return startZones;
	}
	JobClusterObject[] getJobObjectMap(){
		return jobClusterArray;
	}
	SpatialGrid<Double> getTravelTimeAccessibilityGrid(){
		return travelTimeAccessibilityGrid;
	}
	SpatialGrid<Double> getTravelCostAccessibilityGrid(){
		return travelCostAccessibilityGrid;
	}
	SpatialGrid<Double> getTravelDistanceAccessibilityGrid(){
		return travelDistanceAccessibilityGrid;
	}
	
	
	
	/**
	 * cost calculator for travel distances
	 * @author thomas
	 *
	 */
	static class TravelDistanceCostCalculator implements TravelCost{
		private static final Logger log = Logger.getLogger(TravelDistanceCostCalculator.class);
		
		@Override
		public double getLinkGeneralizedTravelCost(final Link link, final double time) {
			if(link != null)
				return link.getLength();
			log.warn("Link is null. Reurned 0 as link length.");
			return 0.;
		}
	}
	
	/**
	 * cost calculator for travel times
	 * @author thomas
	 *
	 */
	static class TravelTimeCostCalculator implements TravelCost{
//		private static final Logger log = Logger.getLogger(TravelTimeCostCalculator.class);
		
		protected final TravelTime timeCalculator;
		
		public TravelTimeCostCalculator(final TravelTime timeCalculator) {
			this.timeCalculator = timeCalculator;
		}
		
		@Override
		public double getLinkGeneralizedTravelCost(final Link link, final double time) {
			return this.timeCalculator.getLinkTravelTime(link, time);
		}
	}
	
	// tnicolai: these methods are not needed anymore 
//	/**
//	 * @param st
//	 * @param toNode
//	 */
//	@Deprecated
//	private double getZone2WorkplaceDistance(LeastCostPathTree st, Node toNode) {
//		HashMap<Id, NodeData> tree = st.getTree();
//		List<Node> nodeList = new ArrayList<Node>();
//		
//		// set destination node ...
//		// ... from there we get the route to the origin node by the following iteration
//		Node tmpNode = toNode;
//		while(true){
//			
//			nodeList.add( tmpNode );
//			
//			NodeData nodeData = tree.get(tmpNode.getId());
//			assert(nodeData != null);
//			tmpNode = nodeData.getPrevNode();
//			
//			if(tmpNode == null)
//				break;
//		}
//		
//		// now we have list with all nodes on the route between "fromNode" and "toNode"
//		// out of that we create a list with all the links (which connect the nodes)
//		List<Link> linkList = RouteUtils.getLinksFromNodes(nodeList);
//		
//		// we are now able to compute the distance
//		// this calculation includes the distance traveled on the start- and end-link of the route
//		// if you don't want that, you can exclude the first and the last link in "linkList"
//		
//		double distance = 0.;
//		for(Link link: linkList)
//			distance += link.getLength();
//		
//		return distance;
//	}
//	
//	/**
//	 * Calculates the travel distance (in meters) between an origin and destination coordinate on the traffic network.
//	 * 
//	 * @param originCoordinate
//	 * @param destinationCoordinate
//	 * @param network
//	 * @param controler
//	 * @return distance in meter between a given origin and destination coordinate
//	 */
//	@Deprecated
//	private double getZone2ZoneDistance(Coord originCoordinate, Coord destinationCoordinate, NetworkImpl network, Controler controler, Scenario sc, double depatureTime){
//		
//		double distance = 0.;
//		Link fromLink = network.getNearestLink(originCoordinate);
//		Link toLink = network.getNearestLink(destinationCoordinate);
//		
//		Node fromNode = fromLink.getToNode();	// start at the end of the "current" link
//		Node toNode = toLink.getFromNode(); 	// the target is the start of the link
//		
//		PlansCalcRoute plancalcrouter = new PlansCalcRoute(sc.getConfig().plansCalcRoute(), sc.getNetwork(), 
//				controler.getTravelCostCalculatorFactory().createTravelCostCalculator(controler.getTravelTimeCalculator(),
//						controler.getConfig().planCalcScore()), controler.getTravelTimeCalculator(), new DijkstraFactory(), 
//						((PopulationFactoryImpl) controler.getPopulation().getFactory()).getModeRouteFactory());
//		
//		LeastCostPathCalculator lcpc = plancalcrouter.getLeastCostPathCalculator();
//		Path path = lcpc.calcLeastCostPath(fromNode, toNode, depatureTime);
//		
//		// tnicolai: this part is not needed anymore but may helps to understand how the routing in MATSim works ...
////		NetworkFactoryImpl routeFactory = new NetworkFactoryImpl(network);
////		NetworkRoute routeTest = (NetworkRoute) routeFactory.createRoute(TransportMode.car, fromLink.getId(), toLink.getId());
////		
////		routeTest.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
////		routeTest.setTravelTime((int) path.travelTime);
////		routeTest.setTravelCost(path.travelCost);
////		routeTest.setDistance(RouteUtils.calcDistance(routeTest, network)); 
//		
//		List<Id> linkIdList = new ArrayList<Id>();
//		Iterator<Link> linkIterator = path.links.iterator();
//		while(linkIterator.hasNext()) // create link id list
//			linkIdList.add(linkIterator.next().getId());
//		
//		if(linkIdList.size() >= 1){
//			NetworkRoute routeNRT = RouteUtils.createNetworkRoute(linkIdList, network);
//			distance = RouteUtils.calcDistance(routeNRT, network);
//		}
////		else // tnicolai: for debugging
////			log.warn("LinkIList has no element!!! FromLink: " + fromLink.getId() + " ToLink:" + toLink.getId());
//		return distance;
//	}
//	
//	/**
//	 * Calculates the travel distance (in meters) between an origin and destination zone on the traffic network.
//	 * 
//	 * @return distance between 2 zones in meter
//	 */
//	@Deprecated
//	private double getZone2ZoneDistanceOld(Coord originCoordinate, Coord destinationCoordinate, NetworkImpl network, Controler controler){
//		
//		double distance = 0.0; // tnicolai: should we take another default value???
//		
//		PersonImpl dummyPerson = new PersonImpl( new IdImpl(1L) );
//		PlanImpl plan = dummyPerson.createAndAddPlan(true);
//		CommonMATSimUtilities.makeHomePlan(plan, originCoordinate, new ActivityFacilitiesImpl("origin_centroid").createFacility(new IdImpl(2L), originCoordinate));
//		CommonMATSimUtilities.completePlanToHwh(plan, destinationCoordinate, new ActivityFacilitiesImpl("destination_centroid").createFacility( new IdImpl(3L), destinationCoordinate));
//		
//		PersonPrepareForSim pps = new PersonPrepareForSim( controler.createRoutingAlgorithm() , network);
//		pps.run(dummyPerson);
//		
//		if( plan.getPlanElements().size() >= 2 ){
//			
//			// get first leg. this contains the route from "fromZone" to "toZone"
//			PlanElement pe = plan.getPlanElements().get(1);
//			if (pe instanceof LegImpl) {
//				LegImpl l = (LegImpl) pe;
//				
//				LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) l.getRoute();
//
//				if(route.getLinkIds().size()  > 0){
//					NetworkRoute nr = RouteUtils.createNetworkRoute(route.getLinkIds(), network);
//					distance = RouteUtils.calcDistance(nr, network);
//				}
//			}
//		}
//		return distance;
//	}
	
}

