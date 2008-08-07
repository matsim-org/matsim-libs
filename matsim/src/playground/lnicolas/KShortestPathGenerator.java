/* *********************************************************************** *
 * project: org.matsim.*
 * KShortestPathGenerator.java
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

package playground.lnicolas;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.TreeMap;
import java.util.TreeSet;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.Route;
import org.matsim.router.AStarLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCost;
import org.matsim.router.util.TravelTime;
import org.matsim.trafficmonitoring.TravelTimeCalculator;

/**
 * Calculates the k minimal cost paths from a start to an end node,
 * on a given network, based on a given TravelMinCost object.
 *
 * The algorithm works as follows:
 * <ol>
 * <li>Calculate the shortest path between the start and the end node and add it to the
 * set of pending routes and to the list of k-shortest paths
 * </li>
 * <li>Loop until k shortest paths have been found
 * 		<ol type="i">
 * 		<li>Take a route from the set of pending routes and store its links in a list L in
 * 		   order of ascending length
 * 		</li>
 * 		<li>Take the first link from L (remove it from L) and remove it from the network,
 * 			if it hasn't been removed before. Otherwise go to ii)
 * 		</li>
 * 		<li>Recalculate the shortest path on the new network. If the resulting route
 * 			 is not in the list of the k-shortest paths yet, add it to the k-shortest paths
 * 			 and to the set of pending routes
 * 		</li>
 * 		<li>If k shortest paths have not been found yet, go to ii) if L is not empty,
 * 			go to i) otherwise
 * 		</li>
 * 		</ol>
 * </li>
 * </ol>
 * @author lnicolas
 */
public class KShortestPathGenerator {

	private NetworkLayer network;
	private TravelCost costFunction;
	private TravelTime timeFunction;
	private ArrayList<Link[]> linkRoutes = new ArrayList<Link[]>();
	private PreProcessLandmarks preProcessData;


	/**
	 * Constructor.
	 * @param network The network on which the routing should be performed.
	 * @param costFunction Determines the cost of each link in the network.
	 * @param timeFunction Determines the travel time on each link in the network.
	 */
	public KShortestPathGenerator(NetworkLayer network,
			TravelCost costFunction, TravelTime timeFunction) {

		this.network = network;
		this.costFunction = costFunction;
		this.timeFunction = timeFunction;

		this.preProcessData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
		this.preProcessData.run(network);
	}

	/**
	 * Constructor.
	 * Takes the TravelTimeCalculator by default as costFunction.
	 * @param network The network on which the routing should be performed.
	 * @see #KShortestPathGenerator(NetworkLayer, TravelCost, TravelTime)
	 */
	public KShortestPathGenerator(NetworkLayer network) {
		this(network, new FreespeedTravelTimeCost(), new TravelTimeCalculator(network));
	}

	/**
	 * Returns {@code routeCount} paths from {@code fromNode} to {@code toNode}.
	 * Calculates the {@code routeCount}*{@code additionalRoutesFactor}
	 * (with additionalRoutesFactor = 3 for example) cheapest paths and returns
	 * {@code routeCount} random paths from it.
	 * @see KShortestPathGenerator#calcCheapestRoutes(Node, Node, int, int)
	 * @param fromNode The start node of the paths.
	 * @param toNode The end node of the paths.
	 * @param startTime The time at which the path should start.
	 * @param routeCount The number of paths that should be returned.
	 * @return The {@code routeCount} cheapest paths from {@code fromNode} to {@code endNode}.
	 */
	public Route[] calcCheapRoutes(Node fromNode, Node toNode,
			int startTime, int routeCount) {
		int addRoutesFactor = 3;

		Route[] routes = calcCheapestRoutes(fromNode, toNode, startTime, routeCount*addRoutesFactor);

		Route[] resultRoutes = new Route[routeCount];
		for (int i = 0; i < resultRoutes.length; i++) {
			int randomRouteIndex = (int)(Math.random()*addRoutesFactor);
			resultRoutes[i] = routes[i*addRoutesFactor + randomRouteIndex];
		}

		return resultRoutes;
	}

	/**
	 * Returns the {@code routeCount} minimal-cost paths from {@code fromNode} to {@code toNode}.
	 * @param fromNode The start node of the paths.
	 * @param toNode The end node of the paths.
	 * @param startTime The time at which the path should start.
	 * @param routeCount The number of paths that should be returned.
	 * @return The {@code routeCount} cheapest paths from {@code fromNode} to {@code endNode}.
	 */
	public Route[] calcCheapestRoutes(Node fromNode, Node toNode,
			int startTime, int routeCount) {
		if (routeCount == 0) {
			return new Route[0];
		}
		this.linkRoutes.clear();

		Route[] routes = new Route[routeCount];
		LinkedList<Route> pendingRoutes = new LinkedList<Route>();
		AStarLandmarks router = new AStarLandmarks(this.network, this.preProcessData,
				this.timeFunction);
		TreeSet<Id> processedLinks = new TreeSet<Id>();
		Route route = router.calcLeastCostPath(fromNode, toNode, startTime);
		if (route == null) {
			Gbl.errorMsg("No route found from node "
					+ fromNode.getId() + " to node "
					+ toNode.getId() + ".");
			return null;
		}
		System.out.println("|----+----|");
		int i = 0;
		routes[i] = route;
		pendingRoutes.add(route);
		i++;
		ArrayList<Link> removedLinks = new ArrayList<Link>();
		while ((i < routeCount) && (pendingRoutes.isEmpty() == false)) {
			route = pendingRoutes.poll();
			LinkedList<Link> pendingLinks = getLinksOrdered(route);
			Iterator<Link> it = pendingLinks.iterator();
			while (it.hasNext()) {
				Link l = it.next();
				if (processedLinks.contains(l.getId())) {
					it.remove();
				} else {
					processedLinks.add(l.getId());
				}
			}
			while ((i < routeCount) && (pendingLinks.isEmpty() == false)) {
				Link link = pendingLinks.poll();
				removeLinkFromNetwork(link);
				removedLinks.add(link);
				it = pendingLinks.iterator();
				while ((i < routeCount) && it.hasNext()) {
					link = it.next();

					removeLinkFromNetwork(link);
					route = router.calcLeastCostPath(fromNode, toNode,
							startTime);
					addLinkToNetwork(link);

					if (route == null) {
//						Gbl.errorMsg("No route found from node "
//								+ fromNode.getId() + " to node "
//								+ toNode.getId() + ".");
//						return null;
					} else {
						if (checkRoute(route) == true) {
							routes[i] = route;
							pendingRoutes.add(route);
							i++;

							if (i % (routeCount / 11) == 0) {
								System.out.print(".");
								System.out.flush();
							}
						}
					}
				}
			}
		}
		System.out.println("");
		for (Link l : removedLinks) {
			addLinkToNetwork(l);
		}

		return routes;
	}

	/**
	 * Checks whether the given route is already in the given routes array.
	 * @param route The route to check.
	 * @param routes The existing routes.
	 * @param cnt The number of existing routes.
	 * @param removedLinks
	 * @return false if the given route exists in routes, true otherwise.
	 */
	private boolean checkRoute(Route route) {
		Link[] links = route.getLinkRoute();

		for (int i = 0; i < this.linkRoutes.size(); i++) {
			Link[] ls = this.linkRoutes.get(i);
			if (ls.length == links.length) {
				int j = 0;
				while ((j < links.length)
						&& links[j].getId().equals(ls[j].getId())) {
					j++;
				}
				if (j == links.length) {
//					System.out.println("Route " + i + " and route " + linkRoutes.size()
//							+ " are equal!! (" + j + " links)");
					return false;
				}
			}
		}

		this.linkRoutes.add(links);
		return true;
	}

	/**
	 * Adds the given link to the inLinks of its toNode and to
	 * the outLinks of its fromNode.
	 * @param link The link to add.
	 */
	private void addLinkToNetwork(Link link) {
		link.getFromNode().addOutLink(link);
		link.getToNode().addInLink(link);
	}

	/**
	 * Removes the given link to the inLinks from its toNode and from
	 * the outLinks of its fromNode.
	 * @param link The link to remove.
	 */
	private void removeLinkFromNetwork(Link link) {
		link.getFromNode().removeOutLink(link);
		link.getToNode().removeInLink(link);
	}

	/**
	 * Returns the list of links the given route contains, ordered in ascending order
	 * by their cost.
	 * @param route The route for which to return the links.
	 * @return The list of links contained in the given route, ordered in ascending order by
	 * their cost.
	 */
	private LinkedList<Link> getLinksOrdered(Route route) {
		TreeMap<Double, Link> sortedMap = new TreeMap<Double, Link>();
		Link[] links = route.getLinkRoute();
		double time = route.getTravTime();
		for (Link link : links) {
			sortedMap.put(this.costFunction.getLinkTravelCost(link, time), link);
			time += this.timeFunction.getLinkTravelTime(link, time);
		}
		LinkedList<Link> orderedLinks = new LinkedList<Link>(sortedMap.values());
		return orderedLinks;
	}

}
