/* *********************************************************************** *
 * project: org.matsim.*
 * LinkRoute.java
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
 * *********************************************************************** */

package org.matsim.core.population.routes;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.LinkNetworkRoute;

/**
 * Implementation of {@link NetworkRouteWRefs} which internally stores the route as a series of {@link Link}s.
 *
 * @author mrieser
 */
public class LinkNetworkRouteImpl extends AbstractRoute implements NetworkRouteWRefs, LinkNetworkRoute, Cloneable {

	private static final long serialVersionUID = 1L;
	private ArrayList<Link> route = new ArrayList<Link>();
	private double travelCost = Double.NaN;
	private Id vehicleId = null;

	public LinkNetworkRouteImpl(final Link startLink, final Link endLink){
		super(startLink, endLink);
	}

	@Override
	public LinkNetworkRouteImpl clone() {
		LinkNetworkRouteImpl cloned = (LinkNetworkRouteImpl) super.clone();
		ArrayList<Link> tmp = cloned.route;
		cloned.route = new ArrayList<Link>(tmp); // deep copy of route
		return cloned;
	}

	@Override
	public double getDistance() {
		double dist = super.getDistance();
		if (Double.isNaN(dist)) {
			dist = 0;
			for (Link link : this.route) {
				dist += link.getLength();
			}
			this.setDistance(dist);
		}
		return dist;
	}

	@Override
	public List<Id> getLinkIds() {
		ArrayList<Id> ids = new ArrayList<Id>(this.route.size());
		for (Link link : this.route) {
			ids.add(link.getId());
		}
		ids.trimToSize();
		return ids;
	}

	@Override
	public List<Link> getLinks() {
		return Collections.unmodifiableList(this.route);
	}

	@Override
	public NetworkRouteWRefs getSubRoute(final Node fromNode, final Node toNode) {
		Link fromLink = getStartLink();
		Link toLink = getEndLink();
		int fromIndex = -1;
		int toIndex = -1;
		int max = this.route.size();
		if (fromNode == toNode) {
			boolean found = false;
			for (int i = 0; i < max; i++) {
				Link link = this.route.get(i);
				if (found) {
					toLink = link;
					break;
				}
				Node node = link.getToNode();
				if (node.equals(fromNode)) {
					found = true;
					fromIndex = 0; // value doesn't really matter, just >= 0
					fromLink = link;
				}
			}
			if (fromIndex == -1) {
				if (fromNode.equals(getStartLink().getToNode())) {
					fromIndex = 0;
					fromLink = getStartLink();
					if (this.route.size() > 0) {
						toLink = this.route.get(0);
					}
				} else {
					throw new IllegalArgumentException("Can't create subroute because fromNode is not in the original Route");
				}
			}
		} else {
			for (int i = 0; i < max; i++) {
				Link link = this.route.get(i);
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
				Link link = this.route.get(i);
				if (toIndex >= 0) {
					toLink = link;
					break;
				}
				Node node = link.getToNode();
				if (node.equals(toNode)) {
					toIndex = i;
				}
			}
			if (toIndex == -1) {
				throw new IllegalArgumentException("Can't create subroute because toNode is not in the original Route");
			}
		}
		LinkNetworkRouteImpl ret = new LinkNetworkRouteImpl(fromLink, toLink);
		if (toIndex >= fromIndex) {
			ret.setLinks(fromLink, this.route.subList(fromIndex, toIndex + 1), toLink);
		} else {
			ret.setLinks(fromLink, null, toLink);
		}
		return ret;
	}

	@Override
	public double getTravelCost() {
		return this.travelCost;
	}

	@Override
	public void setTravelCost(final double travelCost) {
		this.travelCost = travelCost;
	}

	@Override
	public void setLinks(final Link startLink, final List<Link> srcRoute, final Link endLink) {
		this.route.clear();
		setStartLink(startLink);
		setEndLink(endLink);
		if (srcRoute != null) {
			this.route.addAll(srcRoute);
		}
		this.route.trimToSize();
	}

	@Override
	public void setNodes(final Link startLink, final List<Node> srcRoute, final Link endLink) {
		setStartLink(startLink);
		setEndLink(endLink);
		setNodes(srcRoute);
	}

	@Override
	public void setNodes(final List<Node> srcRoute) {
		this.route.clear();
		Node prevNode = null;
		for (Node node : srcRoute) {
			if (prevNode != null) {
				findAndAddLink(prevNode, node);
			}
			prevNode = node;
		}
		this.route.trimToSize();
	}

	private void findAndAddLink(Node prevNode, Node node) {
		Link foundLink = findLink(prevNode, node);
		if (foundLink != null) {
			this.route.add(foundLink);
		}
	}

	private Link findLink(Node prevNode, Node node) {
		for (Link link : prevNode.getOutLinks().values()) {
			if (link.getToNode().equals(node)) {
				return link;
			}
		}
		return null;
	}

	@Override
	public Id getVehicleId() {
		return this.vehicleId;
	}

	@Override
	public void setVehicleId(final Id vehicleId) {
		this.vehicleId = vehicleId;
	}

}
