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
package playground.thibautd.socnetsim.framework.replanning.modules;

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author thibautd
 */
public class ActivitySequenceMutatorModule extends AbstractMultithreadedModule {
	private final StageActivityTypes additionalBlackList;


	public ActivitySequenceMutatorModule(
			final int numOfThreads ) {
		this( numOfThreads , null );
	}

	public ActivitySequenceMutatorModule(
			final int numOfThreads,
			final StageActivityTypes additionalBlackList) {
		super(numOfThreads);
		this.additionalBlackList = additionalBlackList;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		final CompositeStageActivityTypes actualBlackList = new CompositeStageActivityTypes();
		actualBlackList.addActivityTypes(
				getReplanningContext().getTripRouter().getStageActivityTypes() );

		if ( additionalBlackList != null ) {
			actualBlackList.addActivityTypes(
					additionalBlackList );
		}

		return new ActivitySequenceMutatorAlgorithm(
				MatsimRandom.getLocalInstance(),
				actualBlackList );
	}
}

