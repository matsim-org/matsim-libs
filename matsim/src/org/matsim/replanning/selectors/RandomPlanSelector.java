/* *********************************************************************** *
 * project: org.matsim.*
 * RandomPlanSelector.java
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

package org.matsim.replanning.selectors;

import org.matsim.population.Person;
import org.matsim.population.Plan;

/**
 * Select randomly one of the existing plans of the person.
 *
 * @author mrieser
 */
public class RandomPlanSelector implements PlanSelectorI {

	/**
	 * Choose a random plan from the person and return it.
	 * @return The newly selected plan for this person; <code>null</code> if the person has no plans.
	 */
	public Plan selectPlan(final Person person) {
		return person.getRandomPlan();
	}
}
