/* *********************************************************************** *
 * project: org.matsim.*
 * ExecuteModuleOnAllPlansModule.java
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
package playground.thibautd.socnetsim.cliques.replanning.modules;

import org.matsim.core.config.Config;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * @author thibautd
 */
public class ExecuteModuleOnAllPlansModule extends AbstractMultithreadedModule {
	private final AbstractMultithreadedModule delegate;

	public ExecuteModuleOnAllPlansModule(
			final Config config,
			final AbstractMultithreadedModule delegate) {
		this( config.global().getNumberOfThreads() , delegate );
	}

	public ExecuteModuleOnAllPlansModule(
			final int nThreads,
			final AbstractMultithreadedModule delegate) {
		super( nThreads );
		this.delegate = delegate;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlanAlgorithmForAllPlansRunner( delegate.getPlanAlgoInstance() );
	}
}

