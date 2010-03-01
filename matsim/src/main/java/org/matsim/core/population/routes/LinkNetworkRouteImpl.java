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
import org.matsim.api.core.v01.population.LinkNetworkRoute;

/**
 * Implementation of {@link NetworkRoute} which internally stores the route as a series of {@link Link}s.
 *
 * @author mrieser
 */
public class LinkNetworkRouteImpl extends AbstractRoute implements NetworkRoute, LinkNetworkRoute, Cloneable {

	private static final long serialVersionUID = 1L;
	private ArrayList<Id> route = new ArrayList<Id>();
	private double travelCost = Double.NaN;
	private Id vehicleId = null;

	@Deprecated
	public LinkNetworkRouteImpl(final Id startLinkId, final Id endLinkId, final Network network) {
		this(startLinkId, endLinkId);
	}
	public LinkNetworkRouteImpl(final Id startLinkId, final Id endLinkId) {
		super(startLinkId, endLinkId);
	}

	@Override
	public LinkNetworkRouteImpl clone() {
		LinkNetworkRouteImpl cloned = (LinkNetworkRouteImpl) super.clone();
		ArrayList<Id> tmp = cloned.route;
		cloned.route = new ArrayList<Id>(tmp); // deep copy of route
		return cloned;
	}

	@Override
	public List<Id> getLinkIds() {
		ArrayList<Id> ids = new ArrayList<Id>(this.route.size());
		ids.addAll(this.route);
		return ids;
	}

	@Override
	public NetworkRoute getSubRoute(Id fromLinkId, Id toLinkId) {
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
		} else if (fromLinkId.equals(this.getEndLinkId())) {
			fromIndex = this.route.size();
		} else {
			for (int i = 0, n = this.route.size(); (i < n) && (fromIndex < 0); i++) {
				if (fromLinkId.equals(this.route.get(i))) {
					fromIndex = i+1;
				}
			}
			if (fromIndex < 0) {
				throw new IllegalArgumentException("Cannot create subroute because fromLinkId is not part of the route.");
			}
		}

		if (fromLinkId.equals(toLinkId)) {
			toIndex = fromIndex - 1;
		} else if (toLinkId.equals(this.getEndLinkId())) {
			toIndex = this.route.size();
		} else {
			for (int i = fromIndex, n = this.route.size(); (i < n) && (toIndex < 0); i++) {
				if (toLinkId.equals(this.route.get(i))) {
					toIndex = i;
				}
			}
			if (toIndex < 0) {
				throw new IllegalArgumentException("Cannot create subroute because toLinkId is not part of the route.");
			}
		}
		LinkNetworkRouteImpl ret = new LinkNetworkRouteImpl(fromLinkId, toLinkId);
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
