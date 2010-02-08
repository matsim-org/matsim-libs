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
import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;
import org.matsim.api.core.v01.population.LinkNetworkRoute;

/**
 * Implementation of {@link NetworkRouteWRefs} which internally stores the route as a series of {@link Link}s.
 *
 * @author mrieser
 */
public class LinkNetworkRouteImpl extends AbstractRoute implements NetworkRouteWRefs, LinkNetworkRoute, Cloneable {

	private static final long serialVersionUID = 1L;
	private ArrayList<Id> route = new ArrayList<Id>();
	private double travelCost = Double.NaN;
	private Id vehicleId = null;
	private final Network network;

	public LinkNetworkRouteImpl(final Id startLinkId, final Id endLinkId, final Network network) {
		super(startLinkId, endLinkId);
		this.network = network;
	}

	@Override
	public LinkNetworkRouteImpl clone() {
		LinkNetworkRouteImpl cloned = (LinkNetworkRouteImpl) super.clone();
		ArrayList<Id> tmp = cloned.route;
		cloned.route = new ArrayList<Id>(tmp); // deep copy of route
		return cloned;
	}

	@Override
	public double getDistance() {
		double dist = super.getDistance();
		if (Double.isNaN(dist)) {
			dist = 0;
			for (Id linkId : this.route) {
				dist += this.network.getLinks().get(linkId).getLength();
			}
			this.setDistance(dist);
		}
		return dist;
	}

	@Override
	public List<Id> getLinkIds() {
		ArrayList<Id> ids = new ArrayList<Id>(this.route.size());
		ids.addAll(this.route);
		return ids;
	}

	@Override
	public NetworkRouteWRefs getSubRoute(final Node fromNode, final Node toNode) {
		Link startLink = this.network.getLinks().get(getStartLinkId());
		Link fromLink = startLink;
		Link endLink = this.network.getLinks().get(getEndLinkId());
		Link toLink = endLink;
		int fromIndex = -1;
		int toIndex = -1;
		int max = this.route.size();
		if (fromNode == toNode) {
			boolean found = false;
			for (int i = 0; i < max; i++) {
				Link link = this.network.getLinks().get(this.route.get(i));
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
				if (fromNode.equals(startLink.getToNode())) {
					fromIndex = 0;
					fromLink = startLink;
					if (this.route.size() > 0) {
						toLink = this.network.getLinks().get(this.route.get(0));
					}
				} else {
					throw new IllegalArgumentException("Can't create subroute because fromNode is not in the original Route");
				}
			}
		} else {
			for (int i = 0; i < max; i++) {
				Link link = this.network.getLinks().get(this.route.get(i));
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
				Link link = this.network.getLinks().get(this.route.get(i));
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
		LinkNetworkRouteImpl ret = new LinkNetworkRouteImpl(fromLink.getId(), toLink.getId(), this.network);
		if (toIndex >= fromIndex) {
			ret.setLinkIds(fromLink.getId(), this.route.subList(fromIndex, toIndex + 1), toLink.getId());
		} else {
			ret.setLinkIds(fromLink.getId(), null, toLink.getId());
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
	public void setLinkIds(final Id startLinkId, final List<Id> srcRoute, final Id endLinkId) {
		this.route.clear();
		setStartLinkId(startLinkId);
		setEndLinkId(endLinkId);
		if (srcRoute != null) {
			this.route.addAll(srcRoute);
		}
		this.route.trimToSize();
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
