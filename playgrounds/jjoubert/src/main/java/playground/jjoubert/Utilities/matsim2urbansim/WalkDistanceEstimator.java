/* *********************************************************************** *
 * project: org.matsim.*
 * RunEthekwiniNetworkFilter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities.matsim2urbansim;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.ConfigUtils;


public class WalkDistanceEstimator {
	private final static Logger log = Logger.getLogger(WalkDistanceEstimator.class);
	private String studyArea;
	private Scenario sAll;
	private Scenario sPt;
	public M2UStringbuilder sb;
	private List<MyZone> spList;
	private Map<Id, Double> distanceMap;
	private Map<Id, Double> ptMap;


	/**
	 * Estimates the walk distance to public transport.
	 * @param root
	 * @param studyArea
	 * @param version of the study area's data;
	 * @param percentage of population sampled. 
	 */
	public WalkDistanceEstimator(String root, String studyArea, 
			String version, String percentage) {
		this.studyArea = studyArea;
		this.sb = new M2UStringbuilder(root, studyArea, version, percentage);
	}
	/**
	 * Implements the walk distance estimator. Creates a distance and weight 
	 * table for each subplace. The output is used in R to estimate a function 
	 * to convert walking distance to walking time. Once estimated, the function
	 * is implemented in {@code MyConverter}.
	 * @param args a String-array containing:
	 * <ol>
	 * 	<li> the root folder;
	 * 	<li> study area name. Currently allowed values are:
	 * 		<ul>
	 * 			<li> "eThekwini"
	 * 		</ul> 
	 * 	<li> version (year) of the study area to consider;
	 * 	<li> the population sample size, e.g. "10" if a 10% sample was used;
	 * </ol>
	 */
	public static void main(String[] args) {
		WalkDistanceEstimator wte = null;	
		if(args.length != 4){
			throw new RuntimeException("Incorrect number of arguments passed.");
		} else{
			wte = new WalkDistanceEstimator(args[0], args[1], args[2], args[3]);
		}
		log.info("================================================================================");
		log.info("   Estimating walk distance to public transport for " + args[1]);
		log.info("--------------------------------------------------------------------------------");

		wte.deriveTransitNetworkFromEmme();
		wte.calculateSubplacedistances();
		wte.writeSubplaceDistanceAndWeight();

		log.info("--------------------------------------------------------------------------------");
		log.info("             PROCESS COMPLETE");
		log.info("================================================================================");	
	}
	
	public void deriveTransitNetworkFromEmme(){
		log.info("Filtering " + studyArea + "'s MATSim network to account for public transport.");
		// Read network.
		sAll = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		sPt = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader nr = new MatsimNetworkReader(sAll);
		nr.readFile(sb.getEmmeNetworkFilename());		
		
		/*----------------------------------------------------------------------
		 * Process transit-lines-by-link file. The OD node pair does not always
		 * describe a single LINK, but a PATH. So I have to process an events 
		 * file and get a router going. 
		 * NOTE: I always use the 100th iteration's plans file. If this is NOT
		 *       correct, I'll have to change this and pass the iteration number
		 *       as an argument.
		 *---------------------------------------------------------------------*/
		// Set up router.
		TravelTimeCalculatorFactory ttcf = new TravelTimeCalculatorFactoryImpl();
		TravelTimeCalculator ttc = ttcf.createTravelTimeCalculator(sAll.getNetwork(), sAll.getConfig().travelTimeCalculator());
		TravelCostCalculatorFactory tccf = new TravelCostCalculatorFactoryImpl();
		PersonalizableTravelCost tc = tccf.createTravelCostCalculator(ttc, sAll.getConfig().planCalcScore());
		EventsManagerImpl em = new EventsManagerImpl();
		em.addHandler(ttc);
		new EventsReaderTXTv1(em).readFile(sb.getIterationEventsFile("100"));
		Dijkstra router = new Dijkstra(sAll.getNetwork(),tc,ttc);
		Set<String> ptSet = new TreeSet<String>();
		ptSet.add(TransportMode.pt);
		
		// Process TransitLinesByLink file.
		try {
			BufferedReader br = IOUtils.getBufferedReader(sb.getEmmeTransitLinesByLinkFilename());
			try{
				@SuppressWarnings("unused")
				String header = br.readLine();
				String line = null;
				int allCounter = 0;
				int ptCounter = 0;
				int directCounter = 0;
				String allPt = "";
				while((line = br.readLine()) != null){
					allCounter++;
					String[] link = line.split("\t");
					if(link.length > 8){
						if(link[8].equalsIgnoreCase("0")){
							// It is car... or free for all.
						} else{
							// It has public transport modes.
							allPt += link[8];
							ptCounter++;
							Path p = null;
							Node oNode = sAll.getNetwork().getNodes().get(new IdImpl(link[1]));
							Node dNode = sAll.getNetwork().getNodes().get(new IdImpl(link[2]));
							if(oNode != null && dNode != null){
								p = router.calcLeastCostPath(oNode, dNode, 21600); // at 06:00 in the morning.
								// Set all links in path.
								for(Link ptLink : p.links){
									ptLink.setAllowedModes(ptSet);
								}
		
								Map<Id, ? extends Link> outLinks = sAll.getNetwork().getNodes().get(oNode.getId()).getOutLinks();
								boolean found = false;
								for(Link l : outLinks.values()){
									if(l.getToNode().equals(dNode)){
										found = true;
										directCounter++;
									}
								}
								if(!found){
									log.info("No direct trip from " + oNode.getId().toString() + " to " + dNode.getId().toString());
								}
							}
						}
					}
				}
				log.info("A total of " + allCounter + " node pairs in network");
				log.info("Found " + ptCounter + " node-pairs serviced by transit (" + directCounter + " direct )");
				log.info(allPt);
			} finally{
				br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
			throw new RuntimeException("Must have a file to be able to derive a transit network.");
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Done reading transit-lines-by-link file. Now filter the network.");
		TransportModeNetworkFilter nf = new TransportModeNetworkFilter(sAll.getNetwork());
		nf.filter(sPt.getNetwork(), ptSet);
		NetworkWriter nw = new NetworkWriter(sPt.getNetwork());
		nw.writeFileV1(sb.getEmmePtNetworkFilename());
		log.info("Network filterted and written to " + sb.getEmmePtNetworkFilename());
	}
	
	public void calculateSubplacedistances(){
		// Read subplace shapefile.
		MyZoneReader mzr = new MyZoneReader(sb.getSubPlaceShapefile());
		mzr.readZones(sb.getSubplaceIdField());
		spList = mzr.getZoneList();
		log.info("Done reading sub-place shapefile: " + spList.size() + " entries for " + studyArea);
		
		distanceMap = new TreeMap<Id, Double>();
		ptMap = new TreeMap<Id, Double>();
		
		// First read the public transport sub place table.
		log.info("Reading sub-place table from " + sb.getSubPlaceTable());
		int spCounter = 0;
		int spMultiplier = 1;
		try {
			BufferedReader br = IOUtils.getBufferedReader(sb.getSubPlaceTable());
			try{
				@SuppressWarnings("unused")
				String[] header = br.readLine().split(",");
				int idIndex = 0;
				int ptIndex = 1;
				String line = null;
				while((line = br.readLine()) != null){
					String[] entries = line.split(",");
					if(entries.length == 3){
						ptMap.put(new IdImpl(entries[idIndex]), Double.parseDouble(entries[ptIndex]));
					} else{
						log.warn(" Found a line with " + entries.length + " items; expected 3.");
					}
					// Report progress.
					if(++spCounter == spMultiplier){
						log.info("   sub-places read: " + spCounter);
						spMultiplier *= 2;
					}
				}
				log.info("   sub-places read: " + spCounter + " (Done)");
			} finally {
				br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		
		// Process distances.
		spCounter = 0;
		spMultiplier = 1;
		log.info("Calculating sub-place distances.");
		NetworkImpl ni = (NetworkImpl) sPt.getNetwork();
		for(MyZone sp : spList){
			CoordImpl centroid = new CoordImpl(sp.getCentroid().getX(), sp.getCentroid().getY());
			Node closest = ni.getNearestNode(centroid);
			Double d = centroid.calcDistance(closest.getCoord());
			distanceMap.put(sp.getId(), d);			
			
			// Report progress.
			if(++spCounter == spMultiplier){
				log.info("   sub-places calculated: " + spCounter);
				spMultiplier *= 2;
			}
		}
		log.info("   sub-places calculated: " + spCounter + " (Done)");
	}
	
	public void writeSubplaceDistanceAndWeight(){
		try {
			BufferedWriter bw = IOUtils.getBufferedWriter(sb.getSubPlaceDistanceFilename());
			try{
				bw.write("SP_ID,Dist,Weight");
				bw.newLine();
				for(MyZone mz : spList){
					Double dist = distanceMap.get(mz.getId());
					Double weight = ptMap.get(mz.getId());
					if(dist != null && weight != null){
						bw.write(mz.getId().toString());
						bw.write(",");
						bw.write(String.valueOf(dist));
						bw.write(",");
						bw.write(String.valueOf(weight));
						bw.newLine();						
					} else{
						log.warn(String.format("Subplace %s: distance %.2f; weight %.2f", mz.getId(), dist, weight));
					}
				}
			} finally {
				bw.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public NetworkImpl getPtNetwork(){
		return (NetworkImpl) this.sPt.getNetwork();
	}
	
}
