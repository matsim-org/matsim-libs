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
package playground.tnicolai.urbansim.ersa;

import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.contrib.sna.gis.Zone;
import org.matsim.contrib.sna.gis.ZoneLayer;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.events.ShutdownEvent;
import org.matsim.core.controler.listener.ShutdownListener;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.DijkstraFactory;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.population.algorithms.PersonPrepareForSim;

import playground.johannes.socialnetworks.gis.SpatialGrid;
import playground.tnicolai.urbansim.constants.Constants;
import playground.tnicolai.urbansim.utils.CommonMATSimUtilities;
import playground.tnicolai.urbansim.utils.helperObjects.JobsObject;
import playground.tnicolai.urbansim.utils.helperObjects.ZoneObject;
import playground.toronto.ttimematrix.SpanningTree;

import com.vividsolutions.jts.geom.Point;

/**
 * @author thomas
 *
 */
public class ERSAControlerListener implements ShutdownListener{
	
	
	private static final Logger log = Logger.getLogger(ERSAControlerListener.class);
	
	private Map<Id, JobsObject> jobObjectMap;
	private ZoneLayer<ZoneObject> startZones;
	
	private SpatialGrid<Double> travelTimeAccessibilityGrid;
	private SpatialGrid<Double> travelCostAccessibilityGrid;
	private SpatialGrid<Double> travelDistanceAccessibilityGrid;
	
	/**
	 * constructor
	 * @param jobObjectMap
	 */
	public ERSAControlerListener(ZoneLayer<ZoneObject> startZones, Map<Id, JobsObject> jobObjectMap, 
			SpatialGrid<Double> travelTimeAccessibilityGrid, SpatialGrid<Double> travelCostAccessibilityGrid, 
			SpatialGrid<Double> travelDistanceAccessibilityGrid){
		
		this.jobObjectMap 	= jobObjectMap;
		this.startZones		= startZones;	
		this.travelTimeAccessibilityGrid = travelTimeAccessibilityGrid;
		this.travelCostAccessibilityGrid = travelCostAccessibilityGrid;
		this.travelDistanceAccessibilityGrid = travelDistanceAccessibilityGrid;
	}
	
	/**
	 * calculating accessibility indicators
	 */
	public void notifyShutdown(ShutdownEvent event){
		log.info("Entering notifyShutdown ..." ) ;
		
		// get the controller and scenario
		Controler controler = event.getControler();
		Scenario sc = controler.getScenario();
		// init spannig tree in order to calculate travel times and travel costs
		TravelTime ttc = controler.getTravelTimeCalculator();
		SpanningTree st = new SpanningTree(ttc,new TravelTimeDistanceCostCalculator(ttc, controler.getConfig().planCalcScore()));
		
		NetworkImpl network = controler.getNetwork() ;
		double depatureTime = 8.*3600 ;
		st.setDepartureTime(depatureTime);
		
		try{
			BufferedWriter accessibilityIndicatorWriter = initCSVWriter( sc );
			
			log.info("Computing and writing accessibility measures ..." );
			Iterator<Zone<ZoneObject>> startZoneIterator = startZones.getZones().iterator();
			log.info(startZones.getZones().size() + " measurement points are now processed ...");
	
			// init progress bar
			System.out.println("|--------------------------------------------------------------------------------------------------|") ;
			long cnt = 0; 
			long percentDone = 0;
			
			// iterates through all starting points (fromZone) and calculates their workplace accessibility
			while( startZoneIterator.hasNext() ){
				
				Zone<ZoneObject> startZone = startZoneIterator.next();
				// get coordinate from origin (start point)
				Coord coordFromZone = getStartingPointCoordinate(startZone, st, network);
				
				double accessibilityTravelTimes = 0.;
				double accessibilityTravelCosts = 0.;
				double accessibilityTravelDistance = 0.;

				// go through all jobs and calculate workplace accessibility
				for(JobsObject job :jobObjectMap.values()){
					
					assert( job.getCoord() != null );
					Node toNode = network.getNearestNode( job.getCoord() );
					assert( toNode != null );
					double arrivalTime = st.getTree().get( toNode.getId() ).getTime();
					
					// travel times in minutes
					double travelTime = (arrivalTime - depatureTime) / 60.;
					// travel costs in utils
					double travelCosts = st.getTree().get( toNode.getId() ).getCost();
					// travel distance by car in meter
					double travelDistance = getZone2ZoneDistance(coordFromZone, job.getCoord(), network, controler, sc, depatureTime);
					
					
					double beta_per_hr = sc.getConfig().planCalcScore().getTraveling_utils_hr() - sc.getConfig().planCalcScore().getPerforming_utils_hr();
					double beta = beta_per_hr / 60.; // get utility per minute
					
					// sum travel times
					accessibilityTravelTimes += Math.exp( beta * travelTime );
					// sum travel costs
					accessibilityTravelCosts += Math.exp( beta * travelCosts ); // tnicolai: find another beta for travel costs
					// sum travel distances
					accessibilityTravelDistance += Math.exp( beta * travelDistance ); // tnicolai: find another beta for travel distance
				}
				
				setAccessibilityValue2StartZone(startZone,
						accessibilityTravelTimes, accessibilityTravelCosts,
						accessibilityTravelDistance);
				
				// sets the accessibility values for each spatial grid (travel times, travel costs and travel distance)
				setAccessiblityValue2SpatialGrid(startZone);
				
				// dumping results into csv file
				dumpCSVData(accessibilityIndicatorWriter, startZone, coordFromZone);
				
				cnt++;
				// progress bar
				while ( (int) (100.*cnt/startZones.getZones().size()) > percentDone ) {
					percentDone++; System.out.print('|');
				}
				
			}
			System.out.println("");
			// finish and close writing
			closeCSVFile(accessibilityIndicatorWriter);
		
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	/**
	 * @param startZone
	 * @param accessibilityTravelTimes
	 * @param accessibilityTravelCosts
	 * @param accessibilityTravelDistance
	 */
	private void setAccessibilityValue2StartZone(Zone<ZoneObject> startZone,
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
	private void setAccessiblityValue2SpatialGrid(Zone<ZoneObject> startZone) {
		
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
			Zone<ZoneObject> startZone, Coord coordFromZone) throws IOException {
		
		// dumping results into output file
		accessibilityIndicatorWriter.write( startZone.getAttribute().getZoneID() + "," +
											coordFromZone.getX() + "," +
											coordFromZone.getY() + "," +
											startZone.getAttribute().getTravelTimeAccessibility() + "," +
											startZone.getAttribute().getTravelCostAccessibility() + "," +
											startZone.getAttribute().getTravelDistanceAccessibility() + ",");
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
	}

	/**
	 * @param sc
	 * @return
	 * @throws FileNotFoundException
	 * @throws IOException
	 */
	private BufferedWriter initCSVWriter(Scenario sc)
			throws FileNotFoundException, IOException {
		String filename = Constants.OPUS_HOME + sc.getConfig().getParam(Constants.MATSIM_4_URBANSIM, Constants.TEMP_DIRECTORY) + "accessibility_indicators_ersa.csv";
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

	/**
	 * returns the coordinate of an origin zone
	 * 
	 * @param st <code>SpanningTree</code>
	 * @param network <code>NetworkImpl</code>
	 * @return coordinate of a given zone
	 */
	private Coord getStartingPointCoordinate(Zone<ZoneObject> startZone, SpanningTree st, NetworkImpl network) {
			
		Point point = startZone.getGeometry().getCentroid();
		Coord coordFromZone = new CoordImpl( point.getX(), point.getY());
		assert( coordFromZone!=null );
		Node fromNode = network.getNearestNode(coordFromZone);
		assert( fromNode != null );
		st.setOrigin(fromNode);
		st.run(network);
		
		return coordFromZone;
	}
	
	/**
	 * Calculates the travel distance (in meters) between an origin and destination coordinate on the traffic network.
	 * 
	 * @param originCoordinate
	 * @param destinationCoordinate
	 * @param network
	 * @param controler
	 * @return distance in meter between a given origin and destination coordinate
	 */
	private double getZone2ZoneDistance(Coord originCoordinate, Coord destinationCoordinate, NetworkImpl network, Controler controler, Scenario sc, double depatureTime){

		double distance = 0.;
		
		Link fromLink = network.getNearestLink(originCoordinate);
		Link toLink = network.getNearestLink(destinationCoordinate);
		
		Node fromNode = fromLink.getToNode();	// start at the end of the "current" link
		Node toNode = toLink.getFromNode(); 	// the target is the start of the link
		
		PlansCalcRoute plancalcrouter = new PlansCalcRoute(sc.getConfig().plansCalcRoute(), sc.getNetwork(), 
				controler.getTravelCostCalculatorFactory().createTravelCostCalculator(controler.getTravelTimeCalculator(),
						controler.getConfig().planCalcScore()), controler.getTravelTimeCalculator(), new DijkstraFactory());

		LeastCostPathCalculator lcpc = plancalcrouter.getLeastCostPathCalculator();
		Path path = lcpc.calcLeastCostPath(fromNode, toNode, depatureTime);
		
		// tnicolai: this part is not needed anymore but may helps to understand how the routing in MATSim works ...
//		NetworkFactoryImpl routeFactory = new NetworkFactoryImpl(network);
//		NetworkRoute routeTest = (NetworkRoute) routeFactory.createRoute(TransportMode.car, fromLink.getId(), toLink.getId());
//		
//		routeTest.setLinkIds(fromLink.getId(), NetworkUtils.getLinkIds(path.links), toLink.getId());
//		routeTest.setTravelTime((int) path.travelTime);
//		routeTest.setTravelCost(path.travelCost);
//		routeTest.setDistance(RouteUtils.calcDistance(routeTest, network)); 
		
		List<Id> linkIdList = new ArrayList<Id>();
		Iterator<Link> linkIterator = path.links.iterator();
		while(linkIterator.hasNext()) // create link id list
			linkIdList.add(linkIterator.next().getId());
		
		if(linkIdList.size() >= 1){
			NetworkRoute routeNRT = RouteUtils.createNetworkRoute(linkIdList, network);
			distance = RouteUtils.calcDistance(routeNRT, network);
		}
//		else // tnicolai TODO : Why are there no links? ???
//			log.warn("LinkIList has no element!!! FromLink: " + fromLink.getId() + " ToLink:" + toLink.getId());

		return distance;
	}
	
	/**
	 * Calculates the travel distance (in meters) between an origin and destination zone on the traffic network.
	 * 
	 * @return distance between 2 zones in meter
	 */
	@Deprecated
	private double getZone2ZoneDistanceOld(Coord originCoordinate, Coord destinationCoordinate, NetworkImpl network, Controler controler){
		
		double distance = 0.0; // tnicolai: should we take another default value???
		
		PersonImpl dummyPerson = new PersonImpl( new IdImpl(1L) );
		PlanImpl plan = dummyPerson.createAndAddPlan(true);
		CommonMATSimUtilities.makeHomePlan(plan, originCoordinate, new ActivityFacilitiesImpl("origin_centroid").createFacility(new IdImpl(2L), originCoordinate));
		CommonMATSimUtilities.completePlanToHwh(plan, destinationCoordinate, new ActivityFacilitiesImpl("destination_centroid").createFacility( new IdImpl(3L), destinationCoordinate));
		
		PersonPrepareForSim pps = new PersonPrepareForSim( controler.createRoutingAlgorithm() , network);
		pps.run(dummyPerson);
		
		if( plan.getPlanElements().size() >= 2 ){
			
			// get first leg. this contains the route from "fromZone" to "toZone"
			PlanElement pe = plan.getPlanElements().get(1);
			if (pe instanceof LegImpl) {
				LegImpl l = (LegImpl) pe;
				
				LinkNetworkRouteImpl route = (LinkNetworkRouteImpl) l.getRoute();

				if(route.getLinkIds().size()  > 0){
					NetworkRoute nr = RouteUtils.createNetworkRoute(route.getLinkIds(), network);
					distance = RouteUtils.calcDistance(nr, network);
				}
			}
		}
		return distance;
	}
	
	
	// getter methods
	
	public ZoneLayer<ZoneObject> getStartZones(){
		return startZones;
	}
	public Map<Id, JobsObject> getJobObjectMap(){
		return jobObjectMap;
	}
	public SpatialGrid<Double> getTravelTimeAccessibilityGrid(){
		return travelTimeAccessibilityGrid;
	}
	public SpatialGrid<Double> getTravelCostAccessibilityGrid(){
		return travelCostAccessibilityGrid;
	}
	public SpatialGrid<Double> getTravelDistanceAccessibilityGrid(){
		return travelDistanceAccessibilityGrid;
	}
}

