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

import java.util.HashMap;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.thibautd.jointtripsoptimizer.population.Clique;
import playground.thibautd.jointtripsoptimizer.population.JointPlan;

/**
 * Returns the plan with the lowest score.
 * Always keeps the more "general" plan, that is, the "father" plan
 * from which we can obtain the others by desaffecting joint trips.
 *
 * Plans which are duplicated are removed in priority.
 *
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

	//public Plan selectPlan(Clique clique) {
	//	Double worstScore = Double.POSITIVE_INFINITY;
	//	Double currentScore;
	//	int currentLongestType = 0;
	//	int currentTypeSize;
	//	int countFathers = 0;
	//	Plan fatherPlan=null;
	//	Plan worstPlan=null;
	//	List<? extends Plan> individualPlans = clique.getPlans();

	//	// count "father" plans.
	//	// assumes that there is only one initial plan, so that the more
	//	// general plan is the one with the longest type.
	//	for (Plan plan: individualPlans) {
	//		currentTypeSize = ((JointPlan) plan).getType().length();
	//		if (currentTypeSize > currentLongestType) {
	//			currentLongestType = currentTypeSize;
	//			fatherPlan = plan;
	//			countFathers = 1;
	//		}
	//		else if (currentTypeSize == currentLongestType) {
	//			countFathers++;
	//		}
	//	}

	//	for (Plan plan : individualPlans) {
	//		// consider the plan only if it is not the only "father" plan
	//		if ((plan != fatherPlan)||(countFathers > 1)) {
	//			currentScore = plan.getScore();			
	//			if (currentScore==null) {
	//				//if there is an unscored plan, consider it as worst
	//				return plan;
	//			}
	//			else if (currentScore < worstScore) {
	//				worstScore = currentScore;
	//				worstPlan = plan;
	//			}
	//		}
	//	}

	//	return worstPlan;
	//}

	public Plan selectPlan(Clique clique) {
		Double worstScore = Double.POSITIVE_INFINITY;
		Double currentScore;
		String currentType;
		int currentLongestType = 0;
		int currentTypeSize;
		int countFathers = 0;
		Plan fatherPlan=null;
		Plan worstPlan=null;

		// hashmap that returns "Integer" count for given plans type:
		HashMap<String, Integer> typeCounts = new HashMap<String, Integer>();

		for (Plan plan : clique.getPlans()) {
			// count how many plans per type an agent has:
			currentType = ((JointPlan) plan).getType();
			Integer cnt = typeCounts.get(currentType);
			if (cnt == null) {
				typeCounts.put(currentType, Integer.valueOf(1));
			} else {
				typeCounts.put(currentType, Integer.valueOf(cnt.intValue() + 1));
			}

			// length check (always keep "father")
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

		for (Plan plan : clique.getPlans()) {

			// if we have more than one plan of the same type:
			if (typeCounts.get(((JointPlan) plan).getType()).intValue() > 1) {

				// if there is a plan without score:
				if (plan.getScore() == null) {
					// say that the plan without score now is the "worst":
					worstPlan = plan;

					// make sure that this one remains the selected plan:
					worstScore = Double.NEGATIVE_INFINITY;

				// otherwise do the usual logic to find the plan with the minimum score,
				// avoiding to select the only father (if it exists)
				} else if ((plan.getScore().doubleValue() < worstScore) &&
						( (plan != fatherPlan) || (countFathers > 1) )
						) {
					worstPlan = plan;
					worstScore = plan.getScore().doubleValue();
				}
			}
			
			// (otherwise we just keep "worst=null") 
		}

		if (worstPlan == null) {
			// there is exactly one plan, or we have of each plan-type exactly one.
			// select the one with worst score globally
			for (Plan plan : clique.getPlans()) {
				if ( (plan.getScore() == null)  &&
						( (plan != fatherPlan) || (countFathers > 1) )
						) {
					return plan;
				}
				if ( (plan.getScore().doubleValue() < worstScore) &&
						( (plan != fatherPlan) || (countFathers > 1) )
						) {
					worstPlan = plan;
					worstScore = plan.getScore().doubleValue();
				}
			}
		}
		return worstPlan;
	}
}

