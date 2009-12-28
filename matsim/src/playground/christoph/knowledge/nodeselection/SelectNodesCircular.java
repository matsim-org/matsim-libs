/* *********************************************************************** *
 * project: org.matsim.*
 * SelectNodesCircular.java
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

/**
 * @author Christoph Dobler
 * 
 * Ziel: Alle Knoten die innerhalb eines vorgegebenen Abstandes zu einem
 * Zielknoten oder -link liegen sollen gefunden und zur�ck gegeben werden.
 * Wird zus�tzlich eine ArrayList mit Nodes �bergeben, so wird diese erweitert.
 */

package playground.christoph.knowledge.nodeselection;

import java.util.Map;
import java.util.TreeMap;

import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.utils.geometry.CoordUtils;

public class SelectNodesCircular extends BasicSelectNodesImpl{

	double distance;
	Node centerNode;
	Link centerLink;
	
	public SelectNodesCircular(Network net)
	{
		this.network = net;
		distance = 0.0;
	}
	
	public Map<Id, Node> getNodes(Node center, double dist)
	{
		distance = dist;
		centerNode = center;
		centerLink = null;
		return getNodes();
	}
	
	public void getNodes(Node center, double dist, Map<Id, Node> nodesMap)
	{
		distance = dist;
		centerNode = center;
		centerLink = null;
		addNodesToMap(nodesMap);
	}
	
	public Map<Id, Node> getNodes(Link link, double dist)
	{
		distance = dist;
		centerLink = link;
		centerNode = null;
		return getNodes();
	}
	
	public void getNodes(Link link, double dist, Map<Id, Node> nodesMap)
	{
		distance = dist;
		centerLink = link;
		centerNode = null;
		addNodesToMap(nodesMap);
	}
	
	@Override
	public Map<Id, Node> getNodes()
	{
		Map<Id, Node> nodesMap = new TreeMap<Id, Node>();
		addNodesToMap(nodesMap);
		
		return nodesMap;
	}
	
	@Override
	public void addNodesToMap(Map<Id, Node> nodesMap) 
	{	
		if(centerNode != null || centerLink != null)
		{
			if(nodesMap == null) nodesMap = new TreeMap<Id, Node>();
			
			// get all nodes of the network
			Map<Id,? extends Node> networkNodesMap = network.getNodes();
			
			// iterate over Array or Iteratable 
			for (Node node : networkNodesMap.values())
			{
				Coord coord = node.getCoord();
	
				double dist;
				
				if(centerNode != null) dist = CoordUtils.calcDistance(centerNode.getCoord(), coord);
				else 
				{
					// if it is a LinkImpl we use its calcDistance Method
					if (centerLink instanceof LinkImpl)
					{
						dist = ((LinkImpl)centerLink).calcDistance(coord);
					}
					// so we use instead the global calcDistance Method
					else dist = CoordUtils.calcDistance(centerLink.getCoord(), coord);
				}
					
				// within the distance?
				if (dist <= distance)
				{
					// add node to the Map, if he is not already included
					if (!nodesMap.containsKey(node.getId())) nodesMap.put(node.getId(), node);
				}
				
			}	// for iterator
		
		}	// if 
	}

	public double getDistance() {
		return distance;
	}

	public void setDistance(double distance) {
		this.distance = distance;
	}

	public Node getNode() {
		return centerNode;
	}

	public void setNode(Node centerNode) {
		centerLink = null;
		this.centerNode = centerNode;
	}

	public Link getLink() {
		return centerLink;
	}

	public void setLink(Link centerLink) {
		centerNode = null;
		this.centerLink = centerLink;
	}
	
	@Override
	public SelectNodesCircular clone()
	{
		SelectNodesCircular clone = new SelectNodesCircular(this.network);
	
		return clone;
	}
	
}
