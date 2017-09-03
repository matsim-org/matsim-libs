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
import org.matsim.core.network.NetworkUtils;
import org.matsim.vehicles.Vehicle;

/**
 * Implementation of {@link NetworkRoute} which internally stores the route as a series of {@link Link}s.
 *
 * @author mrieser
 */
final class LinkNetworkRouteImpl extends AbstractRoute implements NetworkRoute {

	/*package*/ final static String ROUTE_TYPE = "links";
	
	private ArrayList<Id<Link>> route = new ArrayList<>();
	private List<Id<Link>> safeRoute = Collections.unmodifiableList(this.route);
	private double travelCost = Double.NaN;
	private Id<Vehicle> vehicleId = null;

	LinkNetworkRouteImpl(final Id<Link> startLinkId, final Id<Link> endLinkId) {
		super(startLinkId, endLinkId);
	}
	
	LinkNetworkRouteImpl(final Id<Link> startLinkId, final List<Id<Link>> linkIds, final Id<Link> endLinkId) {
		super(startLinkId, endLinkId);
		setLinkIds(startLinkId, linkIds, endLinkId);
	}
	
	LinkNetworkRouteImpl(final Id<Link> startLinkId, final Id<Link>[] linkIds, final Id<Link> endLinkId) {
		super(startLinkId, endLinkId);
        Collections.addAll(this.route, linkIds);
		this.route.trimToSize();
	}

	@Override
	public LinkNetworkRouteImpl clone() {
		LinkNetworkRouteImpl cloned = (LinkNetworkRouteImpl) super.clone();
		ArrayList<Id<Link>> tmp = cloned.route;
		cloned.route = new ArrayList<>(tmp); // deep copy of route
		cloned.safeRoute = Collections.unmodifiableList(cloned.route);
		return cloned;
	}

	@Override
	public List<Id<Link>> getLinkIds() {
		return this.safeRoute;
	}

	@Override
	public NetworkRoute getSubRoute(Id<Link> fromLinkId, Id<Link> toLinkId) {
		/**
		 * the index where the link after fromLinkId can be found in the route:
		 * fromIndex==0 --> fromLinkId == startLinkId,
		 * fromIndex==1 --> fromLinkId == first link in the route, etc.
		 */
		int fromIndex = -1;
		/**
		 * the index where toLinkId can be found in the route
		 */
		int toIndex = -1;

		if (fromLinkId.equals(this.getStartLinkId())) {
			fromIndex = 0;
		} else {
			for (int i = 0, n = this.route.size(); (i < n) && (fromIndex < 0); i++) {
				if (fromLinkId.equals(this.route.get(i))) {
					fromIndex = i+1;
				}
			}
			if (fromIndex < 0 && fromLinkId.equals(this.getEndLinkId())) {
				fromIndex = this.route.size();
			}
			if (fromIndex < 0) {
				throw new IllegalArgumentException("Cannot create subroute because fromLinkId is not part of the route.");
			}
		}

		if (fromLinkId.equals(toLinkId)) {
			toIndex = fromIndex - 1;
		} else {
			for (int i = fromIndex, n = this.route.size(); (i < n) && (toIndex < 0); i++) {
				if (fromLinkId.equals(this.route.get(i))) {
					fromIndex = i+1; // in case of a loop, cut it short
				}
				if (toLinkId.equals(this.route.get(i))) {
					toIndex = i;
				}
			}
			if (toIndex < 0 && toLinkId.equals(this.getEndLinkId())) {
				toIndex = this.route.size();
			}
			if (toIndex < 0) {
				throw new IllegalArgumentException("Cannot create subroute because toLinkId is not part of the route.");
			}
		}
		NetworkRoute ret = RouteUtils.createLinkNetworkRouteImpl(fromLinkId, toLinkId);
		if (toIndex > fromIndex) {
			ret.setLinkIds(fromLinkId, this.route.subList(fromIndex, toIndex), toLinkId);
		} else {
			ret.setLinkIds(fromLinkId, null, toLinkId);
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
	public void setLinkIds(final Id<Link> startLinkId, final List<Id<Link>> srcRoute, final Id<Link> endLinkId) {
		this.route.clear();
		setStartLinkId(startLinkId);
		setEndLinkId(endLinkId);
		if (srcRoute != null) {
			this.route.addAll(srcRoute);
		}
		this.route.trimToSize();
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
	
	@Override
	public String getRouteType() {
		return ROUTE_TYPE;
	}
	
	@Override
	public String toString() {
		String str = super.toString();
		str += " linkIds=" + this.getLinkIds() ;
		str += " travelCost=" + this.getTravelCost() ;
		return str ;
	}
}
