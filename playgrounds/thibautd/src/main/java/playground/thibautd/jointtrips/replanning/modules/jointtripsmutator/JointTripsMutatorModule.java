/* *********************************************************************** *
 * project: org.matsim.*
 * JointTripsMutatorModule.java
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
package playground.thibautd.jointtrips.replanning.modules.jointtripsmutator;

import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

import playground.thibautd.jointtrips.config.JointTripsMutatorConfigGroup;

/**
 * A module using instances of {@link JointTripsMutatorAlgorithm}.
 * @author thibautd
 */
public class JointTripsMutatorModule extends AbstractMultithreadedModule {
	private final Controler controler;

	/**
	 * @param config the config for the current run
	 */
	public JointTripsMutatorModule(
			final Controler controler) {
		super( controler.getConfig().global() );
		this.controler = controler;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new JointTripsMutatorAlgorithm( controler );
	}
}

