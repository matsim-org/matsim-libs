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

import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;

/**
 * @author thibautd
 */
public class HighestScoreSumSelector extends AbstractHighestWeightSelector {
	public HighestScoreSumSelector() {}

	// for tests
	HighestScoreSumSelector(final boolean blocking) {
		super( blocking );
	}

	HighestScoreSumSelector(final boolean blocking , final boolean exploreAll) {
		super( blocking , exploreAll );
	}

	HighestScoreSumSelector(
			final boolean blocking,
			final boolean exploreAll,
			final boolean pruneUnplausiblePlans) {
		super( blocking , exploreAll , pruneUnplausiblePlans );
	}

	@Override
	public double getWeight(
			final Plan indivPlan,
			final ReplanningGroup group) {
		Double score = indivPlan.getScore();
		// if there are unscored plan, one of them is selected
		return score == null ? Double.POSITIVE_INFINITY : score;
	}
}
