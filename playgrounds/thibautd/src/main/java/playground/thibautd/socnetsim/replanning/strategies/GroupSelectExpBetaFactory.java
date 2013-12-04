/* *********************************************************************** *
 * project: org.matsim.*
 * GroupSelectExpBetaFactory.java
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

import org.matsim.core.gbl.MatsimRandom;

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.replanning.NonInnovativeStrategyFactory;
import playground.thibautd.socnetsim.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.replanning.selectors.LogitSumSelector;

/**
 * @author thibautd
 */
public class GroupSelectExpBetaFactory extends NonInnovativeStrategyFactory {

	@Override
	public GroupLevelPlanSelector createSelector(
			final ControllerRegistry registry) {
		return new LogitSumSelector(
			MatsimRandom.getLocalInstance(),
			registry.getIncompatiblePlansIdentifierFactory(),
			registry.getScenario().getConfig().planCalcScore().getBrainExpBeta());
	}
}

