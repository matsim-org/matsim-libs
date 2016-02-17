/* *********************************************************************** *
 * project: org.matsim.*
 * TimeAllocationMutatorModule.java
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
package playground.ivt.replanning;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.PlanMutateTimeAllocationSimplified;

/**
 * @author thibautd
 */
public class BlackListedTimeAllocationMutatorModule extends AbstractMultithreadedModule {
	private final StageActivityTypes blackList;

	private final double mutationRange;
	private final boolean useActivityDurations;

	/**
	 * @param blackList the {@link StageActivityTypes}. If null, an instance is obtained
	 * from the {@link TripRouter} created by the controler's factory when needed.
	 */
	public BlackListedTimeAllocationMutatorModule( final Config config , final StageActivityTypes blackList ) {
		super( config.global() );
		this.blackList = blackList;

		this.mutationRange = config.timeAllocationMutator().getMutationRange();

		PlansConfigGroup.ActivityDurationInterpretation actDurInterpr = ( config.plans().getActivityDurationInterpretation() ) ;
		if ( actDurInterpr == PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime ) {
			useActivityDurations = true ;
		} else if ( actDurInterpr == PlansConfigGroup.ActivityDurationInterpretation.endTimeOnly ) {
			useActivityDurations = false ;
		} else if ( actDurInterpr == PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration ) {
			useActivityDurations = false ;
		} else {
			throw new IllegalStateException( "beahvior not defined for this configuration setting") ;
		}
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlanAlgorithm mutator =
			new PlanMutateTimeAllocationSimplified(
					blackList,
					this.mutationRange,
					useActivityDurations,
					MatsimRandom.getLocalInstance());
		return mutator;
	}
}
