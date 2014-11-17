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

package playground.sergioo.ptsim2013.replanning;

import java.util.Map;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.gbl.MatsimRandom;
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
public class ArrivalTimeToStopMutator extends AbstractMultithreadedModule {

	private final double mutationRange;
	private final Map<Id<Person>, Double> originalTimes;

	/**
	 * Creates a new TimeAllocationMutator with a mutation range as defined in
	 * the configuration (module "TimeAllocationMutator", param "mutationRange").
	 */
	public ArrivalTimeToStopMutator(Config config, Map<Id<Person>, Double> originalTimes) {
		super(config.global());
		this.mutationRange = config.timeAllocationMutator().getMutationRange();
		this.originalTimes = originalTimes;
	}

	public ArrivalTimeToStopMutator(Config config, final int mutationRange, Map<Id<Person>, Double> originalTimes) {
		super(config.global());
		this.mutationRange = mutationRange;
		this.originalTimes = originalTimes;
	}
	
	public ArrivalTimeToStopMutator(Config config, final double mutationRange, Map<Id<Person>, Double> originalTimes) {
		super(config.global());
		this.mutationRange = mutationRange;
		this.originalTimes = originalTimes;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PlanMutateEndFirstActivity(originalTimes, mutationRange, MatsimRandom.getLocalInstance());
	}

}
