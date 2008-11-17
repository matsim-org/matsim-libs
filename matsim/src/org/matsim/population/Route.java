/* *********************************************************************** *
 * project: org.matsim.*
 * Route.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim.population;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.basic.v01.BasicRouteImpl;
import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;
import org.matsim.utils.misc.Time;

public class Route extends BasicRouteImpl {

	
	protected ArrayList<Node> route = new ArrayList<Node>();

	
	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private double cost = Double.NaN;

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Route() {
	}

	public Route(final String dist, final String travTime) {

		if (dist != null) {
			super.setDist(Double.parseDouble(dist));
		}
		if (travTime != null) {
			super.setTravTime(Time.parseTime(travTime));
		}
	}

	public Route(final Route route) {
		super.setDist(route.getDist());
		super.setTravTime(route.getTravTime());
		this.route = new ArrayList<Node>(route.route);
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public final void setRoute(final String route) {
		NetworkLayer layer = (NetworkLayer)Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE);
		if (layer == null) {
			throw new RuntimeException("NetworkLayer does not exist in world.");
		}

		String[] parts = route.split("[ \t\n]+");
		// IMPORTANT NOTE:
		// split does not always work as one would expect!
		// if a string starts with one of the delimiters, then
		// parts[0] is equal to ""!!! if not, then parts[0] holds the first
		// string one is intended to keep.
		// Example:
		// route=" 0 1   2 " -> parts = ["","0","1","2"]
		// route="0 1   2 "  -> parts = ["0","1","2"]
		int min = 0;
		if ((parts.length > 0) && (parts[0].equals(""))) { min = 1; }

		for (int i = min; i < parts.length; i++) {
			Node n = layer.getNode(parts[i]);
			if (n == null) {
				throw new RuntimeException("Node not found in network. node id = " + parts[i]);
			}
			this.route.add(n);
		}
		this.route.trimToSize();
		this.cost = Double.NaN;
	}

	
	public ArrayList<Node> getRoute() {
		return this.route;
	}
	
	public void setLinkRoute(List<Link> srcRoute) {
		this.route.clear();
		if (srcRoute != null) {
			Link l = srcRoute.get(0);
			this.route.add(l.getFromNode());
			for (int i = 0; i < srcRoute.size(); i++) {
				l = srcRoute.get(i);
				this.route.add(l.getToNode());
			}
		}	
	}

	public void setRoute(List<Node> srcRoute) {
		if (srcRoute == null) {
			this.route.clear();
		}
		else if (srcRoute instanceof ArrayList) {
			this.route = (ArrayList<Node>) srcRoute;
		}
		else {
			this.route.clear();
			this.route.addAll(srcRoute);
		}
		this.route.trimToSize();
	}

	
	public final void setRoute(final ArrayList<Node> route, final double travelTime, final double travelCost) {
		setRoute(route);
		super.setTravTime(travelTime);
		this.cost = travelCost;
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////
	@Override
	public final double getDist() {
		if (Double.isNaN(super.getDist())) {
			super.setDist(this.calcDistance());
		}
		return super.getDist();
	}


	public final double getTravelCost() {
		return this.cost;
	}

	@Override
	public List<Id> getLinkIds() {
		List<Id> ret = new ArrayList<Id>(this.route.size()-1);
		for (Link l : getLinkRoute()) {
			ret.add(l.getId());
		}
		return ret;
	}
	
	/**
	 * Returns the list of links that build the route. The links where the route
	 * starts and ends (the links where the activities are on) are <b>not</b>
	 * included in the list.
	 * @return an array containing the links the agents plans to travel along
	 */
	public final Link[] getLinkRoute() {
		// marcel, 2006-09-05: added getLinkRoute
		/* Nodes have proved to not be the best solution to store routes.
		 * Thus it should be changed sooner or later to links instead of nodes
		 * This function is a first step for this as it helps to create the
		 * list of links the route leads through */
		if (this.route == null) {
			return new Link[0];
		}
		if (this.route.size() == 0) {
			return new Link[0];
		}

		boolean notfirst = false;
		Node prevNode = null;
		Link[] links = new Link[this.route.size() - 1];
		int idx = 0;
		for (Node node : this.route) {
			if (notfirst) {
				// search link from prevNode to node
				boolean linkFound = false;
				for (Iterator<? extends Link> iter = prevNode.getOutLinks().values().iterator(); iter.hasNext() && !linkFound; ) {
					Link link = iter.next();
					if (link.getToNode() == node) {
						links[idx] = link;
						idx++;
						linkFound = true;
					}
				}
				if (!linkFound) {
					throw new RuntimeException("No link found from node " + prevNode.getId() + " to node " + node.getId());
				}
			} else {
				notfirst = true;
			}
			prevNode = node;
		}
		return links;
	}

	private final double calcDistance() {
		/* TODO we cannot calculate the real distance, as we do not know the
		 * very first or the very last link of the route, only the links in between.
		 * fix this somehow, but how?? MR, jan07
		 */
		Link[] links = getLinkRoute();
		double distance = 0;
		for (Link link : links) {
			distance += link.getLength();
		}
		return distance;
	}

	/**
	 * This method returns a new Route object with the subroute of this beginning at fromNode
	 * till toNode. If from or twoNode are not found in this, an IllegalArgumentException is thrown.
	 * @param fromNode
	 * @param toNode
	 * @return A flat copy of the original Route  // FIXME reading the doc above, this clearly does NOT return a flat copy of the original Route!
	 */
	public Route getSubRoute(final Node fromNode, final Node toNode) {
		int fromIndex = -1;
		int toIndex = -1;
		int max = this.route.size();
		for (int i = 0; i < max; i++) {
			Node node = this.route.get(i);
			if (node.equals(fromNode)) {
				fromIndex = i;
				break;
			}
		}
		if (fromIndex == -1) {
			throw new IllegalArgumentException("Can't create subroute because fromNode is not in the original Route");
		}
		for (int i = fromIndex; i < max; i++) {
			Node node = this.route.get(i);
			if (node.equals(toNode)) {
				toIndex = i;
				break;
			}
		}
		if (toIndex == -1) {
			throw new IllegalArgumentException("Can't create subroute because toNode is not in the original Route");
		}
		Route ret = new Route();
		ret.route = new ArrayList<Node>(this.route.subList(fromIndex, toIndex + 1));
		return ret;
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[dist=" + this.getDist() + "]" +
				"[trav_time=" + Time.writeTime(this.getTravTime()) + "]" +
				"[nof_nodes=" + this.route.size() + "]";
	}
}
