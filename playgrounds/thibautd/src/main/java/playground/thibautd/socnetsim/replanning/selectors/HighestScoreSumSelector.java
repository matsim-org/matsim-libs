/* *********************************************************************** *
 * project: org.matsim.*
 * AbstractHighestWeightSelectorTest.java
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
package playground.thibautd.socnetsim.replanning.selectors;

import org.matsim.api.core.v01.population.Plan;

import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.HighestWeightSelector;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.HighestWeightSelector.WeightCalculator;

/**
 * @author thibautd
 */
public class HighestScoreSumSelector implements GroupLevelPlanSelector {
	private final GroupLevelPlanSelector delegate;
	
	public HighestScoreSumSelector(
			final IncompatiblePlansIdentifierFactory fact) {
		delegate = new HighestWeightSelector( fact , new Weight() );
	}

	// for tests
	HighestScoreSumSelector(
			final IncompatiblePlansIdentifierFactory fact,
			final boolean blocking) {
		delegate = new HighestWeightSelector( blocking , fact , new Weight() );
	}

	private static class Weight implements WeightCalculator {
		@Override
		public double getWeight(
				final Plan indivPlan,
				final ReplanningGroup group) {
			Double score = indivPlan.getScore();
			// if there are unscored plan, one of them is selected
			return score == null ? Double.POSITIVE_INFINITY : score;
		}
	}

	@Override
	public GroupPlans selectPlans(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		return delegate.selectPlans( jointPlans, group );
	}
}
