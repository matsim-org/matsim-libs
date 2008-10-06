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

import org.apache.log4j.Logger;
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
	double costFactor = Double.MAX_VALUE;	
	TravelCost costCalculator = new FreespeedTravelTimeCost();	// CostCalculator
	double time = Time.UNDEFINED_TIME;	// time for the CostCalculator
	ArrayList<Node> networkNodes;
	DijkstraForSelectNodes dijkstra;
	
	private static final Logger log = Logger.getLogger(SelectNodesDijkstra.class);
	
	public SelectNodesDijkstra(NetworkLayer network)
	{
		this.network = network;
		
		// get all nodes of the network
		networkNodes = new GetAllNodes().getAllNodes(network);
		
		dijkstra = new DijkstraForSelectNodes(this.network, networkNodes);
	}
	
	public SelectNodesDijkstra(NetworkLayer network, Node startNode, Node endNode, double costFactor)
	{
		this.network = network;
		this.startNode = startNode;
		this.endNode = endNode;
		this.costFactor = costFactor;
		
		// get all nodes of the network
		networkNodes = new GetAllNodes().getAllNodes(network);
		
		dijkstra = new DijkstraForSelectNodes(this.network, networkNodes);
	}
	
	public void setStartNode(Node startNode)
	{
		this.startNode = startNode;
	}
	
	public void setEndNode(Node endNode)
	{
		this.endNode = endNode;
	}
	
	public void setCostFactor(double costFactor)
	{
		this.costFactor = costFactor;
	}
	
	public void setCostCalculator(TravelCost calculator)
	{
		costCalculator = calculator;
		dijkstra.setCostCalculator(costCalculator);
	}
	
	public TravelCost getCostCalculator()
	{
		return costCalculator;
	}
	
	public void setCalculationTime(double time)
	{
		this.time = time;
		dijkstra.setCalculationTime(time);
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
		dijkstra.executeNetwork(startNode);
		Map<Node, Double> startMap = dijkstra.getMinDistances();
		
		dijkstra.executeNetwork(endNode);
		Map<Node, Double> endMap = dijkstra.getMinDistances();
		
		// calculate distances
		//Map<Node, Double> distances = new HashMap<Node, Double>();
		
		//log.info("Minimal costs from start- to endnode: " + startMap.get(endNode));
		
		// get the minimal costs to get from the start- to the endnode
		double minCosts = startMap.get(endNode);

		for(int i = 0; i < networkNodes.size(); i++)
		{
			Node node = networkNodes.get(i);
			
			// if the node exists in start- and endMap
			if (startMap.containsKey(node) && endMap.containsKey(node))
			{
				double cost = startMap.get(node) + endMap.get(node);
				
				//System.out.println("NodeID..." + node.getId().toString() + " Dist..." + dist);
				
				/* 
				 * If the costs are smaller than the specified limit and 
				 * the node isn't already in the nodeslist -> add it.
				 */
				if (cost < minCosts*costFactor && !nodes.contains(node)) nodes.add(node);
			}
		}
		//return distances;
	}
	
}
