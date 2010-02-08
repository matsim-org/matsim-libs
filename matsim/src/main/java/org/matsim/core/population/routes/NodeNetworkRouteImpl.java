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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.core.utils.misc.Time;

public class NodeNetworkRouteImpl extends AbstractRoute implements NetworkRouteWRefs, Cloneable {

	protected ArrayList<Node> route = new ArrayList<Node>();

	private double cost = Double.NaN;

	private Id vehicleId = null;
	protected final Network network;

	/**
	 * This constructor is only needed for backwards compatibility reasons and thus is
	 * set to deprecated. New code should make use of the constructor which sets the
	 * start and the end link of a Route correctly.
	 */
	@Deprecated
	public NodeNetworkRouteImpl(){
		this.network = null; // FIXME [MR]
	}

	public NodeNetworkRouteImpl(final Id startLinkId, final Id endLinkId, final Network network) {
		super(startLinkId, endLinkId);
		this.network = network;
	}

	@Override
	public NodeNetworkRouteImpl clone() {
		NodeNetworkRouteImpl cloned = (NodeNetworkRouteImpl) super.clone();
		ArrayList<Node> tmp = cloned.route;
		cloned.route = new ArrayList<Node>(tmp); // deep copy of route
		return cloned;
	}

	@Override
	public void setLinkIds(final Id startLinkId, final List<Id> srcRoute, final Id endLinkId) {
		this.route.clear();
		setStartLinkId(startLinkId);
		setEndLinkId(endLinkId);
		if (srcRoute == null) {
			if ((startLinkId != null) && (endLinkId != null) && (!startLinkId.equals(endLinkId))) {
				Link startLink = this.network.getLinks().get(startLinkId);
				// we do not check that start link and end link are really connected with the same node
				this.route.add(startLink.getToNode());
			}
		} else {
			Link startLink = this.network.getLinks().get(startLinkId);
			if (srcRoute.size() == 0) {
				if (!startLinkId.equals(endLinkId)) {
					// we do not check that start link and end link are really connected with the same node
					this.route.add(startLink.getToNode());
				}
			} else {
				Link l = this.network.getLinks().get(srcRoute.get(0));
				this.route.add(l.getFromNode());
				for (int i = 0; i < srcRoute.size(); i++) {
					l = this.network.getLinks().get(srcRoute.get(i));
					this.route.add(l.getToNode());
				}
			}
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

	@Override
	public final void setTravelCost(final double travelCost) {
		this.cost = travelCost;
	}

	@Override
	public final double getTravelCost() {
		return this.cost;
	}

	@Override
	public final List<Id> getLinkIds() {
		if (this.route.size() == 0) {
			return new ArrayList<Id>(0);
		}

		Node prevNode = null;
		ArrayList<Id> linkIds = new ArrayList<Id>(this.route.size() - 1);
		for (Node node : this.route) {
			if (prevNode != null) {
				// search link from prevNode to node
				boolean linkFound = false;
				for (Iterator<? extends Link> iter = prevNode.getOutLinks().values().iterator(); iter.hasNext() && !linkFound; ) {
					Link link = iter.next();
					if (link.getToNode() == node) {
						linkIds.add(link.getId());
						linkFound = true;
					}
				}
				if (!linkFound) {
					throw new RuntimeException("No link found from node " + prevNode.getId() + " to node " + node.getId());
				}
			}
			prevNode = node;
		}
		return linkIds;
	}

	protected double calcDistance() {
		/* TODO we cannot calculate the real distance, as we do not know the
		 * very first or the very last link of the route, only the links in between.
		 * fix this somehow, but how?? MR, jan07
		 */
		double distance = 0;
		for (Id linkId : getLinkIds()) {
			distance += this.network.getLinks().get(linkId).getLength();
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
	@Override
	public NetworkRouteWRefs getSubRoute(final Node fromNode, final Node toNode) {
		Link fromLink = this.network.getLinks().get(getStartLinkId());
		Link toLink = this.network.getLinks().get(getEndLinkId());
		int fromIndex = -1;
		int toIndex = -1;
		List<Id> linkIds = getLinkIds();
		int max = linkIds.size();
		if (fromNode == toNode) {
			if (this.route.size() > 1) {
				for (int i = 0; i < max; i++) {
					Link link = this.network.getLinks().get(linkIds.get(i));
					Node node = link.getFromNode();
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
				Link link = this.network.getLinks().get(linkIds.get(i));
				Node node = link.getFromNode();
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
				Link link = this.network.getLinks().get(linkIds.get(i));
				if (toIndex >= 0) {
					toLink = link;
					break;
				}
				Node node = link.getToNode();
				if (node.equals(toNode)) {
					toIndex = i + 1;
				}
			}
			if (toIndex == -1) {
				throw new IllegalArgumentException("Can't create subroute because toNode is not in the original Route");
			}
		}
		NodeNetworkRouteImpl ret = new NodeNetworkRouteImpl(fromLink.getId(), toLink.getId(), this.network);
		ret.route.addAll(this.route.subList(fromIndex, toIndex + 1));
		ret.route.trimToSize();
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

	@Override
	public Id getVehicleId() {
		return this.vehicleId;
	}

	@Override
	public void setVehicleId(final Id vehicleId) {
		this.vehicleId  = vehicleId;
	}

}
