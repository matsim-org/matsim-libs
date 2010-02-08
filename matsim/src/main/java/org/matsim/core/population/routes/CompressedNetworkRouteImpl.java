/* *********************************************************************** *
 * project: org.matsim.*
 * CompressedRoute.java
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
import java.util.Map;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.network.Network;
import org.matsim.api.core.v01.network.Node;

/**
 * Implementation of {@link NetworkRouteWRefs} that tries to minimize the amount of
 * data needed to be stored for each route. This will give some memory savings,
 * allowing for larger scenarios (=more agents), especially on detailed
 * networks, but is likely a bit slower due to the more complex access of the
 * route information internally.
 *
 * @author mrieser
 */
public class CompressedNetworkRouteImpl extends AbstractRoute implements NetworkRouteWRefs, Cloneable {

	private final static Logger log = Logger.getLogger(CompressedNetworkRouteImpl.class);

	private ArrayList<Id> route = new ArrayList<Id>(0);
	private final Map<Id, Id> subsequentLinks;
	private double travelCost = Double.NaN;
	/** number of links in uncompressed route */
	private int uncompressedLength = -1;
	private int modCount = 0;
	private int routeModCountState = 0;
	private Id vehicleId = null;
	private final Network network;

	public CompressedNetworkRouteImpl(final Id startLinkId, final Id endLinkId, Network network, final Map<Id, Id> subsequentLinks) {
		super(startLinkId, endLinkId);
		this.network = network;
		this.subsequentLinks = subsequentLinks;
	}

	@Override
	public CompressedNetworkRouteImpl clone() {
		CompressedNetworkRouteImpl cloned = (CompressedNetworkRouteImpl) super.clone();
		ArrayList<Id> tmpRoute = cloned.route;
		cloned.route = new ArrayList<Id>(tmpRoute); // deep copy
		return cloned;
	}

	@Override
	public List<Id> getLinkIds() {
		if (this.uncompressedLength < 0) { // it seems the route never got initialized correctly
			return new ArrayList<Id>(0);
		}
		ArrayList<Id> links = new ArrayList<Id>(this.uncompressedLength);
		if (this.modCount != this.routeModCountState) {
			log.error("Route was modified after storing it! modCount=" + this.modCount + " routeModCount=" + this.routeModCountState);
			return links;
		}
		Id previousLinkId = getStartLinkId();
		Id endLinkId = getEndLinkId();
		if ((previousLinkId == null) || (endLinkId == null)) {
			return links;
		}
		if (previousLinkId.equals(endLinkId)) {
			return links;
		}
		for (Id linkId : this.route) {
			getLinksTillLink(links, linkId, previousLinkId);
			links.add(linkId);
			previousLinkId = linkId;
		}
		getLinksTillLink(links, endLinkId, previousLinkId);

		return links;
	}

	private void getLinksTillLink(final List<Id> links, final Id nextLinkId, final Id startLinkId) {
		Id linkId = startLinkId;
		Link nextLink = this.network.getLinks().get(nextLinkId);
		while (true) { // loop until we hit "return;"
			Link link = this.network.getLinks().get(linkId);
			if (link.getToNode() == nextLink.getFromNode()) {
				return;
			}
			linkId = this.subsequentLinks.get(linkId);
			links.add(linkId);
		}
	}

	@Override
	public void setEndLinkId(final Id linkId) {
		this.modCount++;
		super.setEndLinkId(linkId);
	}

	@Override
	public void setStartLinkId(final Id linkId) {
		this.modCount++;
		super.setStartLinkId(linkId);
	}


	@Override
	public NetworkRouteWRefs getSubRoute(final Node fromNode, final Node toNode) {
		Link newStartLink = null;
		Link newEndLink = null;
		List<Id> newLinkIds = new ArrayList<Id>(10);

		Link startLink = this.network.getLinks().get(getStartLinkId());
		if (startLink.getToNode() == fromNode) {
			newStartLink = startLink;
		}
		Link endLink = this.network.getLinks().get(getEndLinkId());
		for (Id linkId : getLinkIds()) {
			Link link = this.network.getLinks().get(linkId);
			if (link.getFromNode() == toNode) {
				newEndLink = link;
				break;
			}
			if (newStartLink != null) {
				newLinkIds.add(link.getId());
			}
			if (link.getToNode() == fromNode) {
				newStartLink = link;
			}
		}
		if (newStartLink == null) {
			throw new IllegalArgumentException("fromNode is not part of this route.");
		}
		if (newEndLink == null) {
			if (endLink.getFromNode() == toNode) {
				newEndLink = endLink;
			} else {
				throw new IllegalArgumentException("toNode is not part of this route.");
			}
		}

		NetworkRouteWRefs subRoute = new CompressedNetworkRouteImpl(newStartLink.getId(), newEndLink.getId(), this.network, this.subsequentLinks);
		subRoute.setLinkIds(newStartLink.getId(), newLinkIds, newEndLink.getId());
		return subRoute;
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
		this.routeModCountState = this.modCount;
		if ((srcRoute == null) || (srcRoute.size() == 0)) {
			this.uncompressedLength = 0;
			return;
		}
		Id previousLinkId = startLinkId;
		for (Id linkId : srcRoute) {
			if (!this.subsequentLinks.get(previousLinkId).equals(linkId)) {
				this.route.add(linkId);
			}
			previousLinkId = linkId;
		}
		this.route.trimToSize();
		this.uncompressedLength = srcRoute.size();
//		System.out.println("uncompressed size: \t" + this.uncompressedLength + "\tcompressed size: \t" + this.route.size());
	}

	@Override
	public double getDistance() {
		double dist = super.getDistance();
		if (Double.isNaN(dist)) {
			dist = calcDistance();
		}
		return dist;
	}

	private double calcDistance() {
		if (this.modCount != this.routeModCountState) {
			log.error("Route was modified after storing it! modCount=" + this.modCount + " routeModCount=" + this.routeModCountState);
			return 99999.999;
		}
		double dist = 0;
		for (Id linkId: getLinkIds()) {
			dist += this.network.getLinks().get(linkId).getLength();
		}
		setDistance(dist);
		return dist;
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
