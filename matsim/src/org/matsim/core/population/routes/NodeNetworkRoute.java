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

package org.matsim.core.population.routes;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.network.LinkImpl;
import org.matsim.core.network.NodeImpl;
import org.matsim.core.utils.misc.Time;

public class NodeNetworkRoute extends AbstractRoute implements NetworkRoute {

	protected final ArrayList<NodeImpl> route = new ArrayList<NodeImpl>();

	private double cost = Double.NaN;

	
	/**
	 * This constructor is only needed for backwards compatibility reasons and thus is
	 * set to deprecated. New code should make use of the constructor which sets the
	 * start and the end link of a Route correctly.
	 */
	@Deprecated
	public NodeNetworkRoute(){}
	
	public NodeNetworkRoute(LinkImpl startLink, LinkImpl endLink) {
		super(startLink, endLink);
	}

	public NodeNetworkRoute(final NetworkRoute route) {
		super(route.getStartLink(), route.getEndLink());
		super.setDistance(route.getDistance());
		super.setTravelTime(route.getTravelTime());
		this.route.addAll(route.getNodes());
		this.route.trimToSize();
	}

	@Override
	public void setLinkIds(final List<Id> linkids) {
		throw new UnsupportedOperationException("Setting only the link ids is not possible at this " +
				"level in the inheritance hierachy! If the Interfaces Link/Node/Route are used you " +
				"have to set the route by object references not by Ids.");
	}

	public List<NodeImpl> getNodes() {
		return this.route;
	}

	public void setLinks(final LinkImpl startLink, final List<LinkImpl> srcRoute, final LinkImpl endLink) {
		this.route.clear();
		setStartLink(startLink);
		setEndLink(endLink);
		if (srcRoute == null) {
			if (startLink != endLink) {
				// we do not check that start link and end link are really connected with the same node
				this.route.add(startLink.getToNode());
			}
		} else {
			if (srcRoute.size() == 0) {
				if (startLink != endLink) {
					// we do not check that start link and end link are really connected with the same node
					this.route.add(startLink.getToNode());
				}
			} else {
				LinkImpl l = srcRoute.get(0);
				this.route.add(l.getFromNode());
				for (int i = 0; i < srcRoute.size(); i++) {
					l = srcRoute.get(i);
					this.route.add(l.getToNode());
				}
			}
		}
		this.route.trimToSize();
	}

	@Deprecated
	public void setNodes(final List<NodeImpl> srcRoute) {
		setNodes(null, srcRoute, null);
	}

	public void setNodes(final LinkImpl startLink, final List<NodeImpl> srcRoute, final LinkImpl endLink) {
		setStartLink(startLink);
		setEndLink(endLink);
		if (srcRoute == null) {
			this.route.clear();
		} else {
			this.route.clear();
			this.route.addAll(srcRoute);
		}
		this.route.trimToSize();
	}

	@Override
	public final double getDistance() {
		if (Double.isNaN(super.getDistance())) {
			super.setDistance(this.calcDistance());
		}
		return super.getDistance();
	}

	public final void setTravelCost(final double travelCost) {
		this.cost = travelCost;
	}

	public final double getTravelCost() {
		return this.cost;
	}

	public List<Id> getLinkIds() {
		List<Id> ret = new ArrayList<Id>(Math.max(0, this.route.size() - 1));
		for (LinkImpl l : getLinks()) {
			ret.add(l.getId());
		}
		return ret;
	}

	public final List<LinkImpl> getLinks() {
		// marcel, 2006-09-05: added getLinkRoute
		/* Nodes have proved to not be the best solution to store routes.
		 * Thus it should be changed sooner or later to links instead of nodes
		 * This function is a first step for this as it helps to create the
		 * list of links the route leads through */
		if (this.route.size() == 0) {
			return new ArrayList<LinkImpl>(0);
		}

		NodeImpl prevNode = null;
		ArrayList<LinkImpl> links = new ArrayList<LinkImpl>(this.route.size() - 1);
		for (NodeImpl node : this.route) {
			if (prevNode != null) {
				// search link from prevNode to node
				boolean linkFound = false;
				for (Iterator<? extends LinkImpl> iter = prevNode.getOutLinks().values().iterator(); iter.hasNext() && !linkFound; ) {
					LinkImpl link = iter.next();
					if (link.getToNode() == node) {
						links.add(link);
						linkFound = true;
					}
				}
				if (!linkFound) {
					throw new RuntimeException("No link found from node " + prevNode.getId() + " to node " + node.getId());
				}
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
		double distance = 0;
		for (LinkImpl link : getLinks()) {
			distance += link.getLength();
		}
		return distance;
	}

	/**
	 * This method returns a new Route object with the subroute of this beginning at fromNode and
	 * ending at toNode.
	 * @param fromNode
	 * @param toNode
	 * @return a route leading from <code>fromNode</code> to <code>toNode</code> along this route
	 * @throws IllegalArgumentException if <code>fromNode</code> or <code>toNode</code> are not part of this route
	 */
	public NetworkRoute getSubRoute(final NodeImpl fromNode, final NodeImpl toNode) {
		LinkImpl fromLink = getStartLink();
		LinkImpl toLink = getEndLink();
		int fromIndex = -1;
		int toIndex = -1;
		List<LinkImpl> links = getLinks();
		int max = links.size();
		if (fromNode == toNode) {
			if (this.route.size() > 1) {
				for (int i = 0; i < max; i++) {
					LinkImpl link = links.get(i);
					NodeImpl node = link.getFromNode();
					if (node.equals(fromNode)) {
						fromIndex = i;
						toIndex = i;
						toLink = link;
						break;
					}
					fromLink = link;
				}
				if (fromIndex == -1) {
					// not yet found, maybe it's the last node in the route?
					if (fromNode.equals(fromLink.getToNode())) {
						fromIndex = max;
						toIndex = max;
					} else {
						throw new IllegalArgumentException("Can't create subroute because fromNode is not in the original Route");
					}
				}
			} else if (this.route.size() == 1) {
				if (this.route.get(0) == fromNode) {
					fromIndex = 0;
					toIndex = 0;
				} else {
					throw new IllegalArgumentException("Can't create subroute because fromNode is not in the original Route");
				}
			} else {
				throw new IllegalArgumentException("Can't create subroute because route does not contain any nodes.");
			}
		} else { // --> fromNode != toNode
			for (int i = 0; i < max; i++) {
				LinkImpl link = links.get(i);
				NodeImpl node = link.getFromNode();
				if (node.equals(fromNode)) {
					fromIndex = i;
					break;
				}
				fromLink = link;
			}
			if (fromIndex == -1) {
				throw new IllegalArgumentException("Can't create subroute because fromNode is not in the original Route");
			}
			for (int i = fromIndex; i < max; i++) {
				LinkImpl link = links.get(i);
				if (toIndex >= 0) {
					toLink = link;
					break;
				}
				NodeImpl node = link.getToNode();
				if (node.equals(toNode)) {
					toIndex = i + 1;
				}
			}
			if (toIndex == -1) {
				throw new IllegalArgumentException("Can't create subroute because toNode is not in the original Route");
			}
		}
		NodeNetworkRoute ret = new NodeNetworkRoute();
		ret.setNodes(fromLink, this.route.subList(fromIndex, toIndex + 1), toLink);
		return ret;
	}

	@Override
	public final String toString() {
		StringBuilder b = new StringBuilder();
		b.append("NodeCarRoute: [dist=");
		b.append(this.getDistance());
		b.append("]");
		b.append("[trav_time=" );
		b.append(Time.writeTime(this.getTravelTime()));
		b.append("]");
		b.append("[nof_nodes=");
		b.append(this.route.size());
		b.append("]");
		return b.toString();		
	}

}
