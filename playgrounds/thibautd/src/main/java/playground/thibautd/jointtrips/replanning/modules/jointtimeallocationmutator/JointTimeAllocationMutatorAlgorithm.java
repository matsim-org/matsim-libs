/* *********************************************************************** *
 * project: org.matsim.*
 * JointTimeAllocationMutatorAlgorithm.java
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
package playground.thibautd.jointtrips.replanning.modules.jointtimeallocationmutator;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Random;

import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.StageActivityTypesImpl;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.jointtrips.population.JointActingTypes;
import playground.thibautd.jointtrips.population.JointPlan;
import playground.thibautd.router.replanning.BlackListedTimeAllocationMutator;

/**
 * Executes a time allocation mutator on each individual plan.
 * @author thibautd
 */
public class JointTimeAllocationMutatorAlgorithm implements PlanAlgorithm {
	private final PlanAlgorithm individualMutator;
	private final Random random;

	public JointTimeAllocationMutatorAlgorithm(
			final Random random,
			final StageActivityTypes routerStageTypes,
			final double mutationRange) {
		this.random = random;
		CompositeStageActivityTypes blackList = new CompositeStageActivityTypes();
		blackList.addActivityTypes( routerStageTypes );
		blackList.addActivityTypes(
			new StageActivityTypesImpl(
					Arrays.asList(
						JointActingTypes.PICK_UP,
						JointActingTypes.DROP_OFF ) ) );
		this.individualMutator =
			new BlackListedTimeAllocationMutator(
					blackList,
					mutationRange,
					random);
	}

	@Override
	public void run(final Plan plan) {
		List<Plan> plans = new ArrayList<Plan>( ((JointPlan) plan).getIndividualPlans().values() );
		Plan individualPlanToMutate = plans.get( random.nextInt( plans.size() ) );
		individualMutator.run( individualPlanToMutate );
	}
}
