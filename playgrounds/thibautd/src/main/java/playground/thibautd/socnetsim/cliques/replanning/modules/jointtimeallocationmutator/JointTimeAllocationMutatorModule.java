/* *********************************************************************** *
 * project: org.matsim.*
 * JointTimeAllocationMutatorModule.java
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
package playground.thibautd.socnetsim.cliques.replanning.modules.jointtimeallocationmutator;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.router.TripRouterFactory;

import playground.thibautd.socnetsim.population.JointPlan;
import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGenericStrategyModule;

/**
 * @author thibautd
 */
public class JointTimeAllocationMutatorModule extends AbstractMultithreadedGenericStrategyModule<JointPlan> {
	private final TripRouterFactory tripRouterFactory;
	private final double mutationRange;

	public JointTimeAllocationMutatorModule(final Controler controler) {
		this( controler.getConfig() , controler.getTripRouterFactory() );
	}

	public JointTimeAllocationMutatorModule(
			final Config config,
			final TripRouterFactory tripRouterFactory) {
		super( config.global() );
		this.tripRouterFactory = tripRouterFactory;
		this.mutationRange = config.timeAllocationMutator().getMutationRange();
	}

	@Override
	public GenericPlanAlgorithm<JointPlan> createAlgorithm() {
		return new JointTimeAllocationMutatorAlgorithm(
				MatsimRandom.getLocalInstance(),
				tripRouterFactory.instantiateAndConfigureTripRouter().getStageActivityTypes(),
				mutationRange);
	}
}

