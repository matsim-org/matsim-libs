/* *********************************************************************** *
 * project: org.matsim.*
 * LinearInterpolationLegTravelTimeEstimatorTest.java
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

import org.matsim.core.config.Config;
import org.matsim.core.network.NetworkLayer;
import org.matsim.core.router.PlansCalcRoute;
import org.matsim.core.router.costcalculators.TravelTimeDistanceCostCalculator;
import org.matsim.core.router.util.TravelCost;
import org.matsim.core.trafficmonitoring.TravelTimeCalculator;
import org.matsim.testcases.MatsimTestCase;

public class LinearInterpolationLegTravelTimeEstimatorTest extends MatsimTestCase {

	protected void setUp() throws Exception {
		super.setUp();
	}

	public void testGetLegTravelTimeEstimation() {

		Config config = super.loadConfig(null);

		NetworkLayer network = new NetworkLayer();

		
		
		DepartureDelayAverageCalculator tDepDelayCalc = new DepartureDelayAverageCalculator(network, 900);
		TravelTimeCalculator linkTravelTimeEstimator = new TravelTimeCalculator(network, config.travelTimeCalculator());
		TravelCost linkTravelCostEstimator = new TravelTimeDistanceCostCalculator(linkTravelTimeEstimator, config.charyparNagelScoring());

		PlansCalcRoute plansCalcRoute = new PlansCalcRoute(
				config.plansCalcRoute(), 
				network, 
				linkTravelCostEstimator, 
				linkTravelTimeEstimator);

		LegTravelTimeEstimatorFactory legTravelTimeEstimatorFactory = new LegTravelTimeEstimatorFactory(linkTravelTimeEstimator, tDepDelayCalc);
		LinearInterpolationLegTravelTimeEstimator testee = (LinearInterpolationLegTravelTimeEstimator) legTravelTimeEstimatorFactory.getLegTravelTimeEstimator(
				config.planomat().getSimLegInterpretation(),
				config.planomat().getRoutingCapability(),
				plansCalcRoute);

		fail("Not implemented yet.");
		
	}
	
}
