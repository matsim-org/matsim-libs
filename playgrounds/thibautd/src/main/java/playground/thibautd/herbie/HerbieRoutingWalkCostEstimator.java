/* *********************************************************************** *
 * project: org.matsim.*
 * HerbieRoutingWalkCostEstimator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.herbie;

import org.matsim.pt.router.TransitRouterConfig;

/**
 * Provides a static method to estimate the walk cost in pt routing.
 * It uses the code from the fork used for the herbie project,
 * including the hard-coded parameters.
 * @author thibautd
 */
public class HerbieRoutingWalkCostEstimator {
	public static double getWalkCost(
			final TransitRouterConfig config,
			final double distance, 
			final double travelTime) {
		double timeThreashold1 = 20.0 * 60.0; // sec
		double timeThreashold2 = 60.0 * 60.0; // sec
		
		double walkCost = travelTime * ( 0 - config.getMarginalUtilityOfTravelTimeWalk_utl_s());
		
		if(travelTime > timeThreashold1) {
			walkCost += 40d * (travelTime - timeThreashold1) * (0d - config.getMarginalUtilityOfTravelTimeWalk_utl_s());
		}
		if(travelTime > timeThreashold2) {
			walkCost += 0.3 * (travelTime - timeThreashold2) * (0d - config.getMarginalUtilityOfTravelTimeWalk_utl_s());
		}
		
		walkCost += 0.00065 * distance;
		
		return walkCost;
	}

	/**
	 * Several attempts were made, with different versions of the routing cost.
	 * Due to errors in eclipse configuration, different states of the modifications
	 * of the local copies where mixed together at compile time to create the JARs
	 * used for production run. Thus, we need the diverse versions to use them
	 * in the places where they where used in the jars...
	 */
	public static double getFormerWalkCost(
			final TransitRouterConfig config,
			final double distance, 
			final double travelTime) {
		double timeThreashold1 = 20.0 * 60.0; // sec
		double cost = -travelTime * config.getMarginalUtilityOfTravelTimeWalk_utl_s();

		if(travelTime > timeThreashold1) {
			cost -= 6 * (travelTime - timeThreashold1) * config.getMarginalUtilityOfTravelTimeWalk_utl_s();
		}

		cost += distance * 0.00065d;

		return cost;
	}

	public static double getFormerWalkCostNoDistance(
			final TransitRouterConfig config,
			final double travelTime) {
		double timeThreashold1 = 20.0 * 60.0; // sec
		double cost = travelTime * (-config.getMarginalUtilityOfTravelTimeWalk_utl_s());

		if(travelTime > timeThreashold1) {
			cost += 6 * (travelTime - timeThreashold1) * (-config.getMarginalUtilityOfTravelTimeWalk_utl_s());
		}

		return cost;
	}

	public static double getTransitRouterInitialWalkCost(
			final TransitRouterConfig config,
			final double travelTime) {
		return -travelTime * config.getMarginalUtilityOfTravelTimeWalk_utl_s();
	}
}
