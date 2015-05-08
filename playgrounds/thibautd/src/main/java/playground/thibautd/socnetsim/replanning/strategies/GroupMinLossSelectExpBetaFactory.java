/* *********************************************************************** *
 * project: org.matsim.*
 * GroupMinLossSelectExpBetaFactory.java
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
package playground.thibautd.socnetsim.replanning.strategies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;

import com.google.inject.Inject;

import playground.thibautd.socnetsim.population.JointPlans;
import playground.thibautd.socnetsim.replanning.NonInnovativeStrategyFactory;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.replanning.selectors.IncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.replanning.selectors.LogitWeight;
import playground.thibautd.socnetsim.replanning.selectors.LossWeight;
import playground.thibautd.socnetsim.replanning.selectors.LowestScoreOfJointPlanWeight;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.HighestWeightSelector;

/**
 * @author thibautd
 */
public class GroupMinLossSelectExpBetaFactory extends NonInnovativeStrategyFactory {

	private final Scenario sc;
	private final IncompatiblePlansIdentifierFactory incompatiblePlans;
	
	@Inject
	public GroupMinLossSelectExpBetaFactory( Scenario sc , IncompatiblePlansIdentifierFactory incompatiblePlans ) {
		this.sc = sc;
		this.incompatiblePlans = incompatiblePlans;
	}


	@Override
	public GroupLevelPlanSelector createSelector() {
		return new HighestWeightSelector(
			 incompatiblePlans ,
			 new LogitWeight(
				new LowestScoreOfJointPlanWeight(
					new LossWeight(),
					(JointPlans) sc.getScenarioElement( JointPlans.ELEMENT_NAME ) ),
				MatsimRandom.getLocalInstance(),
				sc.getConfig().planCalcScore().getBrainExpBeta()) );
	}
}

