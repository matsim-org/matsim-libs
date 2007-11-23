/* *********************************************************************** *
 * project: org.matsim.*
 * PlansCalcRouteLandmarks.java
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

package org.matsim.router;

import org.matsim.network.NetworkLayer;
import org.matsim.router.costcalculators.FreespeedTravelTimeCost;
import org.matsim.router.util.PreProcessLandmarks;
import org.matsim.router.util.TravelCostI;
import org.matsim.router.util.TravelTimeI;

/**
 * A PersonAlgorithm that calculates and sets the routes of a person's actitivities.
 * Uses AStarLandmarks for routing.
 * @author lnicolas
 *
 */
public class PlansCalcRouteLandmarks extends PlansCalcRoute {

	//////////////////////////////////////////////////////////////////////
	// constructors
	//////////////////////////////////////////////////////////////////////

	public PlansCalcRouteLandmarks(NetworkLayer network, PreProcessLandmarks preProcessData,
			TravelCostI costCalculator, TravelTimeI timeCalculator) {
		this(network, preProcessData, costCalculator, timeCalculator, false);
	}
	
//	public PlansCalcRouteLandmarks(NetworkLayer network, TravelCostI costCalculator,
//			TravelTimeI timeCalculator, boolean calcMissingOnly) {
//		preProcessData = new PreProcessLandmarks(new FreespeedTravelTimeCost());
//		preProcessData.run(network);
//		this(network, null, costCalculator, timeCalculator, calcMissingOnly);
//	}
	
	public PlansCalcRouteLandmarks(NetworkLayer network, PreProcessLandmarks preProcessData,
			TravelCostI costCalculator, TravelTimeI timeCalculator, boolean calcMissingOnly) {
		this(network, preProcessData, costCalculator, timeCalculator, calcMissingOnly,
				new FreespeedTravelTimeCost());
	}
	
	PlansCalcRouteLandmarks(NetworkLayer network, PreProcessLandmarks preProcessData,
			TravelCostI costCalculator, TravelTimeI timeCalculator, boolean calcMissingOnly,
			FreespeedTravelTimeCost timeCostCalc) {
		super(network, costCalculator, timeCalculator, calcMissingOnly,
				new AStarLandmarks(network, preProcessData, costCalculator, timeCalculator),
				new AStarLandmarks(network, preProcessData, timeCostCalc, timeCostCalc));
	}
}
