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
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
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

	private Node startNode;
	private Node endNode;
	private double costFactor = Double.MAX_VALUE;
	private TravelCost costCalculator;
	private double time = Time.UNDEFINED_TIME;	// time for the CostCalculator
	private Map<Id,? extends Node> networkNodesMap;
	private DijkstraForSelectNodes dijkstra;
	
	private static final Logger log = Logger.getLogger(SelectNodesDijkstra.class);
	
	public SelectNodesDijkstra(Network network)
	{
		this.network = network;
		
		// get all nodes of the network
		this.networkNodesMap = network.getNodes();
					
		this.dijkstra = new DijkstraForSelectNodes(this.network, networkNodesMap);
	}
	
	/* 
	 * Uses already existing Map of the networks nodes.
	 * For examples used when creating a clone. 
	 * In general these maps are static, so it is no problem to share them.
	 */
	public SelectNodesDijkstra(Network network, Map<Id,? extends Node> networkNodesMap)
	{
		this.network = network;
		this.networkNodesMap = networkNodesMap;		
		this.dijkstra = new DijkstraForSelectNodes(this.network, networkNodesMap);
	}
	
	public SelectNodesDijkstra(Network network, Node startNode, Node endNode, double costFactor)
	{
		this.network = network;
		this.startNode = startNode;
		this.endNode = endNode;
		this.costFactor = costFactor;
		
		// get all nodes of the network
		this.networkNodesMap = network.getNodes();
		
		this.dijkstra = new DijkstraForSelectNodes(this.network, networkNodesMap);
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
		this.costCalculator = calculator;
		this.dijkstra.setCostCalculator(costCalculator);
	}
	
	public TravelCost getCostCalculator()
	{
		return costCalculator;
	}
	
	public void setCalculationTime(double time)
	{
		this.time = time;
		this.dijkstra.setCalculationTime(time);
	}
	
	public double getCalculationTime()
	{
		return time;
	}
	
	@Override
	public Map<Id, Node> getNodes() {
		
		Map<Id, Node> nodesMap = new TreeMap<Id, Node>();
		addNodesToMap(nodesMap);
		
		return nodesMap;
	}
	
	@Override
	public void addNodesToMap(Map<Id, Node> nodesMap)
	{
/*
		if (startNode.getId() == endNode.getId())
		{
			log.warn("StartNode == EndNode!");
		}
*/	
		dijkstra.executeTotalNetwork(startNode, endNode);
		Map<Node, Double> totalNodes = dijkstra.getTotalMinDistances();		
		
		// get the minimal costs to get from the start- to the endnode
		double minCostsStart = totalNodes.get(endNode);
		double minCostsEnd = totalNodes.get(startNode);
		
		if (minCostsStart / minCostsEnd < 0.95 || minCostsStart / minCostsEnd > 1.05)
		{
			log.warn("Different Costs in different Traveldirection (> 5% Difference found)!");
		}
				
		double minCosts;
		if (minCostsStart > minCostsEnd) minCosts = minCostsStart;
		else minCosts = minCostsEnd;
		
		for (Entry<Node, Double> entry : totalNodes.entrySet())
		{
			Node node = entry.getKey();
			double costs = entry.getValue();
						
			/* 
			 * If the costs are smaller than the specified limit -> add Node.
			 */
			if (costs <= minCosts * costFactor)
			{
				nodesMap.put(node.getId(), node);
			}
		}
		
		/*
		 * If the costFactor is exactly 1.0 we may loose due to some rounding errors
		 * some of the Nodes on the only possible Route. To avoid that we add the entire
		 * Route to the map again (the map SHOULD contain all of the already!).
		 */
		if (costFactor == 1.0)
		{
			List<Node> nodes = dijkstra.getForwardRoute(startNode, endNode);
			for (Node node : nodes)
			{
				nodesMap.put(node.getId(), node);
			}
		}
			
		totalNodes.clear();
		
/*		
		dijkstra.executeForwardNetwork(startNode);
		Map<Node, Double> startMap = dijkstra.getMinDistances();
		
		dijkstra.executeBackwardNetwork(endNode);
		Map<Node, Double> endMap = dijkstra.getMinDistances();
		
		Map<Node, Double> sumNodes = new HashMap<Node, Double>();
		for (Entry<Node, Double> entry : startMap.entrySet())
		{
			double costs = entry.getValue() + endMap.get(entry.getKey());
			sumNodes.put(entry.getKey(), costs);
		}
		
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
		for (Node node : networkNodesMap.values())
		{	
			// if the node exists in start- and endMap
			if (startMap.containsKey(node) && endMap.containsKey(node))
			{
				double cost = startMap.get(node) + endMap.get(node);
			
				// If the costs are smaller than the specified limit -> add Node.
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
*/
	}
	
	public List<Map<Id, Node>> multiRun(List<Double> costFactors)
	{
		// Without any CostFactors we do nothing.
		if (costFactors.size() == 0) return null;
	
		List<Map<Id, Node>> knowledges = new ArrayList<Map<Id, Node>>();
		
		// Sort the List - this is necessary for the Algorithm to work properly!
		Collections.sort(costFactors);
		
		// Calculate the Dijkstra only once.
		dijkstra.executeTotalNetwork(startNode, endNode);
		Map<Node, Double> totalNodes = dijkstra.getTotalMinDistances();
		
		// get the minimal costs to get from the start- to the endnode
		double minCostsStart = totalNodes.get(endNode);
		double minCostsEnd = totalNodes.get(startNode);
		
		if (minCostsStart / minCostsEnd < 0.95 || minCostsStart / minCostsEnd > 1.05)
		{
			log.warn("Different Costs in different Traveldirection (> 5% Difference found)!");
		}

		double minCosts;
		if (minCostsStart > minCostsEnd) minCosts = minCostsStart;
		else minCosts = minCostsEnd;


		// Now start the iterative Knowledge Creation
		Map<Id, Node> previousKnowledge = new HashMap<Id, Node>();
		
		for (double costFactor : costFactors)
		{
			Map<Id, Node> knowledge = new HashMap<Id, Node>();
			knowledge.putAll(previousKnowledge);
			
			Iterator<Entry<Node, Double>> iter = totalNodes.entrySet().iterator();
			while(iter.hasNext())
			{
				Entry<Node, Double> entry = iter.next();
				
				Node node = entry.getKey();
				double costs = entry.getValue();
							
				/* 
				 * If the costs are smaller than the specified limit -> add Node.
				 */
				if (costs <= minCosts * costFactor)
				{
					knowledge.put(node.getId(), node);
					iter.remove();
				}
			}
			
			/*
			 * If the costFactor is exactly 1.0 we may loose due to some rounding errors
			 * some of the Nodes on the only possible Route. To avoid that we add the entire
			 * Route to the map again (the map SHOULD contain all of the already!).
			 */
			if (costFactor == 1.0)
			{
				List<Node> nodes = dijkstra.getForwardRoute(startNode, endNode);
				for (Node node : nodes)
				{
					knowledge.put(node.getId(), node);
					totalNodes.remove(node);
				}
			}
			
			knowledges.add(knowledge);
			
			previousKnowledge.clear();
			previousKnowledge.putAll(knowledge);
		}

		totalNodes.clear();
		
		return knowledges;
	}
	
	@Override
	public SelectNodesDijkstra clone()
	{
		//Map<Id, Node> nodesClone = new HashMap<Id, Node>(); 
		//nodesClone.putAll(networkNodesMap);
		//SelectNodesDijkstra clone = new SelectNodesDijkstra(this.network, nodesClone);
		
		SelectNodesDijkstra clone = new SelectNodesDijkstra(this.network, this.networkNodesMap);
		clone.setCostCalculator(this.costCalculator);
		clone.setCostFactor(this.costFactor);	
		
		return clone;
	}
}