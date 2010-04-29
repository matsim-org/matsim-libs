/* *********************************************************************** *
 * project: org.matsim.*
 * ReNumberNetwork.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package playground.christoph.network;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;

import playground.christoph.controler.WithinDayControler;

/*
 * When writing NodeKnowledge to a Database, the IDs of the
 * Network's Nodes are used to describe the Knowledge. To
 * shorten the String that is written to the Network we renumber
 * the Nodes.
 */
public class ReNumberNetwork {

	private static final Logger log = Logger.getLogger(WithinDayControler.class);
	
	private static NetworkLayer network;
	private static String networkFile = "mysimulations/kt-zurich-cut/network.xml";
	private static String outputNetworkFile = "mysimulations/kt-zurich-cut/mapped_network.xml.gz";
	
	private static int startNumber = 1;
	
	/*
	 * Maybe needed for another Remapping, for example of the agents plans. 
	 */
	private static Map<Id, Id> mapping;		// old Id, new Id
	
	public static void main(String[] args)
	{
		ScenarioImpl scenario = new ScenarioImpl();
		network = scenario.getNetwork();
		new MatsimNetworkReader(scenario).readFile(networkFile);
		
		mapping = new HashMap<Id, Id>();
		
		List<Node> newNodes = new ArrayList<Node>();
		List<Node> oldNodes = new ArrayList<Node>();
				
		for (Node node : network.getNodes().values())
		{
			Id newId = scenario.createId(Integer.toString(startNumber));
			NodeImpl newNode = new NodeImpl(newId);
			newNode.setCoord(node.getCoord());
//			newNode.setOrigId(node.getOrigId());	// Put original Id here?
//			newNode.setType(node.getType());
			
			for (Link link : node.getInLinks().values())
			{
				link.setToNode(newNode);
			}
			
			for (Link link : node.getOutLinks().values())
			{
				link.setFromNode(newNode);
			}
			
			mapping.put(node.getId(), newNode.getId());
			
			newNodes.add(newNode);
			oldNodes.add(node);
			
			startNumber++;
		}
		
		for (Node node : newNodes) network.addNode(node);
		for (Node node : oldNodes) network.getNodes().remove(node.getId());
		
		new NetworkWriter(network).write(outputNetworkFile);
			
		log.info("done!");
	}
}
