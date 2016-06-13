/* *********************************************************************** *
 * project: org.matsim.*
 * RandomPlanSelector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.core.gbl.MatsimRandom;


/**
 * Select randomly one of the existing plans of the person.
 *
 * @author nagel
 */
public class RandomUnscoredPlanSelector<T extends BasicPlan, I> implements PlanSelector<T, I> {

	/**
	 * Choose a random plan from the person and return it.
	 * @return The newly selected plan for this person; <code>null</code> if the person has no plans.
	 */
	@Override
	public T selectPlan(final HasPlansAndId<T, I> person) {
		// following code copied from PersonImpl and then made runnable
		
		int cntUnscored = 0;
		for (T plan : person.getPlans()) {
			if (plan.getScore() == null) {
				cntUnscored++;
			}
		}
		if (cntUnscored > 0) {
			// select one of the unscored plans
			int idxUnscored = MatsimRandom.getRandom().nextInt(cntUnscored);
			cntUnscored = 0;
			for (T plan : person.getPlans()) {
				if (plan.getScore() == null) {
					if (cntUnscored == idxUnscored) {
						return plan;
					}
					cntUnscored++;
				}
			}
		}
		return null;
	}
}
