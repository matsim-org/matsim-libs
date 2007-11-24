/* *********************************************************************** *
 * project: org.matsim.*
 * Route.java
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

package org.matsim.plans;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.basic.v01.BasicRoute;
import org.matsim.gbl.Gbl;
import org.matsim.network.Link;
import org.matsim.network.NetworkLayer;
import org.matsim.network.Node;

public class Route extends BasicRoute<Node> implements  Serializable{

	private static final long serialVersionUID = -3615114784178389239L;

	//////////////////////////////////////////////////////////////////////
	// member variables
	//////////////////////////////////////////////////////////////////////

	private double dist = Double.NaN;
	private double travTime = Gbl.UNDEFINED_TIME;
	private double cost = Double.NaN;
	private static NodeBuilder nodeBuilder = new NodeBuilder();

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public Route() {
	}

	public Route(final String dist, final String travTime) {

		if (dist != null) {
			this.dist = Double.parseDouble(dist);
		}
		if (travTime != null) {
			this.travTime = Gbl.parseTime(travTime);
		}
	}

	public Route(final Route route) {
		this.dist = route.dist;
		this.travTime = route.travTime;
		this.route = new ArrayList<Node>(route.route);
	}

	//////////////////////////////////////////////////////////////////////
	// set methods
	//////////////////////////////////////////////////////////////////////

	public final void setRoute(final String route) {
		NetworkLayer layer = (NetworkLayer)Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE);
		if (layer == null) {
			Gbl.errorMsg(this.toString() + "[layer=" + NetworkLayer.LAYER_TYPE + " does not exist]");
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
			this.route.add(n);
		}
		this.route.trimToSize();
		this.cost = Double.NaN;
	}

	public final void setRoute(final String route, final int travelTime) {
		setRoute(route);
		this.travTime = travelTime;
	}

	public final void setRoute(final ArrayList<Node> route, final double travelTime) {
		setRoute(route);
		this.travTime = travelTime;
	}

	public final void setRoute(final ArrayList<Node> route, final double travelTime, final double travelCost) {
		setRoute(route);
		this.travTime = travelTime;
		this.cost = travelCost;
	}

	public final void setTravTime(final double travTime) {
		this.travTime = travTime;
	}

	public final void setDist(final double dist) {
		this.dist = dist;
	}

	public static void setNodeBuilder(final NodeBuilder builder) {
		nodeBuilder = builder;
	}

	//////////////////////////////////////////////////////////////////////
	// get methods
	//////////////////////////////////////////////////////////////////////

	public final double getDist() {
		if (Double.isNaN(this.dist)) {
			this.calcDistance();
		}
		return this.dist;
	}

	public final double getTravTime() {
		return this.travTime;
	}

	@Override
	public final ArrayList<Node> getRoute() {
		return this.route;
	}

	public final double getTravelCost() {
		return this.cost;
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
					Gbl.errorMsg("No link found from node " + prevNode.getId() + " to node " + node.getId());
				}
			} else {
				notfirst = true;
			}
			prevNode = node;
		}
		return links;
	}

	private final void calcDistance() {
		/* TODO we cannot calculate the real distance, as we do not know the
		 * very first or the very last link of the route, only the links in between.
		 * fix this somehow, but how?? MR, jan07
		 */
		Link[] links = getLinkRoute();
		double distance = 0;
		for (Link link : links) {
			distance += link.getLength();
		}
		this.dist = distance;
	}

	/**
	 * This method returns a new Route object with the subroute of this beginning at fromNode
	 * till toNode. If from or twoNode are not found in this, an IllegalArgumentException is thrown.
	 * @param fromNode
	 * @param toNode
	 * @return A flat copy of the original Route
	 */
	public Route getSubRoute(final Node fromNode, final Node toNode) {
		int fromIndex = this.route.indexOf(fromNode);
		int toIndex = this.route.indexOf(toNode);
		if ((fromIndex == -1) || (toIndex == -1)) {
			throw new IllegalArgumentException("Cann't create subroute cause fromNode or toNode is not in the original Route");
		}
		List<Node> nodeList = this.route.subList(fromIndex, toIndex + 1);
		Route ret = new Route();
		ret.route = new ArrayList<Node>(nodeList);
		return ret;
	}


	// This routebuilder could be exchanged for suppliing other
	//kinds of network e.g. in OnTheFlyClient
	public static class NodeBuilder {
		private static NetworkLayer network = null;

		public void addNode(final List<Node> route, final String nodeId) {
			if (network == null) {
				network = (NetworkLayer)Gbl.getWorld().getLayer(NetworkLayer.LAYER_TYPE);
				if (network == null) {
					Gbl.errorMsg(this + "[network layer does not exist]");
				}
			}

			Node node = network.getNode(nodeId);
			route.add(node);
		}
	}



  /////////////////////////////////////////////////////////////////
	//output methods
  /////////////////////////////////////////////////////////////////

	private void readObject(final ObjectInputStream s)
	  throws IOException, ClassNotFoundException
	{
		// the `standard' fields.
		s.defaultReadObject();
		this.route = new ArrayList<Node>();

		int size = s.readInt();
		for( int i = 0; i < size; i++) {
			nodeBuilder.addNode(this.route, (String)s.readObject());
		};
	}

	private void writeObject(final ObjectOutputStream s) throws IOException {
	    // The standard non-transient fields.
	  s.defaultWriteObject();
	  s.writeInt(this.route.size());
	  for( int i = 0; i < this.route.size(); i++) {
		  Node node = this.route.get(i);
		  s.writeObject(node.getId().toString());
		};
	}

	//////////////////////////////////////////////////////////////////////
	// print methods
	//////////////////////////////////////////////////////////////////////

	@Override
	public final String toString() {
		return "[dist=" + this.dist + "]" +
				"[trav_time=" + Gbl.writeTime(this.travTime) + "]" +
				"[nof_nodes=" + this.route.size() + "]";
	}
}
