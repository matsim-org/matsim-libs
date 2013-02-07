/* *********************************************************************** *
 * project: org.matsim.*
 * JointChooseModeForSubtourAlgorithm.java
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
package playground.thibautd.socnetsim.cliques.replanning.modules;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.socnetsim.population.JointPlan;

/**
 * @author thibautd
 */
public class PlanAlgorithmForRandomPlanRunner implements PlanAlgorithm {
	private final PlanAlgorithm delegate;
	private final Random random;

	public PlanAlgorithmForRandomPlanRunner(
			final PlanAlgorithm delegate,
			final Random random) {
		this.delegate = delegate;
		this.random = random;
	}

	@Override
	public void run(final Plan plan) {
		List<Plan> plans = new ArrayList<Plan>( ((JointPlan) plan).getIndividualPlans().values() );
		Plan individualPlanToMutate = plans.get( random.nextInt( plans.size() ) );
		delegate.run( individualPlanToMutate );
	}
}
