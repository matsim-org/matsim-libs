/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * GenericWorstPlanForRemovalSelector.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */

package org.matsim.core.replanning.selectors;

import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.HasPlansAndId;

/**
 * <p>Selects the worst plan of a person (most likely for removal).
 * <p>Plans without a score are seen as worst and selected accordingly.</p>
 *
 * @author mrieser
 */
public class GenericWorstPlanForRemovalSelector<T extends BasicPlan, I> implements PlanSelector<T, I> {

    private static final String UNDEFINED_TYPE = "undefined";

    @Override
    public T selectPlan(HasPlansAndId<T, I> person) {

        T worst = null;
        double worstScore = Double.POSITIVE_INFINITY;
        for (T plan : person.getPlans()) {

            // if this plan has no score yet:
            if (plan.getScore() == null || plan.getScore().isNaN() ) {
                // say that the plan without score now is the "worst":
                worst = plan;

                // make sure that this one remains the selected plan:
                worstScore = Double.NEGATIVE_INFINITY;

                // otherwise do the usual logic to find the plan with the minimum score:
            } else if (plan.getScore() < worstScore) {
                worst = plan;
                worstScore = plan.getScore();
            }
            // (otherwise we just keep "worst=null")

        }

        if (worst == null) {
            // there is exactly one plan, or we have of each plan-type exactly one.
            // select the one with worst score globally, or the first one with score=null
            for (T plan : person.getPlans()) {
                if (plan.getScore() == null || plan.getScore().isNaN() ) {
                    return plan;
                }
                if (plan.getScore() < worstScore) {
                    worst = plan;
                    worstScore = plan.getScore();
                }
            }
        }
        return worst;
    }

}
