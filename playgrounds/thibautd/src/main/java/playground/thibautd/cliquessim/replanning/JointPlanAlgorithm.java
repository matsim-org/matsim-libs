/* *********************************************************************** *
 * project: org.matsim.*
 * JointPlanAlgorithm.java
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
package playground.thibautd.cliquessim.replanning;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.socnetsim.population.JointPlan;

/**
 * Simple abstract class to extend to provide PlanAlgorithms for
 * cliques.
 *
 * @author thibautd
 */
public abstract class JointPlanAlgorithm implements PlanAlgorithm {

	/**
	 * Checks if the plan is a joint plan, and passes it to run( JointPlan ).
	 *
	 * @throws IllegalArgumentException if the argument is not a {@link JointPlan}
	 */
	@Override
	public final void run(final Plan plan) {
		if (plan instanceof JointPlan) {
			run((JointPlan) plan);
		} else {
			throw new IllegalArgumentException(getClass().getSimpleName()+"launched with"+
					"a non-joint plan");
		}
	}

	/**
	 * executes the algorithm.
	 *
	 * @param plan
	 */
	public abstract void run(final JointPlan plan);

}

