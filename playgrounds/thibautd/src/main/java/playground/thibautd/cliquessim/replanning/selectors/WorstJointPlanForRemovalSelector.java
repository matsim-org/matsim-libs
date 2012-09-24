/* *********************************************************************** *
 * project: org.matsim.*
 * WorstJointPlanForRemoval.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2011 by the members listed in the COPYING,        *
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
package playground.thibautd.cliquessim.replanning.selectors;

import java.util.Comparator;
import java.util.HashMap;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.thibautd.cliquessim.population.Clique;
import playground.thibautd.cliquessim.population.JointPlan;

/**
 * Returns the plan with the lowest score.
 * If there exists duplicated plan types, the plans of those types
 * only are considered for exclusion (diversity is enforced)
 *
 * @author thibautd
 */
public class WorstJointPlanForRemovalSelector implements PlanSelector {

	/**
	 * initialises a non-type aware instance.
	 */
	public WorstJointPlanForRemovalSelector() {
	}

	@Override
	public Plan selectPlan(final Person person) {
		if (person instanceof Clique) {
			return  selectPlan((Clique) person);
		} else {
			throw new IllegalArgumentException("WorstJointPlanForRemoval used "+
					"for a non clique agent");
		}
	}

	private static Plan selectPlan(final Clique clique) {

		// hashmap that returns "Integer" count for given plans type:
		HashMap<String, Integer> typeCounts = new HashMap<String, Integer>();

		// count how many plans per type an agent has:
		for (Plan plan : clique.getPlans()) {
			Integer cnt = typeCounts.get(((JointPlan) plan).getType());
			if (cnt == null) {
				typeCounts.put(((JointPlan) plan).getType(), Integer.valueOf(1));
			} else {
				typeCounts.put(((JointPlan) plan).getType(), Integer.valueOf(cnt.intValue() + 1));
			}
		}

		Plan worst = null;
		double worstScore = Double.POSITIVE_INFINITY;
		for (Plan plan : clique.getPlans()) {

			// if we have more than one plan of the same type:
			if (typeCounts.get(((JointPlan) plan).getType()).intValue() > 1) {

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
			for (Plan plan : clique.getPlans()) {
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

class ScoreComparator implements Comparator<Plan> {

	@Override
	public int compare(final Plan plan1, final Plan plan2) {
		return Double.compare( plan1.getScore() , plan2.getScore() );
	}
}
