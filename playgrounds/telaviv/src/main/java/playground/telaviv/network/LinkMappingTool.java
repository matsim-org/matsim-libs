/* *********************************************************************** *
 * project: org.matsim.*
 * LinkMappingTool.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package playground.telaviv.network;

import java.util.ArrayList;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.collections.QuadTree;

/**
 * <p>
 * In contrast to EMME/2 MATSim's network format does NOT support turning
 * restrictions. They are realized by adapting the network structure. Therefore,
 * the MATSim network is not a 1:1 copy of the EMME/2 network. As a result, not
 * all links and nodes can be mapped directly. This class provides tools to
 * help identifying a links representation in the MATSim network.
 * </p> 
 * 
 * @author cdobler
 */
public class LinkMappingTool {
	
	private static final Logger log = Logger.getLogger(LinkMappingTool.class);
	
	public static Link searchUsingOriginalNodes(Id fromNodeId, Id toNodeId, Network network, Network originalNetwork, QuadTree<Node> quadTree) {
		
		Collection<Node> potentialFromNodes = new ArrayList<Node>();
		Collection<Node> potentialToNodes = new ArrayList<Node>();
		
		Node fromNode = network.getNodes().get(fromNodeId);
		Node toNode = network.getNodes().get(toNodeId);
		
		if (fromNode == null) {
			fromNode = originalNetwork.getNodes().get(fromNodeId);
			if (fromNode == null) {
				log.warn("fromNode is not contained in the original network!");
				return null;
			}
			Coord fromCoord = fromNode.getCoord();
			
			potentialFromNodes = quadTree.getDisk(fromCoord.getX(), fromCoord.getY(), 10.0);
			
		} else potentialFromNodes.add(fromNode);
		
		if (toNode == null) {
			toNode = originalNetwork.getNodes().get(toNodeId);
			if (toNode == null) {
				log.warn("toNode is not contained in the original network!");
				return null;
			}
			Coord toCoord = toNode.getCoord();
			
			potentialToNodes = quadTree.getDisk(toCoord.getX(), toCoord.getY(), 10.0);
			
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

	/**
	 * Use the MATSim network as input here! The created QuadTree can be used in
	 * combination with the searchUsingOriginalNodes(...) method. There, the coordinate
	 * of the original node is taken from the original network. Then, the QuadTree
	 * is used to identify nodes close to that location from the converted network.
	 *  
	 * @return
	 */
	public static QuadTree<Node> buildNodesQuadTree(Network network) {

		double startTime = System.currentTimeMillis();
		
		double minx = Double.POSITIVE_INFINITY;
		double miny = Double.POSITIVE_INFINITY;
		double maxx = Double.NEGATIVE_INFINITY;
		double maxy = Double.NEGATIVE_INFINITY;
		
		for (Node n : network.getNodes().values()) {
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
		for (Node n : network.getNodes().values()) {
			quadTree.put(n.getCoord().getX(), n.getCoord().getY(), n);
		}
		/* assign the quadTree at the very end, when it is complete.
		 * otherwise, other threads may already start working on an incomplete quadtree
		 */
		log.info("Building QuadTree took " + ((System.currentTimeMillis() - startTime) / 1000.0) + " seconds.");
		
		return quadTree;
	}
}
