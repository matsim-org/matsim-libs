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
package playground.thibautd.jointtrips.replanning.selectors;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.thibautd.jointtrips.population.Clique;
import playground.thibautd.jointtrips.population.JointPlan;

/**
 * Returns the plan with the lowest score.
 * If it is initalised as "type aware", keeps the more "general" plan, that is, the "father" plan
 * from which we can obtain the others by desaffecting joint trips.
 * This is useful only if not using JointTripPossibilities.
 *
 * Plans which are duplicated are removed in priority.
 *
 * @author thibautd
 */
public class WorstJointPlanForRemovalSelector implements PlanSelector {
	private final boolean isTypeAware;

	public WorstJointPlanForRemovalSelector(
			final boolean isTypeAware) {
		this.isTypeAware = isTypeAware;
	}

	/**
	 * initialises a non-type aware instance.
	 */
	public WorstJointPlanForRemovalSelector() {
		this( false );
	}

	@Override
	public Plan selectPlan(final Person person) {
		if (person instanceof Clique) {
			return isTypeAware ? selectPlanTypeAware((Clique) person) : selectPlan((Clique) person);
		} else {
			throw new IllegalArgumentException("WorstJointPlanForRemoval used "+
					"for a non clique agent");
		}
	}

	private static Plan selectPlan(final Clique clique) {
		List<? extends Plan> plans = clique.getPlans();

		return Collections.min( plans , new ScoreComparator() );
	}

	private static Plan selectPlanTypeAware(final Clique clique) {
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

class ScoreComparator implements Comparator<Plan> {

	@Override
	public int compare(final Plan plan1, final Plan plan2) {
		return Double.compare( plan1.getScore() , plan2.getScore() );
	}
}
