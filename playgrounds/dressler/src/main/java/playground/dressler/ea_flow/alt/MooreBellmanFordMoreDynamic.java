package playground.dressler.ea_flow.alt;
/* *********************************************************************** *
 * project: org.matsim.*
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
 * *********************************************************************** 

package playground.dressler.ea_flow;

// java imports
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Queue;

import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.population.routes.NetworkRouteWRefs;
import org.matsim.core.population.routes.NodeNetworkRouteImpl;
import org.matsim.core.router.util.LeastCostPathCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.utils.misc.RouteUtils;
import org.matsim.core.utils.misc.Time;

import playground.dressler.Intervall.src.Intervalls.EdgeIntervall;
import playground.dressler.Intervall.src.Intervalls.EdgeIntervalls;

// import org.apache.log4j.Logger;
// import org.matsim.utils.identifiers.IdI;
// import org.matsim.basic.v01.Id;

*//**
 * Implementation of the Moore-Bellman-Ford Algorithm for a static network! i =
 * 1 .. n for all e = (v,w) if l(w) > l(v) + c(e) then l(w) = l(v) + c(e), p(w) =
 * v.
 *
 *//*
public class MooreBellmanFordMoreDynamic implements LeastCostPathCalculator {

	// private final static Logger log =
	// Logger.getLogger(MooreBellmanFord.class);

	 avoid numerical problems when doing comparisons ... 
	// private double ACCURACY = 0.001;
	*//**
	 * The network on which we find routes. We expect the network to change
	 * between runs!
	 *//*
	final NetworkLayer network;

	*//**
	 * The cost calculator. Provides the cost for each link and time step.
	 *//*
	final TravelCost costFunction;

	*//**
	 * The travel time calculator. Provides the travel time for each link and
	 * time step. This is ignored.
	 *//*
	final TravelTime timeFunction;

	private HashMap<Link, EdgeIntervalls> flow;

	private Distances Dists;

	private LinkedList<Link> pathToRoute;

	private int timeHorizon;

	private int gamma;

	final FakeTravelTimeCost length = new FakeTravelTimeCost();

	private HashMap<Node, Link> pred = new HashMap<Node, Link>();

	private HashMap<Node, Integer> waited = new HashMap<Node, Integer>();

	*//**
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
	 *//*
	public MooreBellmanFordMoreDynamic(final NetworkLayer network,
			final TravelCost costFunction, final TravelTime timeFunction,
			HashMap<Link, EdgeIntervalls> flow) {

		this.network = network;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;

		this.flow = flow;
		Dists = new Distances(network);
		timeHorizon = Integer.MAX_VALUE;

		pathToRoute = new LinkedList<Link>();
		gamma = Integer.MAX_VALUE;

		for (Node node : network.getNodes().values()) {
			pred.put(node, null);
			waited.put(node, 0);
		}
	}

	*//**
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
	 *//*
	public MooreBellmanFordMoreDynamic(final NetworkLayer network,
			final TravelCost costFunction, final TravelTime timeFunction,
			HashMap<Link, EdgeIntervalls> flow, int timeHorizon) {

		this.network = network;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;

		this.flow = flow;
		this.timeHorizon = timeHorizon;
		Dists = new Distances(network);

		pathToRoute = new LinkedList<Link>();
		gamma = Integer.MAX_VALUE;

		for (Node node : network.getNodes().values()) {
			pred.put(node, null);
			waited.put(node, 0);
		}

	}

	*//**
	 * Calculates the cheapest route from Node 'fromNode' to Node 'toNode' at
	 * starting time 'startTime'.
	 *
	 * @param fromNode
	 *            The Node at which the route should start.
	 * @param toNode
	 *            The Node at which the route should end.
	 * @param startTime
	 *            ignored
	 * @see org.matsim.core.router.util.LeastCostPathCalculator#calcLeastCostPath(org.matsim.core.network.Node,
	 *      org.matsim.core.network.Node, double)
	 *//*
	public Path calcLeastCostPath(final Node fromNode, final Node toNode,
			final double startTime) {

		// run the algorithm
		boolean found = false;

		// find shortest path with Moore-Bellman-Ford-Algorithm
		found = doCalculations(fromNode, toNode, startTime, flow);

		// no path (route) found
		if (pathToRoute == null) {
			return null;
		}

		if (timeHorizon < Dists.getDistance(toNode)) {
			return null;
		}
		if (!found){
			System.out.println("Warum?");
			return null;
		}

		// now reconstruct the route
		ArrayList<Node> routeNodes = new ArrayList<Node>();
		Node tmpNode = fromNode;
		routeNodes.add(tmpNode);
		if (pathToRoute.peek().getFromNode().equals(fromNode)
				|| pathToRoute.peek().getToNode().equals(fromNode)) {
			for (int i = 0; i < pathToRoute.size(); i++) {
				if (pathToRoute.get(i).getToNode().equals(tmpNode)) {
					tmpNode = pathToRoute.get(i).getFromNode();
				} else if (pathToRoute.get(i).getFromNode().equals(tmpNode)) {
					tmpNode = pathToRoute.get(i).getToNode();
				} else {
					System.out.println("ERROR: couldn't construct route!");
					return null;
				}
				routeNodes.add(tmpNode);
			}
		} else {
			for (int i = pathToRoute.size() - 1; i >= 0; i++) {
				if (pathToRoute.get(i).getToNode().equals(tmpNode)) {
					tmpNode = pathToRoute.get(i).getFromNode();
				} else if (pathToRoute.get(i).getFromNode().equals(tmpNode)) {
					tmpNode = pathToRoute.get(i).getToNode();
				} else {
					System.out.println("ERROR: couldn't construct route!");
					return null;
				}
				routeNodes.add(tmpNode);
			}
		}
		// FIXME [MR] collect links
		return new Path(routeNodes, null, Time.UNDEFINED_TIME, 0);
	}

	*//**
	 * Calculates the cheapest route from Node 'fromNode' to Node 'toNode' at
	 * starting time 'startTime'.
	 *
	 * @param fromNode
	 *            The Node at which the route should start.
	 * @param toNode
	 *            The Node at which the route should end.
	 * @param startTime
	 *            ignored
	 * @see org.matsim.core.router.util.LeastCostPathCalculator#calcLeastCostPath(org.matsim.core.network.Node,
	 *      org.matsim.core.network.Node, double)
	 *//*
	public NetworkRouteWRefs calcLeastCostPath(final Node fromNode, final Node toNode,
			final double startTime, HashMap<Link, EdgeIntervalls> flow) {

		this.flow = flow;

		// run the algorithm
		boolean found = false;

		// find shortest path with Moore-Bellman-Ford-Algorithm
		found = doCalculations(fromNode, toNode, startTime, flow);

		// no path (route) found
		if (pathToRoute == null) {
			return null;
		}

		if (timeHorizon < Dists.getDistance(toNode)) {
			return null;
		}
		if (!found){
			System.out.println("Warum?");
			return null;
		}

		// now reconstruct the route
		ArrayList<Node> routeNodes = new ArrayList<Node>();
		Node tmpNode = fromNode;
		routeNodes.add(tmpNode);
		if (pathToRoute.peek().getFromNode().equals(fromNode)
				|| pathToRoute.peek().getToNode().equals(fromNode)) {
			for (int i = 0; i < pathToRoute.size(); i++) {
				if (pathToRoute.get(i).getToNode().equals(tmpNode)) {
					tmpNode = pathToRoute.get(i).getFromNode();
				} else if (pathToRoute.get(i).getFromNode().equals(tmpNode)) {
					tmpNode = pathToRoute.get(i).getToNode();
				} else {
					System.out.println("ERROR: couldn't construct route!");
					return null;
				}
				routeNodes.add(tmpNode);
			}
		} else {
			for (int i = pathToRoute.size() - 1; i >= 0; i++) {
				if (pathToRoute.get(i).getToNode().equals(tmpNode)) {
					tmpNode = pathToRoute.get(i).getFromNode();
				} else if (pathToRoute.get(i).getFromNode().equals(tmpNode)) {
					tmpNode = pathToRoute.get(i).getToNode();
				} else {
					System.out.println("ERROR: couldn't construct route!");
					return null;
				}
				routeNodes.add(tmpNode);
			}
		}
		NetworkRouteWRefs route = new NodeNetworkRouteImpl(null, null);
		route.setLinks(null, RouteUtils.getLinksFromNodes(routeNodes), null);

		return route;
	}

	*//**
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
	 * @see org.matsim.core.router.util.LeastCostPathCalculator#calcLeastCostPath(org.matsim.core.network.Node,
	 *      org.matsim.core.network.Node, double)
	 *//*
	public ArrayList<Link> calcLeastCostLinkRoute(final Node fromNode,
			final Node toNode, final double startTime) {

		// run the algorithm
		boolean found = false;
		found = doCalculations(fromNode, toNode, startTime, flow);

		// no path (route) found
		if (pathToRoute == null) {
			return null;
		}

		if (timeHorizon < Dists.getDistance(toNode)) {
			return null;
		}
		if (!found){
			System.out.println("Warum?");
			return null;
		}

		// now reconstruct the route
		ArrayList<Link> routeLinks = new ArrayList<Link>();
		if (pathToRoute.peek().getFromNode().equals(fromNode)
				|| pathToRoute.peek().getToNode().equals(fromNode)) {
			for (int i = 0; i < pathToRoute.size(); i++) {
				routeLinks.add(pathToRoute.get(i));
			}
		} else {
			for (int i = pathToRoute.size() - 1; i >= 0; i--) {
				routeLinks.add(pathToRoute.get(i));
			}
		}

		return routeLinks;
	}

	*//**
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
	 * @see org.matsim.core.router.util.LeastCostPathCalculator#calcLeastCostPath(org.matsim.core.network.Node,
	 *      org.matsim.core.network.Node, double)
	 *//*
	public ArrayList<Link> calcLeastCostLinkRoute(final Node fromNode,
			final Node toNode, final double startTime,
			HashMap<Link, EdgeIntervalls> flow) {
		this.flow = flow;
		// run the algorithm
		boolean found = false;
		found = doCalculations(fromNode, toNode, startTime, flow);
		// no path (route) found
		if (pathToRoute == null) {
			return null;
		}
		if (timeHorizon < Dists.getDistance(toNode)) {
			return null;
		}
		if (!found){
			System.out.println("Warum?");
			return null;
		}
		// now reconstruct the route
		ArrayList<Link> routeLinks = new ArrayList<Link>();
		if (pathToRoute.peek().getFromNode().equals(fromNode)
				|| pathToRoute.peek().getToNode().equals(fromNode)) {
			for (int i = 0; i < pathToRoute.size(); i++) {
				routeLinks.add(pathToRoute.get(i));
			}
		} else {
			for (int i = pathToRoute.size() - 1; i >= 0; i--) {
				routeLinks.add(pathToRoute.get(i));
			}
		}
		return routeLinks;
	}

	*//**
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
	 * @see org.matsim.core.router.util.LeastCostPathCalculator#calcLeastCostPath(org.matsim.core.network.Node,
	 *      org.matsim.core.network.Node, double)
	 *//*
	public HashMap<Link, EdgeIntervalls> calcLeastCostFlow(final Node fromNode,
			final Node toNode, final double startTime) {

		// run the algorithm
		boolean found = false;

		// calculate path
		found = doCalculations(fromNode, toNode, startTime, flow);
		if (pathToRoute == null) {
			return null;
		}
		if (pathToRoute == null) {
			return null;
		}

		if (!found){
			System.out.println("Warum?");
			return null;
		}

		System.out.println("Wait:");
		for(Node node : network.getNodes().values()){
			System.out.println("To reach " + node.getId() + " wait " + waited.get(node) + " TU.");
		}

		gamma = calculateGamma(fromNode, toNode, pathToRoute);

		if(gamma == 0){
			return null;
		}

		// augment flow
		Node tmpNode = fromNode;
		Link tmpLink;
		Node node = tmpNode;
		int dist;
		HashMap<Link, EdgeIntervalls> newFlow = flow;
		while (!pathToRoute.isEmpty()) {
			tmpLink = pathToRoute.poll();
			dist = Dists.getDistance(tmpLink.getFromNode());
			// backward edge
			if (tmpNode.equals(tmpLink.getToNode())) {
				newFlow.get(tmpLink).augmentreverse(dist, gamma);
				node = tmpLink.getFromNode();
			}
			// forward edge
			else if (tmpNode.equals(tmpLink.getFromNode())) {
				newFlow.get(tmpLink).augment(dist, gamma,
						(int) (tmpLink.getCapacity(1.)));
				node = tmpLink.getToNode();
			} else {
				System.out.println("Error with LinkedList path!");
			}
			tmpNode = node;
		}
		return newFlow;
	}

	*//**
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
	 * @see org.matsim.core.router.util.LeastCostPathCalculator#calcLeastCostPath(org.matsim.core.network.Node,
	 *      org.matsim.core.network.Node, double)
	 *//*
	public HashMap<Link, EdgeIntervalls> calcLeastCostFlow(final Node fromNode,
			final Node toNode, final double startTime,
			HashMap<Link, EdgeIntervalls> flow) {

		this.flow = flow;

		// run the algorithm
		boolean found = false;

		// calculate path
		found = doCalculations(fromNode, toNode, startTime, flow);
		if (pathToRoute == null) {
			return null;
		}
		if (pathToRoute == null) {
			return null;
		}

		if (!found){
			System.out.println("Warum?");
			return null;
		}

		System.out.println("Wait:");
		for(Node node : network.getNodes().values()){
			System.out.println("To reach " + node.getId() + " wait " + waited.get(node) + " TU.");
		}

		 System.out.println("Preds: ");
		 for (Node node : network.getNodes().values()) {
			 if (pred.get(node)==null) {
				 System.out.println("node " + node.getId() + " has no pred ");
			 }
			 else {
				 if (pred.get(node).getFromNode().equals(node)) {
					 System.out.println("node " + node.getId() + " has pred " +pred.get(node).getToNode().getId());
				 }
				 else if (pred.get(node).getToNode().equals(node)) {
					 System.out.println("node " + node.getId() + " has pred " + pred.get(node).getFromNode().getId());
				 }
				 else {
					 System.out.println("node " + node.getId() + " has pred Error");
				 }
			 }
		}
		System.out.println();
		
		gamma = calculateGamma(fromNode, toNode, pathToRoute);
		System.out.println("Gamma: " + gamma);

		if(gamma == 0){
			return null;
		}

		// augment flow
		Node tmpNode = fromNode;
		Link tmpLink;
		Node node = tmpNode;
		int dist;
		HashMap<Link, EdgeIntervalls> newFlow = flow;
		int tmp = 0;
		//System.out.println("pathToRoute empty (augment flow)? " + pathToRoute.isEmpty());
		while (!pathToRoute.isEmpty()) {
			tmpLink = pathToRoute.poll();
			// System.out.println("2 augment with node " + tmpNode.getId() +
			// "link (" + tmpLink.getFromNode().getId() + "," +
			// tmpLink.getToNode().getId() + ")");
			dist = Dists.getDistance(tmpLink.getFromNode());
			// backward edge
			if (tmpNode.equals(tmpLink.getToNode())) {
				tmp += waited.get(tmpLink.getFromNode());
				newFlow.get(tmpLink).augmentreverse(tmp, gamma);
				tmp -= length.getLinkTravelCost(tmpLink, 0.);
				node = tmpLink.getFromNode();
			}
			// forward edge
			else if (tmpNode.equals(tmpLink.getFromNode())) {
				tmp += waited.get(tmpLink.getToNode());
				newFlow.get(tmpLink).augment(tmp, gamma,
						(int) (tmpLink.getCapacity(1.)));
				tmp += length.getLinkTravelCost(tmpLink, 0.);
				node = tmpLink.getToNode();
			} else {
				System.out.println("Error with LinkedList path!");
			}
			tmpNode = node;
		}
		return newFlow;
	}

	
	 * this is the Moore-Bellman-Ford Algorithm on the residual network with
	 * flow
	 *
	 
	private boolean doCalculations(final Node fromNode, final Node toNode,
			final double startTime, final HashMap<Link, EdgeIntervalls> flow) {
		// outprints
		
		 * for (Link link : network.getLinks().values()) {
		 * System.out.println("(" + link.getFromNode().getId() + ", " +
		 * link.getToNode().getId() + ") hat Laenge " +
		 * length.getLinkTravelCost(link, 0.)); }
		 

		// set the start distances Dists of the vertices
		init(fromNode);

		// queue to save nodes we have to scan
		Queue<Node> queue = new LinkedList<Node>();
		queue.add(fromNode);

		// v is first vertex in the queue
		// w is the vertex we probably want to insert to the queue or to
		// decrease the distance
		Node v, w;
		// dist is the distance from the source to w over v
		int dist;

		// mainloop
		while (!queue.isEmpty()) {
			// gets the first vertex in the queue
			v = queue.poll();

			// visit neighbors
			// link is outgoing edge of v => forward edge
			for (Link link : v.getOutLinks().values()) {
				w = link.getToNode();

				// compute new distance to neighbor
				if (!Dists.getDistance(v).equals(Integer.MAX_VALUE)) {
					dist = (int) length.getLinkTravelCost(link, 1.)
							+ Dists.getDistance(v);
				} else {
					dist = Integer.MAX_VALUE;
				}

				// update the distance of w
				if (Dists.getDistance(w) > dist) {
					// flow could be send at time dist over link
					if (!Dists.getDistance(v).equals(Integer.MAX_VALUE)) {
						//flow.get(link);
						//System.out.println("Flow: " + flow.get(link).getFlowAt(Dists.getDistance(v)));
						//System.out.println("Capacity: " + (int) (link.getCapacity(1.)));
						if (flow.get(link).getFlowAt(Dists.getDistance(v)) < (int) (link.getCapacity(1.))) {
							//System.out.println("forward if");
							visitNode(v, w, dist);
							pred.put(w, link);
							waited.put(w, 0);
							//System.out.println(v.getId() + " wartet " + waited.get(w) + " ZE");
							if (!queue.contains(w)) {
								queue.add(w);
							}
						}
						// flow couldn't be send at time dist, but at time t
						// over link
						else {
							//EdgeIntervall tmpInt = flow.get(link).minPossible(dist, (int) (link.getCapacity(1.)));
							//System.out.println("tmpInt == null? " + (tmpInt == null));
							EdgeIntervall tmpInt = flow.get(link).getIntervallAt(Dists.getDistance(v));
							while(!(tmpInt.getFlow() < link.getCapacity(1.))  && !(tmpInt.equals(flow.get(link).getLast()))){
								tmpInt = flow.get(link).getNext(tmpInt);
							}
							if(tmpInt.equals(flow.get(link).getLast())){
								//System.out.println("tmpInt == null? " + (tmpInt == null));
								//System.out.println("tmpInt == Intervall of Dists(v) " + (tmpInt.equals(flow.get(link).getIntervallAt(Dists.getDistance(v)))));
								if (!(tmpInt == null) && !(tmpInt.equals(flow.get(link).getIntervallAt(Dists.getDistance(v))))) {
									int t = tmpInt.getLowBound() + (int) (length.getLinkTravelCost(link, 1));
									//System.out.println("Dist: " + Dists.getDistance(w));
									//System.out.println("t: " + t);
									if (Dists.getDistance(w) > t) {
										//System.out.println("forward else");
										visitNode(v, w, t);
										pred.put(w, link);
										//System.out.println("1 t: " + t + " - dist: " + dist);
										waited.put(w, t - dist);
										//System.out.println(v.getId() + " wartet " + waited.get(w) + " ZE");
										if (!queue.contains(w)) {
											queue.add(w);
										}
									}
								}
							}
							else if(tmpInt.getFlow() < link.getCapacity(1.)){
								int t = tmpInt.getLowBound() + (int) (length.getLinkTravelCost(link, 1));
								//System.out.println("Dist: " + Dists.getDistance(w));
								//System.out.println("t: " + t);
								//System.out.println("last intervall");
								if (Dists.getDistance(w) > t) {
									//System.out.println("forward else");
									visitNode(v, w, t);
									pred.put(w, link);
									//System.out.println("2 t: " + t + " - dist: " + dist);
									waited.put(w, t - dist);
									//System.out.println(v.getId() + " wartet " + waited.get(w) + " ZE");
									if (!queue.contains(w)) {
										queue.add(w);
									}
								}
							}
						}
					}
				}
			}
			// link is incomming edge of v => backward edge
			for (Link link : v.getInLinks().values()) {
				w = link.getFromNode();

				// compute new distance to neighbor
				if (!Dists.getDistance(v).equals(Integer.MAX_VALUE)) {
					dist = Dists.getDistance(v)
							- (int) length.getLinkTravelCost(link, 1.);
				} else {
					dist = Integer.MAX_VALUE;
				}

				if (dist >= 0) {
					// update the distance of w
					if (Dists.getDistance(w) > dist) {
						// flow could be send at time dist over link
						if (!Dists.getDistance(w).equals(Integer.MAX_VALUE)) {
							if (flow.get(link).getFlowAt(dist) > 0) {
								//System.out.println("backward if");
								visitNode(v, w, dist);
								pred.put(w, link);
								waited.put(v, 0);
								//System.out.println(w.getId() + " wartet " + waited.get(v) + " ZE");
								if (!queue.contains(w)) {
									queue.add(w);
								}
							}

							// flow couldn't be send at time dist, but at
							// time t over link
							else {
								int t = dist;
								EdgeIntervall tmpInt = flow.get(link)
										.getIntervallAt(dist);
								while (tmpInt.getFlow() == 0
										&& !tmpInt.equals(flow.get(link)
												.getLast())) {
									tmpInt = flow.get(link).getNext(tmpInt);
								}
								if (tmpInt.equals(flow.get(link).getLast())) {
									if(tmpInt.getFlow() > 0){
										t = tmpInt.getLowBound() + (int) (length.getLinkTravelCost(link, 1));
										if (Dists.getDistance(w) > t) {
										//System.out.println("backward else");
										visitNode(v, w, t);
										pred.put(w, link);
										//System.out.println("3 t: " + t + " - dist: " + dist);
										waited.put(v, t - dist);
										//System.out.println(w.getId() + " wartet " + waited.get(v) + " ZE");
											if (!queue.contains(w)) {
											queue.add(w);
											}
										}
									}
								} else if(tmpInt.getFlow() > 0) {
									t = tmpInt.getLowBound() + (int) (length.getLinkTravelCost(link, 1));
									if (flow.get(link).getFlowAt(t) > 0) {
										if (Dists.getDistance(w) > t) {
											visitNode(v, w, t);
											pred.put(w, link);
											waited.put(v, t - dist);
											//System.out.println("4 t: " + t + " - dist: " + dist);
											//System.out.println(w.getId() + " wartet " + waited.get(v) + " ZE");
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
		//printAll();
		// calculate shortest path with back-tracking and send flow in find
		// ShortestPath
		if (Dists.getDistance(toNode) == Integer.MAX_VALUE) {
			pathToRoute = null;
			System.out.println("No path found!");
			return false;
		} else if (Dists.getDistance(toNode) > timeHorizon) {
			pathToRoute = null;
			System.out.println("Out of time horizon!");
			System.out.println(timeHorizon + " < " + Dists.getDistance(toNode));
			return false;
		} else {
			pathToRoute = findPath(fromNode, toNode);
			return true;
		}
	}

	*//**
	 * Initializes the nodes of the network
	 *
	 * @param fromNode
	 *            The starting node
	 *//*
	void init(final Node fromNode) {
		// Distances
		Dists = new Distances(network);
		for (Node node : network.getNodes().values()) {
			if (node.equals(fromNode)) {
				Dists.setDistance(node, 0);
			} else {
				Dists.setDistance(node, Integer.MAX_VALUE);
			}
		}
		// other global variables
		if (!pathToRoute.isEmpty()) {
			pathToRoute.clear();
		}
		gamma = Integer.MAX_VALUE;
		for (Node node : network.getNodes().values()) {
			pred.put(node, null);
			waited.put(node, 0);
		}
	}

	// with arrivesAt
	void visitNode(final Node fromNode, final Node toNode, int arrivesAt) {
		int tmpTime = Dists.getMinTime(toNode);
		if (arrivesAt < tmpTime) {
			Dists.setDistance(toNode, arrivesAt);
		}
	}

	// try to use visitNode without arrivesAt
	boolean visitNode(final Node fromNode, final Node toNode) {
		int tmpTime = Dists.getMinTime(toNode);
		boolean found = false;
		Link thisLink = network.getLinks().get(new IdImpl("1"));
		// try to find link from fromNode to toNode
		boolean forward, backward, flowPossibleForward, flowPossibleBackward;

		// TODO use getInLinks() and getOutLinks() oder auch nicht
		for (Link link : network.getLinks().values()) {
			forward = (link.getFromNode().equals(fromNode))
					&& (link.getToNode().equals(toNode));
			backward = (link.getFromNode().equals(toNode))
					&& (link.getToNode().equals(fromNode));
			flowPossibleForward = flow.get(link).getFlowAt(tmpTime) < (int) (link
					.getCapacity(1.));
			flowPossibleBackward = flow.get(link).getFlowAt(tmpTime) > 0;
			if ((forward && flowPossibleForward)
					|| (backward && flowPossibleBackward)) {
				found = true;
				thisLink = link;
				break;
			}
		}
		// see above with arrivesAt
		if (found == true) {
			int arrivesAt = Dists.getDistance(fromNode)
					+ (int) (Math.ceil(((LinkImpl)thisLink).getEuklideanDistance()));
			if (arrivesAt < tmpTime) {
				Dists.setDistance(toNode, arrivesAt);
			}
		}
		// returns that eigther we have found an edge (true) or not (false)
		return found;
	}

	// this is a method to find the shortest path with back-tracking and the
	// distance labels and send flow on it
	// LinkedList<Link> findShortestPath(final Node fromNode, final Node toNode)
	// {
	LinkedList<Link> findPath(final Node fromNode, final Node toNode) {
		LinkedList<Link> path = new LinkedList<Link>();
		Node tmpNode = toNode;
		Node node = tmpNode;
		Link tmpLink;
		while (!tmpNode.equals(fromNode)) {
			tmpLink = pred.get(tmpNode);
			if (tmpLink.getFromNode().equals(tmpNode)) {
				node = tmpLink.getToNode();
				//path.add(tmpLink);
				path.addFirst(tmpLink);
			} else if (tmpLink.getToNode().equals(tmpNode)) {
				node = tmpLink.getFromNode();
				path.addFirst(tmpLink);
				//path.add(tmpLink);
			} else {
				System.out
						.println("AAAAAAAAAAAAAAAHHHHHHHHHHHHHHHHHHH!!!!!!!!!!!!!");
			}
			if (!node.equals(tmpNode)) {
				tmpNode = node;
			} else {
				System.out.println("TOT");
			}
		}
		// printPath(path);
		printWaitPath(path, fromNode);
		return path;
	}

	void printAll() {
		Dists.printAll();
	}

	void printPath(LinkedList<Link> path) {
		System.out.print("Path: ");
		for (Link link : path) {
			System.out.print("(" + link.getFromNode().getId() + ","
					+ link.getToNode().getId() + ") ");
		}
		System.out.println();
	}

	// TODO
	void printWaitPath(LinkedList<Link> path, Node fromNode) {
		System.out.print("Path: ");
		Node tmpNode = fromNode;
		Link tmpLink;
		Node node = fromNode;
		for (int i = 0; i < path.size(); i++) {
			tmpLink = path.get(i);
			if(tmpLink.getFromNode().equals(tmpNode)){
				node = tmpLink.getToNode();
				for(int j = 0; j < waited.get(node); j++){
					System.out.print("(" + tmpNode.getId() + "," + tmpNode.getId() + ") ");
				}
			}
			else if(tmpLink.getToNode().equals(tmpNode)){
				node = tmpLink.getFromNode();
				for(int j = 0; j < waited.get(node); j++){
					System.out.print("(" + tmpNode.getId() + "," + tmpNode.getId() + ") ");
				}
			}
			else{
				System.out.println("Error while trying to print out!");
				break;
			}
			System.out.print("(" + tmpLink.getFromNode().getId() + ","
					+ tmpLink.getToNode().getId() + ") ");
			tmpNode = node;

		}
		System.out.println();
	}

	int calculateGamma(Node fromNode, Node toNode, LinkedList<Link> pathToRoute) {
		gamma = Integer.MAX_VALUE;
		//System.out.println("pathToRoute empty (calculateGamma)? " + pathToRoute.isEmpty());
		LinkedList<Link> path =  pathToRoute;
		Node tmpNode = fromNode;
		Link tmpLink;
		Node node = fromNode;
		int dist;
		int tmp = 0;
		for(int i = 0; i < path.size(); i++){
			tmpLink = path.get(i);
			if (!(tmpLink == null)) {
				dist = Dists.getDistance(tmpLink.getFromNode());
				// backward edge
				if (tmpNode.equals(tmpLink.getToNode())) {
					node = tmpLink.getFromNode();
					tmp += waited.get(node);
					if (gamma > flow.get(tmpLink).getFlowAt(tmp)) {
						gamma = flow.get(tmpLink).getFlowAt(tmp);
					}
					tmp -= length.getLinkTravelCost(tmpLink, 0.);
				}
				// forward edge
				else if (tmpNode.equals(tmpLink.getFromNode())) {
					node = tmpLink.getToNode();
					tmp += waited.get(node);
					if (gamma > ((int) (tmpLink.getCapacity(1.)) - flow.get(
							tmpLink).getFlowAt(tmp))) {
						gamma = (int) (tmpLink.getCapacity(1.))
								- flow.get(tmpLink).getFlowAt(tmp);
					}
					tmp += length.getLinkTravelCost(tmpLink, 0.);
				}
			} else {
				System.out
						.println("Error with HashMap preds! (calculateGamma)");
			}
			tmpNode = node;
		}
		//System.out.println("pathToRoute empty (calculateGamma2)? " + pathToRoute.isEmpty());
		return gamma;
	}
}*/