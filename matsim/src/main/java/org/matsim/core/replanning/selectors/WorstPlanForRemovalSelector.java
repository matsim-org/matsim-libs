/* *********************************************************************** *
 * project: org.matsim.*
 * WorstPlanSelector.java
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

package org.matsim.core.replanning.selectors;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

/**
 * <p>Selects the worst plan of a person (most likely for removal), but respects
 * the set plan types in a way the no plan is selected that is the last one of
 * its type.</p> 
 * <p>(I would say that it can select the last of its type if it finds nothing else.  However, this algo should only
 * be used if an agent has more plans than maxPlansPerAgent, so make sure that that parameter is set large enough for
 * your purposes.  kai, oct'09)</p>
 * <p>Plans without a score are seen as worst and selected accordingly.</p>
 *
 * @author mrieser
 */
public class WorstPlanForRemovalSelector implements PlanSelector<Plan, Person> {

	private static final String UNDEFINED_TYPE = "undefined";

	@Override
	public Plan selectPlan(HasPlansAndId<Plan, Person> person) {

		// hashmap that returns "Integer" count for given plans type:
		Map<String, Integer> typeCounts = new ConcurrentHashMap<String, Integer>();

		// count how many plans per type an agent has:
		for (Plan plan : person.getPlans()) {
			String type = plan.getType();
			if ( type==null ) {
				type = UNDEFINED_TYPE ;
			}
			typeCounts.merge( type, 1, ( a, b ) -> a + b );
		}

		Plan worst = null;
		double worstScore = Double.POSITIVE_INFINITY;
		for (Plan plan : person.getPlans()) {

			String type = plan.getType();
			if ( type==null ) {
				type = UNDEFINED_TYPE;
			}
			if ( typeCounts.get( type ) > 1) {
				// (if we have more than one plan of the same type:)

				// if this plan has no score yet:
				if (plan.getScore() == null || plan.getScore().isNaN() ) {
					// say that the plan without score now is the "worst":
					worst = plan;

					// make sure that this one remains the selected plan:
					worstScore = Double.NEGATIVE_INFINITY;

				// otherwise do the usual logic to find the plan with the minimum score:
				} else if ( plan.getScore() < worstScore) {
					worst = plan;
					worstScore = plan.getScore();
				}
			}
			// (otherwise we just keep "worst=null")
			
		}

		if (worst == null) {
			// there is exactly one plan, or we have of each plan-type exactly one.
			// select the one with worst score globally, or the first one with score=null
			for (Plan plan : person.getPlans()) {
				if (plan.getScore() == null || plan.getScore().isNaN() ) {
					return plan;
				}
				if ( plan.getScore() < worstScore) {
					worst = plan;
					worstScore = plan.getScore();
				}
			}
		}
		return worst;
	}

}
