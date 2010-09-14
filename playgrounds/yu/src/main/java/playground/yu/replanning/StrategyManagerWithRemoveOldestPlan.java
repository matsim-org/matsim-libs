/* *********************************************************************** *
 * project: org.matsim.*
 * StrategyManagerWithRemoveOldestPlan.java
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

/**
 * 
 */
package playground.yu.replanning;

import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.selectors.PlanSelector;

/**
 * test the effect with "removeOldestPlan"
 * 
 * @author yu
 * 
 */
public class StrategyManagerWithRemoveOldestPlan extends StrategyManager {
	public static class OldestPlanForRemovalSelector implements PlanSelector {

		@Override
		public Plan selectPlan(Person person) {
			HashMap<PlanImpl.Type, Integer> typeCounts = new HashMap<PlanImpl.Type, Integer>();

			List<? extends Plan> plans = person.getPlans();
			// initialize list of types
			for (Plan plan : plans) {
				Integer cnt = typeCounts.get(((PlanImpl) plan).getType());
				if (cnt == null) {
					typeCounts.put(((PlanImpl) plan).getType(), 1);
				} else {
					typeCounts.put(((PlanImpl) plan).getType(), cnt + 1);
				}
			}
			Plan oldest = null;
			int size = plans.size();
			for (int i = 0; i < size; i++) {
				Plan plan_i = plans.get(i);
				// if we have more than one plan of the same type:
				if (typeCounts.get(((PlanImpl) plan_i).getType()) > 1) {
					return plan_i;
				}
			}
			if (oldest == null) {
				throw new RuntimeException(
						"the oldest Plan could not be found, this should only happen if the number of plan-types is greater than or equals maxSize");
			}
			return oldest;
		}

	}

	public StrategyManagerWithRemoveOldestPlan() {
		setPlanSelectorForRemoval(new OldestPlanForRemovalSelector());
	}

	@Override
	protected void afterRunHook(Population population) {
		for (Person person : population.getPersons().values()) {
			List<? extends Plan> plans = person.getPlans();
			if (!plans.get(plans.size() - 1).isSelected()) {
				// if selected plan is not last plan, call
				// "makeSelectedPlanYoung"
				this.makeSelectedPlanYoung(person);
			}
		}
	}

	private void makeSelectedPlanYoung(Person person) {
		Plan selected = person.getSelectedPlan();
		((PersonImpl) person).removePlan(selected);
		person.addPlan(selected);
		((PersonImpl) person).setSelectedPlan(selected);
	}
}
