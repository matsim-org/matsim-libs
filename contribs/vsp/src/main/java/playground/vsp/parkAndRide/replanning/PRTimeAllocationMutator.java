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

package playground.vsp.parkAndRide.replanning;

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;

/**
 * Copy/Paste of TransitTimeAllocationMutator, that calls ParkAndRidePlanMutateTimeAllocation instead
 * of TransitPlanMutateTimeAllocation. Ignoring the activities "parkAndRide" and "pt interaction".
 *
 * @author ikaddoura
 */
public class PRTimeAllocationMutator extends AbstractMultithreadedModule {
	public final static String CONFIG_GROUP = "TimeAllocationMutator";
	public final static String CONFIG_MUTATION_RANGE = "mutationRange";

//	private final static Logger log = Logger.getLogger(TimeAllocationMutator.class);

	private Double mutationRange = 1800.;
	private boolean useActivityDurations = true;

	/**
	 * Creates a new TimeAllocationMutator with a mutation range as defined in
	 * the configuration (module "TimeAllocationMutator", param "mutationRange"),
	 * or the default value of 1800 (seconds) if there is no value specified in
	 * the configuration.
	 */
	public PRTimeAllocationMutator(Config config) {
		super(config.global());
		this.mutationRange = config.timeAllocationMutator().getMutationRange() ;
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

	/**
	 * Creates a new TimeAllocationMutator with the specified mutation range.
	 *
	 * @param mutationRange
	 */
	public PRTimeAllocationMutator(Config config, final int mutationRange) {
		super(config.global());
		this.mutationRange = new Double(mutationRange);
	}
	public PRTimeAllocationMutator(Config config, final Double mutationRange) {
		super(config.global());
		this.mutationRange = mutationRange ;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PRPlanMutateTimeAllocation pmta = new PRPlanMutateTimeAllocation(this.mutationRange, MatsimRandom.getLocalInstance());
		pmta.setUseActivityDurations(this.useActivityDurations);
		return pmta;
	}
}
