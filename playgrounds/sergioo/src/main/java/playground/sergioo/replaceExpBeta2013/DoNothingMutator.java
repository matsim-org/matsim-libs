/* *********************************************************************** *
 * project: org.matsim.*
 * TimeAllocationMutator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package playground.sergioo.replaceExpBeta2013;

import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.population.algorithms.PlanAlgorithm;

/**
 * Wraps the {@link org.matsim.population.algorithms.PlanMutateTimeAllocation}-
 * PlanAlgorithm into a {@link PlanStrategyModule} so it can be used for plans
 * replanning. Supports multiple threads.
 *
 * @author mrieser
 * @see org.matsim.population.algorithms.PlanMutateTimeAllocation
 */
public class DoNothingMutator extends AbstractMultithreadedModule {

	/**
	 * Creates a new TimeAllocationMutator with a mutation range as defined in
	 * the configuration (module "TimeAllocationMutator", param "mutationRange").
	 */
	public DoNothingMutator(Config config) {
		super(config.global());
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlanMutateDoNothing();
	}

}
