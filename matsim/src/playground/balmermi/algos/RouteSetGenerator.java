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

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.matsim.gbl.Gbl;
import org.matsim.gbl.MatsimRandom;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.population.routes.CarRoute;
import org.matsim.population.routes.NodeCarRoute;
import org.matsim.router.AStarLandmarks;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelTime;
import org.matsim.router.util.LeastCostPathCalculator.Path;
import org.matsim.trafficmonitoring.TravelTimeCalculator;

public class RouteSetGenerator {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final NetworkLayer network;
	private final TravelTime timeFunction;
	private final PreProcessLandmarks preProcessData;
	private final AStarLandmarks router;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public RouteSetGenerator(NetworkLayer network) {
		this.network = network;
		this.timeFunction = new TravelTimeCalculator(network);
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

	private boolean containsRoute(CarRoute route, LinkedList<CarRoute> routes) {
		List<Node> nodes = route.getNodes();
		Iterator<CarRoute> r_it = routes.iterator();
		while (r_it.hasNext()) {
			List<Node> ns = r_it.next().getNodes();
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

	private boolean isLocalRoute(CarRoute route) {
		boolean isLocal = true;
		for (Link routeLink : route.getLinks()) {
			System.out.print(routeLink.getType()+", ");
			if (!routeLink.getType().equals("39") && !routeLink.getType().equals("83") && !routeLink.getType().equals("90")) {
				isLocal = false;
				break;
			}
		}
		System.out.println();
		return isLocal;
	}

	private final void calcRouteOnSubNet(final Node o, final Node d, final int k, final int l, final int time, final LinkedList<Link[]> links, final LinkedList<CarRoute> nonLocalRoutes, final LinkedList<CarRoute> localRoutes) {

		// the list to handle for the next level (level d+1) of the tree
		LinkedList<Link[]> new_links = new LinkedList<Link[]>();

		System.out.println("--- start a level of the tree ---");
		System.out.println("  links.size = " + links.size() + ", localRoutes.size = " + localRoutes.size() + ", nonLocalRoutes.size = " + nonLocalRoutes.size()  + ", new_links.size = " + new_links.size());

		// go through all given lists at this level (level d) of the tree
		while (!links.isEmpty()) {
			Link[] ls = links.poll();

			// remove the links of the current link set and calc the least cost path
			for (int i=0; i<ls.length; i++) { this.removeLinkFromNetwork(ls[i]); }
//			System.out.println("    ---");
//			System.out.println("    removed " + ls.length + " links from the net");
			Path path = this.router.calcLeastCostPath(o,d,time);
			CarRoute route = null;
			if (path != null) {
				route = new NodeCarRoute(path.links.get(0),path.links.get(path.links.size()-1));
				route.setNodes(path.links.get(0),path.nodes,path.links.get(path.links.size()-1));
			}

			//first check if route is local route (i.e. contains only local road links) 
			//if so, add it to the list of local route (unless already included)
			//else add it to the list of non local routes (unless already included)
			//Also, create the link sets for the next level (d+1) of the tree for the current link set

			if(route != null) {	
				
				boolean newLinkSet = false;
				if (this.isLocalRoute(route) && !this.containsRoute(route, localRoutes)) {
					localRoutes.add(route);
					newLinkSet = true;
				} else if (!this.isLocalRoute(route) && !this.containsRoute(route, nonLocalRoutes)){
					nonLocalRoutes.add(route);
					newLinkSet = true;
				}
				
				// for each link of the calc route create a new link set with the
				// other links of the current link set				
				if (newLinkSet) {					
//					
					for (Link link : route.getLinks()) {
						Link[] new_ls = new Link[ls.length+1];
						for (int jj=0; jj<ls.length; jj++) { new_ls[jj] = ls[jj]; }
						new_ls[new_ls.length-1] = link;
						new_links.addLast(new_ls);
					}
					System.out.println("    -> links.size = " + links.size() + ", localRoutes.size = " + localRoutes.size() + ", nonLocalRoutes.size = " + nonLocalRoutes.size() + ", new_links.size = " + new_links.size());
				}
			}

			// restore the full network
			for (int i=0; i<ls.length; i++) { this.addLinkToNetwork(ls[i]); }
//			System.out.println("    restored " + ls.length + " links from the net");
//			System.out.println("    ---");
		}

		System.out.println("---  end a level of the tree  ---");
		// go to the next level (d+1) of the tree, if not already enough routes are found
		if (((nonLocalRoutes.size() < k) || (localRoutes.size()< l)) && !new_links.isEmpty()) { 
			this.calcRouteOnSubNet(o,d,k,l,time,new_links,nonLocalRoutes,localRoutes);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	public final LinkedList<CarRoute> calcRouteSet(final Node o, final Node d, final int k, final int time, final int var_factor, final float localRoute_factor) {
		if (o.getId().toString().equals(d.getId().toString())) { Gbl.errorMsg("O == D not alloed!"); }
		if (k < 1) { Gbl.errorMsg("k < 1 not allowed!"); }

		LinkedList<CarRoute> routes = new LinkedList<CarRoute>(); // resulting k least cost routes
		LinkedList<CarRoute> localRoutes = new LinkedList<CarRoute>(); // routes containing only local streets
		LinkedList<CarRoute> nonLocalRoutes = new LinkedList<CarRoute>(); // all other routes
		LinkedList<Link[]> links = new LinkedList<Link[]>(); // removed links
		Path path = this.router.calcLeastCostPath(o,d,time);
		if (path == null) { Gbl.errorMsg("There is no route from " + o.getId() + " to " + d.getId() + "!"); }
//		routes.add(route);

		for (Link link : path.links) {
			Link[] lls = new Link[1];
			lls[0] = link;
			links.add(lls);	
		}
		// creating a route set with the minimum of k*var_factor routes
		this.calcRouteOnSubNet(o,d,Math.round(k*var_factor*(1-localRoute_factor)), Math.round(k*var_factor*localRoute_factor),time,links,nonLocalRoutes, localRoutes);

		System.out.println("--- Number of created routes = " + routes.size() + " ---");
		System.out.println("--- Randomly removing routes until " + k + " routes left... ---");
		// Remove randomly some routes from the localRoutes until it contains k*(localRouteFactor) elements
		while (k*localRoute_factor < localRoutes.size()) { 
			localRoutes.remove(MatsimRandom.random.nextInt(localRoutes.size()));
		}
		// Remove randomly some routes from the nonLocalRoutes until it contains k*(1-localRouteFactor)-1 elements
		while (k*(1-localRoute_factor)-1 < nonLocalRoutes.size()) { 
			nonLocalRoutes.remove(MatsimRandom.random.nextInt(nonLocalRoutes.size()));
		}
		// add the least cost path at the beginning of the route
		CarRoute route = new NodeCarRoute(path.links.get(0),path.links.get(path.links.size()-1));
		route.setNodes(path.links.get(0), path.nodes, path.links.get(path.links.size()-1));
		routes.addFirst(route);	

		// joining the resulting routes in one linked list which is returned by the algorithm
		for(CarRoute localRoute : localRoutes){
			routes.add(localRoute);
		}
		for(CarRoute nonLocalRoute : nonLocalRoutes){
			routes.add(nonLocalRoute);
		}
		System.out.println("--- done. ---");

		return routes;
	}
}
