/* *********************************************************************** *
 * project: org.matsim.*
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

package org.matsim.core.replanning;

import org.matsim.core.router.TripRouterFactory;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;

public interface ReplanningContext {

	TripRouterFactory getTripRouterFactory();

	/**
	 * Comments:<ul>
	 * <li> In practical terms, this may have to be a factory.  Since in many cases different PlanAlgorithm instances need different
	 * instances of the TravelDisutilityCalculator in order to be thread safe. kai, jan'13
	 * </ul>
	 */
	TravelDisutility getTravelCostCalculator();

	/**
	 * Comments:<ul>
	 * <li> This one is more tricky.  Many implementations of this are EventHandlers in order to collect information about previous
	 * iterations.  Thus they need to be persistent over the iterations.  HOWEVER, in the end the same may be said about
	 * the travel cost ... it might, for example, collect toll events. kai, jan'13
	 * <li> Thus, overall, we will have to separate the "(travel time/travel cost) observer" from the "(travel time/travel cost) calculator".  And they
	 * probably/possibly all have to be made available here.
	 * kai, based on input from mzilske, jan'13
	 * </ul>
	 */
	TravelTime getTravelTimeCalculator();
	
	ScoringFunctionFactory getScoringFunctionFactory();

	int getIteration();

}
