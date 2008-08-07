/* *********************************************************************** *
 * project: org.matsim.*
 * PlanSelectorI.java
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
 * select a plan of a person
 *
 * @author mrieser
 */
public interface PlanSelectorI {

	/**
	 * Select and return a plan of a person.
	 *
	 * @param person
	 * @return selected plan, or null if the person has no plans.
	 */
	public Plan selectPlan(Person person);

}
