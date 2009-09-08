/******************************************************************************
 *project: org.matsim.*
 * AreaExtractor.java
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 * copyright       : (C) 2009 by the members listed in the COPYING,           *
 *                   LICENSE and WARRANTY file.                               *
 * email           : info at matsim dot org                                   *
 *                                                                            *
 * ************************************************************************** *
 *                                                                            *
 *   This program is free software; you can redistribute it and/or modify     *
 *   it under the terms of the GNU General Public License as published by     *
 *   the Free Software Foundation; either version 2 of the License, or        *
 *   (at your option) any later version.                                      *
 *   See also COPYING, LICENSE and WARRANTY file                              *
 *                                                                            *
 ******************************************************************************/


package playground.rost.controller;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NetworkWriter;
import org.matsim.core.network.NodeImpl;

import playground.rost.graph.Border;
import playground.rost.graph.BoundingBox;
import playground.rost.graph.GraphAlgorithms;
import playground.rost.graph.evacarea.EvacArea;

public class AreaExtractor {
	
	private static final Logger log = Logger.getLogger(AreaExtractor.class);
	
	public static NetworkLayer extractNetwork(Border border, NetworkLayer network, Set<String> evacAreaNodeIds, Set<String> evacBorderNodeIds)
	{
		boolean fromInHull;
		boolean toInHull;
		boolean deleteNode;
		Map<NodeImpl,Boolean> nodeInHull = new HashMap<NodeImpl, Boolean>();
		Set<LinkImpl> linksToRemove = new HashSet<LinkImpl>();
		Set<NodeImpl> nodesToRemove = new HashSet<NodeImpl>();
		Set<NodeImpl> explicitlyDoNotRemoveNode = new HashSet<NodeImpl>();
		List<Node> hull = border.getDistHull();
		BoundingBox bBox = new BoundingBox();
		bBox.run(hull);
		for(LinkImpl link : network.getLinks().values())
		{
			NodeImpl from = link.getFromNode();
			NodeImpl to = link.getToNode();
			if(!nodeInHull.containsKey(from))
			{
				fromInHull = GraphAlgorithms.pointIsInPolygon(hull, from, bBox);
				nodeInHull.put(from, fromInHull);
			}
			else
			{
				fromInHull = nodeInHull.get(from);
			}
			if(!nodeInHull.containsKey(to))
			{
				toInHull = GraphAlgorithms.pointIsInPolygon(hull, to, bBox);
				nodeInHull.put(to, toInHull);
			}
			else
			{
				toInHull = nodeInHull.get(to);
			}
			if(fromInHull && toInHull)
			{
				continue;
			}
			else if (!fromInHull && !toInHull)
			{
				linksToRemove.add(link);
			}
			else
			{
				if(!fromInHull || hull.contains(from))
					explicitlyDoNotRemoveNode.add(from);
				if(!toInHull || hull.contains(to))
					explicitlyDoNotRemoveNode.add(to);
			}
		}
		for(LinkImpl toDelete : linksToRemove)
		{
			network.removeLink(toDelete);
		}
		for(NodeImpl node : network.getNodes().values())
		{
			if(nodeInHull.containsKey(node))
			{
				deleteNode = !nodeInHull.get(node);
			}
			else
			{
				deleteNode = !GraphAlgorithms.pointIsInPolygon(hull, node);
			}
			if(deleteNode)
			{
				nodesToRemove.add(node);
			}
		}
		for(NodeImpl toDelete : nodesToRemove)
		{
			if(!explicitlyDoNotRemoveNode.contains(toDelete))
				network.removeNode(toDelete);
		}
		for(NodeImpl node : nodeInHull.keySet())
		{
			if(nodeInHull.get(node))
			{
				evacAreaNodeIds.add(node.getId().toString());
			}
		}
		for(NodeImpl node : explicitlyDoNotRemoveNode)
		{
			evacBorderNodeIds.add(node.getId().toString());
		}
		for(Node n : hull)
		{
			if(network.getNode(n.getId()) == null)
				throw new RuntimeException("border node is not contained in output network");
		}
		return network;
	}
	
	public static void extractNetworkAndWriteIntoFile(Border border, NetworkLayer network, String networkFile, String evacAreaFile)
	{
		Set<String> nodeIdsInArea = new HashSet<String>();
		Set<String> nodeIdsBorder = new HashSet<String>();
		extractNetwork(border, network, nodeIdsInArea, nodeIdsBorder);
		//write Network
		NetworkWriter writer = new NetworkWriter(network, networkFile);
		writer.write(); 
		//write EvacArea
		EvacArea eArea = new EvacArea(nodeIdsInArea, nodeIdsBorder, border.getDistHull());
		try
		{
			eArea.writeXMLFile(evacAreaFile);
		}
		catch(Exception ex)
		{
			log.debug(ex.getCause());
			log.debug(ex.getMessage());
			log.debug(ex.getStackTrace());
		}
		
	}
}
