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

package playground.jjoubert.TemporaryCode;

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
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.NetworkFactory;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.events.EventsReaderTXTv1;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkFactoryImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.network.algorithms.TransportModeNetworkFilter;
import org.matsim.core.router.Dijkstra;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactory;
import org.matsim.core.router.costcalculators.TravelCostCalculatorFactoryImpl;
import org.matsim.core.router.util.PersonalizableTravelCost;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactory;
import org.matsim.core.trafficmonitoring.TravelTimeCalculatorFactoryImpl;
import org.matsim.core.utils.geometry.CoordImpl;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilder;
import org.matsim.utils.gis.matsim2esri.network.FeatureGeneratorBuilderImpl;
import org.matsim.utils.gis.matsim2esri.network.Links2ESRIShape;

import playground.jjoubert.Utilities.MyGapReader;
import playground.jjoubert.Utilities.matsim2urbansim.M2UStringbuilder;
import playground.jjoubert.Utilities.matsim2urbansim.MyZone;
import playground.jjoubert.Utilities.matsim2urbansim.MyZoneReader;

public class RunEthekwiniNetworkFilter {
	private final static Logger log = Logger.getLogger(RunEthekwiniNetworkFilter.class);
	private static String root;
	private static String studyAreaName;
	private static String version;
	private static String percentage;
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		if(args.length != 4){
			throw new RuntimeException("Incorrect number of arguments passed.");
		} else{
			root = args[0];
			studyAreaName = args[1];
			version = args[2];
			percentage = args[3];
		}
		log.info("Filtering " + studyAreaName + "'s MATSim network to account for public transport.");
		
		// Read network.
		M2UStringbuilder sb = new M2UStringbuilder(root, studyAreaName, version, "100");
		Scenario s = new ScenarioImpl();
		Scenario sPt = new ScenarioImpl();
		MatsimNetworkReader nr = new MatsimNetworkReader(s);
		nr.readFile(sb.getEmmeNetworkFilename());		
		
		/*
		 * Process transit-lines-by-link file.
		 * 
		 * - Doesn't (always) work. Seems the OD node pair does not describe a 
		 * 	 LINK, but a PATH. So I'll have to process events and get a router 
		 *   going. 
		 */
		TravelTimeCalculatorFactory ttcf = new TravelTimeCalculatorFactoryImpl();
		TravelTimeCalculator ttc = ttcf.createTravelTimeCalculator(s.getNetwork(), s.getConfig().travelTimeCalculator());
		TravelCostCalculatorFactory tccf = new TravelCostCalculatorFactoryImpl();
		PersonalizableTravelCost tc = tccf.createTravelCostCalculator(ttc, s.getConfig().charyparNagelScoring());
		EventsManagerImpl em = new EventsManagerImpl();
		em.addHandler(ttc);
		new EventsReaderTXTv1(em).readFile("/Users/johanwjoubert/MATSim/workspace/MATSimData/eThekwini/2005/100.events_10.txt.gz");
		Dijkstra router = new Dijkstra(s.getNetwork(),tc,ttc);
		Set<String> ptSet = new TreeSet<String>();
		ptSet.add(TransportMode.pt);
		
		try {
			BufferedReader br = IOUtils.getBufferedReader(sb.getEmmeTransitLinesByLinkFilename());
			try{
				@SuppressWarnings("unused")
				String header = br.readLine();
				String line = null;
				int ptCounter = 0;
				int directCounter = 0;
				while((line = br.readLine()) != null){
					String[] link = line.split("\t");
					if(link.length > 8){
						if(link[8].equalsIgnoreCase("0")){
							// It is car... or free for all.
						} else{
							// It has public transport modes.
							ptCounter++;
							Path p = null;
							Node oNode = s.getNetwork().getNodes().get(new IdImpl(link[1]));
							Node dNode = s.getNetwork().getNodes().get(new IdImpl(link[2]));
							if(oNode != null && dNode != null){
								p = router.calcLeastCostPath(oNode, dNode, 21600);
								// Set all links in path.
								for(Link ptLink : p.links){
									ptLink.setAllowedModes(ptSet);
								}
								
		
								Map<Id, ? extends Link> outLinks = s.getNetwork().getNodes().get(oNode.getId()).getOutLinks();
								Map<Id, ? extends Link> inLinks = s.getNetwork().getNodes().get(oNode.getId()).getInLinks();
								boolean found = false;
								for(Link l : outLinks.values()){
									Link aLink = l;
									if(l.getToNode().equals(dNode)){
										//								log.info("I found the link.");
										Link theLink = aLink;
										found = true;
										directCounter++;
									}
								}
								if(!found){
									log.info("No direct trip from " + oNode.getId().toString() + " to " + dNode.getId().toString());
								}
								//						for(Link l : inLinks.values()){
								//							Link aLink = l;
								//							if(l.getFromNode().getId().equals(dNode)){
								//								log.info("I found the link.");
								//								Link theLink = aLink;
								//							}
								//						}
							}

						}
					}
				}
				log.info("Found " + ptCounter + " node-pairs serviced by transit (" + directCounter + " direct )");
				
			} finally{
				br.close();
			}
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		log.info("Done reading transit-lines-by-link file. Now filter the network.");
		TransportModeNetworkFilter nf = new TransportModeNetworkFilter(s.getNetwork());
		nf.filter(sPt.getNetwork(), ptSet);
		NetworkWriter nw = new NetworkWriter(sPt.getNetwork());
		nw.writeFileV1(sb.getEmmePtNetworkFilename());
		log.info("Network filterted and written to " + sb.getEmmePtNetworkFilename());
		// Write to ESRI shapefile.
//		FeatureGeneratorBuilder fgb = new FeatureGeneratorBuilderImpl(sPt.getNetwork(), "WGS84_UTM36S");
//		Links2ESRIShape l2e = new Links2ESRIShape(sPt.getNetwork(),"/Users/johanwjoubert/Desktop/PT.shp", fgb);
//		l2e.write();
//		l2e = new Links2ESRIShape(s.getNetwork(), "/Users/johanwjoubert/Desktop/All.shp", "WGS84_UTM36S");
//		l2e.write();
		// Read subplace shapefile.
		MyZoneReader mzr = new MyZoneReader(sb.getSubPlaceShapefile());
		mzr.readZones(2);
		List<MyZone> spList = mzr.getZones();
		log.info("Read sub-place shapefile: " + spList.size() + " entries for " + studyAreaName);
		int smallCounter = 0;
		for(MyZone mz : spList){
			Double area = mz.getArea();
			if(area < 100){
				log.warn("Zone " + mz.getId() + " area: " + area);
				smallCounter++;
			}
		}
		log.warn("Found " + smallCounter + " zones smaller than 100 units.");
		
		Map<Id, Double> distanceMap = new TreeMap<Id, Double>();
		Map<Id, Double> ptMap = new TreeMap<Id, Double>();
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
		
		try {
			BufferedWriter bw = IOUtils.getBufferedWriter(sb.getSubPlaceDistanceFilename());
			try{
				bw.write("SP_ID,Dist,Weight,W_Dist");
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
						bw.write(",");
						bw.write(String.valueOf(dist*weight));
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
		
		
		
		
		log.info("---------------------------------------------");
		log.info("             PROCESS COMPLETE");
		log.info("=============================================");
		
	}

}
