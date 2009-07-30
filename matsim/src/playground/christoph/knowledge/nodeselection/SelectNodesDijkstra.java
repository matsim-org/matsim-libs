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
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.experimental.network.Node;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.utils.misc.Time;

/**
 * Selects Nodes by using a Dijkstra Algorithm. Nodes are included, if
 * they lie on routes that an agent can use to get from the start node to 
 * the end node within a given cost limit.
 *  
 *  @author Christoph Dobler
 */
public class SelectNodesDijkstra extends BasicSelectNodesImpl{

	NodeImpl startNode;
	NodeImpl endNode;
	double costFactor = Double.MAX_VALUE;	
	TravelCost costCalculator = new FreespeedTravelTimeCost();	// CostCalculator
	double time = Time.UNDEFINED_TIME;	// time for the CostCalculator
	Map<Id, NodeImpl> networkNodesMap;
	DijkstraForSelectNodes dijkstra;
	
	private static final Logger log = Logger.getLogger(SelectNodesDijkstra.class);
	
	public SelectNodesDijkstra(NetworkLayer network)
	{
		this.network = network;
		
		// get all nodes of the network
		//this.networkNodesMap = new GetAllNodes().getAllNodes(network);
		this.networkNodesMap = network.getNodes();
		
		//this.networkNodesMap = new HashMap<Id, Node>(); 
		//this.networkNodesMap.putAll(network.getNodes());
			
		this.dijkstra = new DijkstraForSelectNodes(this.network, networkNodesMap);
		this.dijkstra.createTravelCostLookupTable();
	}
	
	/* 
	 * Uses already existing Map of the networks nodes.
	 * For examples used when creating a clone. 
	 * In general these maps are static, so it is no problem to share the map.
	 */
	public SelectNodesDijkstra(NetworkLayer network, Map<Id, NodeImpl> networkNodesMap)
	{
		this.network = network;
		this.networkNodesMap = networkNodesMap;		
		this.dijkstra = new DijkstraForSelectNodes(this.network, networkNodesMap);
		this.dijkstra.createTravelCostLookupTable();
	}
	
	public SelectNodesDijkstra(NetworkLayer network, NodeImpl startNode, NodeImpl endNode, double costFactor)
	{
		this.network = network;
		this.startNode = startNode;
		this.endNode = endNode;
		this.costFactor = costFactor;
		
		// get all nodes of the network
		//this.networkNodesMap = new GetAllNodes().getAllNodes(network);
		this.networkNodesMap = network.getNodes();
		
		//this.networkNodesMap = new HashMap<Id, Node>(); 
		//this.networkNodesMap.putAll(network.getNodes());
		
		this.dijkstra = new DijkstraForSelectNodes(this.network, networkNodesMap);
		this.dijkstra.createTravelCostLookupTable();
	}
	
	public void setStartNode(NodeImpl startNode)
	{
		this.startNode = startNode;
	}
	
	public void setEndNode(NodeImpl endNode)
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
	//public ArrayList<Node> getNodes() {
	public Map<Id, NodeImpl> getNodes() {
		
		Map<Id, NodeImpl> nodesMap = new TreeMap<Id, NodeImpl>();
		addNodesToMap(nodesMap);
		
		return nodesMap;
	}

	@Override
	public void addNodesToMap(Map<Id, NodeImpl> nodesMap)
	{	
/*
		if (startNode.getId() == endNode.getId())
		{
			log.warn("StartNode == EndNode!");
		}
*/		
		dijkstra.executeForwardNetwork(startNode);
		Map<NodeImpl, Double> startMap = dijkstra.getMinDistances();
		
		dijkstra.executeBackwardNetwork(endNode);
		Map<NodeImpl, Double> endMap = dijkstra.getMinDistances();
		
		// get the minimal costs to get from the start- to the endnode
		double minCostsStart = startMap.get(endNode);
		double minCostsEnd = endMap.get(startNode);
		
		if (minCostsStart / minCostsEnd < 0.95 || minCostsStart / minCostsEnd > 1.05)
		{
			log.warn("Different Costs in different Traveldirection (> 5% Difference found)!");
		}
				
		double minCosts;
		if (minCostsStart > minCostsEnd) minCosts = minCostsStart;
		else minCosts = minCostsEnd;

//		if (minCosts == 0.0) log.warn("MinCosts: " + minCosts);

		List<Node> nodes = new ArrayList<Node>();
		// iterate over Array or Iteratable 
		for (NodeImpl node : networkNodesMap.values())
		{	

			
			// if the node exists in start- and endMap
			if (startMap.containsKey(node) && endMap.containsKey(node))
			{
				double cost = startMap.get(node) + endMap.get(node);

				/* 
				 * If the costs are smaller than the specified limit and 
				 * the node isn't already in the nodeslist -> add it.
				 */
				//if (cost < minCosts*costFactor && !nodesMap.containsKey(node.getId())) nodesMap.put(node.getId(), node);
				if (cost <= minCosts * costFactor)
				{
					nodesMap.put(node.getId(), node);
					nodes.add(node);
				}
			}
			else
			{
				if (!startMap.containsKey(node)) log.error("Node was not found in StartMap!");
				else log.error("Node was not found in EndMap!");
			}

		}
		if (nodes.size() == 0)
		{
			log.error("no nodes...");
		}
		startMap.clear();
		endMap.clear();
	}
	
	@Override
	public SelectNodesDijkstra clone()
	{
		//Map<Id, Node> nodesClone = new HashMap<Id, Node>(); 
		//nodesClone.putAll(networkNodesMap);
		//SelectNodesDijkstra clone = new SelectNodesDijkstra(this.network, nodesClone);
		
		SelectNodesDijkstra clone = new SelectNodesDijkstra(this.network, networkNodesMap);
		clone.setCostFactor(this.costFactor);
		clone.setCostCalculator(this.costCalculator);
		
		return clone;
	}
}