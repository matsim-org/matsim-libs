/* *********************************************************************** *
 * project: org.matsim.*
 * networkChange.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.ikaddoura.parkAndRide.prepare;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;
import playground.ikaddoura.parkAndRide.pR.ParkAndRideFacility;


/**
 * @author Ihab
 *
 */

public class NetworkAddParkAndRide {
	static String networkFile = "../../shared-svn/studies/ihab/parkAndRide/input/test_network.xml";
	static String scheduleFile = "../../shared-svn/studies/ihab/parkAndRide/input/scheduleFile.xml";
	static String vehiclesFile = "../../shared-svn/studies/ihab/parkAndRide/input/vehiclesFile.xml";
	
	static String prFacilitiesFile = "../../shared-svn/studies/ihab/parkAndRide/input/prFacilities.txt";
	static String prNetworkFile = "../../shared-svn/studies/ihab/parkAndRide/input/prNetwork.xml";

	private List<ParkAndRideFacility> parkAndRideFacilities = new ArrayList<ParkAndRideFacility>();
	private List<TransitStopFacility> transitStops = new ArrayList<TransitStopFacility>();
	private List<Node> carLinkToNodes = new ArrayList<Node>();
	private Map<TransitStopFacility, Link> transitStop2nearestCarLink = new HashMap<TransitStopFacility, Link>();

	private double extensionRadius = 100;
	private int maxSearchSteps = 50;
	
// parkAndRide Link:
	private double capacity = 2000;
	private double freeSpeed = 2.77778;
	private double length = 20;
	private double nrOfLanes = 40;
	
	ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

//	
	public static void main(String[] args) {
		NetworkAddParkAndRide addParkAndRide = new NetworkAddParkAndRide();
		addParkAndRide.run();
	}

	private void run() {
		
		loadScenario();
		getAllTransitStops();		// gathers all TransitStops in List transitStops
		getParkAndRideNodes();		// searches for each TransitStop in List transitStops the belonging / next car-Link, puts the toNode in List parkAndRideNodes
		addParkAndRideLinks();		// adds two parkAndRideLinks to each Node in List parkAndRideNodes
		writeParkAndRideNetwork();	
		printOutParkAndRideFacilites();

	}

	private void writeParkAndRideNetwork() {
		NetworkWriter networkWriter = new NetworkWriter(scenario.getNetwork());
		networkWriter.write(prNetworkFile);		
	}

	private void getParkAndRideNodes() {
		boolean hasPRnode;
		for (TransitStopFacility stop : transitStops){
			hasPRnode = false;
			Id stopLinkId = stop.getLinkId();
			Link stopLink = scenario.getNetwork().getLinks().get(stopLinkId);
			if (stopLink.getAllowedModes().contains(TransportMode.car)){
				this.transitStop2nearestCarLink.put(stop, stopLink);
				if (!this.carLinkToNodes.contains(stopLink.getToNode())){
					this.carLinkToNodes.add(stopLink.getToNode());
				}
			}
			else {
				System.out.println("***");
				System.out.println("TransitStopFacility "+stop.getId()+" is not on a car-Link.");
				System.out.println("Searching for car-Links in area of TransitStop...");
				Coord coord = stop.getCoord();
				double searchRadius = 0; 
				for (int n = 0; n <= maxSearchSteps; n++){
					if (!hasPRnode){
						searchRadius = searchRadius + this.extensionRadius;
						System.out.println("Suchradius: "+searchRadius+" um die Koordinaten "+coord.toString()+".");

						for (Link link : scenario.getNetwork().getLinks().values()){
							if (!hasPRnode){
								if (link.getAllowedModes().contains(TransportMode.car)){
									System.out.println("Der Link "+link.getId()+" ist ein car-Link.");
									double linkToNodeX = link.getToNode().getCoord().getX();
									double linkToNodeY = link.getToNode().getCoord().getY();
									double xDiff = Math.abs(linkToNodeX - stop.getCoord().getX());
									double yDiff = Math.abs(linkToNodeY - stop.getCoord().getY());
									double hyp = Math.sqrt(Math.pow(xDiff, 2)+Math.pow(yDiff, 2));
									if (hyp <= searchRadius){
										System.out.println("Der Link "+link.getId()+" liegt im Suchradius um die TransitStopCoordinates "+coord.toString()+".");
										this.transitStop2nearestCarLink.put(stop, link);
										if (!this.carLinkToNodes.contains(link.getToNode())){
											this.carLinkToNodes.add(link.getToNode());
										}
										hasPRnode = true;
									}
									else {
										System.out.println("Der Link "+link.getId()+" liegt NICHT im Suchradius um die TransitStopCoordinates "+coord.toString()+".");
									}
								}
							}
						}
					}
				}
			}
		}		
	}

	private void getAllTransitStops() {
		TransitSchedule schedule = scenario.getTransitSchedule();
		for (TransitLine line : schedule.getTransitLines().values()){
			for (TransitRoute route : line.getRoutes().values()){
				for (TransitRouteStop stop : route.getStops()){
					transitStops.add(stop.getStopFacility());
				}
			}
		}	
	}

	private void loadScenario() {
		Config config = scenario.getConfig();
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.transit().setVehiclesFile(vehiclesFile);
		config.network().setInputFile(networkFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario);
		scenarioLoader.loadScenario();		
	}

	private void addParkAndRideLinks() {
		int i = 0;
		for (Node node : this.carLinkToNodes){
			Id pRnodeId1 = new IdImpl("PRa"+i);
			Id pRnodeId2 = new IdImpl("PRb"+i);

			Coord coord1 = scenario.createCoord(node.getCoord().getX(), node.getCoord().getY()+length);
			Coord coord2 = scenario.createCoord(node.getCoord().getX(), node.getCoord().getY());

			Node prNode1 = scenario.getNetwork().getFactory().createNode(pRnodeId1, coord1);
			Node prNode2 = scenario.getNetwork().getFactory().createNode(pRnodeId2, coord2);

			scenario.getNetwork().addNode(prNode1);
			scenario.getNetwork().addNode(prNode2);
			
			Link prLink1in = scenario.getNetwork().getFactory().createLink(new IdImpl("prLink1in_" + i), node, prNode1);
			scenario.getNetwork().addLink(setParkAndRideLinks(prLink1in));
			Link prLink1out = scenario.getNetwork().getFactory().createLink(new IdImpl("prLink1out_" + i), prNode1, node);
			scenario.getNetwork().addLink(setParkAndRideLinks(prLink1out));	
			
			Link prLink2in = scenario.getNetwork().getFactory().createLink(new IdImpl("prLink2in_" + i), prNode1, prNode2);
			scenario.getNetwork().addLink(setParkAndRideLinks(prLink2in));
			Link prLink2out = scenario.getNetwork().getFactory().createLink(new IdImpl("prLink2out_" + i), prNode2, prNode1);
			scenario.getNetwork().addLink(setParkAndRideLinks(prLink2out));	
			
			ParkAndRideFacility prFacility = new ParkAndRideFacility();
			prFacility.setNr(i);
			prFacility.setPrLink1in(prLink1in.getId());
			prFacility.setPrLink1out(prLink1out.getId());
			prFacility.setPrLink2in(prLink2in.getId());
			prFacility.setPrLink2out(prLink2out.getId());
			
			this.parkAndRideFacilities.add(prFacility);
			
			i++;
		}
	}
	
	private Link setParkAndRideLinks(Link link) {
		Set<String> modes = new HashSet<String>();
		modes.add("car");		
		link.setAllowedModes((Set<String>) modes);
		link.setCapacity(capacity);
		link.setFreespeed(freeSpeed);
		link.setLength(length);
		link.setNumberOfLanes(nrOfLanes);
		return link;
	}
	

	private void printOutParkAndRideFacilites() {
		
		for (TransitStopFacility stop : this.transitStop2nearestCarLink.keySet()){
			System.out.println("TranistStopFacility: "+stop.getId().toString()+" "+stop.getCoord().toString()+" / next car-Link: "+this.transitStop2nearestCarLink.get(stop).getId()+" / toNode:"+this.transitStop2nearestCarLink.get(stop).getToNode().getCoord().toString());
		}
		
		File file = new File(prFacilitiesFile);
	
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "Nr ; Link1in ; Link1out ; Link2in ; Link2out";
	    bw.write(zeile1);
	    bw.newLine();
	
	    for (ParkAndRideFacility pr : this.parkAndRideFacilities){
	    	int nr = pr.getNr();
	    	Id link1a = pr.getPrLink1in();
	    	Id link2a = pr.getPrLink1out();
	    	Id link1b = pr.getPrLink2in();
	    	Id link2b = pr.getPrLink2out();
	    	
	    	String zeile = nr+ " ; "+link1a+" ; "+link2a+" ; "+link1b+" ; "+link2b;
	
	    	bw.write(zeile);
	        bw.newLine();
	    }
	
	    bw.flush();
	    bw.close();
    
	    } catch (IOException e) {}
	    System.out.println("ParkAndRideFacilites written to "+file.toString());
	}
}
