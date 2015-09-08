/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.socnetsim.sharedvehicles.replanning;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;

/**
 * to put vehicle Id in legs without route.
 * Try to make this the less dirty as possible: anything except vehicle-related
 * operations throws an UnsupportedOperationException, so that we are sure this
 * is not used for more than what it is meant to do.
 */
class VehicleOnlyNetworkRoute implements NetworkRoute {
	private Id v = null;

	// /////////////////////////////////////////////////////////////////////
	// active methods
	// /////////////////////////////////////////////////////////////////////
	@Override
	public void setVehicleId(final Id vehicleId) {
		this.v = vehicleId;
	}

	@Override
	public Id getVehicleId() {
		return v;
	}

	// /////////////////////////////////////////////////////////////////////
	// inactive methods
	// /////////////////////////////////////////////////////////////////////
	@Override
	@Deprecated
	public double getDistance() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setDistance(double distance) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getTravelTime() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setTravelTime(double travelTime) {
		throw new UnsupportedOperationException();
	}

	@Override
	public Id<Link> getStartLinkId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public Id<Link> getEndLinkId() {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setStartLinkId(Id<Link> linkId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setEndLinkId(Id<Link> linkId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public void setLinkIds(Id<Link> startLinkId, List<Id<Link>> linkIds, Id<Link> endLinkId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public String getRouteDescription() {
		return null;
	}
	
	@Override
	public void setRouteDescription(String routeDescription) {
	}
	
	@Override
	public String getRouteType() {
		return null;
	}
	
	@Override
	public void setTravelCost(double travelCost) {
		throw new UnsupportedOperationException();
	}

	@Override
	public double getTravelCost() {
		throw new UnsupportedOperationException();
	}

	@Override
	public List<Id<Link>> getLinkIds() {
		throw new UnsupportedOperationException();
	}

	@Override
	public NetworkRoute getSubRoute(Id<Link> fromLinkId, Id<Link> toLinkId) {
		throw new UnsupportedOperationException();
	}

	@Override
	public VehicleOnlyNetworkRoute clone() {
		throw new UnsupportedOperationException();
	}
}
