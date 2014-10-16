/* *********************************************************************** *
 * project: org.matsim.*
 * CorridorNetworkWriter.java
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
package playground.ikaddoura.busCorridorPaper.prepare;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author Ihab
 *
 */
public class CorridorNetworkWriter {
	private Scenario scenario;
	private Network network;
	private final int linkNr = 40; // each direction
	private final double capacityCarLinks = 200;
	private final double capacityBusLinks = 4000;
	private final double freeSpeedCarLinks = 13.8888888889;
	private final double freeSpeedBusLinks = 8.3333333333;

	private final double length = 500;
	private final double nrOfLanes = 1;
	// modes: car, bus
	
	public static void main(String[] args) {
		CorridorNetworkWriter corridorNetworkWriter = new CorridorNetworkWriter();
		corridorNetworkWriter.run();
	}

	private void run() {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		network = scenario.getNetwork();

		createNodes(linkNr);
		createBusLinks(linkNr);
		createCarLinks(linkNr);
		setLinks();
		
		NetworkWriter networkWriter = new NetworkWriter(network);
		networkWriter.write("../../shared-svn/studies/ihab/busCorridor/input_Latsis/network_welfareBusCorridor_bus4000_car200.xml");
	}
	
	private void createBusLinks(int linkNr) {
		Map<Integer, Node> nodes = new HashMap<Integer,Node>();
		int counter = 0;
		for (Node node : network.getNodes().values()){
			nodes.put(counter, node);
			counter++;
		}
		
		int nodeNr = 0;
		for (int aa= 0; aa<= linkNr-1; aa++){
			Node nodeA = nodes.get(nodeNr);
			Node nodeB = nodes.get(nodeNr+1);
			Id<Link> linkIdAB = Id.create(nodeA.getId()+"to"+nodeB.getId()+"_bus", Link.class);
			Link linkAB = network.getFactory().createLink(linkIdAB, nodeA, nodeB);
			network.addLink(linkAB);
			Id<Link> linkIdBA = Id.create(nodeB.getId()+"to"+nodeA.getId()+"_bus", Link.class);
			Link linkBA = network.getFactory().createLink(linkIdBA, nodeB, nodeA);
			network.addLink(linkBA);	
			
			nodeNr++;
		}
	}
	
	private void createCarLinks(int linkNr) {
		Map<Integer, Node> nodes = new HashMap<Integer,Node>();
		int counter = 0;
		for (Node node : network.getNodes().values()){
			nodes.put(counter, node);
			counter++;
		}
		
		int nodeNr = 0;
		for (int aa= 0; aa<= linkNr-1; aa++){
			Node nodeA = nodes.get(nodeNr);
			Node nodeB = nodes.get(nodeNr+1);
			Id<Link> linkIdAB = Id.create(nodeA.getId()+"to"+nodeB.getId()+"_car", Link.class);
			Link linkAB = network.getFactory().createLink(linkIdAB, nodeA, nodeB);
			network.addLink(linkAB);
			Id<Link> linkIdBA = Id.create(nodeB.getId()+"to"+nodeA.getId()+"_car", Link.class);
			Link linkBA = network.getFactory().createLink(linkIdBA, nodeB, nodeA);
			network.addLink(linkBA);	
			
			nodeNr++;
		}
	}

	private void setLinks() {
		for (Link link : network.getLinks().values()){
			Set<String> modes = new HashSet<String>();
			if (link.getId().toString().contains("bus")){
				modes.add("bus");
				link.setCapacity(capacityBusLinks);
				link.setFreespeed(freeSpeedBusLinks);
				
			} else if (link.getId().toString().contains("car")){
				modes.add("car");
				link.setCapacity(capacityCarLinks);
				link.setFreespeed(freeSpeedCarLinks);

			} else {
				System.out.println("Unknown mode...");
			}
			
			link.setAllowedModes(modes);
			link.setLength(length);
			link.setNumberOfLanes(nrOfLanes);
		}
	}

	private void createNodes(int linkNr) {
		double xCoord = 0.0;
		int nodeNr = 0;
		for (int ii=0; ii<=linkNr; ii++){
				Id<Node> nodeIdA = Id.create(nodeNr, Node.class);
				Coord fromNodeCoord = scenario.createCoord(xCoord, 0);
				Node nodeA = network.getFactory().createNode(nodeIdA, fromNodeCoord);
				network.addNode(nodeA);
				xCoord = xCoord+length;
				nodeNr++;
		}
		
	}

}
