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
package playground.thibautd.socnetsim.framework.replanning.selectors;


import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.framework.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.framework.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.framework.replanning.selectors.highestweightselection.HighestWeightSelector;

/**
 * @author thibautd
 */
public class HighestScoreSumSelector implements GroupLevelPlanSelector {
	private final GroupLevelPlanSelector delegate;
	
	public HighestScoreSumSelector(
			final IncompatiblePlansIdentifierFactory fact) {
		delegate = new HighestWeightSelector( fact , new ScoreWeight() );
	}

	// for tests
	HighestScoreSumSelector(
			final IncompatiblePlansIdentifierFactory fact,
			final boolean blocking) {
		delegate = new HighestWeightSelector( blocking , fact , new ScoreWeight() );
	}

	@Override
	public GroupPlans selectPlans(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		return delegate.selectPlans( jointPlans, group );
	}
}
