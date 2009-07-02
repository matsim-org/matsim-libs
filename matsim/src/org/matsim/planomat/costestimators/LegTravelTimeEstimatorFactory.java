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
import org.matsim.core.config.groups.PlanomatConfigGroup.PlanomatConfigParameter;
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

	public LegTravelTimeEstimator getLegTravelTimeEstimator(String ltteName, PlansCalcRoute routingAlgorithm) {
		
		LegTravelTimeEstimator legTravelTimeEstimator = null;

		if (ltteName.equals(PlanomatConfigGroup.CETIN_COMPATIBLE)) {
			legTravelTimeEstimator = new CetinCompatibleLegTravelTimeEstimator(travelTime, tDepDelayCalc, routingAlgorithm);
		} else if (ltteName.equals(PlanomatConfigGroup.CHARYPAR_ET_AL_COMPATIBLE)) {
			legTravelTimeEstimator = new CharyparEtAlCompatibleLegTravelTimeEstimator(travelTime, tDepDelayCalc, routingAlgorithm);
		} else {
			throw new RuntimeException("legTravelTimeEstimator value: \"" + PlanomatConfigParameter.LEG_TRAVEL_TIME_ESTIMATOR_NAME.getActualValue() + "\" is not allowed.");
		}
		
		return legTravelTimeEstimator;
	}
	
	
}
