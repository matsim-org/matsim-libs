/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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
package org.matsim.contrib.matsim4opus.matsim4urbansim.router;


import java.io.BufferedReader;
import java.io.IOException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Network;
import org.matsim.contrib.matsim4opus.config.MATSim4UrbanSimControlerConfigModuleV3;
import org.matsim.contrib.matsim4opus.utils.io.HeaderParser;
import org.matsim.contrib.matsim4opus.utils.io.TempDirectoryUtil;
import org.matsim.contrib.matsim4opus.utils.network.NetworkBoundaryBox;
import org.matsim.contrib.matsim4opus.utils.network.NetworkUtil;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.matrices.Entry;
import org.matsim.matrices.Matrix;

/**
 * @author thomas
 *
 */
public class PtMatrix {
	
	private static final Logger log = Logger.getLogger(PtMatrix.class);
	
	// od-trip matrix
	private Matrix originDestinationTravelTimeMatrix;
	private Matrix originDestinationTravelDistanceMatrix;
	// quad tree 
	private QuadTree<PtStop> qTree;
	// walk speed
	private double meterPerSecWalkSpeed;
	
	public PtMatrix(Network network, double meterPerSecWalkSpeed, double meterPerSecPtSpeed, double beelineDistanceFactor, MATSim4UrbanSimControlerConfigModuleV3 m4uccm){

		// get the locations for ptStops, travel times and distances from controler
		String ptStopInputFile = m4uccm.getPtStopsInputFile();
		String ptTravelTimeInputFile = m4uccm.getPtTravelTimesInputFile();
		String ptTravelDistanceInputFile= m4uccm.getPtTravelDistancesInputFile();
		
		this.meterPerSecWalkSpeed = meterPerSecWalkSpeed;
		
		// LeastCostPathCalculator
		// DijkstraFactory dijkstra = new DijkstraFactory();
		// lcpc = dijkstra.createPathCalculator(network, new TravelWalkTimeCostCalculator(meterPerSecWalkSpeed), ttc);
		
		// init quad tree, it contains all pt stops (coordinate, id)
		qTree = initQuadTree( network, ptStopInputFile );

		// init od matrix containing the travel times
		originDestinationTravelTimeMatrix 	 = new Matrix("PtStopTravelTimeMatrix", "Stop to stop origin destination travel time matrix");
		originDestinationTravelDistanceMatrix= new Matrix("PtStopTravelDistanceMatrix", "Stop to stop origin destination travel distance matrix");
		
		if(ptTravelTimeInputFile == null || ptTravelDistanceInputFile == null){
			// if travel times and distances are not provided by external files ...
			initODMatrixBasedOnPtStopsOnly( beelineDistanceFactor, meterPerSecPtSpeed, qTree, originDestinationTravelTimeMatrix, originDestinationTravelDistanceMatrix );
		}
		else{
			initODMatrixFromFile( qTree, originDestinationTravelTimeMatrix, originDestinationTravelDistanceMatrix, ptTravelTimeInputFile, ptTravelDistanceInputFile);
		}
	}
	
	/**
	 * Building QuadTree with pt stops
	 * 
	 * @param controler
	 * @return
	 */
	private QuadTree<PtStop> initQuadTree(Network network, String ptStopInputile){
		
		log.info("Building QuadTree for pt stops.");
		
		NetworkBoundaryBox nbb = new NetworkBoundaryBox();
		nbb.setDefaultBoundaryBox(network);
		
		QuadTree<PtStop> qTree = new QuadTree<PtStop>(nbb.getXMin(), nbb.getYMin(), nbb.getXMax(), nbb.getYMax());
		
		try {
			BufferedReader bwPtStops = IOUtils.getBufferedReader(ptStopInputile);
			
			String separator = ",";
			
			// read header
			String line = bwPtStops.readLine();
			// get and initialize the column number of each header element
			Map<String,Integer> idxFromKey = HeaderParser.createIdxFromKey( line, separator);
			final int idIDX 	= idxFromKey.get("id");
			final int xCoordIDX = idxFromKey.get("x");
			final int yCoordIDX = idxFromKey.get("y");
			
			String parts[];
			
			// read input file line by line
			while( (line = bwPtStops.readLine()) != null ){
				
				parts = line.split(separator);
				
				if(parts.length < idIDX || parts.length < xCoordIDX || parts.length < yCoordIDX){
					log.warn("Could not parse line: " + line);
					continue;
				}
				
				// create id for pt stop
				Long id = Long.parseLong( parts[idIDX] );
				Id stopId = new IdImpl(id);
				// create pt stop coordinate
				Coord ptStopCoord = new CoordImpl(parts[xCoordIDX], parts[yCoordIDX]);
				
				boolean isInBoundary = qTree.getMaxEasting() >= ptStopCoord.getX() && 
									   qTree.getMinEasting() <= ptStopCoord.getX() && 
									   qTree.getMaxNorthing() >= ptStopCoord.getY() && 
									   qTree.getMinNorthing() <= ptStopCoord.getY();
				if(!isInBoundary){
					log.warn("Pt stop " + id + " lies outside the network boundary and will be skipped!");
					continue;
				}
				
				PtStop ptStop = new PtStop(stopId, ptStopCoord);
				
				qTree.put(ptStopCoord.getX(), ptStopCoord.getY(), ptStop);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		log.info("QuadTree for pt stops created.");
		return qTree;
	}
	
	/**
	 * constructs tow origin destination matrices for pt stops, one for travel times and one for travel distances.
	 * travel times are calculated based on travel distances on the network divided by the typical pt speed (normally 25kmh).
	 * the travel distance is determined by the shortest route on the network to get from one stop to another stop.
	 * 
	 * @param controler
	 * @param qTree
	 * @return Matrix
	 */
	private void initODMatrixBasedOnPtStopsOnly(double beelineDistanceFactor, double meterPerSecPtSpeed, QuadTree<PtStop> qTree, Matrix travelTimeOD, Matrix travelDistanceOD){
		
		log.info("Building OD matrix with pt stop to pt stop travel times.");
		
		// int euclidianDistanceCounter= 0;
		// int networkDistanceCounter  = 0;
		
		// convert qtree into array to access ptStops as origins and destinations
		PtStop ptStopIds[] = qTree.values().toArray( new PtStop[0] );
		
		// TravelTimeCalculatorFactory travelTimeCalculatorFactory = new TravelTimeCalculatorFactoryImpl();
		// TravelTimeCalculator travelTimeCalculator = travelTimeCalculatorFactory.createTravelTimeCalculator(network, controler.getScenario().getConfig().travelTimeCalculator());
		// LeastCostPathTree lcpt = new LeastCostPathTree(travelTimeCalculator, new TravelDistanceCalculator());
		
		for(int origin = 0; origin < ptStopIds.length; origin++){
			
			PtStop originStop = ptStopIds[origin];
			Coord originCoord = originStop.getCoord();
			// the least cost path tree calculates travel distances between 2 stops on the network
			// Node originNode = network.getNearestNode(originCoord);
			// lcpt.calculate(network, originNode, 8. * 3600.);
			
			for(int destination = 0; destination < ptStopIds.length; destination++){
				
				// get destination stop
				PtStop destinationStop = ptStopIds[destination];
				// get coordinates
				Coord destinationCoord = destinationStop.getCoord();
				// get nearest network node
				// Node destinationNode = network.getNearestNode(destinationCoord);
				// get distance in meter
				double distance = NetworkUtil.getEuclidianDistance(originCoord, destinationCoord);
				
				// NodeData nd = lcpt.getTree().get( destinationNode.getId() );
				// double distance = nd.getCost();
				// if(distance == 0){
				// 	euclidianDistanceCounter++;
				// 	distance = NetworkUtil.getEuclidianDistance(originCoord, destinationCoord);
				// }
				// else
				// 	networkDistanceCounter++;
				
				// calculate time in seconds (distance * beelineDistanceFactor * 25kmh)
				double travelTime = (distance * beelineDistanceFactor) / meterPerSecPtSpeed;
				
				// create entry - travel times in seconds
				travelTimeOD.createEntry(originStop.getId(), destinationStop.getId(), travelTime);			
				// create entry - travel distances in meter
				travelDistanceOD.createEntry(originStop.getId(), destinationStop.getId(), distance);	
			}
		}
		log.info("Done creating OD matrices with pt stop to pt stop travel times and distances.");
		// log.info(euclidianDistanceCounter + " travel times are on Euclidian distances.");
		// log.info(networkDistanceCounter + " travel times based on newtork distances.");
	}
	
	/**
	 * constructs tow origin destination matrices for pt stops, one for travel times and one for travel distances.
	 * Travel times, distances and pt connections are given via input files.
	 * A consistency check makes sure that only pt connections that are included in the QuadTree can be be added to the matrices.
	 * The QuadTree contains the coordinates for each pt stop.
	 * 
	 * @param qTree
	 * @param travelTimeOD
	 * @param travelDistanceOD
	 * @param ptTravelTimeInputFile
	 * @param ptTravelDistanceInputFile
	 */
	private void initODMatrixFromFile(QuadTree<PtStop> qTree, Matrix travelTimeOD, Matrix travelDistanceOD, String ptTravelTimeInputFile, String ptTravelDistanceInputFile){
		
		// convert qtree into hasMap to query ptStops as origins and destinations
		HashMap<Id, PtStop> ptStopHashMap = convertQuadTree2HashMap(qTree);
		
		BufferedReader brTravelTimes = IOUtils.getBufferedReader(ptTravelTimeInputFile);
		BufferedReader brTravelDistances = IOUtils.getBufferedReader(ptTravelDistanceInputFile);
		
		log.warn("pt connections that contain one or two pt stops that are not included in QuadTree will be skipped!");
		
		// run through travel times and distances files and create/fill od matrices
		try{
			log.info("Creating travel time OD matrix from VISUM pt stop 2 pt stop travel times file: " + ptTravelTimeInputFile);
			fillODMatrix(travelTimeOD, ptStopHashMap, brTravelTimes, true);
			log.info("Done creating travel time OD matrix ...");
			
			log.info("Creating travel distance OD matrix from VISUM pt stop 2 pt stop travel distance file: " + ptTravelDistanceInputFile);
			fillODMatrix(travelDistanceOD, ptStopHashMap, brTravelDistances, false);
			log.info("Done creating travel distance OD matrix ...");
		} catch(Exception e){
			e.printStackTrace();
		}
		log.info("Done creating OD matrices with pt stop to pt stop travel times and distances.");
	}

	/**
	 * @param odMatrix
	 * @param originPtStopIDX
	 * @param destinationPtStopIDX
	 * @param valueIDX
	 * @param EMPTY_ENTRY
	 * @param SEPARATOR
	 * @param ptStopHashMap
	 * @param br
	 * @throws IOException
	 */
	private void fillODMatrix(Matrix odMatrix, HashMap<Id, PtStop> ptStopHashMap,
			BufferedReader br, boolean isTravelTimes) throws IOException {
		
		int originPtStopIDX 		= 0;		// column index for origin pt stop id
		int destinationPtStopIDX 	= 1;		// column index for destination pt stop id
		int valueIDX 				= 2;		// column index for travel time or distance value
		
		final double EMPTY_ENTRY 	= 999999.;	// indicates that a value is not set (VISUM specific)
		final String SEPARATOR 		= ";";		// 
		
		String line 				= null;		// line from input file
		String parts[] 				= null;		// values accessible via array
		
		while ( (line = br.readLine()) != null ) {
			
			line = line.trim().replaceAll("\\s+", SEPARATOR);
			parts = line.split(SEPARATOR);
			
			if(parts.length != 3){
				log.warn("Could not parse line: " + line);
				continue;
			}
			
			try{
				// trying to convert items into integers
				Long originPtStopAsLong 	= Long.parseLong(parts[originPtStopIDX]);
				Long destinationPtStopAsLong= Long.parseLong(parts[destinationPtStopIDX]);
				double value 				= Double.parseDouble(parts[valueIDX]);
				if(isTravelTimes)
					value = value * 60.; 	// convert value from minutes into seconds
				
				if(value == EMPTY_ENTRY){
					log.warn("No parameter set: " + line);
					continue;
				}
				
				// create Id's
				Id originPtStopID 			= new IdImpl(originPtStopAsLong);
				Id destinationPtStopID 		= new IdImpl(destinationPtStopAsLong);
				
				// check if a pt stop with the given id exists
				if( ptStopHashMap.containsKey(originPtStopID) && 
					ptStopHashMap.containsKey(destinationPtStopID)){
					
					// add to od matrix
					odMatrix.createEntry(originPtStopID, destinationPtStopID, value);
				}
				else{
					if(! ptStopHashMap.containsKey(originPtStopID))
						log.warn("Could not find an item in QuadTree (i.e. pt station has no coordinates) with pt stop id:" + originPtStopID);
					if(! ptStopHashMap.containsKey(destinationPtStopID))
						log.warn("Could not find an item in QuadTree (i.e. pt station has no coordinates) with pt stop id:" + destinationPtStopID);
					continue;
				}
			} catch(NumberFormatException nfe){
				log.warn("Could not convert values into integer: " + line);
				continue;
			}
		}
	}
	
	//////////////////////////////////////
	// helper method
	//////////////////////////////////////
	
	private HashMap<Id, PtStop> convertQuadTree2HashMap(QuadTree<PtStop> qTree){
		
		Iterator<PtStop> ptStopIterator = qTree.values().iterator();
		HashMap<Id, PtStop> ptStopHashMap = new HashMap<Id, PtMatrix.PtStop>();
		
		while(ptStopIterator.hasNext()){
			PtStop ptStop = ptStopIterator.next();
			ptStopHashMap.put(ptStop.getId(), ptStop);
		}
		return ptStopHashMap;
	}
	
	//////////////////////////////////////
	// getter methods
	//////////////////////////////////////
	
	public QuadTree<PtStop> getQuadTree(){
		return this.qTree;
	}
	
	protected Matrix getTravelTimeODMatrix(){
		return this.originDestinationTravelTimeMatrix;
	}
	
	protected Matrix getTravelDistanceODMatrix(){
		return this.originDestinationTravelDistanceMatrix;
	}
	
	/**
	 * total travel times (origin location > pt > destination location) in seconds. the travel times are composed as follows:
	 * 
	 * (O)-------(PT)===========(PT)--------(D)
	 * 
	 * O = origin location
	 * D = destination location
	 * PT= next pt station 
	 * 
	 * total travel times = walk travel time from origin O to next pt stop +
	 *                      pt travel time +
	 *                      walk travel time from destination pt stop to destination D
	 * 
	 * @param fromFacilityCoord
	 * @param toFacilityCoord
	 * @return
	 */
	public double getTotalTravelTime(Coord fromFacilityCoord, Coord toFacilityCoord){
		
//		// get nearest network nodes
//		Node fromFacilityNode = network.getNearestNode(fromFacilityCoord);
//		Node toFacilityNode = network.getNearestNode(toFacilityCoord);
//		Node fromPtStopNode = network.getNearestNode(fromPtStop.getCoord());
//		Node toPtStopNode = network.getNearestNode(toPtStop.getCoord());
//		
//		// get the paths between facility and pt stop
//		Path pathFromFacility2FromPtStop = lcpc.calcLeastCostPath(fromFacilityNode, fromPtStopNode, departureTime, null, null);
//		Path pathToPtStop2ToFacility = lcpc.calcLeastCostPath(toPtStopNode, toFacilityNode, departureTime, null, null);
//		
//		// get the travel times
//		double walkDistanceFromFacility2Node = NetworkUtil.getEuclidianDistance(fromFacilityCoord, fromFacilityNode.getCoord());
//		double walkDistanceNode2ToFacility   = NetworkUtil.getEuclidianDistance(toFacilityNode.getCoord(), toFacilityCoord);
//		double walkTimeFromFacility2Node	 = walkDistanceFromFacility2Node / this.meterPerSecWalkSpeed;
//		double walkTimeNode2ToFacility		 = walkDistanceNode2ToFacility / this.meterPerSecWalkSpeed;
//		
//		double walkTimeFromFacility2FromPtStop = pathFromFacility2FromPtStop.travelCost;
//		double walkTimeToPtStop2ToFacility = pathToPtStop2ToFacility.travelCost;
		
		
		// old version
//		double walkTravelTimeFromFacility2FromPtStop = NetworkUtil.getEuclidianDistance(fromFacilityCoord, fromPtStop.getCoord()) / meterPerSecWalkSpeed;
//		double walkTravelTimeToPtStop2ToFacility = NetworkUtil.getEuclidianDistance(toPtStop.getCoord(), toFacilityCoord) / meterPerSecWalkSpeed;
		
		double totalWalkTravelTime = getTotalWalkTravelTime(fromFacilityCoord, toFacilityCoord);
		double ptTravelTime = getPtTravelTime(fromFacilityCoord, toFacilityCoord);
		
		double totalTravelTime = totalWalkTravelTime + ptTravelTime;
		return totalTravelTime;
	}
	
	/**
	 * returns the total walk travel times including
	 * - walk travel time from given coordinate to next pt stop
	 * - walk travel time from destination pt stop to given destination coordinate
	 * 
	 * @param fromFacilityCoord
	 * @param toFacilityCoord
	 * @return
	 */
	public double getTotalWalkTravelTime(Coord fromFacilityCoord, Coord toFacilityCoord){
		
		// get pt stops
		PtStop fromPtStop = this.qTree.get(fromFacilityCoord.getX(), fromFacilityCoord.getY());
		PtStop toPtStop   = this.qTree.get(toFacilityCoord.getX(), toFacilityCoord.getY());
		
		// tnicolai feb'13: This leads to the following behavior:
		// if the origin and destination location are very far away (on the opposite site of the city) from the ptStops 
		// it is likely that the origin and destination ptStop will be the same. This leeds to ptTravelTimes = 0 and walkingTimes = 0.
		// At least the walking times should indicate that it is not feasible to use pt
		//if(fromPtStop == toPtStop)
		//	return 0.;
		
		double walkTravelTimeFromFacility2FromPtStop = NetworkUtil.getEuclidianDistance(fromFacilityCoord, fromPtStop.getCoord()) / meterPerSecWalkSpeed;
		double walkTravelTimeToPtStop2ToFacility = NetworkUtil.getEuclidianDistance(toPtStop.getCoord(), toFacilityCoord) / meterPerSecWalkSpeed;
		
		double totalWalkTravelTime = walkTravelTimeFromFacility2FromPtStop + walkTravelTimeToPtStop2ToFacility;
		return totalWalkTravelTime;
	}
	
	public double getPtTravelTime(Coord fromFacilityCoord, Coord toFacilityCoord){
		
		// get pt stops
		PtStop fromPtStop = this.qTree.get(fromFacilityCoord.getX(), fromFacilityCoord.getY());
		PtStop toPtStop   = this.qTree.get(toFacilityCoord.getX(), toFacilityCoord.getY());
		
		Entry entry = originDestinationTravelTimeMatrix.getEntry(fromPtStop.getId(), toPtStop.getId());
		double ptTravelTime = Double.MAX_VALUE;
		if(entry != null)
			ptTravelTime = entry.getValue();
		else{
			// log.warn("No entry found in od travel times matrix for pt stops: " + fromPtStop.getId() + " " + toPtStop.getId());
			if(fromPtStop == toPtStop)
				ptTravelTime = 0.;
		}
		return ptTravelTime;
	}
	
	/**
	 * total travel distance (origin location > pt > destination location) in seconds. the travel distances are composed as follows:
	 * 
	 * (O)-------(PT)===========(PT)--------(D)
	 * 
	 * O = origin location
	 * D = destination location
	 * PT= next pt station 
	 * 
	 * total travel distance = walk travel distance from origin O to next pt stop +
	 *                         pt travel distance +
	 *                         walk travel distance from destination pt stop to destination D
	 * 
	 * travel distances in meter
	 * @param fromFacilityCoord
	 * @param toFacilityCoord
	 * @return
	 */
	public double getTotalTravelDistance(Coord fromFacilityCoord, Coord toFacilityCoord){
		
		double totalWalkTravelDistance = getTotalWalkTravelDistance(fromFacilityCoord, toFacilityCoord);
		double ptTravelDistance = getPtTravelDistance(fromFacilityCoord, toFacilityCoord); 
			
		double totalTravelDistance = totalWalkTravelDistance + ptTravelDistance;
		return totalTravelDistance;
	}
	
	/**
	 * 
	 * @param fromFacilityCoord
	 * @param toFacilityCoord
	 * @return
	 */
	public double getTotalWalkTravelDistance(Coord fromFacilityCoord, Coord toFacilityCoord){
		
		PtStop fromPtStop = this.qTree.get(fromFacilityCoord.getX(), fromFacilityCoord.getY());
		PtStop toPtStop   = this.qTree.get(toFacilityCoord.getX(), toFacilityCoord.getY());
		
		// tnicolai feb'13: This leads to the following behavior:
		// if the origin and destination location are very far away (on the opposite site of the city) from the ptStops 
		// it is likely that the origin and destination ptStop will be the same. This leeds to ptTravelTimes = 0 and walkingTimes = 0.
		// At least the walking times should indicate that it is not feasible to use pt
		//if(fromPtStop == toPtStop)
		//	return 0.;
		
		double walkTravelDistanceFromFacility2FromPtStop = NetworkUtil.getEuclidianDistance(fromFacilityCoord, fromPtStop.getCoord());
		double walkTravelDistanceToPtStop2ToFacility = NetworkUtil.getEuclidianDistance(toPtStop.getCoord(), toFacilityCoord);
		
		double totalWalkTravelDistance = walkTravelDistanceFromFacility2FromPtStop + walkTravelDistanceToPtStop2ToFacility;
		return totalWalkTravelDistance;
		
	}
	
	/**
	 * 
	 * @param fromFacilityCoord
	 * @param toFacilityCoord
	 * @return
	 */
	public double getPtTravelDistance(Coord fromFacilityCoord, Coord toFacilityCoord){
		
		PtStop fromPtStop = this.qTree.get(fromFacilityCoord.getX(), fromFacilityCoord.getY());
		PtStop toPtStop   = this.qTree.get(toFacilityCoord.getX(), toFacilityCoord.getY());
		
		Entry entry = originDestinationTravelDistanceMatrix.getEntry(fromPtStop.getId(), toPtStop.getId());
		double ptTravelDistance = Double.MAX_VALUE;
		if(entry != null)
			ptTravelDistance = entry.getValue();
		else{
			// log.warn("No entry found in od travel distances matrix for pt stops: " + fromPtStop.getId() + " " + toPtStop.getId());
			if(fromPtStop == toPtStop)
				ptTravelDistance = 0.;
		}
		
		return ptTravelDistance;
	}
	
	//////////////////////////////////////
	// inner class
	//////////////////////////////////////
	
	public class PtStop{
		
		private final Id id;
		private final Coord coord;

		PtStop(Id id, Coord coord) {
			this.id = id;
			this.coord = coord;
		}
		public Id getId() {
			return id;
		}
		public Coord getCoord() {
			return coord;
		}
	}
	
	//////////////////////////////////////
	// main class for testing
	//////////////////////////////////////
	
	/**
	 * for testing
	 * @param args
	 */
	public static void main(String args[]){
		
		log.info("Start testing");
		
		long start = System.currentTimeMillis();
		double defaultWalkSpeed = 1.38888889;
		double defaultPtSpeed 	= 6.94444444;	// 6.94444444m/s corresponds to 25 km/h
		double beelineDistanceFactor = 1.3;
		
		String BrusselNetwork = "/Users/thomas/Development/opus_home/data/brussels_zone/data/matsim/network/belgium_incl_borderArea_hierarchylayer4_clean_simple.xml.gz";
		String ZurichNetwork  = "/Users/thomas/Development/opus_home/data/zurich_parcel/data/data/network/ivtch-osm.xml";
		
		Scenario scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new MatsimNetworkReader(scenario).readFile(ZurichNetwork);
		ScenarioUtils.loadScenario(scenario);
		
		Network network = scenario.getNetwork() ;
		// TravelTime ttc = new TravelTimeCalculator(network,60,30*3600, scenario.getConfig().travelTimeCalculator()).getLinkTravelTimes();
		
		MATSim4UrbanSimControlerConfigModuleV3 m4uccm = new MATSim4UrbanSimControlerConfigModuleV3(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME);
		// m4uccm.setPtStopsInputFile("/Users/thomas/Development/opus_home/data/brussels_zone/data/transit_csvs_from_Dimitris_20121002/underground.csv"); 	// for Brussels
		m4uccm.setPtStopsInputFile("/Users/thomas/Development/opus_home/data/zurich_parcel/data/Matrizen__OeV/Zones_Attributes.csv");					  	// for Zurich
		m4uccm.setPtTravelTimesInputFile("/Users/thomas/Development/opus_home/data/zurich_parcel/data/Matrizen__OeV/OeV_2007_7_8.JRT");						// for Zurich
		m4uccm.setPtTravelDistancesInputFile("/Users/thomas/Development/opus_home/data/zurich_parcel/data/Matrizen__OeV/OeV_2007_7_8.JRD");					// for Zurich

		PtMatrix ptm = new PtMatrix(network, defaultWalkSpeed, defaultPtSpeed, beelineDistanceFactor, m4uccm);
		
		// get QuadTree
		QuadTree<PtStop> qTreeTest = ptm.getQuadTree();
		// convert QuadTree to array
		PtStop ptStopIds[] = qTreeTest.values().toArray( new PtStop[0] );
		
		for(int origin = 0; origin < ptStopIds.length; origin++){
			for(int destination = 0; destination < ptStopIds.length; destination++){
				
				PtStop fromLocation = ptStopIds[origin];
				PtStop toLocation = ptStopIds[destination];
				
				double travelTime = ptm.getTotalTravelTime(fromLocation.getCoord(), toLocation.getCoord());
				double travelDistance= ptm.getTotalTravelDistance(fromLocation.getCoord(), toLocation.getCoord());
				log.info("From: " + fromLocation.getId() + ", To: " + toLocation.getId() + ", TravelTime: " + travelTime + ", Travel Distance: " + travelDistance);
			}
		}
		
		TempDirectoryUtil.cleaningUpCustomTempDirectories();
		
		log.info("Creating pt matrix took " + ((System.currentTimeMillis() - start)/60000) + " minutes. Computation done!");
	}

}
