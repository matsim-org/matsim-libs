/* *********************************************************************** *
 * project: org.matsim.*
 * MultiNodeDijkstra.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.core.router;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Coord;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.DijkstraNodeData;
import org.matsim.core.router.util.PreProcessDijkstra;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.collections.RouterPriorityQueue;

/**
 * <p>An extended implementation of the Dijkstra algorithm that supports multiple
 * start- and/or end nodes.</p>
 * 
 * <p>To do so, ImaginaryNodes and IntialNodes are introduced which is required for
 * backwards compatibility with the default Dijkstra implementation which expects
 * only single nodes as arguments.</p>
 * 
 * <p>Therefore, ImaginaryNodes are introduced. They contain a collection of
 * InitialNodes that represents the start or end nodes to be used by the algorithm.
 * Each InitialNode represents a node from the network. In addition, initial time and
 * costs can be defined, i.e. the time respectively costs to reach that node.<p>
 * 
 * <p>By default, only the cheapest path between is calculated, i.e. the routing algorithm
 * will terminate before all end nodes have been reached. This behaviour can be changed
 * by setting the searchAllEndNodes parameter to true.</p>
 * 
 * @see org.matsim.core.router.Dijkstra
 * @see org.matsim.core.router.InitialNode
 * @see org.matsim.core.router.ImaginaryNode
 * 
 * @author cdobler
 */
public class MultiNodeDijkstra extends Dijkstra {
	
	private final static Logger log = Logger.getLogger(MultiNodeDijkstra.class);
	
	/*
	 * If this value is true, the algorithm tries to find routes to all end nodes.
	 * Otherwise only the one(s) with the lowest costs are found.
	 * 
	 * When enabling this option, select end nodes with care! If only one of them
	 * is in a not reachable part of the network, the algorithm will process the
	 * entire reachable part of the network before terminating.
	 */
	private final boolean searchAllEndNodes;
	
	public MultiNodeDijkstra(Network network, TravelDisutility costFunction, TravelTime timeFunction, boolean searchAllEndNodes) {
		super(network, costFunction, timeFunction);
		this.searchAllEndNodes = searchAllEndNodes;
	}
	
	public MultiNodeDijkstra(final Network network, final TravelDisutility costFunction, final TravelTime timeFunction,
			final PreProcessDijkstra preProcessData, boolean searchAllEndNodes) {
		super(network, costFunction, timeFunction, preProcessData);
		this.searchAllEndNodes = searchAllEndNodes;
	}
	
	public ImaginaryNode createImaginaryNode(Collection<InitialNode> nodes) {
		return new ImaginaryNode(nodes);
	}
	
	public ImaginaryNode createImaginaryNode(Collection<InitialNode> nodes, Coord coord) {
		return new ImaginaryNode(nodes, coord);
	}
	
	/*
	 * We have to extend this method from the original Dijkstra. The given input nodes might be
	 * ImaginaryNodes which contain multiple start / end nodes for the routing process. Those
	 * nodes should be part of the routing network, therefore we perform the check for each of them.
	 * 
	 * Regular nodes are directly passed to the super class.
	 * 
	 * cdobler, jun'14
	 */
	/*package*/ @Override
	void checkNodeBelongToNetwork(Node node) {
		if (node instanceof ImaginaryNode) {
			ImaginaryNode imaginaryNode = (ImaginaryNode) node;
			for (InitialNode initialNode : imaginaryNode.initialNodes) super.checkNodeBelongToNetwork(initialNode.node);
		} else super.checkNodeBelongToNetwork(node);
	}
	
	@Override
	/*package*/ Node searchLogic(final Node fromNode, final Node toNode, final RouterPriorityQueue<Node> pendingNodes) {
		
		// If it is an imaginary node...
		if (toNode instanceof ImaginaryNode) {
			
			Map<Id<Node>, InitialNode> endNodes = new HashMap<>();
			
			Collection<InitialNode> initialNodes = ((ImaginaryNode) toNode).initialNodes;
			for (InitialNode initialNode : initialNodes) endNodes.put(initialNode.node.getId(), initialNode);

			// find out which one is the cheapest end node
			double minCost = Double.POSITIVE_INFINITY;
			Node minCostNode = null;
			
			// continue searching as long as unvisited end nodes are available
			boolean stillSearching = endNodes.size() > 0;
			
			while (stillSearching) {
				Node outNode = pendingNodes.poll();
				
				if (outNode == null) {
					/*
					 * This is not necessarily a problem. Some of the out nodes might be reachable only
					 * by passing another out node. Those nodes will never become the cheapest out node.
					 * Therefore only print a warning if no path to any of the out nodes was found.
					 */
					if (minCostNode == null) {
						if ( log.isTraceEnabled() ) {
							log.trace("No route was found from node " + fromNode.getId() + " to any of the destination nodes was found.");
							// seems we have no more nodes left, but not yet reached all endNodes...
							StringBuffer sb = new StringBuffer("\tnot reached destionation nodes: ");
							for (InitialNode endNode : endNodes.values()) {
								sb.append(endNode.node.getId().toString());
								sb.append("; ");
							}
							log.trace(sb.toString());
						}
					}
					
					if (searchAllEndNodes && endNodes.size() > 0) {
						for (InitialNode endNode : endNodes.values()) {
							log.trace("No route was found from node " + fromNode.getId() + " to destination node " + endNode.node.getId() + ".");
						}
					}
					
					endNodes.clear();
					stillSearching = false;
				} else {
					DijkstraNodeData data = getData(outNode);
					InitialNode initData = endNodes.remove(outNode.getId());
										
					/*
					 * If the node is an end node.
					 * Note: 
					 * The node was head of the priority queue, i.e. the shortest path to the 
					 * node has been found. The algorithm will not re-visit the node on another
					 * route!
					 */
					if (initData != null) {
						double cost = data.getCost() + initData.initialCost;
						if (cost < minCost) {
							minCost = cost;
							minCostNode = outNode;
						}
					}
					
					if (searchAllEndNodes) {
						relaxNode(outNode, null, pendingNodes);
						stillSearching = endNodes.size() > 0;
					} else {
						if (data.getCost() > minCost) {
							endNodes.clear(); // we can't get any better now
							stillSearching = false;
						} else {
							relaxNode(outNode, null, pendingNodes);
						}
					}
				}
			}
			
			return minCostNode;
		} 
		// ... otherwise: default behaviour.
		else return super.searchLogic(fromNode, toNode, pendingNodes);
	}
	
	/*
	 * initFromNode -> first Node in pendingNodes -> calcLeastCostPath will relax that node
	 * -> we relax that dummy node here
	 */
	@Override
	/*package*/ void initFromNode(final Node fromNode, final Node toNode, final double startTime,
			final RouterPriorityQueue<Node> pendingNodes) {
		
		// If it is an imaginary node, we relax it.
		if (fromNode instanceof ImaginaryNode) {			
			relaxImaginaryNode((ImaginaryNode) fromNode, pendingNodes, startTime);
		}
		// ... otherwise: default behaviour.
		else super.initFromNode(fromNode, toNode, startTime, pendingNodes);
	}
	
	protected void relaxImaginaryNode(final ImaginaryNode outNode, final RouterPriorityQueue<Node> pendingNodes,
			final double currTime) {
		
		double currCost = 0.0;	// should be 0

		for (InitialNode initialNode : outNode.initialNodes) {
			double travelTime = initialNode.initialTime;
			double travelCost = initialNode.initialCost;
			DijkstraNodeData data = getData(initialNode.node);
			Link l = null;	// fromLink - use a dummy link here??
			visitNode(initialNode.node, data, pendingNodes, currTime + travelTime, currCost + travelCost, l);			
		}
	}	
	
	@Override
	protected Path constructPath(Node fromNode, Node toNode, double startTime, double arrivalTime) {
		ArrayList<Node> nodes = new ArrayList<Node>();
		ArrayList<Link> links = new ArrayList<Link>();

		nodes.add(0, toNode);
		Link tmpLink = getData(toNode).getPrevLink();
		
		// Only this part has been adapted. Could probably also be changed in the super class.
		while (tmpLink != null) {
			links.add(0, tmpLink);
			nodes.add(0, tmpLink.getFromNode());
			tmpLink = getData(tmpLink.getFromNode()).getPrevLink();
		}

		// Ignore the initial time and cost of the start node!
		DijkstraNodeData startNodeData = getData(nodes.get(0));
		DijkstraNodeData toNodeData = getData(toNode);
		Path path = new Path(nodes, links, toNodeData.getTime() - startNodeData.getTime(), toNodeData.getCost() - startNodeData.getCost());
//		double travelTime = arrivalTime - startTime;
//		Path path = new Path(nodes, links, travelTime, toNodeData.getCost());

		return path;
	}
	
	/**
	 * This method should only be called from outside after calcLeastCostPath(...) has
	 * been executed. After that, the paths between the multiple start and/or end nodes
	 * can be constructed using this method.
	 * 
	 * Is there a way to check whether this method is called as intended??
	 * cdobler, oct'13
	 */
	public Path constructPath(Node fromNode, Node toNode, double startTime) {
		if (toNode == null || fromNode == null) return null;
		else {
			DijkstraNodeData toData = getData(toNode);
			if (!toData.isVisited(this.getIterationId())) return null;

			DijkstraNodeData fromData = getData(fromNode);
			if (!fromData.isVisited(this.getIterationId())) return null;

			double arrivalTime = toData.getTime();
			
			// now construct and return the path
			return constructPath(fromNode, toNode, startTime, arrivalTime);			
		}
	}
	
}