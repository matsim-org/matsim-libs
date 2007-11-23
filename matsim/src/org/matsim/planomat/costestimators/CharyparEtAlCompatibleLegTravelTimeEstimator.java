/* *********************************************************************** *
 * project: org.matsim.*
 * CharyparEtAlCompatibleLegTravelTimeEstimator.java
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
 * deterministic event-driven queue-based agent traffic simulation (DEQSim).
 * <p>
 * (see Charypar, D., K. W. Axhausen and K. Nagel (2007) 
 * Event-driven queue-based traffic flow microsimulation,
 * paper presented at the 86th Annual Meeting of the Transportation Research
 * Board, Washington, D.C., January 2007.)
 * <p>
 * In constrast to <code>CetinCompatibleLegTravelTimeEstimator</code>,
 * <ul>
 * <li> the link of the origin activity is simulated, 
 *   and thus has to be included in this leg travel time estimation
 * <li> the link of the destination activity is not simulated, 
 *   and thus not included in this leg travel time estimation.
 * </ul>
 * @author meisterk
 *
 */
public class CharyparEtAlCompatibleLegTravelTimeEstimator extends
		FixedRouteLegTravelTimeEstimator {

	public CharyparEtAlCompatibleLegTravelTimeEstimator(
			TravelTimeI linkTravelTimeEstimator,
			DepartureDelayAverageCalculator depDelayCalc) {
		super(linkTravelTimeEstimator, depDelayCalc);
		// TODO Auto-generated constructor stub
	}

	@Override
	public double getLegTravelTimeEstimation(IdI personId,
			double departureTime, Location origin, Location destination,
			Route route, String mode) {

		double now = departureTime;
		
		now = this.processDeparture((Link) origin, now);
		now = this.processLink((Link) origin, now);
		now = this.processRouteTravelTime(route, now);
		
		return (now - departureTime);	
		
	}

}
