/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * PlanStrategies.java
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

package org.matsim.core.replanning;

import jakarta.annotation.Nullable;

import org.matsim.api.core.v01.population.BasicPlan;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;

public final class ReplanningUtils {

	static public final String INITIAl_PLAN_ATTRIBUTE = "isInitialPlan";

	public static boolean isInitialPlan(Plan plan) {
		Boolean isInitialPlan = (Boolean) plan.getAttributes().getAttribute(INITIAl_PLAN_ATTRIBUTE);
		return isInitialPlan != null && isInitialPlan;
	}

	@Nullable
	public static Plan getInitialPlan(Person person) {
		for (Plan plan : person.getPlans()) {
			if (isInitialPlan(plan)) {
				return plan;
			}
		}

		return null;
	}

	public static void setInitialPlan(Person person) {
		person.getPlans().forEach(plan -> plan.getAttributes().removeAttribute(INITIAl_PLAN_ATTRIBUTE));
		person.getSelectedPlan().getAttributes().putAttribute(INITIAl_PLAN_ATTRIBUTE, true);
	}

	/**
	 * Return whether a strategy is innovative, i.e. is producing new plans.
	 */
	public static <P extends BasicPlan, R> boolean isInnovativeStrategy(GenericPlanStrategy<P, R> planStrategy) {
		return !isOnlySelector(planStrategy);
	}

	public static <P extends BasicPlan, R> boolean isOnlySelector(GenericPlanStrategy<P, R> planStrategy) {
		if (planStrategy instanceof PlanStrategyImpl) {
			return ((PlanStrategyImpl) planStrategy).getNumberOfStrategyModules() == 0;
		}
		return false;
	}

}
