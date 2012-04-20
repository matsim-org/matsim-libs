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


/**
 * @author Ihab
 *
 */

// todo: Suchbereich in Suchradius umwandeln...

public class NetworkAddParkAndRide {
	static String networkFile = "../../shared-svn/studies/ihab/parkAndRide/input/test_network2.xml";
	static String scheduleFile = "../../shared-svn/studies/ihab/parkAndRide/input/scheduleFile.xml";
	static String vehiclesFile = "../../shared-svn/studies/ihab/parkAndRide/input/vehiclesFile.xml";

	private List<TransitStopFacility> transitStops = new ArrayList<TransitStopFacility>();
	private List<Node> parkAndRideNodes = new ArrayList<Node>();

	private double searchStep = 100;
	private int maxSearchSteps = 50;
	
// parkAndRide Link:
	private double capacity = 2000;
	private double freeSpeed = 13.8888888889;
	private double length = 500;
	private double nrOfLanes = 5;
	
	ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());

//	
	public static void main(String[] args) {
		NetworkAddParkAndRide addParkAndRide = new NetworkAddParkAndRide();
		addParkAndRide.run();
	}

	private void run() {
		
		loadScenario();
		getAllTransitStops();
		getParkAndRideNodes();
		
		for (Node node : this.parkAndRideNodes){
			System.out.println("Node: "+node.getId().toString());
		}
		addParkAndRideLinks();
		
		NetworkWriter networkWriter = new NetworkWriter(scenario.getNetwork());
		networkWriter.write("../../shared-svn/studies/ihab/parkAndRide/input/test_network_modified2.xml");
	}

	private void getParkAndRideNodes() {
		boolean hasPRnode;
		for (TransitStopFacility stop : transitStops){
			hasPRnode = false;
			Id stopLinkId = stop.getLinkId();
			Link stopLink = scenario.getNetwork().getLinks().get(stopLinkId);
			if (stopLink.getAllowedModes().contains(TransportMode.car)){
				if (!this.parkAndRideNodes.contains(stopLink.getToNode())){
					this.parkAndRideNodes.add(stopLink.getToNode());
				}
			}
			else {
				System.out.println("TransitStopFacility "+stop.getId()+" liegt nicht auf einem car-Link.");
				Coord coord = stop.getCoord();
				double searchArea = 0; 
				for (int n = 0; n <= maxSearchSteps; n++){
					if (!hasPRnode){
						searchArea = searchArea + this.searchStep;
						System.out.println("Suchbereich: "+searchArea+" um die Koordinaten "+coord.toString()+".");

						for (Link link : scenario.getNetwork().getLinks().values()){
							if (!hasPRnode){
								if (link.getAllowedModes().contains(TransportMode.car)){
									System.out.println("Der Link "+link.getId()+" ist ein car-Link.");
									double linkToNodeX = link.getToNode().getCoord().getX();
									double linkToNodeY = link.getToNode().getCoord().getY();
									double xDiff = Math.abs(linkToNodeX - coord.getX());
									double yDiff = Math.abs(linkToNodeY - coord.getY());
									if (xDiff <= searchArea && yDiff <= searchArea){
										System.out.println("Der Link "+link.getId()+" liegt im Suchbereich um die TransitStopCoordinates "+coord.toString()+".");
										if (!this.parkAndRideNodes.contains(link.getToNode())){
											this.parkAndRideNodes.add(link.getToNode());
										}
										hasPRnode = true;
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
		for (Node node : this.parkAndRideNodes){
			Id pRnodeId = new IdImpl("PRnode"+i);
			Coord coord = scenario.createCoord(node.getCoord().getX()+250, node.getCoord().getY()+250);
			Node prNode = scenario.getNetwork().getFactory().createNode(pRnodeId, coord);
			scenario.getNetwork().addNode(prNode);
			
			Link link1 = scenario.getNetwork().getFactory().createLink(new IdImpl(node.getId()+"to"+prNode.getId()), node, prNode);
			scenario.getNetwork().addLink(setParkAndRideLinks(link1));
			Link link2 = scenario.getNetwork().getFactory().createLink(new IdImpl(prNode.getId()+"to"+node.getId()), prNode, node);
			scenario.getNetwork().addLink(setParkAndRideLinks(link2));	
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
}
