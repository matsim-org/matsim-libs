/* *********************************************************************** *
 * project: org.matsim.*
 * CetinCompatibleLegTravelTimeEstimator.java
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

import org.matsim.network.Link;
import org.matsim.plans.Route;
import org.matsim.router.util.TravelTimeI;
import org.matsim.utils.identifiers.IdI;
import org.matsim.world.Location;

/**
 * This class estimates travel times from events produced by the
 * deterministic time-step-driven queue-based agent traffic simulation (DQSim).
 * <p>
 *  In constrast to <code>CharyparEtAlCompatibleLegTravelTimeEstimator</code>,
 * <ul>
 * <li> the link of the origin activity is not simulated,
 *   and thus not included in this leg travel time estimation.
 * <li> the link of the destination activity is simulated,
 *   and thus has to be included in this leg travel time estimation.
 * </ul>
 * <p>
 * @see "Cetin, N. (2005) Large-scale parallel graph-based simulations, Ph.D. Thesis, ETH Zurich, Zurich.)"
 * @author meisterk
 *
*/
public class CetinCompatibleLegTravelTimeEstimator extends FixedRouteLegTravelTimeEstimator {

	public CetinCompatibleLegTravelTimeEstimator(
			TravelTimeI linkTravelTimeEstimator,
			DepartureDelayAverageCalculator depDelayCalc) {

		super(linkTravelTimeEstimator, depDelayCalc);

	}

	@Override
	public double getLegTravelTimeEstimation(
			IdI personId,
			double departureTime,
			Location origin,
			Location destination,
			Route route,
			String mode) {

		double now = departureTime;

		now = this.processDeparture((Link) origin, now);
		now = this.processRouteTravelTime(route, now);
		now = this.processLink((Link) destination, now);

		return (now - departureTime);
	}
}
