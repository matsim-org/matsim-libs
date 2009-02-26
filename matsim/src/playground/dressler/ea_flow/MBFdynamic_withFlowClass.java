/* project: org.matsim.*
 * MooreBellmanFordMoreDynamic.java
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

package playground.dressler.ea_flow;

// java imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;
import java.util.List;

// other imports
import playground.dressler.Intervall.src.Intervalls.*;
import playground.dressler.ea_flow.TimeExpandedPath.PathEdge;

// matsim imports
import org.matsim.interfaces.core.v01.Link;
import org.matsim.interfaces.core.v01.Node;
import org.matsim.network.NetworkLayer;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;


/**
 * Implementation of the Moore-Bellman-Ford Algorithm for a dynamic network! i =
 * 1 .. n for all e = (v,w) if dist(w) > dist(v) + cost(e) then dist(w) =
 * dist(v) + cost(e), pred(w) = v.
 * 
 */
 public class MBFdynamic_withFlowClass implements LeastCostPathCalculator {
 //public class MBFdynamic_withFlowClass{

	/* avoid numerical problems when doing comparisons ... */
	// private double ACCURACY = 0.001;
	/**
	 * The network on which we find routes. We expect the network to change
	 * between runs!
	 */
	private final NetworkLayer network;

	/**
	 * The cost calculator. Provides the cost for each link and time step.
	 */
	private final TravelCost costFunction;

	/**
	 * The travel time calculator. Provides the travel time for each link and
	 * time step. This is ignored.
	 */
	private final TravelTime timeFunction;

	/**
	 * The flow calculator. Provides the flow of each link in the network.
	 */
	private Flow flow;

	/**
	 * The distance calculator. Provides the time, where we can at first reach the nodes from the
	 * sources.
	 */
	private HashMap<Node, Distances> first = new HashMap<Node, Distances>();

	/**
	 * The arrival time calculator. Provides the time at which we reach the sink
	 */
	private int arrivalTime = Integer.MAX_VALUE;

	/**
	 * The source calculator. Provides the source from which we reach the sink at first.
	 */
	private Node source;

	/**
	 * The path calculator. Provides a shortest path, as list of links, in the
	 * network.
	 */
	private LinkedList<Link> pathToRoute;

	/**
	 * The latest moment, we want reach the sink.
	 */
	private int timeHorizon = Integer.MAX_VALUE;


	/**
	 * The successor calculator. Provides the successor-link of each node of a
	 * path in the network.
	 */
	private HashMap<Node, LinkedList<TimeNode>> succ = new HashMap<Node, LinkedList<TimeNode>>();

	/**
	 * The waiting time calculator. Provides the waiting time in each node of a
	 * path in the network.
	 */
	private Integer waited = 0;
	
	/**
	 * The path calculator. Provides a shortest path, in each step of MooreBellmanFord-Algorithm.
	 */
	private LinkedList<Link> path = new LinkedList<Link>();
	
	/**
	 * Default constructor.
	 * 
	 * @param network
	 *            The network on which to route.
	 * @param costFunction
	 *            Determines the link cost defining the cheapest route. Note,
	 *            comparisons are only made with accuraracy 0.001 due to
	 *            numerical problems otherwise.
	 * @param timeFunction
	 *            Determines the travel time on links. This is ignored!
	 * @param flow
	 *            Determines the flow on links.
	 */
	public MBFdynamic_withFlowClass(final TravelCost costFunction, final TravelTime timeFunction, Flow flow) {
		// set the local final variables
		this.timeHorizon = flow.getTimeHorizon();
		this.network = flow.getNetwork();
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;
		construct(flow);
	}

	/**
	 * Default constructor.
	 * 
	 * @param network
	 *            The network on which to route.
	 * @param costFunction
	 *            Determines the link cost defining the cheapest route. Note,
	 *            comparisons are only made with accuraracy 0.001 due to
	 *            numerical problems otherwise.
	 * @param timeFunction
	 *            Determines the travel time on links. This is ignored!
	 * @param flow
	 *            Determines the flow on links.
	 * @param timeHorizon
	 *            The latest moment, we want reach the sink.
	 */
	public MBFdynamic_withFlowClass(final TravelCost costFunction, final TravelTime timeFunction, Flow flow, int timeHorizon) {
		// set the local final variables
		this.timeHorizon = timeHorizon;
		this.network = flow.getNetwork();
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;
		construct(flow);
	}
	
	/*
	 * for default constructor
	 */
	private void construct(Flow flow) {
		// initialize other variables
		this.flow = flow;
		pathToRoute = new LinkedList<Link>();
		waited = 0;
		// TODO length with getter of Flow
	}

	
	public Path calcLeastCostPath(Node source, Node sink, double startTime) {
		LinkedList<Node> sources = new LinkedList<Node>();
		sources.add(source);
		return calcLeastCostPath(sources, sink, startTime);
	}
	
	
	/**
	 * Calculates the cheapest route from Node 'fromNode' to Node 'toNode' at
	 * starting time 'startTime'.
	 * 
	 * @param fromNode
	 *            The Node at which the route should start.
	 * @param toNode
	 *            The Node at which the route should end.
	 * @param startTime
	 *            ignored
	 * @see org.matsim.router.util.LeastCostPathCalculator#calcLeastCostPath(org.matsim.interfaces.core.v01.Node,
	 *      org.matsim.interfaces.core.v01.Node, double)
	 */
	 public Path calcLeastCostPath(LinkedList<Node> sources, Node sink, final double startTime) {
		boolean found = false;
		
		// find shortest path with Moore-Bellman-Ford-Algorithm
		found = doCalculation(sources, sink, startTime);

		// no path (route) found
		if (pathToRoute == null) {
			return null;
		}
		
		if(!found){
			System.out.println("Unexpected error!");
			return null;
		}
		
		// if there were no problem, reconstruct the route
		List<Node> routeNodes = new LinkedList<Node>();
		List<Link> routeLinks = new LinkedList<Link>();
		Node tmpNode = source;
		LinkedList<PathEdge> edges = flow.getPaths().getLast().getPathEdges();
		for(int i = 0; i < edges.size(); i++){
			if(edges.get(i).getEdge().getFromNode().equals(tmpNode)){
				tmpNode = edges.get(i).getEdge().getToNode();
			}
			else if(edges.get(i).getEdge().getToNode().equals(tmpNode)){
				tmpNode = edges.get(i).getEdge().getFromNode();
			}
			else{
				//TODO
				System.out.println("Menno!!");
				return null;
			}
			routeNodes.add(tmpNode);
			routeLinks.add(edges.get(i).getEdge());
		}
		Path route = new Path(routeNodes, routeLinks, arrivalTime, 0);
		if(route == null){
			System.out.println("Route is null!");
		}
		return route;
	}
	

	/**
	 * Calculates the cheapest route from Node 'fromNode' to Node 'toNode' at
	 * starting time 'startTime' with respect to the flow.
	 * 
	 * @param fromNode
	 *            The Node at which the route should start.
	 * @param toNode
	 *            The Node at which the route should end.
	 * @param startTime
	 *            ignored
	 * @param flow
	 *            Determines the flow on links.
	 * @see org.matsim.router.util.LeastCostPathCalculator#calcLeastCostPath(org.matsim.interfaces.core.v01.Node,
	 *      org.matsim.interfaces.core.v01.Node, double)
	 */
	public Path calcLeastCostPath(LinkedList<Node> sources, Node sink, final double startTime, Flow flow) {

		this.flow = flow;
		return calcLeastCostPath(sources, sink, startTime);
	}

	/**
	 * Calculates the cheapest route from Node 'fromNode' to Node 'toNode' at
	 * starting time 'startTime'. This returns an array of links which is more
	 * useful than the Route object
	 * 
	 * @param fromNode
	 *            The Node at which the route should start.
	 * @param toNode
	 *            The Node at which the route should end.
	 * @param startTime
	 *            ignored
	 * @see org.matsim.router.util.LeastCostPathCalculator#calcLeastCostPath(org.matsim.interfaces.core.v01.Node,
	 *      org.matsim.interfaces.core.v01.Node, double)
	 */
	 public ArrayList<Link> calcLeastCostLinkRoute(LinkedList<Node> sources, Node sink, final double startTime) {
		boolean found = false;
		
		// run the algorithm
		found = doCalculation(sources, sink, startTime);
		
		// no path (route) found
		if(pathToRoute == null){
			return null;
		}
		
		if(!found){
			System.out.println("Unexpected error!");
		}

		// now reconstruct the route
		ArrayList<Link> routeLinks = new ArrayList<Link>();
		if (pathToRoute.peek().getFromNode().equals(source) || pathToRoute.peek().getToNode().equals(source)) {
			for (int i = 0; i < pathToRoute.size(); i++) {
				routeLinks.add(pathToRoute.get(i));
			}
		} else {
			for (int i = pathToRoute.size() - 1; i >= 0; i--) {
				routeLinks.add(pathToRoute.get(i));
			}
		}
		if(routeLinks == null){
			System.out.println("Route is null!");
		}
		return routeLinks;
	}

	/**
	 * Calculates the cheapest route from Node 'fromNode' to Node 'toNode' at
	 * starting time 'startTime' with respect to the flow. This returns an array
	 * of links which is more useful than the Route object
	 * 
	 * @param fromNode
	 *            The Node at which the route should start.
	 * @param toNode
	 *            The Node at which the route should end.
	 * @param startTime
	 *            ignored
	 * @param flow
	 *            Determines the flow on links.
	 * @see org.matsim.router.util.LeastCostPathCalculator#calcLeastCostPath(org.matsim.interfaces.core.v01.Node,
	 *      org.matsim.interfaces.core.v01.Node, double)
	 */
	public ArrayList<Link> calcLeastCostLinkRoute(LinkedList<Node> sources, Node sink, final double startTime, Flow flow) {
		this.flow = flow;
		return calcLeastCostLinkRoute(sources, sink, startTime);
	}

	/**
	 * Calculates the cheapest route from Node 'fromNode' to Node 'toNode' at
	 * starting time 'startTime'. This returns an array of links which is more
	 * useful than the Route object
	 * 
	 * @param fromNode
	 *            The Node at which the route should start.
	 * @param toNode
	 *            The Node at which the route should end.
	 * @param startTime
	 *            ignored
	 * @see org.matsim.router.util.LeastCostPathCalculator#calcLeastCostPath(org.matsim.interfaces.core.v01.Node,
	 *      org.matsim.interfaces.core.v01.Node, double)
	 */
	public Flow calcLeastCostFlow(LinkedList<Node> sources, final Node sink, final double startTime) {
		boolean addNewPath = false;
		boolean activeSource = false;
		for(Node node : network.getNodes().values()){
			if(flow.isActiveSource(node)){
				activeSource = true;
				break;
			}
		}
		if(!activeSource){
			System.out.println("There is no active source in the network!");
			return flow;
		}
		addNewPath = doCalculation(sources, sink, startTime);
		while(addNewPath){
			activeSource = false;
			for(Node node : network.getNodes().values()){
				if(flow.isActiveSource(node)){
					activeSource = true;
					break;
				}
			}
			if(!activeSource){
				System.out.println("There is no active source in the network!");
				return flow;
			}
			addNewPath = doCalculation(sources, sink, startTime);	
		}
		return flow;
	}

	/**
	 * Calculates the cheapest route from Node 'fromNode' to Node 'toNode' at
	 * starting time 'startTime' with respect to the flow. This returns an array
	 * of links which is more useful than the Route object
	 * 
	 * @param fromNode
	 *            The Node at which the route should start.
	 * @param toNode
	 *            The Node at which the route should end.
	 * @param startTime
	 *            ignored
	 * @param flow
	 *            Determines the flow on links.
	 * @see org.matsim.router.util.LeastCostPathCalculator#calcLeastCostPath(org.matsim.interfaces.core.v01.Node,
	 *      org.matsim.interfaces.core.v01.Node, double)
	 */
	public Flow calcLeastCostFlow(final double startTime, Flow flow) {
		this.flow = flow;
		LinkedList<Node> sources = new LinkedList<Node>();
		sources.addAll(flow.getDemands().keySet());
		return calcLeastCostFlow(sources , flow.getSink(), startTime);
	}

	/*
	 * this is the Moore-Bellman-Ford Algorithm on the residual network with  flow
	 * 
	 */
	private boolean doCalculation(LinkedList<Node> sources, final Node sink, final double startTime) {
		// run the algorithm
		/* boolean we want to return and which returns
		 * true, if we had found a path and
		 * false, if we didn't found one
		*/
		boolean found = false;
		
		// calculate path
		found = calculateArrivalTime(sources, sink, startTime);
		
		
		// no path found
		if (pathToRoute == null) {
			if(!found){
				return false;
			}
			else{
				System.out.println("What is the reason, that we found a path, but it is empty?");
				return false;
			}
		}

		// found is only false, if we didn't found a path and the variable pathToRoute is null
		// => this case would never happen, i hope
		if (!found) {
			System.out.println("Why?");
			return false;
		}
		
		// reconstruct path
		//initializations for the while-loop
		Node tmpNode = source;
		Node node = tmpNode;
		Link tmpLink;
		int time = waited;
		Flow newFlow = flow;
		int gamma = Integer.MAX_VALUE;
		TimeExpandedPath path = new TimeExpandedPath();
		System.out.print("Path: ");
		while (!pathToRoute.isEmpty()) {
			tmpLink = pathToRoute.poll();
			// tmpLink is backward link
			if ((tmpNode.equals(tmpLink.getToNode())) && !(tmpNode.equals(tmpLink.getFromNode()))) {
				// add link to the path and update the time
				time -= tmpLink.getFreespeedTravelTime(1.);
				node = tmpLink.getFromNode();
				path.append(tmpLink, time, false);
				System.out.print(time + " ");
				if(flow.getFlow().get(tmpLink).getFlowAt(time) < gamma){
					gamma = flow.getFlow().get(tmpLink).getFlowAt(time);
				}
			}
			// tmpLink is forward link
			else if ((tmpNode.equals(tmpLink.getFromNode())) && !(tmpNode.equals(tmpLink.getToNode()))) {
				// add link to the path and update the time
				node = tmpLink.getToNode();
				path.append(tmpLink, time, true);
				System.out.print(time + " ");
				if((int)(tmpLink.getCapacity(1.) - flow.getFlow().get(tmpLink).getFlowAt(time)) < gamma){
					gamma = (int)tmpLink.getCapacity(1.) - flow.getFlow().get(tmpLink).getFlowAt(time);
				}
				time += tmpLink.getFreespeedTravelTime(1.);
			}
			// the LinkedList pathToRoute has an error, perhaps wrong order of links or not adjacent links 
			else if(!(tmpNode.equals(tmpLink.getFromNode())) && !(tmpNode.equals(tmpLink.getToNode()))){
				System.out.println("Error with LinkedList path!");
			}
			// else tmpLink is a 'waiting' link
			tmpNode = node;
		}
		System.out.println();
		// augment path and reduce demands
		path.setWait(this.waited);
		path.setArrival(this.arrivalTime);
		path.setFlow(gamma);
		path.print();
		newFlow.augment(path);
		
		// TODO wieder loeschen
		if(gamma != newFlow.getPaths().getLast().getFlow()){
			System.out.println("Gammas stimmen nicht ueberein");
		}
		
		// set flow and return true
		flow = newFlow;
		return true;
	}

	
	/*
	 * this calculates the arrivalTime at the sink and the source from which we reach the sink at this time
	 * 
	 */
	private boolean calculateArrivalTime(LinkedList<Node> sources, final Node sink, final double startTime) {
		//initialize all variables, we need
		init(sources);
		
		// queue to save nodes we have to scan
		Queue<Node> queue = new LinkedList<Node>();
		// add sources to the queue
		for(Node source : sources){
			queue.add(source);
		}

		// v is first vertex in the queue
		// w is the vertex we probably want to insert to the queue or to
		// decrease the distance
		Node v, w;
		// dist is the distance from the source to w over v
		int dist;
		
		while (!queue.isEmpty()) {
			// gets the first vertex in the queue
			v = queue.poll();
			//TODO put this in the for-loops
			// check v == sink
			if(v.equals(sink)){
				boolean step = false;
				for(Node source : sources){
					if(first.get(source).getDistance(sink) < Integer.MAX_VALUE){
						if(flow.isActiveSource(source)){
							// set source and arrivalTime
							this.source = source;
							arrivalTime = first.get(source).getDistance(sink);
							step = true;
							break;
						}
					}
				}
				if(!step){
					System.out.println("Arrives at sink, but any source wants to be the reason for it!");
				}
				break;
			}
			
			// visit neighbors
			// link is outgoing link of v => forward edge
			for (Link link : v.getOutLinks().values()) {
				// w is the other node of the link
				w = link.getToNode();
				//TODO tausche for-schleife + ifs f�r bessere Laufzeit nach oben?
				for(Node source : sources){
					if(flow.isActiveSource(source)){
						// compute new distance to neighbor
						if (!first.get(source).getDistance(v).equals(Integer.MAX_VALUE)) {
							// compute distance
							dist = (int)(link.getFreespeedTravelTime(1.)) + first.get(source).getDistance(v);
							if(dist <= timeHorizon){
								if (first.get(source).getDistance(w) > dist) {
									// flow could be send at time dist over link
									if (flow.getFlow().get(link).getFlowAt(first.get(source).getDistance(v)) < (int)(link.getCapacity(1.))) {
										//TODO entferne visitNode
										first.get(source).setDistance(w, visitNode(w, first.get(source).getDistance(w), dist));
										// add w to the queue, if it isn't still contained
										if (!queue.contains(w)) {
											queue.add(w);
										}	
									}
									// flow couldn't be send at time dist, but at time t
									// over link
									else {
										//search for first EdgeIntervall, on which we can send flow
										EdgeIntervall tmpInt = flow.getFlow().get(link).getIntervallAt(first.get(source).getDistance(v));
										int t = tmpInt.getLowBound();
										while (!(flow.getFlow().get(link).getFlowAt(t) < (int)(link.getCapacity(1.)))&& !(tmpInt.equals(flow.getFlow().get(link).getLast()))) {
											tmpInt = flow.getFlow().get(link).getNext(tmpInt);
											t = tmpInt.getLowBound();
										}
										t = tmpInt.getLowBound() + (int)(link.getFreespeedTravelTime(1.));
										if (flow.getFlow().get(link).getFlowAt(t) < (int)(link.getCapacity(1.))) {
											// flow could be send at time t over link
											if (first.get(source).getDistance(w) > t) {
												first.get(source).setDistance(w, visitNode(w, first.get(source).getDistance(w), t));
												// add w to the queue, if it isn't still contained
												if (!queue.contains(w)) {
													queue.add(w);
												}
											}
										}
									}
								}
							}
						}
					}
				}
			}
			// link is incoming edge of v => backward edge
			for (Link link : v.getInLinks().values()) {
				// w is the other node of the link
				w = link.getFromNode();
				for(Node source : sources){
					if(flow.isActiveSource(source)){
						if (!first.get(source).getDistance(v).equals(Integer.MAX_VALUE)) {
							// compute distance
							dist = first.get(source).getDistance(v) - (int)(link.getFreespeedTravelTime(1.));
							if (dist >= 0) {
								if (first.get(source).getDistance(w) > dist) {
									// flow could be send at time dist over link
									if (flow.getFlow().get(link).getFlowAt(dist) > 0) {
										first.get(source).setDistance(w, visitNode(w, first.get(source).getDistance(w), dist));
										// add w to the queue, if it isn't still contained
										if (!queue.contains(w)) {
											queue.add(w);
										}	
									}
									// flow couldn't be send at time dist, but at
									// time t over link
									else {
										// search for first EdgeIntervall, on which we can send flow
										int t = dist;
										EdgeIntervall tmpInt = flow.getFlow().get(link).getIntervallAt(dist);
										while (!(tmpInt.getFlow() > 0) && !(tmpInt.equals(flow.getFlow().get(link).getLast()))) {
											tmpInt = flow.getFlow().get(link).getNext(tmpInt);
										}
										if (tmpInt.getFlow() > 0) {
											t = tmpInt.getLowBound();
											// flow could be send at time t over link
											if (first.get(source).getDistance(w) > t) {
												first.get(source).setDistance(w, visitNode(w, first.get(source).getDistance(w), t));
												// add w to the queue, if it isn't still contained
												if (!queue.contains(w)) {
													queue.add(w);
												}
											}
										}	 
									}
								}
							}
						}
					}
				}
			}
		}
		//print the 
		/*for(Node source : sources){
			System.out.println("first for source: " + source.getId());
			first.get(source).printAll();
		}*/
		
		
		// didn't reach the sink
		if (arrivalTime == Integer.MAX_VALUE) {
			pathToRoute = null;
			System.out.println("No path found!");
			return false;
		} 
		// out of time horizon
		else if (arrivalTime > timeHorizon) {
			pathToRoute = null;
			System.out.println("Out of time horizon! " + arrivalTime + " > " + timeHorizon);
			return false;
		} 
		// returns
		else {
			// try to calculate predecessors
			if(calculatePreds(source, sink)){
				// reconstruct LinkedList of links
				pathToRoute = findPath(source, sink);
				// if LinkedList is null, return false
				if(pathToRoute == null){
					System.out.println("Path is null!");
					return false;
				}
				// else return true
				return true;
			}
			// if we couldn't calculate the predecessors return false
			else{
				pathToRoute = null;
				return false;
			}
		}
	}
	
	
	/**
	 * Initializes the nodes of the network
	 * 
	 * @param fromNode
	 *            The starting node
	 */
	private void init(LinkedList<Node> sources) {
		// initialize the variables for the path
		arrivalTime = Integer.MAX_VALUE;
		if(!sources.isEmpty()){
			source = sources.getFirst();
		}
		for (Node node : network.getNodes().values()) {
			succ.put(node, new LinkedList<TimeNode>());
			if((sources.contains(node)) && (flow.getDemands().get(node) > 0)){
				first.put(node, new Distances(network, node));
			}
		}
		if (!pathToRoute.isEmpty()) {
			pathToRoute.clear();
		}
		waited = 0;		
	}

	/**
	 * Update the distance of the finish node, if it is possible
	 * 
	 * @param fromNode
	 *            The starting node
	 * @param toNode
	 *            The finish node
	 * @param arrivesAt
	 *            The time at which we could arrive at the finish node
	 */
	//TODO hier koennte noch mehr reinkommen
	private int visitNode(final Node toNode,int oldTime, int newTime) {
		if (newTime < oldTime) {
			return newTime;
		}
		return oldTime;
	}

	
	/**
	 * calculate the predecessor-links of the nodes on a "path" in the network
	 * 
	 * @param fromNode
	 *            The starting node
	 * @param toNode
	 *            The finish node
	 */
	private boolean calculatePreds(final Node fromNode, final Node toNode){
		// calculate the predecessors with BFS over the time
		// initializations
		int time = arrivalTime;
		int newTime = time;
		TimeNode tmpNode = new TimeNode(toNode, time, null);
		HashMap<Node, LinkedList<Integer>> times = new HashMap<Node, LinkedList<Integer>>();
		for(Node node : network.getNodes().values()){
			times.put(node, new LinkedList<Integer>());
		}
		
		//queue is queue of 'active' nodes in the BFS
		LinkedList<TimeNode> queue = new LinkedList<TimeNode>();
		queue.add(tmpNode);
		//nodes is queue of all nodes, we wvisited in the BFS to calculate the predecessors
		LinkedList<TimeNode> nodes = new LinkedList<TimeNode>();		
		nodes.add(tmpNode);
		
		boolean step = false;
		while(!(step)){
			//return false, if queue is empty and we didn't reach the source
			if(queue.isEmpty()){
				System.out.println("Queue is empty before reaching the source " + source.getId() + "!");
				return false;
			}
			// tmpNode is first Node in the queue
			tmpNode = queue.poll();
			// time is the time, when we arrived the Node from the sink
			time = tmpNode.getTime();
			
			// tmpNode is ToNode => forward link in original network
			for(Link link : tmpNode.getNode().getInLinks().values()){
				TimeNode otherNode = new TimeNode();
				// calculate the time when we reach 'otherNode' from the sink over tmpNode
				newTime = time - (int)(link.getFreespeedTravelTime(1.));
				if((newTime >= 0) && (!(times.get(link.getFromNode()).contains(newTime)))){
					if(flow.getFlow().get(link).getFlowAt(newTime) < link.getCapacity(1.)){
						// set otherNode
						otherNode.setNode(link.getFromNode());
						otherNode.setTime(newTime);
						otherNode.setSuccessorLink(link);
						//add otherNode to the queues
						times.get(otherNode.getNode()).add(otherNode.getTime());
						queue.add(otherNode);
						nodes.add(otherNode);
						// break, if we reach the source
						if(otherNode.getNode().equals(fromNode)){
							step = true;
							break;
						}
					}
				}
			}
			
			// tmpNode is fromNode => backward link in original network
			if(!step){
				for(Link link : tmpNode.getNode().getOutLinks().values()){
					TimeNode otherNode = new TimeNode();
					// calculate the time when we reach 'otherNode' from the sink over tmpNode
					newTime = time + (int)(link.getFreespeedTravelTime(1.));
					if((newTime <= timeHorizon) && (!(times.get(link.getToNode()).contains(newTime)))){
						if(flow.getFlow().get(link).getFlowAt(time) > 0){
							// set otherNode
							otherNode.setNode(link.getToNode());
							otherNode.setTime(newTime);
							otherNode.setSuccessorLink(link);
							// set otherNode
							times.get(otherNode.getNode()).add(otherNode.getTime());
							queue.add(otherNode);
							nodes.add(otherNode);
							// break, if we reach the source
							if(otherNode.getNode().equals(fromNode)){
								step = true;
								break;
							}
						}
					}	
				}
			}
		}
		//put the predecessor values (forward)
		Node node = fromNode;
		Link tmpLink;
		time = newTime;
		waited = time;
		step = false;
		while(!(node.equals(toNode))){
			step = false;
			// search node in the LinkedList
			for(int i = 0; i < nodes.size(); i++){
				// found nod ein the LinkedList
				if((nodes.get(i).getNode().equals(node)) && (nodes.get(i).getTime() == time)){
					tmpLink = nodes.get(i).getSuccessorLink();
					// set predecessor
					succ.get(node).add(nodes.get(i));
					node = nodes.get(i).getSuccessorNode();
					// update time
					if(tmpLink.getToNode().equals(node)){
						time = time + (int)(tmpLink.getFreespeedTravelTime(1.));
					}
					else{
						time = time - (int)(tmpLink.getFreespeedTravelTime(1.));
					}
					step = true;
					break;
				}
			}
			// return false if we didn't found a fitting node in the LinkedList
			if(!step){
				System.out.println("Problem with calculation of predecessors! (node: " + node.getId() + ")");
				return false;
			}
		}
		// else return true
		return true;
	}

	/**
	 * find the shortest path with the predecessor-links of the nodes on a path in the network
	 * 
	 * @param fromNode
	 *            The starting node
	 * @param toNode
	 *            The finish node
	 */
	private LinkedList<Link> findPath(final Node fromNode, final Node toNode) {
		// construct LinkedList pathToRoute
		// initializations
		path.clear();
		Node tmpNode = fromNode;
		Node node = tmpNode;
		int time = waited;
		Link tmpLink;
		boolean step;
		while (!tmpNode.equals(toNode)) {
			step = false;
			tmpLink = null;
			// search successor
			for(int i = 0; i < succ.get(tmpNode).size(); i++){
				if(succ.get(tmpNode).get(i).getTime() == time){
					// successor found
					tmpLink = succ.get(tmpNode).get(i).getSuccessorLink();
					step = true;
					break;
				}
			}
			if(step){
				// add SuccessorLink to the path and update time
				if(tmpLink != null){
					// forward link
					if (tmpLink.getFromNode().equals(tmpNode)) {
						node = tmpLink.getToNode();
						path.add(tmpLink);
						time += tmpLink.getFreespeedTravelTime(1.);
					}
					// backward link
					else if (tmpLink.getToNode().equals(tmpNode)) {
						node = tmpLink.getFromNode();
						path.add(tmpLink);
						time -= tmpLink.getFreespeedTravelTime(1.);
					} 
					// return null, if link is wrong
					else {
						System.out.println("ERROR with Link (" + node.getId() + "," + node.getId() + ")");
						return null;
					}
				}
				// return null, if SuccessorLink was null
				else{
					System.out.println("ERROR: Predecessor is null");
					return null;
				}
				// TODO ??? muss nicht sein, wenn zeitunterschied != 0
				if (!node.equals(tmpNode)) {
					tmpNode = node;
				}
				else {
					System.out.println("ERROR with Link (" + node.getId() + "," + node.getId() + ")");
					return null;
				}
			}
			// return null, if we didn't found a successor for a node at a given time
			else{
				System.out.println("ERROR! " + tmpNode.getId() + " has no succesor at time " + time);
				return null;
			}
		}
		// else return path 
		return path;
	}

}
 // TODO kontrolliere alle methodenbeschreibungen!!!!!!!!!!!!!
 // TODO set wait und arrival und gebe path mit print aus
 // TODO benutze flow.getLength(link); f�r L�nge