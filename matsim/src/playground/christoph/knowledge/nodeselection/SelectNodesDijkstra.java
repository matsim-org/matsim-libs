/* *********************************************************************** *
 * project: org.matsim.*
 * SelectNodesDijkstra.java
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

package playground.christoph.knowledge.nodeselection;

import java.util.ArrayList;
import java.util.Map;

import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.TravelCost;
import org.matsim.utils.misc.Time;

import playground.christoph.knowledge.utils.GetAllNodes;

/**
 * Selects Nodes by using a Dijkstra Algorithm. Nodes are included, if
 * they lie on routes that an agent can use to get from the start node to 
 * the end node within a given cost limit.
 */
public class SelectNodesDijkstra extends BasicSelectNodesImpl{

	Node startNode;
	Node endNode;
	double maxDist = Double.MAX_VALUE;	
	TravelCost costCalculator = new FreespeedTravelTimeCost();	// Kostenrechner
	double time = Time.UNDEFINED_TIME;	// Zeit für den Kostenrechner
	
	public SelectNodesDijkstra(NetworkLayer network, Node startNode, Node endNode, double maxDist)
	{
		this.network = network;
		this.startNode = startNode;
		this.endNode = endNode;
		this.maxDist = maxDist;
	}
	
	public void setStartNode(Node startNode)
	{
		this.startNode = startNode;
	}
	
	public void setEndNode(Node endNode)
	{
		this.endNode = endNode;
	}
	
	public void setMaxDist(double maxDist)
	{
		this.maxDist = maxDist;
	}
	
	public void setCostCalculator(TravelCost calculator)
	{
		costCalculator = calculator;
	}
	
	public TravelCost getCostCalculator()
	{
		return costCalculator;
	}
	
	public void setCalculationTime(double time)
	{
		this.time = time;
	}
	
	public double getCalculationTime()
	{
		return time;
	}
	
	@Override
	public ArrayList<Node> getNodes() {
		
		ArrayList<Node> nodes = new ArrayList<Node>();
		getNodes(nodes);
		
		return nodes;
	}

	@Override
	public void getNodes(ArrayList<Node> nodes)
	{
		DijkstraForSelectNodes dijkstra = new DijkstraForSelectNodes(this.network);
		dijkstra.setCostCalculator(costCalculator);
		dijkstra.setCalculationTime(time);
		
		dijkstra.executeNetwork(startNode);
		Map<Node, Double> startMap = dijkstra.getMinDistances();
		
		dijkstra.executeNetwork(endNode);
		Map<Node, Double> endMap = dijkstra.getMinDistances();
		
		// Wege berechnen
		//Map<Node, Double> distances = new HashMap<Node, Double>();
		
		System.out.println("Kürzeste Weglänge vom Start zum Ende: " + startMap.get(endNode));
		
		// Alles Nodes des Netzwerks holen
		ArrayList<Node> networkNodes = new GetAllNodes().getAllNodes(network);
		
		for(int i = 0; i < networkNodes.size(); i++)
		{
			Node node = networkNodes.get(i);
			
			// Konten in beiden Maps vorhanden?
			if (startMap.containsKey(node) && endMap.containsKey(node))
			{
				double dist = startMap.get(node) + endMap.get(node);
				
				//System.out.println("NodeID..." + node.getId().toString() + " Dist..." + dist);
				
				// Distanz kleiner als die zugelassene?
				//if (dist < maxDist) distances.put(node, dist);
				if (dist < maxDist && !nodes.contains(node)) nodes.add(node);
			}
		}
		//return distances;
	}
	
}
