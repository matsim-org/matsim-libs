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

import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;

/**
 * Selects the worst plan of a person (most likely for removal), but respects
 * the set plan types in a way the no plan is selected that is the last one of
 * its type. Plans without a score are seen as worst and selected accordingly.
 *
 * @author mrieser
 */
public class WorstPlanForRemovalSelector implements PlanSelector {

	public PlanImpl selectPlan(PersonImpl person) {
		
		HashMap<PlanImpl.Type, Integer> typeCounts = new HashMap<PlanImpl.Type, Integer>();
		// initialize list of types
		for (PlanImpl plan : person.getPlans()) {
			Integer cnt = typeCounts.get(plan.getType());
			if (cnt == null) {
				typeCounts.put(plan.getType(), Integer.valueOf(1));
			} else {
				typeCounts.put(plan.getType(), Integer.valueOf(cnt.intValue() + 1));
			}
		}
			PlanImpl worst = null;
			double worstScore = Double.POSITIVE_INFINITY;
			for (PlanImpl plan : person.getPlans()) {
				if (typeCounts.get(plan.getType()).intValue() > 1) {
					if (plan.getScore() == null) {
						worst = plan;
						// make sure no other score could be less than this
						worstScore = Double.NEGATIVE_INFINITY;
					} else if (plan.getScore().doubleValue() < worstScore) {
						worst = plan;
						worstScore = plan.getScore().doubleValue();
					}
				}
			}

		if (worst == null) {
			// there is exactly one plan, or we have of each plan-type exactly one.
			// select the one with worst score globally
			for (PlanImpl plan : person.getPlans()) {
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
