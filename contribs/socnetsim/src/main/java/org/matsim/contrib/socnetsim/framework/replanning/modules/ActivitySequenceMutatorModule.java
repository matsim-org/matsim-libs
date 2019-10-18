/* *********************************************************************** *
 * project: org.matsim.*
 * ActivitySequenceMutatorModule.java
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
package org.matsim.contrib.socnetsim.framework.replanning.modules;

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripStructureUtils.StageActivityHandling;

/**
 * @author thibautd
 */
public class ActivitySequenceMutatorModule extends AbstractMultithreadedModule {

	public ActivitySequenceMutatorModule(
			final int numOfThreads) {
		super(numOfThreads);
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new ActivitySequenceMutatorAlgorithm(
				MatsimRandom.getLocalInstance(),
				StageActivityHandling.ExcludeStageActivities );
	}
}

