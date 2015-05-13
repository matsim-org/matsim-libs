/* *********************************************************************** *
 * project: org.matsim.*
 * SynchronizeCoTravelerPlansModule.java
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
package playground.thibautd.socnetsim.jointtrips.replanning.modules;

import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.StageActivityTypes;

import playground.thibautd.socnetsim.framework.population.JointPlan;
import playground.thibautd.socnetsim.framework.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.framework.replanning.modules.AbstractMultithreadedGenericStrategyModule;

/**
 * @author thibautd
 */
public class SynchronizeCoTravelerPlansModule extends AbstractMultithreadedGenericStrategyModule<JointPlan> {
	private final StageActivityTypes checker;

	public SynchronizeCoTravelerPlansModule(
			final int nThreads,
			final StageActivityTypes checker) {
		super( nThreads );
		this.checker = checker;
	}

	@Override
	public GenericPlanAlgorithm<JointPlan> createAlgorithm(ReplanningContext replanningContext) {
		return new SynchronizeCoTravelerPlansAlgorithm( checker );
	}
}

