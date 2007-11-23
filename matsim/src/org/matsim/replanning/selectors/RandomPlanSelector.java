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

import org.matsim.plans.Person;
import org.matsim.plans.Plan;

/**
 * randomly selects one of the existing plans of the person
 * 
 * @author mrieser
 */
public class RandomPlanSelector implements PlanSelectorI {
	
	/**
	 * selects a random plan from the person
	 */
	public Plan selectPlan(Person person) {
		return person.selectRandomPlan();
	}

}
