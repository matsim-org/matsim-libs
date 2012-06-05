/* *********************************************************************** *
 * project: org.matsim.*
 * PlanAlgorithmForAllPlansRunner.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.thibautd.jointtrips.replanning.modules;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.jointtrips.population.JointPlan;

/**
 * @author thibautd
 */
public class PlanAlgorithmForAllPlansRunner implements PlanAlgorithm {
	private final PlanAlgorithm delegate;

	public PlanAlgorithmForAllPlansRunner(
			final PlanAlgorithm delegate) {
		this.delegate = delegate;
	}

	@Override
	public void run(final Plan plan) {
		for (Plan individualPlanToMutate : ((JointPlan) plan).getIndividualPlans().values() ) {
			delegate.run( individualPlanToMutate );
		}
	}
}
