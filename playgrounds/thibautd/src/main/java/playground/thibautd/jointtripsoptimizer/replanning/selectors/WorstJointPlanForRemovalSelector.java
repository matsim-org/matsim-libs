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

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.thibautd.jointtripsoptimizer.population.Clique;

/**
 * Returns the plan with the lowest score.
 * Cruder than the WorstPlanForRemoval selector, which takes the type of plans
 * into account.
 * @author thibautd
 */
public class WorstJointPlanForRemovalSelector implements PlanSelector {

	@Override
	public Plan selectPlan(Person person) {
		Double worstScore = Double.POSITIVE_INFINITY;
		Double currentScore;
		Plan worstPlan=null;

		if (person instanceof Clique) {
			for (Plan plan : person.getPlans()) {
				currentScore = plan.getScore();			
				if ((currentScore==null)||(currentScore < worstScore)) {
					//if there is an unscored plan, consider it as worst
					if (currentScore==null) return plan;
					worstScore = currentScore;
					worstPlan = plan;
				}
			}
			return worstPlan;
		} else {
			throw new IllegalArgumentException("WorstJointPlanForRemoval used "+
					"for a non clique agent");
		}
	}
}

