/* *********************************************************************** *
 * project: org.matsim.*
 * CoalitionExpBetaFactory.java
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
package playground.thibautd.socnetsim.framework.replanning.strategies;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.gbl.MatsimRandom;

import com.google.inject.Inject;

import playground.thibautd.socnetsim.framework.replanning.NonInnovativeStrategyFactory;
import playground.thibautd.socnetsim.framework.replanning.selectors.GroupLevelPlanSelector;
import playground.thibautd.socnetsim.framework.replanning.selectors.LogitWeight;
import playground.thibautd.socnetsim.framework.replanning.selectors.coalitionselector.CoalitionSelector;
import playground.thibautd.socnetsim.framework.replanning.selectors.coalitionselector.CoalitionSelector.ConflictSolver;

/**
 * @author thibautd
 */
public class CoalitionExpBetaFactory extends NonInnovativeStrategyFactory  {
	private final ConflictSolver conflictSolver;

	@Inject
	private Scenario sc = null;

	public CoalitionExpBetaFactory(
			final ConflictSolver conflictSolver) {
		this.conflictSolver = conflictSolver;
	}

	@Override
	public GroupLevelPlanSelector createSelector() {
		return new CoalitionSelector(
				new LogitWeight(
					MatsimRandom.getLocalInstance(),
					sc.getConfig().planCalcScore().getBrainExpBeta()),
				conflictSolver);
	}
}

