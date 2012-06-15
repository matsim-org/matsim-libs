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

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.ActivityDurationInterpretation;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.PlanMutateTimeAllocation;
import org.matsim.population.algorithms.PlanMutateTimeAllocationSimplified;

/**
 * Wraps the {@link org.matsim.population.algorithms.PlanMutateTimeAllocation}-
 * PlanAlgorithm into a {@link PlanStrategyModule} so it can be used for plans
 * replanning. Supports multiple threads.
 *
 * @author mrieser
 * @see org.matsim.population.algorithms.PlanMutateTimeAllocation
 */
public class TimeAllocationMutator extends AbstractMultithreadedModule {

//	public final static String CONFIG_GROUP = "TimeAllocationMutator";
//	public final static String CONFIG_MUTATION_RANGE = "mutationRange";

	private final static Logger log = Logger.getLogger(TimeAllocationMutator.class);

	private Double mutationRange = null;
//	private boolean useActivityDurations = true;
	private ActivityDurationInterpretation activityDurationInterpretation = ActivityDurationInterpretation.minOfDurationAndEndTime ;

	/**
	 * Creates a new TimeAllocationMutator with a mutation range as defined in
	 * the configuration (module "TimeAllocationMutator", param "mutationRange"),
	 * or the default value of 1800 (seconds) if there is no value specified in
	 * the configuration.
	 */
	public TimeAllocationMutator(Config config) {
		super(config.global());
//		String range = config.findParam(CONFIG_GROUP, CONFIG_MUTATION_RANGE);
//		if (range == null) {
//			log.info("No mutation range defined in the config file. Using default of " + mutationRange + " sec.");
//		} else {
//			this.mutationRange = Integer.parseInt(range);
//			log.info("mutation range = " + this.mutationRange);
//		}

		this.mutationRange = config.timeAllocationMutator().getMutationRange() ;
		if ( this.mutationRange==null ) {
			throw new RuntimeException("found mutationRange==null; don't know what that means; throwing exception ...") ;
		}

//		if ( config.vspExperimental().getActivityDurationInterpretation().equals( ActivityDurationInterpretation.minOfDurationAndEndTime) ) {
//			useActivityDurations = true ;
//		} else if ( config.vspExperimental().getActivityDurationInterpretation().equals( ActivityDurationInterpretation.endTimeOnly ) ) {
//			useActivityDurations = false ;
//		} else if ( config.vspExperimental().getActivityDurationInterpretation().equals( ActivityDurationInterpretation.tryEndTimeThenDuration ) ) {
//			throw new UnsupportedOperationException( "need to clarify the correct setting here.  Probably not a big deal, but not done yet.  kai, aug'10") ;
//		} else {
//			throw new IllegalStateException( "beahvior not defined for this configuration setting") ;
//		}
	}

	/**
	 * Creates a new TimeAllocationMutator with the specified mutation range.
	 *
	 * @param mutationRange
	 */
	public TimeAllocationMutator(Config config, final int mutationRange) {
		super(config.global());
		this.mutationRange = new Double(mutationRange);
	}
	public TimeAllocationMutator(Config config, final Double mutationRange) {
		super(config.global());
		this.mutationRange = mutationRange;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		PlanAlgorithm pmta ;
		switch( this.activityDurationInterpretation ) {
		case minOfDurationAndEndTime:
			pmta = new PlanMutateTimeAllocation(mutationRange, MatsimRandom.getLocalInstance());
			break ;
		default:
			pmta = new PlanMutateTimeAllocationSimplified(mutationRange, MatsimRandom.getLocalInstance());
		}
		return pmta;
	}

//	public void setUseActivityDurations(boolean useActivityDurations) {
//		this.useActivityDurations = useActivityDurations;
//	}

}
