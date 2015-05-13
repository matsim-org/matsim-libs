/* *********************************************************************** *
 * project: org.matsim.*
 * LogitSumSelector.java
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

import java.util.Random;

import org.apache.log4j.Logger;

import playground.thibautd.socnetsim.framework.population.JointPlans;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.grouping.ReplanningGroup;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.HighestWeightSelector;

/**
 * A selector which draws gumbel-distributed scores for individual plans.
 * Without joint plan, this results in logit marginals for the selection for
 * each agent.
 *
 * @author thibautd
 */
public class LogitSumSelector implements GroupLevelPlanSelector {
	static final Logger log =
		Logger.getLogger(LogitSumSelector.class);

	private final GroupLevelPlanSelector delegate;

	public LogitSumSelector(
			final Random random,
			final IncompatiblePlansIdentifierFactory incompFact,
			final double scaleParameter) {
		this.delegate = new HighestWeightSelector( incompFact , new LogitWeight( random , scaleParameter ) );
	}
	
	@Override
	public GroupPlans selectPlans(
			final JointPlans jointPlans,
			final ReplanningGroup group) {
		return delegate.selectPlans( jointPlans , group);
	}

}

