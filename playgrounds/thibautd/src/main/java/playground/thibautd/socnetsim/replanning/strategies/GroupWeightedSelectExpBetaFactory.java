/* *********************************************************************** *
 * project: org.matsim.*
 * GroupWeightedSelectExpBetaFactory.java
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

import playground.thibautd.socnetsim.replanning.GroupReplanningConfigGroup;
import playground.thibautd.socnetsim.framework.replanning.NonInnovativeStrategyFactory;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.replanning.selectors.IncompatiblePlansIdentifierFactory;
import playground.thibautd.socnetsim.replanning.selectors.LogitWeight;
import playground.thibautd.socnetsim.replanning.selectors.WeightedWeight;
import playground.thibautd.socnetsim.replanning.selectors.highestweightselection.HighestWeightSelector;

/**
 * @author thibautd
 */
public class GroupWeightedSelectExpBetaFactory extends NonInnovativeStrategyFactory {

	private final IncompatiblePlansIdentifierFactory incompatiblePlansIdentifierFactory;
	private final Scenario sc;

	@Inject
	public GroupWeightedSelectExpBetaFactory( IncompatiblePlansIdentifierFactory incompatiblePlansIdentifierFactory , Scenario sc ) {
		this.incompatiblePlansIdentifierFactory = incompatiblePlansIdentifierFactory;
		this.sc = sc;
	}

	@Override
	public GroupLevelPlanSelector createSelector() {
		final GroupReplanningConfigGroup configGroup = (GroupReplanningConfigGroup)
				sc.getConfig().getModule(
						GroupReplanningConfigGroup.GROUP_NAME );

		return 
				 new HighestWeightSelector(
					 incompatiblePlansIdentifierFactory ,
					 new WeightedWeight(
						 new LogitWeight(
							MatsimRandom.getLocalInstance(),
							sc.getConfig().planCalcScore().getBrainExpBeta()),
						 configGroup.getWeightAttributeName(),
						 sc.getPopulation().getPersonAttributes() ) );

	}

}

