/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.socnetsim.usage.replanning.removers;

import com.google.inject.Inject;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.contrib.socnetsim.framework.population.JointPlans;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.ReplanningGroup;
import org.matsim.contrib.socnetsim.framework.replanning.removers.AbstractDumbRemoverFactory;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.*;
import org.matsim.contrib.socnetsim.framework.replanning.selectors.highestweightselection.HighestWeightSelector;
import org.matsim.contrib.socnetsim.usage.replanning.GroupReplanningConfigGroup;

public class MinimumSumOfMinimumLossSelectorFactory extends AbstractDumbRemoverFactory {
	
	private final IncompatiblePlansIdentifierFactory incompatiblePlansIdentifierFactory;
	private final JointPlans jointPlans;

	@Inject
	public MinimumSumOfMinimumLossSelectorFactory(
			final Config conf,
			final JointPlans jointPlans,
			final IncompatiblePlansIdentifierFactory incompatiblePlansIdentifierFactory ) {
		super( getMaxPlansPerAgent( conf ) );
		this.jointPlans = jointPlans;
		this.incompatiblePlansIdentifierFactory = incompatiblePlansIdentifierFactory;
	}

	private static int getMaxPlansPerAgent(final Config conf) {
		final GroupReplanningConfigGroup group = (GroupReplanningConfigGroup) conf.getModule( GroupReplanningConfigGroup.GROUP_NAME );
		return group.getMaxPlansPerAgent();
	}

	@Override
	public GroupLevelPlanSelector createSelector() {
		final WeightCalculator baseWeight =
			new LowestScoreOfJointPlanWeight(
					new LossWeight(),
					jointPlans );
		return new HighestWeightSelector(
				true ,
				incompatiblePlansIdentifierFactory,
				new WeightCalculator() {
					@Override
					public double getWeight(
							final Plan indivPlan,
							final ReplanningGroup replanningGroup) {
						return -baseWeight.getWeight( indivPlan , replanningGroup );
					}
				});
	}
}
