/* *********************************************************************** *
 * project: org.matsim.*
 * TransitTimeAllocationMutator.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

import com.google.inject.Inject;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.algorithms.TripPlanMutateTimeAllocation;
import org.matsim.core.router.TripRouter;

import javax.inject.Provider;

/**
 * Copy/Paste of TimeAllocationMutator, that calls TransitPlanMutateTimeAllocation instead
 * of PlanMutateTimeAllocation.
 *
 * @author mrieser
 */
public class TripTimeAllocationMutator extends AbstractMultithreadedModule {

	private final Provider<TripRouter> tripRouterProvider;

	public final static String CONFIG_GROUP = "TimeAllocationMutator";
	public final static String CONFIG_MUTATION_RANGE = "mutationRange";

	private double mutationRange = 1800.0;
	private boolean useActivityDurations = true;
	private final boolean affectingDuration;

	/**
	 * Creates a new TimeAllocationMutator with a mutation range as defined in
	 * the configuration (module "TimeAllocationMutator", param "mutationRange").
	 */
	public TripTimeAllocationMutator(Config config, Provider<TripRouter> tripRouterProvider) {
		super(config.global());
		this.tripRouterProvider = tripRouterProvider;
		this.mutationRange = config.timeAllocationMutator().getMutationRange() ;
		this.affectingDuration = config.timeAllocationMutator().isAffectingDuration() ;
		PlansConfigGroup.ActivityDurationInterpretation actDurInterpr = ( config.plans().getActivityDurationInterpretation() ) ;
		if ( actDurInterpr == PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime ) {
			useActivityDurations = true ;
		} else if ( actDurInterpr == PlansConfigGroup.ActivityDurationInterpretation.endTimeOnly ) {
			useActivityDurations = false ;
		} else if ( actDurInterpr == PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration ) {
			throw new UnsupportedOperationException( "need to clarify the correct setting here.  Probably not a big deal, but not done yet.  kai, aug'10") ;
		} else {
			throw new IllegalStateException( "beahvior not defined for this configuration setting") ;
		}
	}

	public TripTimeAllocationMutator(Config config, Provider<TripRouter> tripRouterProvider, final double mutationRange, boolean affectingDuration) {
		super(config.global());
		this.tripRouterProvider = tripRouterProvider;
		this.mutationRange = mutationRange;
		this.affectingDuration = affectingDuration;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		TripPlanMutateTimeAllocation pmta =
			new TripPlanMutateTimeAllocation(
					tripRouterProvider.get().getStageActivityTypes(),
					this.mutationRange,
					affectingDuration, MatsimRandom.getLocalInstance());
		pmta.setUseActivityDurations(this.useActivityDurations);
		return pmta;
	}

}
