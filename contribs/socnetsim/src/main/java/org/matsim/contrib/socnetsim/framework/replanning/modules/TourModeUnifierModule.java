/* *********************************************************************** *
 * project: org.matsim.*
 * TourModeUnifierModule.java
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

import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.MainModeIdentifier;
import org.matsim.core.router.StageActivityTypes;

import java.util.Set;

import org.matsim.contrib.socnetsim.framework.replanning.modules.TourModeUnifierAlgorithm.SubtourFirstModeIdentifier;
import org.matsim.contrib.socnetsim.framework.replanning.modules.TourModeUnifierAlgorithm.SubtourModeIdentifier;

/**
 * @author thibautd
 */
public class TourModeUnifierModule extends AbstractMultithreadedModule {
	final Set<String> stages; // formerly StageActivityTypes
	final SubtourModeIdentifier modeIdentifier;

	public TourModeUnifierModule(
			final int nThreads,
			final Set<String> stages,
			final MainModeIdentifier modeIdentifier) {
		this( nThreads,
				stages,
				new SubtourFirstModeIdentifier(
					modeIdentifier ) );
	}

	public TourModeUnifierModule(
			final int nThreads,
			final Set<String> stages,
			final SubtourModeIdentifier modeIdentifier) {
		super( nThreads );
		this.stages = stages;
		this.modeIdentifier = modeIdentifier;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new TourModeUnifierAlgorithm( stages , modeIdentifier );
	}
}

