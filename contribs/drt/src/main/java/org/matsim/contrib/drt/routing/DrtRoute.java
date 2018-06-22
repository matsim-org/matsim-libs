/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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
package org.matsim.contrib.drt.routing;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.AbstractRoute;

/**
 * Assumptions:
 * <ul>
 * <li>{@code directRideTime} is an optimistic estimate equal to the time of an unshared ride</li>
 * <li>{@code travelTime} is the maximum travel (wait+ride) time (excluding walk to/from the stop)</li>
 * </ul>
 */
public class DrtRoute extends AbstractRoute {
	final static String ROUTE_TYPE = "drt";

	private double directRideTime;

	public DrtRoute(Id<Link> startLinkId, Id<Link> endLinkId) {
		super(startLinkId, endLinkId);
	}

	public double getDirectRideTime() {
		return directRideTime;
	}

	public void setUnsharedRideTime(double directRideTime) {
		this.directRideTime = directRideTime;
	}

	@Override
	public String getRouteDescription() {
		return Double.toString(directRideTime);
	}

	@Override
	public void setRouteDescription(String routeDescription) {
		directRideTime = Double.parseDouble(routeDescription);
	}

	@Override
	public String getRouteType() {
		return ROUTE_TYPE;
	}

	@Override
	public DrtRoute clone() {
		return (DrtRoute)super.clone();
	}
}
