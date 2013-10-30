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

package playground.thibautd.socnetsim.replanning.removers;

import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.replanning.selectors.LowestScoreOfJointPlanWeight;
import playground.thibautd.socnetsim.replanning.selectors.WeightCalculator;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.HighestWeightSelector;

public class MinimumSumOfMinimumsSelectorFactory extends AbstractDumbRemoverFactory {
	@Override
	public GroupLevelPlanSelector createSelector(
			final ControllerRegistry controllerRegistry) {
		final WeightCalculator baseWeight =
			new LowestScoreOfJointPlanWeight(
					controllerRegistry.getJointPlans());
		return new HighestWeightSelector(
				true ,
				controllerRegistry.getIncompatiblePlansIdentifierFactory(),
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
