/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripMutatorModule.java
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
package playground.thibautd.cliquessim.replanning.modules.jointtripmutator;

import org.matsim.core.controler.Controler;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.cliquessim.utils.JointControlerUtils;

/**
 * @author thibautd
 */
public class JointTripMutatorModule extends AbstractMultithreadedModule {
	private final Controler controler;

	public JointTripMutatorModule(final Controler controler) {
		super( controler.getConfig().global() );
		this.controler = controler;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new JointTripMutatorAlgorithm(
				controler.getScenario().getNetwork(),
				controler.getTripRouterFactory().createTripRouter(),
				JointControlerUtils.getJointTripPossibilities( controler.getScenario() ),
				MatsimRandom.getLocalInstance());
	}
}

