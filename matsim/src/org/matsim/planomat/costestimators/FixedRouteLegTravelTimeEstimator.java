/* *********************************************************************** *
 * project: org.matsim.*
 * FixedRouteLegTravelTimeEstimator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.planomat.costestimators;

import org.matsim.basic.v01.Id;
import org.matsim.network.Link;
import org.matsim.plans.Route;
import org.matsim.router.util.TravelTimeI;
import org.matsim.world.Location;

/**
 * Abstract class for <code>LegTravelTimeEstimator</code>s
 * that estimate the travel time of a fixed route.
 *
 * @author meisterk
 *
 */
public abstract class FixedRouteLegTravelTimeEstimator implements
		LegTravelTimeEstimator {

	protected TravelTimeI linkTravelTimeEstimator;
	protected DepartureDelayAverageCalculator tDepDelayCalc;

	public FixedRouteLegTravelTimeEstimator(
			TravelTimeI linkTravelTimeEstimator,
			DepartureDelayAverageCalculator depDelayCalc) {

		this.linkTravelTimeEstimator = linkTravelTimeEstimator;
		this.tDepDelayCalc = depDelayCalc;

	}

	public abstract double getLegTravelTimeEstimation(Id personId,
			double departureTime, Location origin, Location destination,
			Route route, String mode);

	protected double processDeparture(final Link link, final double start) {

		double departureDelayEnd = start + this.tDepDelayCalc.getLinkDepartureDelay(link, start);
		return departureDelayEnd;

	}

	protected double processRouteTravelTime(final Route route, final double start) {

		double now = start;
		Link[] links = route.getLinkRoute();
		for (Link link : links) {
			now = this.processLink(link, now);
		}
		return now;

	}

	protected double processLink(final Link link, final double start) {

		double linkEnd = start + this.linkTravelTimeEstimator.getLinkTravelTime(link, start);
		return linkEnd;

	}

	@Override
	public String toString() {
		return this.getClass().getSimpleName();
	}

}
