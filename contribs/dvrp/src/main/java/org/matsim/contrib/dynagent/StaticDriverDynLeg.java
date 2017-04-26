/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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

package org.matsim.contrib.dynagent;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.NetworkRoute;
import org.matsim.vehicles.Vehicle;

public class StaticDriverDynLeg implements DriverDynLeg {
	private final NetworkRoute route;
	private int currentLinkIdx;
	private final String mode;

	public StaticDriverDynLeg(String mode, NetworkRoute route) {
		this.mode = mode;
		this.route = route;
		currentLinkIdx = -1;
	}

	@Override
	public void movedOverNode(Id<Link> newLinkId) {
		currentLinkIdx++;
	}

	@Override
	public Id<Link> getNextLinkId() {
		List<Id<Link>> linkIds = route.getLinkIds();

		if (currentLinkIdx == linkIds.size()) {
			return null;
		}

		if (currentLinkIdx == linkIds.size() - 1) {
			return route.getEndLinkId();
		}

		return linkIds.get(currentLinkIdx + 1);
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return route.getEndLinkId();
	}

	@Override
	public void finalizeAction(double now) {
	}

	@Override
	public String getMode() {
		return mode;
	}

	@Override
	public void arrivedOnLinkByNonNetworkMode(Id<Link> linkId) {
		if (!getDestinationLinkId().equals(linkId)) {
			throw new IllegalStateException();
		}

		currentLinkIdx = route.getLinkIds().size();
	}

	@Override
	public Id<Vehicle> getPlannedVehicleId() {
		return route.getVehicleId();
	}

	@Override
	public Double getExpectedTravelTime() {
		// TODO add travel time at the destination link??
		return route.getTravelTime();
	}

	public Double getExpectedTravelDistance() {
		// TODO add length of the destination link??
		return route.getDistance();
	}
}
