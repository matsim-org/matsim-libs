/* *********************************************************************** *
 * project: org.matsim.*
 * JointChooseModeForSubtourModule.java
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
package playground.thibautd.cliquessim.replanning.modules.jointchoosemodeforsubtour;

import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.replanning.modules.SubtourModeChoice;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.cliquessim.replanning.modules.PlanAlgorithmForRandomPlanRunner;

/**
 * @author thibautd
 */
public class JointChooseModeForSubtourModule extends AbstractMultithreadedModule {
	private final AbstractMultithreadedModule delegateFactory;

	public JointChooseModeForSubtourModule(
			final Config config) {
		super( config.global() );
		delegateFactory = new SubtourModeChoice( config );
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlanAlgorithmForRandomPlanRunner(
				delegateFactory.getPlanAlgoInstance(),
				MatsimRandom.getLocalInstance());
	}
}

