/* *********************************************************************** *
 * project: org.matsim.*
 * CountsCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package playground.telaviv.counts;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.Counter;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import playground.telaviv.config.TelAvivConfig;
import playground.telaviv.network.NetworkEmme2MATSim2012;

/*
 * Assigning the counts from Count Stations to a MATSim Network.
 *
 * When creating the MATSim Network some Links of the Emme2Network have to be
 * converted due to the fact that they contain turning conditions that cannot
 * be directly converted to the MATSim Links. Therefore not for all Links a
 * 1:1 mapping is possible. In those cases the link with the best fit is
 * searched. (Alternatively the mapping could be done via the origId Tag of the
 * link. This Attribute could contain the Ids of the original nodes).
 */
public class CountsCreatorV2 {

	private static final Logger log = Logger.getLogger(CountsCreatorV2.class);
	
	private String nodesFile = TelAvivConfig.basePath + "/network/nodes.csv";
	private String networkFile = TelAvivConfig.basePath + "/network/network.xml";
//	private String countsFile = TelAvivConfig.basePath + "/counts/linkflows1000.csv";
//	private String countsFile = TelAvivConfig.basePath + "./counts/selected_flows.csv";
	private String countsFile = TelAvivConfig.basePath + "/counts/Traffic_counts_V2-revised_2013-09.csv";
	private String outFile = TelAvivConfig.basePath + "/counts/counts.xml";

	private Scenario scenario;
	
	private Map<Id, ? extends Node> originalNodes = new HashMap<Id, Node>();
	private QuadTree<Node> nodeQuadTree;
	
	public static void main(String[] args) {
		new CountsCreatorV2(((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig()))).createCounts();
	}

	public CountsCreatorV2(Scenario scenario) {
		this.scenario = scenario;
		new MatsimNetworkReader(scenario).readFile(networkFile);
	}

	public void createCounts() {
		Counts counts = new Counts();
		counts.setName("Tel Aviv Model");
		counts.setDescription("Tel Aviv Model");
		counts.setYear(2012);
		
		if (this.nodesFile != null) {
			try {
			Config config = ConfigUtils.createConfig();
			Scenario scenario = ScenarioUtils.createScenario(config);
				NetworkEmme2MATSim2012.readNodes((NetworkImpl) scenario.getNetwork(), scenario, false);
				originalNodes = scenario.getNetwork().getNodes();
				buildNodesQuadTree();
			} catch (Exception e) {
				Gbl.errorMsg(e);
			}
		}

		List<CountV2> emme2Counts = new CountsFileParserV2(countsFile).readFile();
		
		Counter counter = new Counter("Count stations mapped to MATSim network: #");
		for (CountV2 emme2Count : emme2Counts) {
			Id fromNodeId = scenario.createId(String.valueOf(emme2Count.inode));
			Id toNodeId = scenario.createId(String.valueOf(emme2Count.jnode));

			Node fromNode = scenario.getNetwork().getNodes().get(fromNodeId);
			Node toNode = scenario.getNetwork().getNodes().get(toNodeId);

			Link link = null;
			if (fromNode == null || toNode == null) {
				// try searching using original nodes
				if (this.nodesFile != null) link = searchUsingOriginalNodes(fromNodeId, toNodeId);
				
				// if link is still null try searching in transformed links
				if (link == null) link = searchTransformedLink(emme2Count, fromNode, toNode);
			}
			else link = searchLink(emme2Count, fromNode, toNode);

			if (link != null) {
				Count count = counts.getCount(link.getId());
				
				if (count == null) count = counts.createCount(link.getId(), emme2Count.inode + "_" + emme2Count.jnode);
				
				count.createVolume(emme2Count.hour, emme2Count.value);
				
				if (link.getCapacity() < emme2Count.value) {
					log.warn("Links capacity is exceeded. Link " + link.getId() + 
							" (from Node " + link.getFromNode().getId() +
							" to Node " + link.getToNode().getId() + ")" +
							", capacity=" + link.getCapacity() +
							", count value=" + emme2Count.value + " in hour " + emme2Count.hour);
				}
				
				counter.incCounter();
			}
			else log.warn("Link from Node " + fromNodeId + " to Node " + toNodeId + " could not be found!");
		}
		counter.printCounter();
		log.info("Input file contained count information for " + counts.getCounts().size() + " links.");
		new CountsWriter(counts).write(outFile);
	}

	private Link searchLink(CountV2 emme2Count, Node fromNode, Node toNode) {
		for (Link link : fromNode.getOutLinks().values()) {
			if (link.getToNode().getId().equals(toNode.getId())) {
				return link;
			}
		}
		return null;
	}

	private void buildNodesQuadTree() {

		double startTime = System.currentTimeMillis();
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		for (Node n : this.scenario.getNetwork().getNodes().values()) {
			if (n.getCoord().getX() < minx) { minx = n.getCoord().getX(); }
			if (n.getCoord().getY() < miny) { miny = n.getCoord().getY(); }
			if (n.getCoord().getX() > maxx) { maxx = n.getCoord().getX(); }
			if (n.getCoord().getY() > maxy) { maxy = n.getCoord().getY(); }
		}
		minx -= 1.0;
		miny -= 1.0;
		maxx += 1.0;
		maxy += 1.0;
		log.info("building QuadTree for nodes: xrange(" + minx + "," + maxx + "); yrange(" + miny + "," + maxy + ")");
		QuadTree<Node> quadTree = new QuadTree<Node>(minx, miny, maxx, maxy);
		for (Node n : this.scenario.getNetwork().getNodes().values()) {
			quadTree.put(n.getCoord().getX(), n.getCoord().getY(), n);
		}
		/* assign the quadTree at the very end, when it is complete.
		 * otherwise, other threads may already start working on an incomplete quadtree
		 */
		this.nodeQuadTree = quadTree;
		log.info("Building QuadTree took " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
	}
	
	private Link searchUsingOriginalNodes(Id fromNodeId, Id toNodeId) {
				
		Collection<Node> potentialFromNodes = new ArrayList<Node>();
		Collection<Node> potentialToNodes = new ArrayList<Node>();
		
		Node fromNode = scenario.getNetwork().getNodes().get(fromNodeId);
		Node toNode = scenario.getNetwork().getNodes().get(toNodeId);
		
		if (fromNode == null) {
			fromNode = this.originalNodes.get(fromNodeId);
			if (fromNode == null) {
				log.warn("fromNode is not contained in the original network!");
				return null;
			}
			Coord fromCoord = fromNode.getCoord();
			
			potentialFromNodes = this.nodeQuadTree.get(fromCoord.getX(), fromCoord.getY(), 10.0);			
			
		} else potentialFromNodes.add(fromNode);
		
		if (toNode == null) {
			toNode = this.originalNodes.get(toNodeId);
			if (toNode == null) {
				log.warn("toNode is not contained in the original network!");
				return null;
			}
			Coord toCoord = toNode.getCoord();
			
			potentialToNodes = this.nodeQuadTree.get(toCoord.getX(), toCoord.getY(), 10.0);
			
		} else potentialToNodes.add(toNode);
		
		for (Node potentialFromNode : potentialFromNodes) {
			if (!potentialFromNode.getId().toString().contains(fromNodeId.toString())) continue;
				
			for (Node potentialToNode : potentialToNodes) {
				if (!potentialToNode.getId().toString().contains(toNodeId.toString())) continue;
				
				for (Link potentialLink : potentialFromNode.getOutLinks().values()) {
					if (potentialLink.getToNode().equals(potentialToNode)) {
						return potentialLink;
					}
				}
			}
		}
		
		log.warn("Link from Node " + fromNodeId + " to Node " + toNodeId + " could not be found using original node data!");
		return null;
	}
	
	/*
	 * If the Link cannot be found it may have be transformed to
	 * represent turning conditions. In that case we try to find
	 * the new created link that fits best to the original one.
	 */
	private Link searchTransformedLink(CountV2 emme2Count, Node fromNode, Node toNode) {
		List<Node> possibleFromNodes = new ArrayList<Node>();
		List<Node> possibleToNodes = new ArrayList<Node>();
		List<Link> possibleLinks = new ArrayList<Link>();

		if (fromNode == null) {
			/*
			 * fromNode not found - so maybe it has been transformed to represent
			 * turn conditions...
			 */
			int i = 0;
			while (true) {
				Id nextId = scenario.createId(String.valueOf(emme2Count.inode) + "-" + i++);
				Node nextNode = scenario.getNetwork().getNodes().get(nextId);
				if (nextNode == null && i > 20) break;
				else if (nextNode == null) continue;
				else possibleFromNodes.add(nextNode);
			}
		}
		else possibleFromNodes.add(fromNode);

		if (possibleFromNodes.size() == 0) {
			log.info("No potential FromNode found!");
			return null;
		}

		if (toNode == null) {
			/*
			 * toNode not found - so maybe it has been transformed to represent
			 * turn conditions...
			 */
			int i = 0;
			while (true) {
				Id nextId = scenario.createId(String.valueOf(emme2Count.jnode) + "-" + i++);
				Node nextNode = scenario.getNetwork().getNodes().get(nextId);
				if (nextNode == null && i > 20) break;
				else if (nextNode == null) continue;
				else possibleToNodes.add(nextNode);
			}
		}
		else possibleToNodes.add(toNode);

		if (possibleToNodes.size() == 0) {
			log.info("No potential ToNode found!");
			return null;
		}

		/*
		 * Search all possible Links
		 */
		for (Node possibleFromNode : possibleFromNodes) {
			for (Node possibleToNode : possibleToNodes) {
				Link possibleLink = searchLink(emme2Count, possibleFromNode, possibleToNode);
				if (possibleLink != null) possibleLinks.add(possibleLink);
			}
		}

		// If no possible Link has been found we give it up...
		if (possibleLinks.size() == 0) return null;

		/*
		 * Look for the longest among the possible Links
		 */
		Link longestLink = possibleLinks.get(0);
		for (Link link : possibleLinks) {
			if (link.getLength() > longestLink.getLength()) longestLink = link;
		}
		if (longestLink.getLength() < 1.0) log.info("Link is very short...");
		return longestLink;
	}
}
