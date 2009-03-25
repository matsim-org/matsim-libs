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

import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.population.BasicLeg;
import org.matsim.core.api.network.Network;
import org.matsim.core.api.population.Activity;
import org.matsim.core.api.population.CarRoute;
import org.matsim.core.api.population.Leg;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.router.util.TravelTime;

/**
 * This class estimates travel times from events produced by the
 * deterministic time-step-driven queue-based agent traffic simulation (DQSim).
 * <p>
 * In contrast to {@link CharyparEtAlCompatibleLegTravelTimeEstimator},
 * <ul>
 * <li> the link of the origin activity is not simulated,
 *   and thus not included in this leg travel time estimation.
 * <li> the link of the destination activity is simulated,
 *   and thus has to be included in this leg travel time estimation.
 * </ul>
 * <p>
 * 
 * @see "Cetin, N. (2005) Large-scale parallel graph-based simulations, Ph.D. Thesis, ETH Zurich, Zurich.)"
 * @author meisterk
 * 
 */
public class CetinCompatibleLegTravelTimeEstimator extends FixedRouteLegTravelTimeEstimator {

	public CetinCompatibleLegTravelTimeEstimator(
			TravelTime linkTravelTimeEstimator,
			TravelCost linkTravelCostEstimator,
			DepartureDelayAverageCalculator depDelayCalc, Network network) {
		super(linkTravelTimeEstimator, linkTravelCostEstimator, depDelayCalc, network);
	}

	@Override
	public double getLegTravelTimeEstimation(Id personId, double departureTime,
			Activity actOrigin, Activity actDestination, Leg legIntermediate) {

		double legTravelTimeEstimation;
		
		if (legIntermediate.getMode().equals(BasicLeg.Mode.car)) {

			double now = departureTime;

			now = this.processDeparture(actOrigin.getLink(), now);
			now = this.processRouteTravelTime((CarRoute) legIntermediate.getRoute(), now);
			now = this.processLink(actDestination.getLink(), now);

			legTravelTimeEstimation = (now - departureTime);

		} else {
	
			legTravelTimeEstimation = super.getLegTravelTimeEstimation(personId, departureTime, actOrigin, actDestination, legIntermediate);
			
		}
		
		return legTravelTimeEstimation;
	}

}
