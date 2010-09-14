/******************************************************************************
 *project: org.matsim.*
 * ShortestDistanceFromSupersource.java
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


package playground.rost.graph.shortestdistances;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.Map.Entry;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.utils.geometry.CoordImpl;

import playground.rost.eaflow.ea_flow.GlobalFlowCalculationSettings;
import playground.rost.graph.evacarea.EvacArea;



public class ShortestDistanceFromSupersource {

	private static final Logger log = Logger.getLogger(ShortestDistanceFromSupersource.class);

	CostFunction cf;
	NetworkImpl network;
	EvacArea evacArea;

	Id sSID = new IdImpl(GlobalFlowCalculationSettings.superSinkId);

	public Map<Node, Double> costs = new HashMap<Node, Double>();

	protected Map<Node, NodeCost> costMap = new HashMap<Node, NodeCost>();

	protected Set<Id> superSinkLinks = new HashSet<Id>();

	protected PriorityQueue<NodeCost> queue = new PriorityQueue<NodeCost>();

	public ShortestDistanceFromSupersource(CostFunction cf, NetworkImpl network, EvacArea evacArea)
	{
		this.cf = cf;
		this.network = network;
		this.evacArea = evacArea;
	}

	public void calcShortestDistances()
	{
		//addSuperSink();

		initQueue();
		while(!queue.isEmpty())
		{
			NodeCost nd = queue.poll();
			if(nd.cost == Double.MAX_VALUE)
			{
				log.debug("found " + queue.size() + " nodes that are unreachable!");
				break;
			}
			//iterate the connected links
			for(Link l : nd.node.getOutLinks().values())
			{
				Node neighbor = l.getToNode();
				double cost = cf.cost(l);
				NodeCost ndNeighbor = costMap.get(neighbor);
				if(ndNeighbor != null)
				{
					double newCost = nd.cost + cost;
					if(newCost < ndNeighbor.cost)
					{
						ndNeighbor.cost = newCost;
						queue.remove(ndNeighbor);
						queue.add(ndNeighbor);
					}
				}
			}
		}
		for(Entry<Node, NodeCost> entry : costMap.entrySet())
		{
			costs.put(entry.getKey(), entry.getValue().cost);
		}
		costMap.clear();
		//removeSuperSink();
	}

	protected void initQueue()
	{
		for(String s : evacArea.evacBorderNodeIds)
		{
			Node node = network.getNodes().get(new IdImpl(s));
			NodeCost nd = new NodeCost(node, 0);
			costMap.put(node, nd);
			queue.add(nd);
		}
		for(String s : evacArea.evacAreaNodeIds)
		{
			Node node = network.getNodes().get(new IdImpl(s));
			NodeCost nd = new NodeCost(node, Double.MAX_VALUE);
			costMap.put(node, nd);
			queue.add(nd);
		}
	}

	protected void addSuperSink()
	{
		long i = 0;
		network.createAndAddNode(sSID, new CoordImpl(0,0));
		Node sink = network.getNodes().get(sSID);

		for(String s : evacArea.evacBorderNodeIds)
		{
			Node node = network.getNodes().get(new IdImpl(s));
			Id linkId = new IdImpl("superlink" + ++i);
			network.createAndAddLink(linkId,
								network.getNodes().get(sSID),
								node,
								0,
								0,
								0,
								0);
			superSinkLinks.add(linkId);
		}
	}

	protected void removeSuperSink()
	{
		for(Id id : superSinkLinks)
		{
			network.removeLink(id);
		}
		network.removeNode(sSID);
	}

	public Set<Node> getNodes(double cost)
	{
		Set<Node> result = new HashSet<Node>();

		for(Node n : costs.keySet())
		{
			if(costs.get(n) == cost)
			{
				result.add(n);
			}
		}

		return result;
	}






}
