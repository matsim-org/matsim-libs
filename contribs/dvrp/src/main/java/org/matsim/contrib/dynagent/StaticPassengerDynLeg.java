/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2016 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.api.core.v01.population.Route;
import org.matsim.core.utils.misc.OptionalTime;

/**
 * @author jbischoff
 */
public class StaticPassengerDynLeg implements PassengerDynLeg {
	private final Route route;
	private final String mode;

	public StaticPassengerDynLeg(Route route, String mode) {
		this.route = route;
		this.mode = mode;
	}

	@Override
	public String getMode() {
		return mode;
	}

	@Override
	public void arrivedOnLinkByNonNetworkMode(Id<Link> linkId) {
	}

	@Override
	public Id<Link> getDestinationLinkId() {
		return route.getEndLinkId();
	}

	@Override
	public OptionalTime getExpectedTravelTime() {
		return route.getTravelTime();
	}

	@Override
	public Double getExpectedTravelDistance() {
		return route.getDistance();
	}
}
