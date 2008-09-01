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
 * Zielknoten oder -link liegen sollen gefunden und zurück gegeben werden.
 * Wird zusätzlich eine ArrayList mit Nodes übergeben, so wird diese erweitert.
 */

package playground.christoph.knowledge.nodeselection;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;
import java.util.TreeMap;

import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.geometry.Coord;


public class SelectNodesCircular extends BasicSelectNodesImpl{

	double distance;
	Node centerNode;
	Link centerLink;
	
	public SelectNodesCircular(NetworkLayer net)
	{
		this.network = net;
		distance = 0.0;
	}
	
	public ArrayList<Node> getNodes(Node center, double dist)
	{
		distance = dist;
		centerNode = center;
		centerLink = null;
		return getNodes();
	}
	
	public void getNodes(Node center, double dist, ArrayList<Node> nodeList)
	{
		distance = dist;
		centerNode = center;
		centerLink = null;
		getNodes(nodeList);
	}
	
	public ArrayList<Node> getNodes(Link link, double dist)
	{
		distance = dist;
		centerLink = link;
		centerNode = null;
		return getNodes();
	}
	
	public void getNodes(Link link, double dist, ArrayList<Node> nodeList)
	{
		distance = dist;
		centerLink = link;
		centerNode = null;
		getNodes(nodeList);
	}
	
	@Override
	public ArrayList<Node> getNodes()
	{
		ArrayList<Node> nodeList = new ArrayList<Node>();
		
		getNodes(nodeList);
		
		return nodeList;
	}
	
	@Override
	public void getNodes(ArrayList<Node> nodeList) {
		
		if(centerNode != null || centerLink != null)
		{
			if(nodeList == null) nodeList = new ArrayList<Node>();
			
			// Alle Knoten des Netzwerks holen
			TreeMap<Id, Node> nodeMap = (TreeMap<Id, Node>)network.getNodes();
			
			Iterator nodeIterator = nodeMap.entrySet().iterator();
			
			while(nodeIterator.hasNext())
			{
				// Wir wissen ja, was für Elemente zurückgegeben werden :)
				Map.Entry<Id, Node> nextNode = (Map.Entry<Id, Node>)nodeIterator.next();
				//Id id = nextLink.getKey();
				Node node = nextNode.getValue();
				
				Coord coord = node.getCoord();
	
				double dist;
				
				if(centerNode != null) dist = centerNode.getCoord().calcDistance(coord);
				else dist = centerLink.calcDistance(coord);
					
				// Innerhalb des Bereichs?
				if (dist <= distance)
				{
					// Knoten in Liste speichern, falls dort noch nicht hinterlegt
					if (!nodeList.contains(node)) nodeList.add(node);
				}
				
			}	// while nodeIterator.hasNext()	
		
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
	
}
