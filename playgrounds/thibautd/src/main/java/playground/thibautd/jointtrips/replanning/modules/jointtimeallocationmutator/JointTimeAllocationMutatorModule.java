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
package playground.thibautd.jointtrips.replanning.modules.jointtimeallocationmutator;

import org.apache.log4j.Logger;

import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.router.controler.MultiLegRoutingControler;
import playground.thibautd.router.replanning.TimeAllocationMutatorModule;

/**
 * @author thibautd
 */
public class JointTimeAllocationMutatorModule extends AbstractMultithreadedModule {
	private static final Logger log =
		Logger.getLogger(JointTimeAllocationMutatorModule.class);

	private final MultiLegRoutingControler controler;
	private final int mutationRange;

	public JointTimeAllocationMutatorModule(final Controler controler) {
		super( controler.getConfig().global() );
		this.controler = (MultiLegRoutingControler) controler;

		String range = controler.getConfig().findParam(
				TimeAllocationMutatorModule.CONFIG_GROUP,
				TimeAllocationMutatorModule.CONFIG_MUTATION_RANGE);
		if (range == null) {
			mutationRange = 1800;
			log.info("No mutation range defined in the config file. Using default of " + mutationRange + " sec.");
		}
		else {
			mutationRange = Integer.parseInt(range);
			log.info("mutation range = " + mutationRange);
		}
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new JointTimeAllocationMutatorAlgorithm(
				MatsimRandom.getLocalInstance(),
				controler.getTripRouterFactory().createTripRouter().getStageActivityTypes(),
				mutationRange);
	}
}

