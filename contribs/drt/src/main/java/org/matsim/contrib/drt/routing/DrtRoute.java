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

import static com.google.common.base.Preconditions.checkArgument;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.api.core.v01.network.Link;
import org.matsim.core.population.routes.AbstractRoute;
import org.matsim.core.utils.misc.OptionalTime;

import com.google.common.base.MoreObjects;

/**
 * Assumptions:
 * <ul>
 * <li>{@code maxWaitTime} is the maximum wait time</li>
 * <li>{@code directRideTime} is the time of an unshared ride</li>
 * <li>{@code travelTime} is the maximum travel (wait+ride) time (excluding walk to/from the stop)</li>
 * </ul>
 *
 * @author michalm (Michal Maciejewski)
 */
public class DrtRoute extends AbstractRoute {
	public final static String ROUTE_TYPE = TransportMode.drt;

	private OptionalTime maxWaitTime = OptionalTime.undefined();
	private OptionalTime directRideTime = OptionalTime.undefined();

	public DrtRoute(Id<Link> startLinkId, Id<Link> endLinkId) {
		super(startLinkId, endLinkId);
	}

	public double getDirectRideTime() {
		return directRideTime.seconds();
	}

	public double getMaxWaitTime() {
		return maxWaitTime.seconds();
	}

	public void setDirectRideTime(double directRideTime) {
		this.directRideTime = OptionalTime.defined(directRideTime);
	}

	public void setMaxWaitTime(double maxWaitTime) {
		this.maxWaitTime = OptionalTime.defined(maxWaitTime);
	}

	public double getMaxTravelTime() {
		return getTravelTime().seconds(); // currently DrtRoute.travelTime is set to the max allowed travel time
	}

	@Override
	public String getRouteDescription() {
		return maxWaitTime.seconds() + " " + directRideTime.seconds();
	}

	@Override
	public void setRouteDescription(String routeDescription) {
		String[] values = routeDescription.split(" ");
		maxWaitTime = OptionalTime.defined(requiresZeroOrPositive(Double.parseDouble(values[0])));
		directRideTime = OptionalTime.defined(requiresZeroOrPositive(Double.parseDouble(values[1])));
	}

	@Override
	public String getRouteType() {
		return ROUTE_TYPE;
	}

	@Override
	public DrtRoute clone() {
		return (DrtRoute)super.clone();
	}

	private double requiresZeroOrPositive(double value) {
		checkArgument(value >= 0 || value <= Double.MAX_VALUE, "Value: (%s) must be zero or positive", value);
		return value;
	}

	@Override
	public String toString() {
		return MoreObjects.toStringHelper(this)
				.add("maxWaitTime", maxWaitTime)
				.add("directRideTime", directRideTime)
				.add("super", super.toString())
				.toString();
	}
}
