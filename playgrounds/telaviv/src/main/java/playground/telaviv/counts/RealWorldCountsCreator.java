/* *********************************************************************** *
 * project: org.matsim.*
 * RealWorldCountsCreator.java
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
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.collections.QuadTree;
import org.matsim.core.utils.misc.Counter;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

import playground.telaviv.config.XMLParameterParser;
import playground.telaviv.network.LinkMappingTool;

/**
 * <p>
 * This class creates a MATSim counts file based on real world count data.
 * </p>
 * <p>
 * When creating the MATSim Network some links of the EMME/2 network have to be
 * converted due to the fact that they contain turning conditions that cannot
 * be directly converted to the MATSim links. Therefore not for all links a
 * 1:1 mapping is possible. In those cases the link with the best fit is
 * searched. (Alternatively the mapping could be done via the origId Tag of the
 * link. This attribute could contain the Ids of the original nodes).
 * </p>
 * 
 * @author cdobler
 */
public class RealWorldCountsCreator {

	private static final Logger log = Logger.getLogger(RealWorldCountsCreator.class);
	
	private String basePath = "";
	private String networkFile = "";
	private String originalNetworkFile = "";
	private String inputFile = "";
	private String outputFile = "";
	private String separator = "";
		
	public static void main(String[] args) {
		try {
			if (args.length > 0) {
				String file = args[0];
				new RealWorldCountsCreator(file);
			} else {
				log.error("No input config file was given. Therefore cannot proceed. Aborting!");
				return;
			}
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	// NOT a MATSim config file!
	public RealWorldCountsCreator(String configurationFile) throws Exception {

		Map<String, String> parameterMap = new XMLParameterParser().parseFile(configurationFile);
		String value;
		
		value = parameterMap.remove("basePath");
		if (value != null) basePath = value;
		
		value = parameterMap.remove("networkFile");
		if (value != null) networkFile = value;
		
		value = parameterMap.remove("originalNetworkFile");
		if (value != null) originalNetworkFile = value;
		
		value = parameterMap.remove("inputFile");
		if (value != null) inputFile = value;
		
		value = parameterMap.remove("separator");
		if (value != null) separator = value;
		
		value = parameterMap.remove("outputFile");
		if (value != null) outputFile = value;
		
		for (String key : parameterMap.keySet()) log.warn("Found parameter " + key + " which is not handled!");
		
		Config config = ConfigUtils.createConfig(); 
		config.network().setInputFile(basePath + networkFile);
		Scenario scenario = ScenarioUtils.loadScenario(config);
		Network network = scenario.getNetwork();
		
		Config originalConfig = ConfigUtils.createConfig(); 
		originalConfig.network().setInputFile(basePath + originalNetworkFile);
		Scenario originalScenario = ScenarioUtils.loadScenario(originalConfig);
		Network originalNetwork = originalScenario.getNetwork();
		
		Counts counts = new Counts();
		counts.setName("Real world counts for Tel Aviv Model");
		counts.setDescription("Real world counts for Tel Aviv Model");
		counts.setYear(2012);
		
		QuadTree<Node> quadTree = LinkMappingTool.buildNodesQuadTree(scenario.getNetwork());

		List<RealWorldCount> emme2Counts = new RealWorldCountsFileParser(separator).readFile(basePath + inputFile);	
		
		int linkExists = 0;
		int searchByOriginalNodes = 0;
		int searchTransformedLink = 0;
		Counter counter = new Counter("Count stations mapped to MATSim network: #");
		for (RealWorldCount realWorldCount : emme2Counts) {
			Id<Node> fromNodeId = Id.create(String.valueOf(realWorldCount.inode), Node.class);
			Id<Node> toNodeId = Id.create(String.valueOf(realWorldCount.jnode), Node.class);

			Node fromNode = scenario.getNetwork().getNodes().get(fromNodeId);
			Node toNode = scenario.getNetwork().getNodes().get(toNodeId);
			
			Link link = null;
			if (fromNode == null || toNode == null) {
				// try searching using original nodes
				link = LinkMappingTool.searchUsingOriginalNodes(fromNodeId, toNodeId, network, originalNetwork, quadTree);
				
				// if link is still null try searching in transformed links
				if (link == null) {
					searchTransformedLink++;
					link = searchTransformedLink(realWorldCount, fromNode, toNode, scenario);
				} else {
					searchByOriginalNodes++;
				}
			}
			else {
				linkExists++;
				link = searchLink(fromNode, toNode);
			}

			if (link != null) {
				Count count = counts.getCount(link.getId());
				
				if (count == null) count = counts.createAndAddCount(link.getId(), realWorldCount.inode + "_" + realWorldCount.jnode);
				
				count.createVolume(realWorldCount.hour, realWorldCount.value);
				
				if (link.getCapacity() < realWorldCount.value) {
					log.warn("Links capacity is exceeded. Link " + link.getId() + 
							" (from Node " + link.getFromNode().getId() +
							" to Node " + link.getToNode().getId() + ")" +
							", capacity=" + link.getCapacity() +
							", count value=" + realWorldCount.value + " in hour " + realWorldCount.hour);
				}
				
				counter.incCounter();
			}
			else log.warn("Link from Node " + fromNodeId + " to Node " + toNodeId + " could not be found!");
		}
		counter.printCounter();
		log.info("Found " + linkExists + " count stations directly in the new network.");
		log.info("Found " + searchByOriginalNodes + " count stations by using the original network.");
		log.info("Found " + searchTransformedLink + " count stations by looking at the transformed links.");
		log.info("Input file contained count information for " + counts.getCounts().size() + " links.");
		new CountsWriter(counts).write(basePath + outputFile);
	}

	private Link searchLink(Node fromNode, Node toNode) {
		for (Link link : fromNode.getOutLinks().values()) {
			if (link.getToNode().getId().equals(toNode.getId())) {
				return link;
			}
		}
		return null;
	}

	/*
	 * If the Link cannot be found it may have be transformed to
	 * represent turning conditions. In that case we try to find
	 * the new created link that fits best to the original one.
	 */
	private Link searchTransformedLink(RealWorldCount realWorldCount, Node fromNode, Node toNode, Scenario scenario) {
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
				Id<Node> nextId = Id.create(String.valueOf(realWorldCount.inode) + "-" + i++, Node.class);
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
				Id<Node> nextId = Id.create(String.valueOf(realWorldCount.jnode) + "-" + i++, Node.class);
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

		// search all possible Links
		for (Node possibleFromNode : possibleFromNodes) {
			for (Node possibleToNode : possibleToNodes) {
				Link possibleLink = searchLink(possibleFromNode, possibleToNode);
				if (possibleLink != null) possibleLinks.add(possibleLink);
			}
		}

		// if no possible link has been found we give it up...
		if (possibleLinks.size() == 0) return null;

		/*
		 * Look for the longest among the possible Links
		 */
		Link longestLink = possibleLinks.get(0);
		for (Link link : possibleLinks) {
			if (link.getLength() > longestLink.getLength()) longestLink = link;
		}
		if (longestLink.getLength() < 1.0) log.warn("Link is very short. It might be the wrong one...");
		return longestLink;
	}
}
