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
public class NetworkChange {
	static String networkFile = "../../shared-svn/studies/ihab/parkAndRide/input/test_network.xml";
	static String scheduleFile = "../../shared-svn/studies/ihab/parkAndRide/input/scheduleFile.xml";
	static String vehiclesFile = "../../shared-svn/studies/ihab/parkAndRide/input/vehiclesFile.xml";

	private List<TransitStopFacility> transitStops = new ArrayList<TransitStopFacility>();
	private List<Link> carLinks = new ArrayList<Link>();

// parkAndRide Link:
	private double capacity = 2000;
	private double freeSpeed = 13.8888888889;
	private double length = 500;
	private double nrOfLanes = 5;
//	
	public static void main(String[] args) {
		NetworkChange addParkAndRide = new NetworkChange();
		addParkAndRide.run();
	}

	private void run() {
		
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Config config = scenario.getConfig();
		config.scenario().setUseTransit(true);
		config.scenario().setUseVehicles(true);
		config.transit().setTransitScheduleFile(scheduleFile);
		config.transit().setVehiclesFile(vehiclesFile);
		config.network().setInputFile(networkFile);
		ScenarioLoaderImpl scenarioLoader = new ScenarioLoaderImpl(scenario);
		scenarioLoader.loadScenario();
		
		Network modNetwork = scenario.getNetwork();
		
		TransitSchedule schedule = scenario.getTransitSchedule();
		for (TransitLine line : schedule.getTransitLines().values()){
			for (TransitRoute route : line.getRoutes().values()){
				for (TransitRouteStop stop : route.getStops()){
					transitStops.add(stop.getStopFacility());
				}
			}
		}
		
		Network network = scenario.getNetwork();
		for (Link link : network.getLinks().values()){
			if (link.getAllowedModes().contains(TransportMode.car)){
				carLinks.add(link);
			}
		}
		
		int i = 0;
		for (TransitStopFacility stop : transitStops){
			Id stopLinkId = stop.getLinkId();
			Link link = network.getLinks().get(stopLinkId);
			if (!link.getAllowedModes().contains(TransportMode.car)){
				System.out.println("TransitStopFacility liegt nicht auf einem car-Link.");
			}
			else {
				Id nodeId = new IdImpl("PRnode"+i);
				Coord coord = scenario.createCoord(stop.getCoord().getX()+50, stop.getCoord().getY()+50);
				Node prNode = network.getFactory().createNode(nodeId, coord);
				modNetwork.addNode(prNode);
				
				Link link1 = network.getFactory().createLink(new IdImpl(link.getToNode().getId()+"to"+prNode.getId()), link.getToNode(), prNode);
				network.addLink(setParkAndRideLinks(link1));
				Link link2 = network.getFactory().createLink(new IdImpl(prNode.getId()+"to"+link.getToNode().getId()), prNode, link.getToNode());
				network.addLink(setParkAndRideLinks(link2));	
				i++;
				
			}
		}
		
		
		
		NetworkWriter networkWriter = new NetworkWriter(modNetwork);
		networkWriter.write("../../shared-svn/studies/ihab/parkAndRide/input/test_network_modified.xml");
	}

	/**
	 * @param link1
	 * @return 
	 */
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
	
//	private void createLinks(int linkNr) {
//		Map<Integer, Node> nodes = new HashMap<Integer,Node>();
//		int counter = 0;
//		for (Node node : network.getNodes().values()){
//			nodes.put(counter, node);
//			counter++;
//		}
//		
//		int nodeNr = 0;
//		for (int aa= 0; aa<= linkNr-1; aa++){
//			Node nodeA = nodes.get(nodeNr);
//			Node nodeB = nodes.get(nodeNr+1);
//			Id linkIdAB = new IdImpl(nodeA.getId()+"to"+nodeB.getId());
//			Link linkAB = network.getFactory().createLink(linkIdAB, nodeA, nodeB);
//			network.addLink(linkAB);
//			Id linkIdBA = new IdImpl(nodeB.getId()+"to"+nodeA.getId());
//			Link linkBA = network.getFactory().createLink(linkIdBA, nodeB, nodeA);
//			network.addLink(linkBA);	
//			
//			nodeNr++;
//		}
//	}
//
//	private void setLinks() {
//		for (Link link : network.getLinks().values()){
//			Set<String> modes = new HashSet<String>();
//			modes.add("car");
//			modes.add("bus");
//			
//			link.setAllowedModes((Set<String>) modes);
//			link.setCapacity(capacity);
//			link.setFreespeed(freeSpeed);
//			link.setLength(length);
//			link.setNumberOfLanes(nrOfLanes);
//		}
//	}
//
//	private void createNodes(int linkNr) {
//		double xCoord = 0.0;
//		int nodeNr = 0;
//		for (int ii=0; ii<=linkNr; ii++){
//				Id nodeIdA = new IdImpl(nodeNr);
//				Coord fromNodeCoord = scenario.createCoord(xCoord, 0);
//				Node nodeA = network.getFactory().createNode(nodeIdA, fromNodeCoord);
//				network.addNode(nodeA);
//				xCoord = xCoord+length;
//				nodeNr++;
//		}
//		
//	}
}
