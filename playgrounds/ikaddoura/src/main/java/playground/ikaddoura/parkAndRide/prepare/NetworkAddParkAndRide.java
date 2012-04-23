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
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.pt.config.TransitConfigGroup;
import org.matsim.pt.transitSchedule.TransitScheduleFactoryImpl;
import org.matsim.pt.transitSchedule.api.TransitLine;
import org.matsim.pt.transitSchedule.api.TransitRoute;
import org.matsim.pt.transitSchedule.api.TransitRouteStop;
import org.matsim.pt.transitSchedule.api.TransitSchedule;
import org.matsim.pt.transitSchedule.api.TransitScheduleFactory;
import org.matsim.pt.transitSchedule.api.TransitStopFacility;

import playground.ikaddoura.parkAndRide.pR.ParkAndRideFacility;


/**
 * @author Ihab
 *
 */

// todo: Suchbereich in Suchradius umwandeln...

public class NetworkAddParkAndRide {
	static String networkFile = "../../shared-svn/studies/ihab/parkAndRide/input/test_network.xml";
	static String scheduleFile = "../../shared-svn/studies/ihab/parkAndRide/input/scheduleFile.xml";
	static String vehiclesFile = "../../shared-svn/studies/ihab/parkAndRide/input/vehiclesFile.xml";

	private List<ParkAndRideFacility> parkAndRideFacilities = new ArrayList<ParkAndRideFacility>();
	private List<TransitStopFacility> transitStops = new ArrayList<TransitStopFacility>();
	private List<Node> carLinkToNodes = new ArrayList<Node>();
	private Map<TransitStopFacility, Link> transitStop2nearestCarLink = new HashMap<TransitStopFacility, Link>();

	private double extensionRadius = 100;
	private int maxSearchSteps = 50;
	
// parkAndRide Link:
	private double capacity = 2000;
	private double freeSpeed = 13.8888888889;
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
		
		getAllTransitStops();
		// sammelt ALLE TransitStops und speicher sie in der Liste transitStops
		
		getParkAndRideNodes();
		// sucht für jeden TransitStop in der Liste transitStops den zugehörigen / nächstliegenden car-Link
		// und speichert jeweils den toNode dieses car-Links in der Liste parkAndRideNodes
		// toDo: statt quadratweise Ausweitung: Radius
		
		for (Node node : this.carLinkToNodes){
			System.out.println("Node: "+node.getId().toString());
		}
		addParkAndRideLinks();
		// fügt an jeden Node der Liste parkAndRideNodes zwei parkAndRideLink ein.
				
		for (TransitStopFacility stop : this.transitStop2nearestCarLink.keySet()){
			System.out.println("TranistStop: "+stop.getId().toString()+" "+stop.getCoord().toString()+" --> nächster car-Link: "+this.transitStop2nearestCarLink.get(stop).getId()+" toNode:"+this.transitStop2nearestCarLink.get(stop).getToNode().getCoord().toString());
		}
		
		printOutParkAndRideFacilites();
		NetworkWriter networkWriter = new NetworkWriter(scenario.getNetwork());
		networkWriter.write("../../shared-svn/studies/ihab/parkAndRide/input/testNetworkParkAndRide.xml");
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
				System.out.println("TransitStopFacility "+stop.getId()+" liegt nicht auf einem car-Link.");
				System.out.println("Suche schrittweise nach car-Links im Umkreis des TransitStops...");
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
			Id pRnodeId1 = new IdImpl("PrA"+i);
			Id pRnodeId2 = new IdImpl("PrB"+i);

			Coord coord1 = scenario.createCoord(node.getCoord().getX()+20, node.getCoord().getY()+20);
			Coord coord2 = scenario.createCoord(node.getCoord().getX()+10, node.getCoord().getY()+10);

			Node prNode1 = scenario.getNetwork().getFactory().createNode(pRnodeId1, coord1);
			Node prNode2 = scenario.getNetwork().getFactory().createNode(pRnodeId2, coord2);

			scenario.getNetwork().addNode(prNode1);
			scenario.getNetwork().addNode(prNode2);
			
			Link link1a = scenario.getNetwork().getFactory().createLink(new IdImpl(node.getId()+"to"+prNode1.getId()), node, prNode1);
			scenario.getNetwork().addLink(setParkAndRideLinks(link1a));
			Link link2a = scenario.getNetwork().getFactory().createLink(new IdImpl(prNode1.getId()+"to"+node.getId()), prNode1, node);
			scenario.getNetwork().addLink(setParkAndRideLinks(link2a));	
			
			Link link1b = scenario.getNetwork().getFactory().createLink(new IdImpl(prNode1.getId()+"to"+prNode2.getId()), prNode1, prNode2);
			scenario.getNetwork().addLink(setParkAndRideLinks(link1b));
			Link link2b = scenario.getNetwork().getFactory().createLink(new IdImpl(prNode2.getId()+"to"+prNode1.getId()), prNode2, prNode1);
			scenario.getNetwork().addLink(setParkAndRideLinks(link2b));	
			
			this.parkAndRideFacilities.add(new ParkAndRideFacility(i, link1a.getId(), link2a.getId(), link1b.getId(), link2b.getId()));
			
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
		File file = new File("../../shared-svn/studies/ihab/parkAndRide/input/parkAndRideFacilities.txt");
	
	    try {
	    BufferedWriter bw = new BufferedWriter(new FileWriter(file));
	    String zeile1 = "Nr ; Link1a ; Link2a ; Link1b ; Link2b";
	    bw.write(zeile1);
	    bw.newLine();
	
	    for (ParkAndRideFacility pr : this.parkAndRideFacilities){
	    	int nr = pr.getNr();
	    	Id link1a = pr.getLinkArein();
	    	Id link2a = pr.getLinkAraus();
	    	Id link1b = pr.getLinkBrein();
	    	Id link2b = pr.getLinkBraus();
	    	
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
