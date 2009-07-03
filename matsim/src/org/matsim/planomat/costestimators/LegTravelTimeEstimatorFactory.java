/* *********************************************************************** *
 * project: org.matsim.*
 * LegTravelTimeEstimatorFactory.java
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

package org.matsim.planomat.costestimators;

import org.matsim.core.config.groups.PlanomatConfigGroup;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.util.TravelTime;

public class LegTravelTimeEstimatorFactory {

	private final TravelTime travelTime;
	private final DepartureDelayAverageCalculator tDepDelayCalc;

	public LegTravelTimeEstimatorFactory(TravelTime travelTime, DepartureDelayAverageCalculator depDelayCalc) {
		super();
		this.travelTime = travelTime;
		this.tDepDelayCalc = depDelayCalc;
	}

	public LegTravelTimeEstimator getLegTravelTimeEstimator(PlanomatConfigGroup.SimLegInterpretation ltteName, PlansCalcRoute routingAlgorithm) {
		
		LegTravelTimeEstimator legTravelTimeEstimator = new FixedRouteLegTravelTimeEstimator(this.travelTime, this.tDepDelayCalc, routingAlgorithm, ltteName);
		
		return legTravelTimeEstimator;
	}
	
}
