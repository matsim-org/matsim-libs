/* *********************************************************************** *
 * project: org.matsim.*
 * PlanSelector.java
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

package org.matsim.core.replanning.selectors;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Plan;

/**
 * select a plan of a person
 *
 * @author mrieser
 */
public interface PlanSelector extends GenericPlanSelector<Plan>{

//	/**
//	 * Select and return a plan of a person.
//	 * Note that is is <b>NOT</b> the responsibility of this method to set the 
//	 * plan status as "selected". It is the responsability of the caller code,
//	 * which may decide to select, copy, remove, modify, or do whatever with the
//	 * returned plan.
//	 *
//	 * @param person
//	 * @return selected plan, or null if the person has no plans.
//	 */
//	public Plan selectPlan(HasPlansAndId<Plan> person);

}
