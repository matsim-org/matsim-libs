/* *********************************************************************** *
 * project: org.matsim.*
 * PathSetGenerator.java
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

package playground.balmermi.algos;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;

import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.plans.Route;
import org.matsim.router.AStarLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelTimeI;
import org.matsim.trafficmonitoring.TravelTimeCalculatorArray;

public class RouteSetGenerator {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final NetworkLayer network;
	private final TravelTimeI timeFunction;
	private final PreProcessLandmarks preProcessData;
	private final AStarLandmarks router;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public RouteSetGenerator(NetworkLayer network) {
		this.network = network;
		this.timeFunction = new TravelTimeCalculatorArray(network);
		this.preProcessData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
		this.preProcessData.run(network);
		this.router = new AStarLandmarks(this.network,this.preProcessData,this.timeFunction);
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private void addLinkToNetwork(Link link) {
		link.getFromNode().addOutLink(link);
		link.getToNode().addInLink(link);
	}

	private void removeLinkFromNetwork(Link link) {
		link.getFromNode().removeOutLink(link);
		link.getToNode().removeInLink(link);
	}

	private boolean containsRoute(Route route, LinkedList<Route> routes) {
		ArrayList<Node> nodes = route.getRoute();
		Iterator<Route> r_it = routes.iterator();
		while (r_it.hasNext()) {
			ArrayList<Node> ns = r_it.next().getRoute();
			if (ns.size() == nodes.size()) {
				boolean is_equal = true;
				for (int i=0; i<ns.size(); i++) {
					if (!ns.get(i).getId().equals(nodes.get(i).getId())) { is_equal = false; }
				}
				if (is_equal) { return true; }
			}
		}
		return false;
	}

	private final void calcRouteOnSubNet(final Node o, final Node d, final int k, final int time, final LinkedList<Link[]> links, final LinkedList<Route> routes) {

		// the list to handle for the next level (level d+1) of the tree
		LinkedList<Link[]> new_links = new LinkedList<Link[]>();

		System.out.println("--- start a level of the tree ---");
//		System.out.println("  links.size = " + links.size() + ", routes.size = " + routes.size() + ", new_links.size = " + new_links.size());

		// go through all given lists at this level (level d) of the tree
		while (!links.isEmpty()) {
			Link[] ls = links.poll();

			// remove the links of the current link set and calc the least cost path
			for (int i=0; i<ls.length; i++) { this.removeLinkFromNetwork(ls[i]); }
//			System.out.println("    ---");
//			System.out.println("    removed " + ls.length + " links from the net");
			Route route = this.router.calcLeastCostPath(o,d,time);

			// add it to the resulting list of routes if exists. Also, create the link sets for
			// the next level (d+1) of the tree for the current link set
			// TODO: add the route only if not already exists!!!
			if ((route != null) && !this.containsRoute(route,routes)) {
				routes.add(route);
//				System.out.println("    -> route found with " + route.getLinkRoute().length + " links");

				// for each link of the calc route create a new link set with the
				// other links of the current link set
				Link[] route_links = route.getLinkRoute();
				for (int j=0; j<route_links.length; j++) {
					Link[] new_ls = new Link[ls.length+1];
					for (int jj=0; jj<ls.length; jj++) { new_ls[jj] = ls[jj]; }
					new_ls[new_ls.length-1] = route_links[j];
					new_links.addLast(new_ls);
				}
//				System.out.println("    -> links.size = " + links.size() + ", routes.size = " + routes.size() + ", new_links.size = " + new_links.size());
			}

			// restore the full network
			for (int i=0; i<ls.length; i++) { this.addLinkToNetwork(ls[i]); }
//			System.out.println("    restored " + ls.length + " links from the net");
//			System.out.println("    ---");
		}

		System.out.println("---  end a level of the tree  ---");
		// go to the next level (d+1) of the tree, if not already enough routes are found
		if ((routes.size() < k) && !new_links.isEmpty()) { this.calcRouteOnSubNet(o,d,k,time,new_links,routes); }
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	public final LinkedList<Route> calcRouteSet(final Node o, final Node d, final int k, final int time, final int var_factor) {
		if (o.getId().toString().equals(d.getId().toString())) { Gbl.errorMsg("O == D not alloed!"); }
		if (k < 1) { Gbl.errorMsg("k < 1 not allowed!"); }

		LinkedList<Route> routes = new LinkedList<Route>(); // resulting k least cost routes
		LinkedList<Link[]> links = new LinkedList<Link[]>(); // removed links
		Route route = this.router.calcLeastCostPath(o,d,time);
		if (route == null) { Gbl.errorMsg("There is no route from " + o.getId() + " to " + d.getId() + "!"); }
//		routes.add(route);

		Link[] ls = route.getLinkRoute();
		for (int i=0; i<ls.length; i++) {
			Link[] lls = new Link[1];
			lls[0] = ls[i];
			links.add(lls);
		}
		// creating a route set with the minimum of k*var_factor routes
		this.calcRouteOnSubNet(o,d,k*var_factor,time,links,routes);

		System.out.println("--- Number of created routes = " + routes.size() + " ---");
		System.out.println("--- Randomly removing routes until " + k + " routes left... ---");
		// Remove randomly some routes until it contains k-1 elements
		while (k-1 < routes.size()) { routes.remove((int)Math.random()*routes.size()); }
		// add the least cost path at the beginning of the route
		routes.addFirst(route);
		System.out.println("--- done. ---");

		return routes;
	}
}
