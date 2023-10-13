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

package org.matsim.core.replanning.strategies;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.TimeAllocationMutatorConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.algorithms.MutateActivityTimeAllocation;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;

/**
 * Wraps the {@link org.matsim.core.population.algorithms.TripPlanMutateTimeAllocation}-
 * PlanAlgorithm into a {@link PlanStrategyModule} so it can be used for plans
 * replanning. Supports multiple threads.
 *
 * @author mrieser
 * @see org.matsim.core.population.algorithms.TripPlanMutateTimeAllocation
 */
class TimeAllocationMutatorModule extends AbstractMultithreadedModule{


	private static final Logger log = LogManager.getLogger( TimeAllocationMutatorModule.class );
	private final double mutationRange;
	private final boolean affectingDuration;
	private final TimeAllocationMutatorConfigGroup timeAllocationMutatorConfigGroup;


	TimeAllocationMutatorModule( TimeAllocationMutatorConfigGroup timeAllocationMutatorConfigGroup, GlobalConfigGroup globalConfigGroup) {
		super(globalConfigGroup);
		this.mutationRange = timeAllocationMutatorConfigGroup.getMutationRange();
		this.affectingDuration = timeAllocationMutatorConfigGroup.isAffectingDuration();
		this.timeAllocationMutatorConfigGroup = timeAllocationMutatorConfigGroup;

	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlanAlgorithm pmta = new MutateActivityTimeAllocation
				(this.mutationRange, this.affectingDuration, MatsimRandom.getLocalInstance(),
						timeAllocationMutatorConfigGroup.getLatestActivityEndTime(), timeAllocationMutatorConfigGroup.isMutateAroundInitialEndTimeOnly(),
						timeAllocationMutatorConfigGroup.getMutationRangeStep());
		return pmta;
	}
}
