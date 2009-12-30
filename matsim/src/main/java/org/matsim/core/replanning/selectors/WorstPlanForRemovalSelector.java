/* *********************************************************************** *
 * project: org.matsim.*
 * WorstPlanSelector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import java.util.HashMap;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PlanImpl;

/**
 * <p>Selects the worst plan of a person (most likely for removal), but respects
 * the set plan types in a way the no plan is selected that is the last one of
 * its type.</p> 
 * <p>(I would say that it can select the last of its type if it finds nothing else.  However, this algo should only
 * be used if an agent has more plans than maxPlansPerAgent, so make sure that that parameter is set large enough for
 * your purposes.  kai, oct'09)</p>
 * <p>Plans without a score are seen as worst and selected accordingly.</p>
 *
 * @author mrieser
 */
public class WorstPlanForRemovalSelector implements PlanSelector {

	public Plan selectPlan(Person person) {

		// hashmap that returns "Integer" count for given plans type:
		HashMap<PlanImpl.Type, Integer> typeCounts = new HashMap<PlanImpl.Type, Integer>();

		// count how many plans per type an agent has:
		for (Plan plan : person.getPlans()) {
			Integer cnt = typeCounts.get(((PlanImpl) plan).getType());
			if (cnt == null) {
				typeCounts.put(((PlanImpl) plan).getType(), Integer.valueOf(1));
			} else {
				typeCounts.put(((PlanImpl) plan).getType(), Integer.valueOf(cnt.intValue() + 1));
			}
		}

		Plan worst = null;
		double worstScore = Double.POSITIVE_INFINITY;
		for (Plan plan : person.getPlans()) {

			// if we have more than one plan of the same type:
			if (typeCounts.get(((PlanImpl) plan).getType()).intValue() > 1) {

				// if there is a plan without score:
				if (plan.getScore() == null) {
					// say that the plan without score now is the "worst":
					worst = plan;

					// make sure that this one remains the selected plan:
					worstScore = Double.NEGATIVE_INFINITY;

				// otherwise do the usual logic to find the plan with the minimum score:
				} else if (plan.getScore().doubleValue() < worstScore) {
					worst = plan;
					worstScore = plan.getScore().doubleValue();
				}
			}
			
			// (otherwise we just keep "worst=null") 
		}

		if (worst == null) {
			// there is exactly one plan, or we have of each plan-type exactly one.
			// select the one with worst score globally
			for (Plan plan : person.getPlans()) {
				if (plan.getScore() == null) {
					return plan;
				}
				if (plan.getScore().doubleValue() < worstScore) {
					worst = plan;
					worstScore = plan.getScore().doubleValue();
				}
			}
		}
		return worst;
	}

}
