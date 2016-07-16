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
package playground.ikaddoura.utils.prepare;

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
import org.matsim.core.network.io.NetworkWriter;
import org.matsim.core.scenario.ScenarioUtils;

/**
 * @author ikaddoura
 *
 */
public class CorridorNetworkWriter_opt3 {
	private Scenario scenario;
	private Network network;
	private final int linkNr = 40; // each direction
	private final double capacityCarLinks = 1000;
	private final double capacityBusLinks = 1000;
	private final double freeSpeedCarLinks = 8.34;
	private final double freeSpeedBusLinks = 8.34;
	
	private final double capacityMixedLinks = 2000;
	private final double freeSpeedMixedLinks = 8.34;

	private final double length = 500;
	private final double nrOfLanes_singleModeLink = 1;
	private final double nrOfLanes_mixedModes = 2;

	// modes: car, bus
	
	public static void main(String[] args) {
		CorridorNetworkWriter_opt3 corridorNetworkWriter = new CorridorNetworkWriter_opt3();
		corridorNetworkWriter.run();
	}

	private void run() {
		scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		network = scenario.getNetwork();

		createNodes(linkNr);
		
//		createBusLinks(linkNr);
		createCarLinks(linkNr);
		
//		createMixedLinks(linkNr);
		
		setLinks();
		
		NetworkWriter networkWriter = new NetworkWriter(network);
//		networkWriter.write("/Users/Ihab/Desktop/network_busLane.xml");
		networkWriter.write("/Users/Ihab/Desktop/network_car.xml");

	}
	
	private void createMixedLinks(int linkNr) {
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
			Id<Link> linkIdAB = Id.create(nodeA.getId()+"to"+nodeB.getId()+"_mixed", Link.class);
			Link linkAB = network.getFactory().createLink(linkIdAB, nodeA, nodeB);
			network.addLink(linkAB);
			Id<Link> linkIdBA = Id.create(nodeB.getId()+"to"+nodeA.getId()+"_mixed", Link.class);
			Link linkBA = network.getFactory().createLink(linkIdBA, nodeB, nodeA);
			network.addLink(linkBA);	
			
			nodeNr++;
		}		
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
				link.setNumberOfLanes(nrOfLanes_singleModeLink);
				
			} else if (link.getId().toString().contains("car")){
				modes.add("car");
				link.setCapacity(capacityCarLinks);
				link.setFreespeed(freeSpeedCarLinks);
				link.setNumberOfLanes(nrOfLanes_singleModeLink);
				
			} else if (link.getId().toString().contains("mixed")){
				modes.add("car");
				modes.add("bus");
				link.setCapacity(capacityMixedLinks);
				link.setFreespeed(freeSpeedMixedLinks);
				link.setNumberOfLanes(nrOfLanes_mixedModes);

			} else {
				throw new RuntimeException("Unknown mode...");
			}
			
			link.setAllowedModes(modes);
			link.setLength(length);
		}
	}

	private void createNodes(int linkNr) {
		double xCoord = 0.0;
		int nodeNr = 0;
		for (int ii=0; ii<=linkNr; ii++){
				Id<Node> nodeIdA = Id.create(nodeNr, Node.class);
			Coord fromNodeCoord = new Coord(xCoord, (double) 0);
				Node nodeA = network.getFactory().createNode(nodeIdA, fromNodeCoord);
				network.addNode(nodeA);
				xCoord = xCoord+length;
				nodeNr++;
		}
		
	}

}
