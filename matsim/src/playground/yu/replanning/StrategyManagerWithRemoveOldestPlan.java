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
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;

/**
 * test the effect with "removeOldestPlan"
 * 
 * @author yu
 * 
 */
public class StrategyManagerWithRemoveOldestPlan extends StrategyManager {
	private Set<Id> noNewPlans = new HashSet<Id>();

	public void run(final Population population) {
		// initialize all strategies
		for (PlanStrategy strategy : this.strategies)
			strategy.init();

		int maxPlansPerAgent = getMaxPlansPerAgent();
		// then go through the population and assign each person to a strategy
		for (Person person : population.getPersons().values()) {
			if (maxPlansPerAgent > 0)
				removeOldestPlan(person, maxPlansPerAgent);// removes the plan,
			// which was not
			// used for the
			// longst time

			PlanStrategy strategy = this.chooseStrategy();
			if (strategy != null) {
				strategy.run(person);
				if (strategy.getNumberOfStrategyModules() == 0)
					noNewPlans.add(person.getId());
			} else {
				Gbl.errorMsg("No strategy found!");
			}
		}

		// finally make sure all strategies have finished there work
		for (PlanStrategy strategy : this.strategies)
			strategy.finish();

		// make the new selected Plan "young" (with the biggest index in List),
		// if no new plan was created by the strategy.
		for (Id personId : noNewPlans) {
			Person person = population.getPersons().get(personId);
			makeSelectedPlanYoung(person, person.getSelectedPlan());
		}
		// empty noNewPlans
		noNewPlans.clear();
	}

	private void makeSelectedPlanYoung(Person person, Plan selectedPlan) {
		person.removePlan(selectedPlan);
		person.addPlan(selectedPlan);
		person.setSelectedPlan(selectedPlan);
	}

	private void removeOldestPlan(Person person, int maxSize) {
		List<Plan> plans = person.getPlans();
		int size = plans.size();
		if (size <= maxSize) {
			return;
		}

		HashMap<Plan.Type, Integer> typeCounts = new HashMap<Plan.Type, Integer>();
		// initialize list of types
		for (Plan plan : plans) {
			Integer cnt = typeCounts.get(plan.getType());
			if (cnt == null) {
				typeCounts.put(plan.getType(), 1);
			} else {
				typeCounts.put(plan.getType(), cnt + 1);
			}
		}

		while (size > maxSize) {
			Plan oldestPlan = null;
			for (int i = 0; i < size; i++) {
				Plan plan_i = plans.get(i);
				if (typeCounts.get(plan_i.getType()) > 1) {
					oldestPlan = plan_i;
					break;
				}
			}
			if (oldestPlan != null) {
				person.removePlan(oldestPlan);
				size = plans.size();
				if (oldestPlan.isSelected()) {
					person.setSelectedPlan(person.getRandomPlan());
				}
				// reduce the number of plans of this type
				Integer cnt = typeCounts.get(oldestPlan.getType());
				typeCounts.put(oldestPlan.getType(), cnt - 1);
			} else {
				return; // should only happen if we have more different
				// plan-types than maxSize
			}
		}
	}
}
