/* *********************************************************************** *
 * project: org.matsim.*
 * MyMultiNodeDijkstra.java
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
import org.matsim.core.utils.geometry.CoordImpl;

public class MyMultiNodeDijkstra extends Dijkstra {
	
	private final static Logger log = Logger.getLogger(MyMultiNodeDijkstra.class);
	
	public MyMultiNodeDijkstra(Network network, TravelDisutility costFunction, TravelTime timeFunction) {
		super(network, costFunction, timeFunction);
	}
	
	public MyMultiNodeDijkstra(final Network network, final TravelDisutility costFunction, final TravelTime timeFunction,
			final PreProcessDijkstra preProcessData) {
		super(network, costFunction, timeFunction, preProcessData);
	}
	
	public ImaginaryNode createImaginaryNode(Collection<InitialNode> nodes) {
		return new ImaginaryNode(nodes);
	}
	
	public ImaginaryNode createImaginaryNode(Collection<InitialNode> nodes, Coord coord) {
		return new ImaginaryNode(nodes, coord);
	}
	
	@Override
	/*package*/ Node searchLogic(final Node fromNode, final Node toNode, final RouterPriorityQueue<Node> pendingNodes) {
		
		// If it is an imaginary node...
		if (toNode instanceof ImaginaryNode) {
			
			Map<Id, InitialNode> endNodes = new HashMap<Id, InitialNode>();
			
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
						log.warn("No route was found from node " + fromNode.getId() + " to any of the destination nodes was found.");							
						// seems we have no more nodes left, but not yet reached all endNodes...
						for (InitialNode endNode : endNodes.values()) {
							log.warn("\tnot reached destionation node " + endNode.node.getId());
						}
					}
					
					endNodes.clear();
					stillSearching = false;
				} else {
					DijkstraNodeData data = getData(outNode);
					InitialNode initData = endNodes.remove(outNode.getId());
					
					// if the node is an end node
					if (initData != null) {
						double cost = data.getCost() + initData.initialCost;
						if (cost < minCost) {
							minCost = cost;
							minCostNode = outNode;
						}
					} 
					if (data.getCost() > minCost) {
						endNodes.clear(); // we can't get any better now
						stillSearching = false;
					} else {
						relaxNode(outNode, null, pendingNodes);
					}
					
//					// if the node is an end node
//					if (initData != null) {
//						double cost = data.getCost() + initData.initialCost;
//						if (cost < minCost) {
//							minCost = cost;
//							minCostNode = outNode;
//						}
//						stillSearching = endNodes.size() > 0;
//					} else {
//						relaxNode(outNode, toNode, pendingNodes);
//					}
				}
			}
			
			if (minCostNode == null) log.warn("No route was found to any of the to nodes!");
			
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

		DijkstraNodeData toNodeData = getData(toNode);
		Path path = new Path(nodes, links, arrivalTime - startTime, toNodeData.getCost());

		return path;
	}
	
	/*package*/ static class ImaginaryNode implements Node {

		/*package*/ final Collection<InitialNode> initialNodes;
		/*package*/ final Coord coord;
		
		public ImaginaryNode(Collection<InitialNode> initialNodes, Coord coord) {
			this.initialNodes = initialNodes;
			this.coord = coord;
		}
		
		public ImaginaryNode(Collection<InitialNode> initialNodes) {
			this.initialNodes = initialNodes;
			
			double sumX = 0.0;
			double sumY = 0.0;
			
			for (InitialNode initialNode : initialNodes) {
				sumX += initialNode.node.getCoord().getX();
				sumY += initialNode.node.getCoord().getY();
			}
			
			sumX /= initialNodes.size();
			sumY /= initialNodes.size();
			
			this.coord = new CoordImpl(sumX, sumY);
		}
		
		@Override
		public Coord getCoord() {
			return this.coord;
		}

		@Override
		public Id getId() {
			return null;
		}

		@Override
		public boolean addInLink(Link link) {
			return false;
		}

		@Override
		public boolean addOutLink(Link link) {
			return false;
		}

		@Override
		public Map<Id, ? extends Link> getInLinks() {
			return null;
		}

		@Override
		public Map<Id, ? extends Link> getOutLinks() {
			return null;
		}
		
	}
	
	public static class InitialNode {
		public Node node;
		public final double initialCost;
		public final double initialTime;
		
		public InitialNode(final Node node, final double initialCost, final double initialTime) {
			this.node = node;
			this.initialCost = initialCost;
			this.initialTime = initialTime;
		}
		
		@Override
		public String toString() {
			return "[id=" + this.node.getId() + "]" +
					"[initialCost=" + this.initialCost + "]" +
					"[initialTime=" + this.initialTime + "]";
		}
	}
	
}