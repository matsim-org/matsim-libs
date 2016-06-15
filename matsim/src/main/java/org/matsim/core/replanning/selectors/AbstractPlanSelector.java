/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

import java.util.List;
import java.util.Map;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.gbl.MatsimRandom;

/**
 * Standardizing plans selection when weights are given.
 * 
 * @author nagel
 *
 */
public abstract class AbstractPlanSelector implements PlanSelector<Plan, Person> {

	@Override
	public final Plan selectPlan(HasPlansAndId<Plan, Person> person) {
		// First check if there are any unscored plans
		Plan selectedPlan = new RandomUnscoredPlanSelector<Plan, Person>().selectPlan(person);
		if (selectedPlan != null) return selectedPlan;
		// Okay, no unscored plans...

		// Build the weights of all plans

		// - now calculate the weights
		Map<Plan,Double> wc = calcWeights(person.getPlans() );
		double sumWeights = 0. ;
		for ( Double score : wc.values() ) {
			sumWeights += score ;
		}

		// choose a random number over interval [0,sumWeights[
		double selnum = sumWeights*MatsimRandom.getRandom().nextDouble();
		for (Plan plan : person.getPlans()) {
			selnum -= wc.get(plan);
			if (selnum <= 0.0) {
				return plan;
			}
		}

		// this case should never happen, except a person has no plans at all.
		return null;

	}
	
	abstract protected Map<Plan,Double> calcWeights( List<? extends Plan> plans ) ;
	
}
