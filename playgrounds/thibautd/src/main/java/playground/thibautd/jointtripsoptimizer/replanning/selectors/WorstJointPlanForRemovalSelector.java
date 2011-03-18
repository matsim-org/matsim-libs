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
package playground.thibautd.jointtripsoptimizer.replanning.selectors;

import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.thibautd.jointtripsoptimizer.population.Clique;
import playground.thibautd.jointtripsoptimizer.population.JointPlan;

/**
 * Returns the plan with the lowest score.
 * Cruder than the WorstPlanForRemoval selector, which takes the type of plans
 * into account.
 * @todo: include the "type" 
 * @author thibautd
 */
public class WorstJointPlanForRemovalSelector implements PlanSelector {

	@Override
	public Plan selectPlan(Person person) {

		if (person instanceof Clique) {
			return selectPlan((Clique) person);
		} else {
			throw new IllegalArgumentException("WorstJointPlanForRemoval used "+
					"for a non clique agent");
		}
	}

	public Plan selectPlan(Clique clique) {
		Double worstScore = Double.POSITIVE_INFINITY;
		Double currentScore;
		int currentLongestType = 0;
		int currentTypeSize;
		int countFathers = 0;
		Plan fatherPlan=null;
		Plan worstPlan=null;
		List<? extends Plan> individualPlans = clique.getPlans();

		// count "father" plans.
		// assumes that there is only one initial plan, so that the more
		// general plan is the one with the longest type.
		for (Plan plan: individualPlans) {
			currentTypeSize = ((JointPlan) plan).getType().length();
			if (currentTypeSize > currentLongestType) {
				currentLongestType = currentTypeSize;
				fatherPlan = plan;
				countFathers = 1;
			}
			else if (currentTypeSize == currentLongestType) {
				countFathers++;
			}
		}

		for (Plan plan : individualPlans) {
			// consider the plan only if it is not the only "father" plan
			if ((plan != fatherPlan)||(countFathers > 1)) {
				currentScore = plan.getScore();			
				if (currentScore==null) {
					//if there is an unscored plan, consider it as worst
					return plan;
				}
				else if (currentScore < worstScore) {
					worstScore = currentScore;
					worstPlan = plan;
				}
			}
		}

		return worstPlan;
	}
}

