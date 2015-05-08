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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Plan;

import com.google.inject.Inject;

import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.replanning.selectors.IncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.replanning.selectors.LossWeight;
import playground.thibautd.socnetsim.replanning.selectors.LowestScoreOfJointPlanWeight;
import playground.thibautd.socnetsim.replanning.selectors.WeightCalculator;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.HighestWeightSelector;

public class MinimumSumOfMinimumLossSelectorFactory extends AbstractDumbRemoverFactory {
	
	private final IncompatiblePlansIdentifierFactory incompatiblePlansIdentifierFactory;
	private final Scenario sc;

	@Inject
	public MinimumSumOfMinimumLossSelectorFactory( Scenario sc , IncompatiblePlansIdentifierFactory incompatiblePlansIdentifierFactory ) {
		super( sc );
		this.sc = sc;
		this.incompatiblePlansIdentifierFactory = incompatiblePlansIdentifierFactory;
	}

	@Override
	public GroupLevelPlanSelector createSelector() {
		final WeightCalculator baseWeight =
			new LowestScoreOfJointPlanWeight(
					new LossWeight(),
					(JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME ));
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
