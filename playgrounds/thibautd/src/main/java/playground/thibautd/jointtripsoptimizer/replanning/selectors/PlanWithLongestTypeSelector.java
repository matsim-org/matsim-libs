/* *********************************************************************** *
 * project: org.matsim.*
 * PlanWithLongestTypeSelector.java
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
package playground.thibautd.jointtripsoptimizer.replanning.selectors;

import java.util.Collections;
import java.util.Comparator;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.selectors.PlanSelector;

import playground.thibautd.jointtripsoptimizer.population.Clique;
import playground.thibautd.jointtripsoptimizer.population.JointPlan;

/**
 * (very) quick implementation of a plan selector aiming at selecting
 * always the most general plan for joint trip replanning.
 *
 * What it really does (as says its name) is to select (one of) the plan(s)
 * with the longest type.
 *
 * This will have the desired effect only in the case where there is only one
 * "father" plan (which is currently the case, but may change).
 *
 * @author thibautd
 */
public class PlanWithLongestTypeSelector implements PlanSelector {

	private final TypeComp comparator = new TypeComp();

	@Override
	public Plan selectPlan(Person person) {

		if (person instanceof Clique) {
			return selectPlan((Clique) person);
		} else {
			throw new IllegalArgumentException("longestJointPlanForRemoval used "+
					"for a non clique agent");
		}
	}

	public Plan selectPlan(Clique clique) {
		return Collections.max(clique.getPlans(), comparator);
	}

	private class TypeComp implements Comparator<Plan> {

		@Override
		public int compare(final Plan arg0, final Plan arg1) {
			return ((JointPlan) arg0).getType().length() -
					((JointPlan) arg1).getType().length();
		}
	}
}

