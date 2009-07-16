/* *********************************************************************** *
 * project: org.matsim.*
 * LegTravelTimeEstimator.java
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
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PlanImpl;

/**
 * Interface for the estimation of the travel time of a leg. Implement it
 * with your theory about what travel time an agent expects: The same all over
 * the day, bee line distance, based on events from the last iteration...
 *
 * @author meisterk
 *
 */
public interface LegTravelTimeEstimator {

	/**
	 * Override this method to initialize plan specific information.
	 * 
	 * @param plan
	 */
	public void initPlanSpecificInformation(final PlanImpl plan);
	
	/**
	 * Implement your assumption on travel time estimation here.
	 * 
	 * TODO don't have to pass a Leg-type object, a TransportMode object would be enough
	 * TODO provide a base implementation of this interface processing planomat config sim leg interpretation
	 * 
	 * @param personId identifier of the replanning person
	 * @param departureTime the departure time of the leg
	 * @param actOrigin the activity at the beginning of the leg
	 * @param actDestination the activity at the end of the leg
	 * @param legIntermediate the leg for which a travel time is estimated
	 * @param doModifyLeg control parameter indicating whether it is allowed that the leg object is modified or not 
	 * @return a travel time estimation.
	 */
	public double getLegTravelTimeEstimation(
			Id personId,
			double departureTime,
			ActivityImpl actOrigin,
			ActivityImpl actDestination,
			LegImpl legIntermediate,
			boolean doModifyLeg);
	
	/**
	 * Override this method to reset plan specific information.
	 * 
	 * @param plan
	 */
	public void resetPlanSpecificInformation();
	
}
