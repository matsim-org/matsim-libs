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
import java.util.Map;
import java.util.TreeMap;
import java.util.Map.Entry;

import org.matsim.api.basic.v01.Id;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.gbl.Gbl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.utils.collections.Tuple;

/**
 * test the effect with "removeOldestPlan"
 * 
 * @author yu
 * 
 */
public class StrategyManagerWithRemoveOldestPlan2 extends StrategyManager {
	private Map<Plan, Tuple<Integer, Integer>> selectedHistory = new HashMap<Plan, Tuple<Integer, Integer>>();
	private boolean isInitialized = false;
	private Map<Id, Boolean> newPlansCreated = new HashMap<Id, Boolean>();
	private int maxPlansPerAgent = 0;

	public void run(final Population population) {
		// initialize all "selectedHistory" of plans
		if (!isInitialized) {
			for (Person person : population.getPersons().values()) {
				for (Plan plan : person.getPlans()) {
					Tuple<Integer, Integer> sh = new Tuple<Integer, Integer>(
							plan.isSelected() ? 1 : 0, 1);
					selectedHistory.put(plan, sh);
				}
			}
			isInitialized = true;
			System.out.println("----->selectHistory size :\t"
					+ selectedHistory.size());
		}

		// initialize all strategies
		for (PlanStrategy strategy : this.strategies)
			strategy.init();

		maxPlansPerAgent = getMaxPlansPerAgent();
		// then go through the population and assign each person to a strategy
		for (Person person : population.getPersons().values()) {

			if (maxPlansPerAgent > 0)
				removeOldestPlan(person);// removes the plan,
			// which was not used for the longst time

			PlanStrategy strategy = this.chooseStrategy();
			if (strategy != null) {
				strategy.run(person);
				// judge, whether new plans were created.
				newPlansCreated.put(person.getId(), (strategy
						.getNumberOfStrategyModules() > 0));
			} else {
				Gbl.errorMsg("No strategy found!");
			}
		}

		// finally make sure all strategies have finished there work
		for (PlanStrategy strategy : this.strategies)
			strategy.finish();

		for (Entry<Id, Boolean> entry : newPlansCreated.entrySet()) {
			if (entry.getValue()) {// new plans were created
				for (Plan plan : population.getPersons().get(entry.getKey())
						.getPlans()) {
					if (plan.isSelected()) {
						selectedHistory.put(plan, new Tuple<Integer, Integer>(
								0, 1));
					} else {
						Tuple<Integer, Integer> sh = selectedHistory.get(plan);
						selectedHistory.put(plan, new Tuple<Integer, Integer>(
								sh.getFirst(), sh.getSecond() + 1));
					}
				}
			} else {// no new plans were created
				for (Plan plan : population.getPersons().get(entry.getKey())
						.getPlans()) {
					Tuple<Integer, Integer> sh = selectedHistory.get(plan);
					selectedHistory.put(plan, new Tuple<Integer, Integer>(plan
							.isSelected() ? sh.getFirst() + 1 : sh.getFirst(),
							sh.getSecond() + 1));
				}
			}
		}
		newPlansCreated.clear();
	}

	private static double selectedRate(Tuple<Integer, Integer> tuple) {
		return tuple.getFirst().doubleValue() / tuple.getSecond().doubleValue();
	}

	private static boolean oldEnough(Tuple<Integer, Integer> tuple) {
		return tuple.getSecond().intValue() >= 4;
	}

	private void removeOldestPlan(Person person) {
		List<Plan> plans = person.getPlans();

		int oldPlansSize = 0;
		TreeMap<Double, Plan> oldPlans = new TreeMap<Double, Plan>();

		for (Plan plan : plans) {
			Tuple<Integer, Integer> tuple = selectedHistory.get(plan);
			if (oldEnough(tuple)) {
				oldPlansSize++;
				oldPlans.put(selectedRate(tuple), plan);
			}
		}

		if (oldPlansSize <= maxPlansPerAgent) {
			return;
		}

		HashMap<Plan.Type, Integer> typeCounts = new HashMap<Plan.Type, Integer>();
		// initialize list of types
		for (Plan plan : oldPlans.values()) {
			Integer cnt = typeCounts.get(plan.getType());
			if (cnt == null) {
				typeCounts.put(plan.getType(), 1);
			} else {
				typeCounts.put(plan.getType(), cnt + 1);
			}
		}

		while (oldPlansSize > maxPlansPerAgent) {
			Plan oldestPlan = null;
			Double keyOfOldestPlan = null;
			for (Entry<Double, Plan> entry : oldPlans.entrySet()) {
				Plan plan_i = entry.getValue();
				if (typeCounts.get(plan_i.getType()) > 1) {
					oldestPlan = plan_i;
					keyOfOldestPlan = entry.getKey();
					break;
				}
			}
			if (oldestPlan != null) {
				person.removePlan(oldestPlan);
				oldPlans.remove(keyOfOldestPlan);
				oldPlansSize = oldPlans.size();
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
