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

import org.matsim.basic.v01.Id;
import org.matsim.plans.Route;
import org.matsim.world.Location;

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
	 * Implement your assumption on travel time estimation here.
	 *
	 * @param personId identifier of the replanning person
	 * @param departureTime the departure time of the leg
	 * @param origin the location of the prior activity
	 * @param destination the location of the next activity
	 * @param route
	 * @param mode
	 * @return Returns a travel time estimation.
	 */
	public double getLegTravelTimeEstimation(
			Id personId,
			double departureTime,
			Location origin,
			Location destination,
			Route route,
			String mode);

}
