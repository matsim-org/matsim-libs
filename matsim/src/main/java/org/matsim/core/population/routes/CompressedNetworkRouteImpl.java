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
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

/**
 * Implementation of {@link NetworkRoute} that tries to minimize the amount of
 * data needed to be stored for each route. This will give some memory savings,
 * allowing for larger scenarios (=more agents), especially on detailed
 * networks, but is likely a bit slower due to the more complex access of the
 * route information internally.
 *
 * <p>Description of the compression algorithm:<br />
 * Given a map containing for each link a defined successor (subsequentLinks-map), this implementation
 * does not store the links in its route-information that are the same as the successor defined in the
 * subsequentLinks-map.<br />
 * Given a startLinkId, endLinkId and a list of linkIds to be stored, this implementation stores
 * first the startLinkId. Next, if the successor of the startLinkId is different from the first linkId
 * in the list, this linkId is stored, otherwise not. Then the successor of that linkId is compared to
 * the next linkId in the list. If the successor is different, the linkId is stored, otherwise not.
 * This procedure is repeated until the complete list of linkIds is processed.
 * </p>
 *
 * @author mrieser
 */
final class CompressedNetworkRouteImpl extends AbstractRoute implements NetworkRoute, Cloneable {

	private final static Logger log = Logger.getLogger(CompressedNetworkRouteImpl.class);

	private ArrayList<Id<Link>> route = new ArrayList<Id<Link>>(0);
	private final Map<Id<Link>, Id<Link>> subsequentLinks;
	private double travelCost = Double.NaN;
	/** number of links in uncompressed route */
	private int uncompressedLength = -1;
	private int modCount = 0;
	private int routeModCountState = 0;
	private Id<Vehicle> vehicleId = null;
	private final Network network;

	public CompressedNetworkRouteImpl(final Id<Link> startLinkId, final Id<Link> endLinkId, Network network, final Map<Id<Link>, Id<Link>> subsequentLinks) {
		super(startLinkId, endLinkId);
		this.network = network;
		this.subsequentLinks = subsequentLinks;
	}

	@Override
	public CompressedNetworkRouteImpl clone() {
		CompressedNetworkRouteImpl cloned = (CompressedNetworkRouteImpl) super.clone();
		ArrayList<Id<Link>> tmpRoute = cloned.route;
		cloned.route = new ArrayList<Id<Link>>(tmpRoute); // deep copy
		return cloned;
	}

	@Override
	public List<Id<Link>> getLinkIds() {
		if (this.uncompressedLength < 0) { // it seems the route never got initialized correctly
			return new ArrayList<Id<Link>>(0);
		}
		ArrayList<Id<Link>> links = new ArrayList<Id<Link>>(this.uncompressedLength);
		if (this.modCount != this.routeModCountState) {
			log.error("Route was modified after storing it! modCount=" + this.modCount + " routeModCount=" + this.routeModCountState);
			return links;
		}
		Id<Link> previousLinkId = getStartLinkId();
		Id<Link> endLinkId = getEndLinkId();
		if ((previousLinkId == null) || (endLinkId == null)) {
			return links;
		}
		if (previousLinkId.equals(endLinkId)) {
			return links;
		}
		for (Id<Link> linkId : this.route) {
			getLinksTillLink(links, linkId, previousLinkId);
			links.add(linkId);
			previousLinkId = linkId;
		}
		getLinksTillLink(links, endLinkId, previousLinkId);

		return links;
	}

	private void getLinksTillLink(final List<Id<Link>> links, final Id<Link> nextLinkId, final Id<Link> startLinkId) {
		Id<Link> linkId = startLinkId;
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

//	@Override
//	public void setEndLinkId(final Id<Link> linkId) {
//		this.modCount++;
//		super.setEndLinkId(linkId);
//	}
//
//	@Override
//	public void setStartLinkId(final Id<Link> linkId) {
//		this.modCount++;
//		super.setStartLinkId(linkId);
//	}
	// AbstractRoute is now implements Lockable and I have addressed this via that feature.  kai, sep/17

	@Override
	public NetworkRoute getSubRoute(Id<Link> fromLinkId, Id<Link> toLinkId) {
		List<Id<Link>> newLinkIds = new ArrayList<Id<Link>>(10);
		boolean foundFromLink = fromLinkId.equals(this.getStartLinkId());
		boolean collectLinks = foundFromLink;
		boolean equalFromTo = fromLinkId.equals(toLinkId);

		if (!foundFromLink || !equalFromTo) {
			for (Id<Link> linkId : getLinkIds()) {
				if (linkId.equals(toLinkId)) {
					collectLinks = false;
					if (equalFromTo) {
						foundFromLink = true;
					}
					if (foundFromLink) {
						// only break if from is also found, as endLink could be part of a loop/circle
						break; // we found start and end, stop looping
					}
				}
				if (collectLinks) {
					newLinkIds.add(linkId);
				}
				if (linkId.equals(fromLinkId)) {
					foundFromLink = true;
					collectLinks = true; // we found the start, start collecting
					newLinkIds.clear(); // in case of a loop, cut it out
				}
			}
			if (!foundFromLink) {
				foundFromLink = fromLinkId.equals(this.getEndLinkId());
				collectLinks = foundFromLink;
			}
			if (!foundFromLink) {
				throw new IllegalArgumentException("fromLinkId is not part of this route.");
			}
			if ((collectLinks) && (toLinkId.equals(this.getEndLinkId()))) {
				collectLinks = false;
			}
			if (collectLinks) {
				throw new IllegalArgumentException("toLinkId is not part of this route.");
			}
		}
		NetworkRoute subRoute = new CompressedNetworkRouteImpl(fromLinkId, toLinkId, this.network, this.subsequentLinks);
		subRoute.setLinkIds(fromLinkId, newLinkIds, toLinkId);
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
	public void setLinkIds(final Id<Link> startLinkId, final List<Id<Link>> srcRoute, final Id<Link> endLinkId) {
		this.route.clear();
		setStartLinkId(startLinkId);
		setEndLinkId(endLinkId);
		this.routeModCountState = this.modCount;
		if ((srcRoute == null) || (srcRoute.size() == 0)) {
			this.uncompressedLength = 0;
			return;
		}
		Id<Link> previousLinkId = startLinkId;
		for (Id<Link> linkId : srcRoute) {
			if (!this.subsequentLinks.get(previousLinkId).equals(linkId)) {
				this.route.add(linkId);
			}
			previousLinkId = linkId;
		}
		this.route.trimToSize();
		this.uncompressedLength = srcRoute.size();
//		System.out.println("uncompressed size: \t" + this.uncompressedLength + "\tcompressed size: \t" + this.route.size());
		
		this.setLocked() ;
	}

	@Override
	public Id<Vehicle> getVehicleId() {
		return this.vehicleId;
	}

	@Override
	public void setVehicleId(final Id<Vehicle> vehicleId) {
		this.vehicleId = vehicleId;
	}

	@Override
	public String getRouteType() {
		return "links";
	}
	
	@Override
	public String getRouteDescription() {
		StringBuilder desc = new StringBuilder(100);
		desc.append(this.getStartLinkId().toString());
		for (Id<Link> linkId : this.getLinkIds()) {
			desc.append(" ");
			desc.append(linkId.toString());
		}
		// If the start links equals the end link additionally check if its is a round trip. 
		if (!this.getEndLinkId().equals(this.getStartLinkId()) || this.getLinkIds().size() > 0) {
			desc.append(" ");
			desc.append(this.getEndLinkId().toString());
		}
		return desc.toString();
	}
	
	@Override
	public void setRouteDescription(String routeDescription) {
		List<Id<Link>> linkIds = NetworkUtils.getLinkIds(routeDescription);
		Id<Link> startLinkId = getStartLinkId();
		Id<Link> endLinkId = getEndLinkId();
		if (linkIds.size() > 0) {
			startLinkId = linkIds.remove(0);
			setStartLinkId(startLinkId);
		}
		if (linkIds.size() > 0) {
			endLinkId = linkIds.remove(linkIds.size() - 1);
			setEndLinkId(endLinkId);
		}
		this.setLinkIds(startLinkId, linkIds, endLinkId);
		
	}
	
}
