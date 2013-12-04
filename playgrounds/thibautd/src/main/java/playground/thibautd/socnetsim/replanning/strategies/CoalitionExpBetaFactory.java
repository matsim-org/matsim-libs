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
package playground.thibautd.socnetsim.replanning.strategies;

import org.matsim.core.gbl.MatsimRandom;

import playground.thibautd.socnetsim.controller.ControllerRegistry;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategy;
import playground.thibautd.socnetsim.replanning.GroupPlanStrategyFactory;
import playground.thibautd.socnetsim.replanning.selectors.coalitionselector.CoalitionSelector;
import playground.thibautd.socnetsim.replanning.selectors.coalitionselector.CoalitionSelector.ConflictSolver;
import playground.thibautd.socnetsim.replanning.selectors.LogitWeight;

/**
 * @author thibautd
 */
public class CoalitionExpBetaFactory implements GroupPlanStrategyFactory {
	private final ConflictSolver conflictSolver;

	public CoalitionExpBetaFactory(
			final ConflictSolver conflictSolver) {
		this.conflictSolver = conflictSolver;
	}

	@Override
	public GroupPlanStrategy createStrategy(
			final ControllerRegistry registry) {
		return new GroupPlanStrategy(
				new CoalitionSelector(
					new LogitWeight(
						MatsimRandom.getLocalInstance(),
						registry.getScenario().getConfig().planCalcScore().getBrainExpBeta()),
					conflictSolver) );
	}
}

