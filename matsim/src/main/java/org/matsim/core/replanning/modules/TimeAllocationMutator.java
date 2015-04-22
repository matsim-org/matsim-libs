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

package org.matsim.core.replanning.modules;

import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.PlanMutateTimeAllocationSimplified;
import org.matsim.population.algorithms.TripPlanMutateTimeAllocation;

/**
 * Wraps the {@link org.matsim.population.algorithms.PlanMutateTimeAllocation}-
 * PlanAlgorithm into a {@link PlanStrategyModule} so it can be used for plans
 * replanning. Supports multiple threads.
 *
 * @author mrieser
 * @see org.matsim.population.algorithms.PlanMutateTimeAllocation
 */
public class TimeAllocationMutator extends AbstractMultithreadedModule {

	private final double mutationRange;
	private final boolean affectingDuration ;
	private final PlansConfigGroup.ActivityDurationInterpretation activityDurationInterpretation;

	/**
	 * Creates a new TimeAllocationMutator with a mutation range as defined in
	 * the configuration (module "TimeAllocationMutator", param "mutationRange").
	 */
	public TimeAllocationMutator(Config config) {
		super(config.global());
		this.mutationRange = config.timeAllocationMutator().getMutationRange();
		this.affectingDuration = config.timeAllocationMutator().isAffectingDuration() ;
		this.activityDurationInterpretation = (config.plans().getActivityDurationInterpretation());
	}

//	public TimeAllocationMutator(Config config, final int mutationRange, boolean affectingDuration) {
//		super(config.global());
//		this.mutationRange = mutationRange;
//		this.affectingDuration = affectingDuration ;
//		this.activityDurationInterpretation = config.vspExperimental().getActivityDurationInterpretation();
//	}
	
	public TimeAllocationMutator(Config config, final double mutationRange, boolean affectingDuration) {
		super(config.global());
		this.affectingDuration = affectingDuration ;
		this.mutationRange = mutationRange;
		this.activityDurationInterpretation = (config.plans().getActivityDurationInterpretation());
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlanAlgorithm pmta;
		switch (this.activityDurationInterpretation) {
		case minOfDurationAndEndTime:
			pmta = new TripPlanMutateTimeAllocation(
					getReplanningContext().getTripRouter().getStageActivityTypes(), 
					mutationRange, 
					affectingDuration, MatsimRandom.getLocalInstance());
			break;
		default:
			pmta = new PlanMutateTimeAllocationSimplified(
					getReplanningContext().getTripRouter().getStageActivityTypes(),
					mutationRange,
					affectingDuration, MatsimRandom.getLocalInstance());
		}
		return pmta;
	}

}
