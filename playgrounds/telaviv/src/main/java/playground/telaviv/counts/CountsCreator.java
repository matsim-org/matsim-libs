/* *********************************************************************** *
 * project: org.matsim.*
 * CountsCreator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.counts.Count;
import org.matsim.counts.Counts;
import org.matsim.counts.CountsWriter;

/*
 * Assigning the counts from a Emme2Model to a MATSim Network.
 * 
 * When creating the MATSim Network some Links of the Emme2Network have to be
 * converted due to the fact that they contain turning conditions that cannot
 * be directly converted to the MATSim Links. Therefore not for all Links a 
 * 1:1 mapping is possible. In those cases the link with the best fit is
 * searched. (Alternatively the mapping could be done via the origId Tag of the
 * link. This Attribute could contain the Ids of the original nodes).
 */
public class CountsCreator {

	private String networkFile = "../../matsim/mysimulations/telaviv/network/network.xml";
//	private String countsFile = "../../matsim/mysimulations/telaviv/counts/linkflows1000.csv";
	private String countsFile = "../../matsim/mysimulations/telaviv/counts/selected_flows.csv";
	private String outFile = "../../matsim/mysimulations/telaviv/counts/counts.xml";
	
	private Scenario scenario;
	
	public static void main(String[] args)
	{
		new CountsCreator(new ScenarioImpl()).createCounts();
	}
	
	public CountsCreator(Scenario scenario)
	{
		this.scenario = scenario;
		new MatsimNetworkReader(scenario).readFile(networkFile);
	}
	
	public void createCounts()
	{
		Counts counts = new Counts();
		counts.setName("Tel Aviv Model");
		counts.setDescription("Tel Aviv Model");
		counts.setYear(2000);
		counts.setLayer("0");
		
		List<Emme2Count> emme2Counts = new CountsFileParser(countsFile).readFile();
		
		for (Emme2Count emme2Count : emme2Counts)
		{
			Id fromNodeId = scenario.createId(String.valueOf(emme2Count.inode));
			Id toNodeId = scenario.createId(String.valueOf(emme2Count.jnode));
			
			Node fromNode = scenario.getNetwork().getNodes().get(fromNodeId);
			Node toNode = scenario.getNetwork().getNodes().get(toNodeId);
			
			Link link = null;
			if (fromNode == null || toNode == null) link = searchTransformedLink(emme2Count, fromNode, toNode);
			else link = searchLink(emme2Count, fromNode, toNode);
			
			if (link != null)
			{
				Count count = counts.createCount(link.getId(), link.getId().toString());
				
				count.createVolume(6, emme2Count.volau);
				count.createVolume(7, emme2Count.volau);
				count.createVolume(8, emme2Count.volau);
				count.createVolume(9, emme2Count.volau);				
			}
			else System.out.println("Link not found! From Node " + fromNodeId + " to Node " + toNodeId);
		}
		new CountsWriter(counts).write(outFile);
	}
	
	private Link searchLink(Emme2Count emme2Count, Node fromNode, Node toNode)
	{	
		for (Link link : fromNode.getOutLinks().values())
		{
			if (link.getToNode().getId().equals(toNode.getId()))
			{
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
	private Link searchTransformedLink(Emme2Count emme2Count, Node fromNode, Node toNode)
	{	
		List<Node> possibleFromNodes = new ArrayList<Node>();
		List<Node> possibleToNodes = new ArrayList<Node>();
		List<Link> possibleLinks = new ArrayList<Link>();
		
		if (fromNode == null)
		{
			/*
			 * fromNode not found - so maybe it has been transformed to represent
			 * turn conditions...
			 */
			int i = 0;
			while (true)
			{
				Id nextId = scenario.createId(String.valueOf(emme2Count.inode) + "-" + i++);
				Node nextNode = scenario.getNetwork().getNodes().get(nextId);
				if (nextNode == null && i > 20) break; 
				else if (nextNode == null) continue;
				else possibleFromNodes.add(nextNode);
			}
		}
		else possibleFromNodes.add(fromNode);
		
		if (possibleFromNodes.size() == 0)
		{
			System.out.println("No potential FromNode found!");
			return null;
		}
		
		if (toNode == null)
		{
			/*
			 * toNode not found - so maybe it has been transformed to represent
			 * turn conditions...
			 */
			int i = 0;
			while (true)
			{
				Id nextId = scenario.createId(String.valueOf(emme2Count.jnode) + "-" + i++);
				Node nextNode = scenario.getNetwork().getNodes().get(nextId);
				if (nextNode == null && i > 20) break; 
				else if (nextNode == null) continue;
				else possibleToNodes.add(nextNode);
			}
		}
		else possibleToNodes.add(toNode);
		
		if (possibleToNodes.size() == 0)
		{
			System.out.println("No potential ToNode found!");
			return null;
		}
		
		/*
		 * Search all possible Links
		 */
		for (Node possibleFromNode : possibleFromNodes)
		{
			for (Node possibleToNode : possibleToNodes)
			{
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
		for (Link link : possibleLinks)
		{
			if (link.getLength() > longestLink.getLength()) longestLink = link;
		}
		if (longestLink.getLength() < 1.0) System.out.println("Link is very short...");
		return longestLink;
	}
}
