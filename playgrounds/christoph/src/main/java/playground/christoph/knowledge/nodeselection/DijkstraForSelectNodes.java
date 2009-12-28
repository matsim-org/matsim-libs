/* *********************************************************************** *
 * project: org.matsim.*
 * DijkstraForSelectNodes.java
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

// Zus�tzlich Punkte innerhalb des aufgespannten Polygons finden?
// -> http://www.coding-board.de/board/showthread.php?t=23953 : 
// Herausfinden, ob ein beliebiger Punkt innerhalb eines Polygons liegt

package playground.christoph.knowledge.nodeselection;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.utils.misc.Time;

import playground.christoph.network.MyLinkImpl;

/*
 * Using the LookupTable seems to improve the speed a tiny little bit.
 * This effect may vary depending on the Network and CostCalculator.
 * So maybe not using it could be faster...
 */
public class DijkstraForSelectNodes {
	
	private static final Logger log = Logger.getLogger(DijkstraForSelectNodes.class);
		
	// mapping between nodes and dijkstraNodes
	private Map<Node, DijkstraNode> dijkstraNodeMap;
		
	// CostCalculator for the Dijkstra Algorithm
	private TravelCost costCalculator;

	// time for the cost calculator
	double time = Time.UNDEFINED_TIME;
	
	// initial length of the priority queue - value taken from Dijkstra.java
	private static final int INITIAL_CAPACITY = 500;
	
	// all nodes of the network
	private Map<Id,? extends Node> networkNodesMap;
	
	// List of nodes, sorted by their distance to the startnode.
	private final Comparator<DijkstraNode> shortestDistanceComparator = new Comparator<DijkstraNode>()
	{
		public int compare(DijkstraNode a, DijkstraNode b)
	    {
			// note that this trick doesn't work for huge distances, close to Integer.MAX_VALUE
	        double diff = a.getMinDist() - b.getMinDist();
	        
	        // distance from b is bigger than distance from a
	        if (diff < 0) return -1;

	        // distance from a is bigger than distance from b
	        else if (diff > 0) return 1;
	        
	        // same distance
	        else return 0;
	    }
	};
	
	   
	// List of nodes, sorted by their distance to the startnode.
	private final PriorityQueue<DijkstraNode> unvisitedNodes = new PriorityQueue<DijkstraNode>(INITIAL_CAPACITY, shortestDistanceComparator);
	
	public DijkstraForSelectNodes(Map<Id,? extends Node> networkNodesMap)
	{
		this.networkNodesMap = networkNodesMap;
		
		dijkstraNodeMap = new HashMap<Node, DijkstraNode>();
		DijkstraNode.setNodeMap(dijkstraNodeMap);
	}
	
	public DijkstraForSelectNodes(Network network, Map<Id,? extends Node> networkNodesMap)
	{
		this.networkNodesMap = networkNodesMap;
		
		dijkstraNodeMap = new HashMap<Node, DijkstraNode>();
		DijkstraNode.setNodeMap(dijkstraNodeMap);
		initDijkstraNodes();	
	}
	
	public DijkstraForSelectNodes(Network network)
	{
		this.networkNodesMap = network.getNodes();
		
		dijkstraNodeMap = new HashMap<Node, DijkstraNode>();
		DijkstraNode.setNodeMap(dijkstraNodeMap);
		initDijkstraNodes();	
	}
		
	public void setNetworkNodes(Map<Id, Node> networkNodesMap)
	{
		this.networkNodesMap = networkNodesMap;
	}
	
	protected void initDijkstraNodes()
	{		
		// clear map
		dijkstraNodeMap.clear();
		 
		for (Node node : networkNodesMap.values())
		{		
			DijkstraNode dijkstraNode = new DijkstraNode(node);
			
			// fill DijkstraNodeMap
			dijkstraNodeMap.put(node, dijkstraNode);
		}
		
	}

	// initialize
	private void init(DijkstraNode start)
	{
		// reset nodes
		resetDijkstraNodes();

	    unvisitedNodes.clear();
	 	        
	    // add source
	    setMinDistance(start, 0.0);
	    unvisitedNodes.add(start);
	}  
	
	// Search shortest Path for a Route. Break if distance has been found.
	public void executeRoute(Node start, Node end)
	{	
		DijkstraNode startNode = dijkstraNodeMap.get(start);
		init(startNode);
	        
		DijkstraNode endNode = dijkstraNodeMap.get(end);
		
	    // the current node
	    DijkstraNode node;
	        
	    // extract the node with the shortest distance
	    while ((node = unvisitedNodes.poll()) != null)
	    {
	    	// catch possible error
	    	assert !isVisited(node);
	            
	        // reached destination node -> break
	        if (node.getNode().equals(endNode.getNode())) break;
	            
	        node.setVisited(true);
	        
	        relaxOutgoingNode(node);
	    }
	}

	// Search shortest Path for a List of Routes. Break if distances have been found.
	public void executeRoute(Node start, List<Node> ends)
	{	
		DijkstraNode startNode = dijkstraNodeMap.get(start);
		init(startNode);
	        
		List<DijkstraNode> endNodes = new ArrayList<DijkstraNode>(); 
		
		for (Node end : ends)
		{
			endNodes.add(dijkstraNodeMap.get(end));
		}
		
	    // the current node
	    DijkstraNode node;
	        
	    // extract the node with the shortest distance
	    while ((node = unvisitedNodes.poll()) != null)
	    {
	    	// catch possible error
	    	assert !isVisited(node);
	                
	        for (Iterator<DijkstraNode> iterator = endNodes.iterator(); iterator.hasNext();) 
	        {
				DijkstraNode endNode = (DijkstraNode) iterator.next();
				
				if (node.getNode().equals(endNode.getNode()))
				{
					iterator.remove();
					break;
				}
			}
	        // reached all destination nodes -> break
	        if (endNodes.size() == 0) break;
	        
	        node.setVisited(true);
	           
	        relaxOutgoingNode(node);
	    }
	}
	
	// Search shortest Fowardpaths to all nodes within the network.
	public void executeForwardNetwork(Node start)
	{	
		DijkstraNode startNode = dijkstraNodeMap.get(start);
		init(startNode);
		
	    // the current node
	    DijkstraNode node;
	    
	    // extract the node with the shortest distance
	    while ((node = unvisitedNodes.poll()) != null)
	    {
	    	// catch possible error
	    	assert !isVisited(node);
	    	
	        node.setVisited(true);
	           
	        relaxOutgoingNode(node);
	    }
	}
	
	// Search shortest Backwardpaths to all nodes within the network.
	public void executeBackwardNetwork(Node start)
	{		
		DijkstraNode startNode = dijkstraNodeMap.get(start);
		init(startNode);
		
	    // the current node
	    DijkstraNode node;
	    
	    // extract the node with the shortest distance
	    while ((node = unvisitedNodes.poll()) != null)
	    {
	    	// catch possible error
	    	assert !isVisited(node);
	    	
	        node.setVisited(true);
	           
	        relaxIngoingNode(node);
	    }
	}
	
	/*
	 * Executes Forward and Backward Dijkstra and stores
	 * the Backward and Forward Costs in the DijkstraNode Object.
	 */
	public void executeTotalNetwork(Node start, Node end)
	{
		// Reset values
		for (DijkstraNode dijkstraNode : dijkstraNodeMap.values())
		{
			dijkstraNode.setMinDistForward(Double.MAX_VALUE);
			dijkstraNode.setMinDistBackward(Double.MAX_VALUE);
		}
		
		executeBackwardNetwork(end);
	    for (DijkstraNode dijkstraNode : dijkstraNodeMap.values())
	    {
	    	dijkstraNode.setMinDistBackward(dijkstraNode.getMinDist());
	    }
		
	    // We execute this after the BackwardNetwork so we can call getForwardRoute aferwards!
		executeForwardNetwork(start);
	    for (DijkstraNode dijkstraNode : dijkstraNodeMap.values())
	    {
	    	dijkstraNode.setMinDistForward(dijkstraNode.getMinDist());
	    }
		
//		executeBackwardNetwork(end);
//	    for (DijkstraNode dijkstraNode : dijkstraNodeMap.values())
//	    {
//	    	dijkstraNode.setMinDistBackward(dijkstraNode.getMinDist());
//	    }
	}
	
	/*
	 * Return Map with the distances to every node.
	 * Is called from outside this class, so return nodes instead of
	 * DijkstraNodes.
	 */
	public Map<Node, Double> getMinDistances()
	{
		/*
		 * Don't change the Type of the Map! We need ordered Results!
		 * Maybe change the return value to TreeMap<Node, Double>?
		 */
		Map<Node, Double> minDistances = new TreeMap<Node, Double>(new NodeComparator());
		
		for (DijkstraNode node : dijkstraNodeMap.values()) 
		{
			// add the node and its shortest path to the map
			minDistances.put(node.getNode(), node.getMinDist());
		}
		
		return minDistances;
	}
	
	/*
	 * Based on the Dijkstra Data returns the ForwardRoute
	 * from Start to End.
	 */
	public List<Node> getForwardRoute(Node start, Node end)
	{
	    LinkedList<Node> forwardRoute = new LinkedList<Node>();
		DijkstraNode startNode = dijkstraNodeMap.get(start);
		DijkstraNode endNode = dijkstraNodeMap.get(end);
		DijkstraNode node = endNode.getPrevNode();
		while (node != startNode)
		{
			forwardRoute.addFirst(node.getNode());
			node = node.getPrevNode();
		}
		forwardRoute.addFirst(startNode.getNode());
		forwardRoute.addLast(endNode.getNode());
		
		return forwardRoute;
	}
	
	/*
	 * Return Map with the distances to every node.
	 * Is called from outside this class, so return nodes instead of
	 * DijkstraNodes.
	 */
	public Map<Node, Double> getTotalMinDistances()
	{
		/*
		 * Don't change the Type of the Map! We need ordered Results!
		 * Maybe change the return value to TreeMap<Node, Double>?
		 */
		Map<Node, Double> minDistances = new TreeMap<Node, Double>(new NodeComparator());
		
		for (DijkstraNode node : dijkstraNodeMap.values()) 
		{
			// add the node and its shortest path to the map
			minDistances.put(node.getNode(), node.getMinDistBackward() + node.getMinDistForward());
		}
		
		return minDistances;
	}
	
	// expand ingoing node
	private void relaxIngoingNode(DijkstraNode node)
	{		
		ArrayList<Link> ingoingLinks = node.ingoingLinks();
		for (int i = 0; i < ingoingLinks.size(); i++)
		{
			// get current link
			Link link = ingoingLinks.get(i);
			
			// get destination node
			DijkstraNode fromNode = dijkstraNodeMap.get(link.getFromNode());
			
			// if node has not been visited
			if (!fromNode.isVisited())
			{			
				double shortDist = node.getMinDist() + getLinkCost(link, time); 
	            
				// Found new shortest path to the destination node?
				if (shortDist < fromNode.getMinDist())
				{
					// store new "shortest" distance
					setMinDistance(fromNode, shortDist);
		                                
					// store new "previous" node
					setPreviousNode(fromNode, node);
				}

			}
		}        
	}
	
	// expand outgoing node
	private void relaxOutgoingNode(DijkstraNode node)
	{		
		ArrayList<Link> outgoingLinks = node.outgoingLinks();
		for (int i = 0; i < outgoingLinks.size(); i++)
		{
			// get current link
			Link link = outgoingLinks.get(i);
			
			// get destination node
			DijkstraNode toNode = dijkstraNodeMap.get(link.getToNode());
			
			// if node has not been visited
			if (!toNode.isVisited())
			{			
				double shortDist = node.getMinDist() + getLinkCost(link, time); 
	            
				// Found new shortest path to the destination node?
				if (shortDist < toNode.getMinDist())
				{
					// store new "shortest" distance
					setMinDistance(toNode, shortDist);
		                                
					// store new "previous" node
					setPreviousNode(toNode, node);
				}

			}
		}        
	}

	private boolean isVisited(DijkstraNode node)
	{
		return node.isVisited();
	}

	// for external call
	public double getMinDistance(DijkstraNode node)
	{
		return node.getMinDist(); 
	}

	/*
	 * Store the "shortest" distances to every node.
	 * Also actualize the Queue (remove node and add it again to put it at the right place in the Queue).
	 * If the node was not already in the queue: add it.
	 */
	private void setMinDistance(DijkstraNode node, double distance)
	{
		unvisitedNodes.remove(node);

		node.setMinDist(distance);

		// add node to the queue
		unvisitedNodes.add(node);
	}
		
	/*
	 * Get cost of the given link. This can be for example its length or the time to pass the link.
	 */
	protected double getLinkCost(Link link, double time)
	{   
		if (link instanceof MyLinkImpl) return ((MyLinkImpl) link).getTravelCost();
		
		return costCalculator.getLinkTravelCost(link, time);
	}

	public void setCostCalculator(TravelCost calculator)
	{
		this.costCalculator = calculator;
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
	
	public DijkstraNode getPreviousNode(DijkstraNode node)
	{
		return node.getPrevNode();
	}
	
	
	private void setPreviousNode(DijkstraNode a, DijkstraNode b)
	{
		a.setPrevNode(b);
	}
	

	/* 
	 * Reset nodes.
	 * visited -> false
	 * mindist -> Double.MAX_DOUBLE
	 */
	private void resetDijkstraNodes()
	{
		for (DijkstraNode node : dijkstraNodeMap.values()) 
		{	
			node.reset();
		}
	}	// resetDijkstraNodes
	
}	// class DijkstraForSelectNodes


/*
 * Compare two Nodes by their Ids. This allows to use Nodes as Keys
 * in a TreeMap.
 */
class NodeComparator implements Comparator<Node>, Serializable {

	private static final long serialVersionUID = 1L;

	public int compare(final Node o1, final Node o2) {
		return o1.getId().compareTo(o2.getId());
	}
}

/*
 * Internal data structure with information about a node, the "distance" to it and its previous node.
 */
class DijkstraNode
{
	static private Map<Node, DijkstraNode> dijkstraNodeMap;
	
	Node node = null;
	DijkstraNode prevNode = null;
	boolean visited;
	double minDist;
	
	double minDistForward;
	double minDistBackward;

	static void setNodeMap(Map<Node, DijkstraNode> map)
	{
		dijkstraNodeMap = map;
	}
	
	protected DijkstraNode(Node node) 
	{
		this.node = node;
		reset();
	}
	
	protected Node getNode()
	{
		return node;
	}
	
	protected void setPrevNode(DijkstraNode node)
	{
		prevNode = node;
	}
	
	protected DijkstraNode getPrevNode() 
	{
		return prevNode;
	}
	
	public boolean isVisited() {
		return visited;
	}

	public void setVisited(boolean visited) {
		this.visited = visited;
	}

	public double getMinDist() 
	{
		return minDist;
	}

	public void setMinDist(double minDist) 
	{
		this.minDist = minDist;
	}
	
	public double getMinDistForward() 
	{
		return minDistForward;
	}

	public void setMinDistForward(double minDist) 
	{
		this.minDistForward = minDist;
	}
	
	public double getMinDistBackward() 
	{
		return minDistBackward;
	}

	public void setMinDistBackward(double minDist) 
	{
		this.minDistBackward = minDist;
	}
	
	protected void reset()
	{
		visited = false;
		minDist = Double.MAX_VALUE;
	}
	
	protected ArrayList<DijkstraNode> ingoingNodes()
	{
		ArrayList<DijkstraNode> ingoingNodes = new ArrayList<DijkstraNode>();
		
		Map<Id, Node> myMap = getInNodes(node);
		
		Iterator<Node> nodeIterator = myMap.values().iterator();
		while(nodeIterator.hasNext())
		{
			Node node = nodeIterator.next();

			// zugehörigen DijsktraNode aus der Map holen
			DijkstraNode dijkstraNode = dijkstraNodeMap.get(node);
			ingoingNodes.add(dijkstraNode);
		}
		return ingoingNodes;
	}
	
	private Map<Id, Node> getInNodes(Node node) 
	{
		Map<Id, Node> nodes = new TreeMap<Id, Node>();
		for (Link link : node.getInLinks().values())
		{
			Node n = link.getFromNode();
			nodes.put(n.getId(), n);
		}
		return nodes;
	}
	
	protected ArrayList<DijkstraNode> outgoingNodes()
	{
		ArrayList<DijkstraNode> outgoingNodes = new ArrayList<DijkstraNode>();
		
		Map<Id, Node> myMap = getOutNodes(node);
		
		Iterator<Node> nodeIterator = myMap.values().iterator();
		while(nodeIterator.hasNext())
		{
			Node node = nodeIterator.next();

			// zugeh�rigen DijsktraNode aus der Map holen
			DijkstraNode dijkstraNode = dijkstraNodeMap.get(node);
			outgoingNodes.add(dijkstraNode);
		}
		return outgoingNodes;
	}
	
	private Map<Id, Node> getOutNodes(Node node) 
	{
		Map<Id, Node> nodes = new TreeMap<Id, Node>();
		for (Link link : node.getOutLinks().values())
		{
			Node n = link.getToNode();
			nodes.put(n.getId(), n);
		}
		return nodes;
	}

	protected ArrayList<Link> ingoingLinks()
	{
		ArrayList<Link> ingoingLinks = new ArrayList<Link>();
		
		Map<Id,? extends Link> myMap = node.getInLinks();
		
		Iterator<? extends Link> linkIterator = myMap.values().iterator();
		while(linkIterator.hasNext())
		{
			Link link = linkIterator.next();
			ingoingLinks.add(link);
		}
		return ingoingLinks;
	}
	
	protected ArrayList<Link> outgoingLinks()
	{
		ArrayList<Link> outgoingLinks = new ArrayList<Link>();
		
		Map<Id,? extends Link> myMap = node.getOutLinks();
		
		Iterator<? extends Link> linkIterator = myMap.values().iterator();
		while(linkIterator.hasNext())
		{
			Link link = linkIterator.next();
			outgoingLinks.add(link);
		}
		return outgoingLinks;
	}	
	
}	// class DijkstraNode
