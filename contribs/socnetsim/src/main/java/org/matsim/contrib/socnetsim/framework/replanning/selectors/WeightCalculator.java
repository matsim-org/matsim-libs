/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.socnetsim.framework.replanning.selectors;

import org.matsim.api.core.v01.population.Plan;

import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;

public interface WeightCalculator {
	/**
	 * Defines the weight of a plan, used for selection.
	 * The method is called once for each plan: it is not required that
	 * the method returns the same result if called twice with the same
	 * arguments (ie it can return a random number).
	 *
	 * @param indivPlan the plan to weight
	 * @param replanningGroup the group for which plans are being selected.
	 * Selectors using "niching" measures may need this. No modifications should
	 * be done to the group.
	 */
	public double getWeight(
			final Plan indivPlan,
			final ReplanningGroup replanningGroup);
}
