/* *********************************************************************** *
 * project: org.matsim.*
 * RandomPlanSelectorWithPlanType.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package playground.yu.replanning.selectors;

import java.util.HashMap;
import java.util.List;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.selectors.PlanSelector;

public class RandomPlanSelectorWithPlanType implements PlanSelector {

	@Override
	public Plan selectPlan(Person person) {
		// hashmap that returns "Integer" count for given plans type:
		HashMap<String, Integer> typeCounts = new HashMap<String, Integer>();
		// count how many plans per type an agent has:
		List<Plan> plans = (List<Plan>) person.getPlans();
		for (Plan plan : plans) {
			Integer cnt = typeCounts.get(((PlanImpl) plan).getType());
			if (cnt == null) {
				typeCounts.put(((PlanImpl) plan).getType(), Integer.valueOf(1));
			} else {
				typeCounts.put(((PlanImpl) plan).getType(), Integer.valueOf(cnt
						.intValue() + 1));
			}
		}
		Plan toRemove;
		do {
			toRemove = ((PersonImpl) person).getRandomPlan();
		} while (typeCounts.get(((PlanImpl) toRemove).getType()) <= 1);
		return toRemove;
	}
}
