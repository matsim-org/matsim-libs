/* *********************************************************************** *
 * project: org.matsim.*
 * NetworkDijkstra.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.lnicolas.routing;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.PriorityQueue;

import org.apache.log4j.Logger;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Route;
import org.matsim.router.util.LeastCostPathCalculator;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.utils.misc.TreeMultiMap;

public class NetworkDijkstra implements LeastCostPathCalculator {

//	private final static String ROLE_NAME = "NetworkDijkstra";

	private final NetworkLayer network;
	private final TravelCost costFunction_;
	private final TravelTime timeFunction_;
	private static int roleIndex_ = -1;

	long totalResetVisitedTime = 0;

	double avgRouteLength = 0;
	double avgTravelTime = 0;
	int routeCnt = 0;

	private final static Logger log = Logger.getLogger(NetworkDijkstra.class);
	
	/**
	 * A constant for the export to GDF (Guess file format) of the route.
	 */
	final int exportedMarkedNodeSize = 1024;

	public NetworkDijkstra(final NetworkLayer network, final TravelCost costFunction,
			final TravelTime timeFunction) {
		this.network = network;
		this.costFunction_ = costFunction;
		this.timeFunction_ = timeFunction;
		if (roleIndex_ == -1) roleIndex_ = network.requestNodeRole();
	}

	public Route calcLeastCostPath(final Node fromNode, final Node toNode, final double starttime) {
		// run either the PQ (PriorityQueue) or the MM (MultiMap) version of the algorithm
		// for me, PQ was about 10% faster than the MM version despite all the needed list.remove()
		// I guess the complex tree-structure behind the MultiMap eats too much CPU time
		return calcCheapestRoute_PQ(fromNode, toNode, starttime);
//		return calcCheapestRoute_MM(fromNode, toNode, starttime);
	}

	// calcs the route using a PriorityQueue to store the pending nodes
	private final Route calcCheapestRoute_PQ(final Node fromNode, final Node toNode, final double startTime) {
		double arrivalTime = 0;

		long now = System.currentTimeMillis();
		// first make sure the cost and visit flags are all reset
		for (Node node : this.network.getNodes().values()) {
			DijkstraRole role = getDijkstraRole(node);
			role.resetVisited();
		}
		this.totalResetVisitedTime += System.currentTimeMillis() - now;

		// now start the dijkstra-algorithm
		boolean stillSearching = true;

		DijkstraRoleCostComparator comparator = new DijkstraRoleCostComparator(roleIndex_);
		PriorityQueue<Node> pendingNodes = new PriorityQueue<Node>(100, comparator);
		pendingNodes.add(fromNode);
		DijkstraRole role = getDijkstraRole(fromNode);
		role.visit(null, 0, startTime);

		// loop over pendingNodes, always handling the first one.
		while (stillSearching) {
			Node outNode = pendingNodes.poll();
			if (outNode == null) {
			log.warn("No route was found from node " + fromNode.getId() + " to node " + toNode.getId() + " .");
				return null;
			}

			if (outNode.getId() == toNode.getId()) {
				stillSearching = false;
				DijkstraRole outRole = getDijkstraRole(outNode);
				arrivalTime = outRole.getTime();
				break;
			}

			DijkstraRole outRole = getDijkstraRole(outNode);
			double currTime = outRole.getTime();
			double currCost = outRole.getCost();
			for (Link l : outNode.getOutLinks().values()) {
				Node n = l.getToNode();
				DijkstraRole nRole = getDijkstraRole(n);
				double travelTime = this.timeFunction_.getLinkTravelTime(l, currTime);
				double travelCost = this.costFunction_.getLinkTravelCost(l, currTime);
				double nCost = nRole.getCost();
				if (!nRole.isVisited()) {
					nRole.visit(outNode, currCost + travelCost, currTime + travelTime);
					pendingNodes.add(n);
				} else if (currCost + travelCost < nCost) {
					// PriorityQueue.remove() uses the comparator given at instanciating to find the matching Object.
					// This can lead to removing a wrong object which happens to have the same key for comparision, but
					// is a completly different object...
					// Thus we tell the comparator to check the IDs too if two objects are considered "equal"
					comparator.setCheckIDs(true);
					pendingNodes.remove(n);
					comparator.setCheckIDs(false);

					nRole.visit(outNode, currCost + travelCost, currTime + travelTime);

					pendingNodes.add(n);
				}
			}
		}

		// now construct the route
		ArrayList<Node> routeNodes = new ArrayList<Node>();
		Node tmpNode = toNode;
		while (tmpNode != fromNode) {
			routeNodes.add(0, tmpNode);
			DijkstraRole tmpRole = getDijkstraRole(tmpNode);
			tmpNode = tmpRole.getPrevNode();
		}
		routeNodes.add(0, tmpNode);	// add the fromNode at the beginning of the list

		Route route = new Route();
		route.setRoute(routeNodes);
		route.setTravTime((int)(arrivalTime - startTime));

		this.avgTravelTime = (this.routeCnt * this.avgTravelTime + route.getTravTime())
			/ (this.routeCnt + 1);
		this.avgRouteLength = (this.routeCnt * this.avgRouteLength + route.getDist())
			/ (this.routeCnt + 1);
		this.routeCnt++;

		return route;
	}


	private final Route calcCheapestRoute_MM(final Node fromNode, final Node toNode, final int starttime) {
		// first make sure the cost and visit flags are all reset
		for (Node node : this.network.getNodes().values()) {
			DijkstraRole role = getDijkstraRole(node);
			role.resetVisited();
		}

		// now start the dijkstra-algorithm
		boolean stillSearching = true;

		TreeMultiMap<Integer, Node> pendingNodes = new TreeMultiMap<Integer, Node>();
		DijkstraRole fromRole = getDijkstraRole(fromNode);
		pendingNodes.put(new Integer((int)fromRole.getCost()), fromNode);
		fromRole.visit(null, 0, starttime);

		// loop over pendingNodes, always handling the first one.
		while (stillSearching) {
			Node outNode = pendingNodes.remove(pendingNodes.firstKey());
			DijkstraRole outRole = getDijkstraRole(outNode);
			if (outNode == null) {
				log.warn("No route was found from node " + fromNode.getId() + " to node " + toNode.getId() + " .");
				return null;
			}
			if (!outRole.isHandled) {
//				System.out.println(outNode.getCost());
				if (outNode.getId() == toNode.getId()) {
					stillSearching = false;
					break;
				}
				outRole.isHandled = true;

				double currTime = outRole.getTime();
				double currCost = outRole.getCost();
				for (Link l : outNode.getOutLinks().values()) {
					Node n = l.getToNode();
					DijkstraRole nRole = getDijkstraRole(n);
					double travelTime = this.timeFunction_.getLinkTravelTime(l, currTime);
					double travelCost = this.costFunction_.getLinkTravelCost(l, currTime);
					double nCost = nRole.getCost();
					if (!nRole.isVisited()) {
						nRole.visit(outNode, currCost + travelCost, currTime + travelTime);
						pendingNodes.put(new Integer((int)nRole.getCost()), n);
					} else if (currCost + travelCost < nCost) {
						// PriorityQueue.remove() uses the comparator given at instanciating to find the matching Object.
						// This can lead to removing a wrong object which happens to have the same key for comparision, but
						// is a completly different object...
						// Thus we tell the comparator to check the IDs too if two objects are considered "equal"

						nRole.visit(outNode, currCost + travelCost, currTime + travelTime);

						pendingNodes.put(new Integer((int)nRole.getCost()), n);
					}
				}
			} else {
//				System.out.println("ignoring node " + outNode.getId());
			}
		}

		// now construct the route
		ArrayList<Node> route = new ArrayList<Node>();
		Node tmpNode = toNode;
		while (tmpNode != fromNode) {
			route.add(0, tmpNode);
			DijkstraRole tmpRole = getDijkstraRole(tmpNode);
			tmpNode = tmpRole.getPrevNode();
		}
		route.add(0, tmpNode);	// add the fromNode at the beginning of the list

		Route retVal = new Route();
		retVal.setRoute(route);
		return retVal;
	}

	public void printInformation() {
		System.out.println("Number of routes: " + this.routeCnt);
		System.out.println("Average route length: " + this.avgRouteLength);
		System.out.println("Average travel time per route: " + this.avgTravelTime);
	}

	private DijkstraRole getDijkstraRole(final Node n) {
		DijkstraRole r = (DijkstraRole)n.getRole(roleIndex_);
		if (null == r) {
			r = new DijkstraRole();
			n.setRole(roleIndex_, r);
		}
		return r;
	}

	private class DijkstraRole {
		private boolean visited_ = false;
		private Node prev_ = null;
		private double cost_ = 0;
		private double time_ = 0;
		private boolean isHandled = false;

		public void resetVisited() {
			this.visited_ = false;
			this.prev_ = null;
			this.isHandled = false;
		}

		/**
		 * returns true if the visited-flag or the arrival time changed, false otherwise
		 */
		public boolean visit(final Node comingFrom, final double cost, final double time) {
			this.visited_ = true;
			this.prev_ = comingFrom;
			this.cost_ = cost;
			this.time_ = time;
			return true;
		}

		public boolean isVisited() {
			return this.visited_;
		}

		public double getCost() {
			return this.cost_;
		}

		public double getTime() {
			return this.time_;
		}

		public Node getPrevNode() {
			return this.prev_;
		}

	};

	public static class DijkstraRoleCostComparator implements Comparator<Node> {

		private boolean checkIDs_ = false;
		private final int roleIndex__;

		public DijkstraRoleCostComparator(final int roleIndex) {
			this.roleIndex__ = roleIndex;
		}

		public int compare(final Node n1, final Node n2) {
			DijkstraRole r1 = (DijkstraRole)n1.getRole(this.roleIndex__);
			DijkstraRole r2 = (DijkstraRole)n2.getRole(this.roleIndex__);

			double c1 = 0;	// if a node
			double c2 = 0;

			if (r1 != null) {
				c1 = r1.cost_;
			}
			if (r2 != null) {
				c2 = r2.cost_;
			}

			if (c1 < c2) {
				return -1;
			} else if (c1 == c2) {
				if (this.checkIDs_) {
					int id1 = Integer.parseInt(n1.getId().toString());
					int id2 = Integer.parseInt(n2.getId().toString());
					if (id1 < id2) {
						return -1;
					} else if (id1 == id2) {
						return 0;
					} else {
						return +1;
					}
				} else {
					return 0;
				}
			} else {
				return +1;
			}
		}

		public void setCheckIDs(final boolean flag) {
			this.checkIDs_ = flag;
		}

	}

	public long getAvgResetVisitedTime() {
		return this.totalResetVisitedTime;
	}
}
