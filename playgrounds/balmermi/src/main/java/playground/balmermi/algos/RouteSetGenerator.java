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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.population.routes.LinkNetworkRouteImpl;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.core.router.AStarLandmarks;
import org.matsim.core.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.core.router.util.PreProcessLandmarks;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.router.util.LeastCostPathCalculator.Path;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.core.utils.misc.NetworkUtils;
import org.matsim.core.utils.misc.RouteUtils;

public class RouteSetGenerator {

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private final NetworkImpl network;
	private final TravelTime timeFunction;
	private final PreProcessLandmarks preProcessData;
	private final AStarLandmarks router;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public RouteSetGenerator(NetworkImpl network, Config config) {
		this.network = network;
		this.timeFunction = new TravelTimeCalculator(network, config.travelTimeCalculator());
		this.preProcessData = new PreProcessLandmarks(new FreespeedTravelTimeCost(config.charyparNagelScoring()));
		this.preProcessData.run(network);
		this.router = new AStarLandmarks(this.network,this.preProcessData,this.timeFunction);
	}

	//////////////////////////////////////////////////////////////////////
	// private methods
	//////////////////////////////////////////////////////////////////////

	private void addLinkToNetwork(Id linkId) {
		Link link = this.network.getLinks().get(linkId);
		((NodeImpl) link.getFromNode()).addOutLink(link);
		((NodeImpl) link.getToNode()).addInLink(link);
	}

	private void removeLinkFromNetwork(Id linkId) {
		Link link = this.network.getLinks().get(linkId);
		((NodeImpl) link.getFromNode()).removeOutLink(link);
		((NodeImpl) link.getToNode()).removeInLink(link);
	}

	private boolean containsRoute(NetworkRoute route, LinkedList<NetworkRoute> routes) {
		List<Node> nodes = RouteUtils.getNodes(route, this.network);
		Iterator<NetworkRoute> r_it = routes.iterator();
		while (r_it.hasNext()) {
			List<Node> ns = RouteUtils.getNodes(r_it.next(), this.network);
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

	private boolean isLocalRoute(NetworkRoute route) {
		boolean isLocal = true;
		for (Id routeLink2Id : route.getLinkIds()) {
			LinkImpl routeLink = (LinkImpl) this.network.getLinks().get(routeLink2Id);
			if (!routeLink.getType().equals("39") && !routeLink.getType().equals("83") && !routeLink.getType().equals("90")) {
				isLocal = false;
				break;
			}
		}
		return isLocal;
	}

	private final void calcRouteOnSubNet(final Node o, final Node d, final int k, final int l, final int time, final LinkedList<Id[]> linkIds, final LinkedList<NetworkRoute> nonLocalRoutes, final LinkedList<NetworkRoute> localRoutes) {

		// the list to handle for the next level (level d+1) of the tree
		LinkedList<Id[]> new_linkIds = new LinkedList<Id[]>();

		System.out.println("--- start a level of the tree ---");
		System.out.println("  linkIds.size = " + linkIds.size() + ", localRoutes.size = " + localRoutes.size() + ", nonLocalRoutes.size = " + nonLocalRoutes.size()  + ", new_linkIds.size = " + new_linkIds.size());

		// go through all given lists at this level (level d) of the tree
		while (!linkIds.isEmpty()) {
			Id[] ls = linkIds.poll();

			// remove the links of the current link set and calc the least cost path
			for (int i=0; i<ls.length; i++) { this.removeLinkFromNetwork(ls[i]); }
//			System.out.println("    ---");
//			System.out.println("    removed " + ls.length + " links from the net");
			Path path = this.router.calcLeastCostPath(o,d,time);
			NetworkRoute route = null;
			if (path != null) {
				route = new LinkNetworkRouteImpl(path.links.get(0).getId(), path.links.get(path.links.size()-1).getId());
				route.setLinkIds(path.links.get(0).getId(), NetworkUtils.getLinkIds(path.links), path.links.get(path.links.size()-1).getId());
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

					for (Id linkId : route.getLinkIds()) {
						Id[] new_ls = new Id[ls.length+1];
						for (int jj=0; jj<ls.length; jj++) { new_ls[jj] = ls[jj]; }
						new_ls[new_ls.length-1] = linkId;
						new_linkIds.addLast(new_ls);
					}
					System.out.println("    -> linkIds.size = " + linkIds.size() + ", localRoutes.size = " + localRoutes.size() + ", nonLocalRoutes.size = " + nonLocalRoutes.size() + ", new_linkIds.size = " + new_linkIds.size());
				}
			}

			// restore the full network
			for (int i=0; i<ls.length; i++) { this.addLinkToNetwork(ls[i]); }
//			System.out.println("    restored " + ls.length + " links from the net");
//			System.out.println("    ---");
		}

		System.out.println("---  end a level of the tree  ---");
		// go to the next level (d+1) of the tree, if not already enough routes are found
		if (((nonLocalRoutes.size() < k) || ((localRoutes.size()< l) && (nonLocalRoutes.size() < k*3))) && !new_linkIds.isEmpty()) {
			this.calcRouteOnSubNet(o,d,k,l,time,new_linkIds,nonLocalRoutes,localRoutes);
		}
	}

	//////////////////////////////////////////////////////////////////////
	// calc methods
	//////////////////////////////////////////////////////////////////////

	public final LinkedList<NetworkRoute> calcRouteSet(final NodeImpl o, final NodeImpl d, final int k, final int time, final int var_factor, final float localRoute_factor) {
		if (o.getId().equals(d.getId())) { Gbl.errorMsg("O == D not alloed!"); }
		if (k < 1) { Gbl.errorMsg("k < 1 not allowed!"); }

		LinkedList<NetworkRoute> routes = new LinkedList<NetworkRoute>(); // resulting k least cost routes
		LinkedList<NetworkRoute> localRoutes = new LinkedList<NetworkRoute>(); // routes containing only local streets
		LinkedList<NetworkRoute> nonLocalRoutes = new LinkedList<NetworkRoute>(); // all other routes
		LinkedList<Id[]> links = new LinkedList<Id[]>(); // removed links
		Path path = this.router.calcLeastCostPath(o,d,time);
		if (path == null) { throw new RuntimeException("There is no route from " + o.getId() + " to " + d.getId() + "!"); }
//		routes.add(route);

		for (Link link : path.links) {
			Id[] lls = new Id[1];
			lls[0] = link.getId();
			links.add(lls);
		}
		// creating a route set with the minimum of k*var_factor routes
		this.calcRouteOnSubNet(o,d,Math.round(k*var_factor*(1-localRoute_factor)), Math.round(k*var_factor*localRoute_factor),time,links,nonLocalRoutes, localRoutes);

		System.out.println("--- Number of created routes = " + routes.size() + " ---");
		System.out.println("--- Randomly removing routes until " + k + " routes left... ---");
		// Remove randomly some routes from the localRoutes until it contains k*(localRouteFactor) elements
		while (k*localRoute_factor < localRoutes.size()) {
			localRoutes.remove(MatsimRandom.getRandom().nextInt(localRoutes.size()));
		}
		// Remove randomly some routes from the nonLocalRoutes until it contains k*(1-localRouteFactor)-1 elements
		while (k*(1-localRoute_factor)-1 < nonLocalRoutes.size()) {
			nonLocalRoutes.remove(MatsimRandom.getRandom().nextInt(nonLocalRoutes.size()));
		}
		// add the least cost path at the beginning of the route
		NetworkRoute route = new LinkNetworkRouteImpl(path.links.get(0).getId(), path.links.get(path.links.size()-1).getId());
		route.setLinkIds(path.links.get(0).getId(), NetworkUtils.getLinkIds(path.links), path.links.get(path.links.size()-1).getId());
		routes.addFirst(route);

		// joining the resulting routes in one linked list which is returned by the algorithm

		if (!localRoutes.isEmpty()) {
			for (NetworkRoute localRoute : localRoutes){
				routes.add(localRoute);
			}
		}
		for (NetworkRoute nonLocalRoute : nonLocalRoutes){
			routes.add(nonLocalRoute);
		}
		System.out.println("--- done. ---");

		return routes;
	}
}
