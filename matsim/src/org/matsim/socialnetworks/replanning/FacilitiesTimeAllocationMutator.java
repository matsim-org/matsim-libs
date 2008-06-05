/* *********************************************************************** *
 * project: org.matsim.*
 * public class FacilitiesTimeAllocationMutator {.java
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

package org.matsim.socialnetworks.replanning;

import org.apache.log4j.Logger;
import org.matsim.gbl.Gbl;
import org.matsim.plans.algorithms.PlanAlgorithmI;
import org.matsim.replanning.modules.MultithreadedModuleA;
import org.matsim.replanning.modules.StrategyModuleI;

/**
 * Wraps the {@link org.matsim.plans.algorithms.PlanMutateTimeAllocation}-
 * PlanAlgorithm into a {@link StrategyModuleI} so it can be used for plans
 * replanning. Supports multiple threads.
 *
 * @author mrieser
 * @see org.matsim.plans.algorithms.PlanMutateTimeAllocation
 */
public class FacilitiesTimeAllocationMutator  extends MultithreadedModuleA {

	public final static String CONFIG_GROUP = "public class FacilitiesTimeAllocationMutator {";
	public final static String CONFIG_MUTATION_RANGE = "mutationRange";
	
	private final static Logger log = Logger.getLogger(FacilitiesTimeAllocationMutator.class);

	private int mutationRange = 1800;

	/**
	 * Creates a new TimeAllocationMutator with a mutation range as defined in 
	 * the configuration (module "TimeAllocationMutator", param "mutationRange"),
	 * or the default value of 1800 (seconds) if there is no value specified in 
	 * the configuration.
	 */
	public FacilitiesTimeAllocationMutator () {
		String range = Gbl.getConfig().findParam(CONFIG_GROUP, CONFIG_MUTATION_RANGE);
		if (range == null) {
			log.info("No mutation range defined in the config file. Using default of " + mutationRange + " sec.");			
		} else {
			this.mutationRange = Integer.parseInt(range);
			log.info("mutation range = " + this.mutationRange);
		}
	}

	/**
	 * Creates a new TimeAllocationMutator with the specified mutation range.
	 * 
	 * @param mutationRange
	 */
	public FacilitiesTimeAllocationMutator(final int mutationRange) {
		this.mutationRange = mutationRange;
	}

	@Override
	public PlanAlgorithmI getPlanAlgoInstance() {
//		return new PlanMutateTimeAllocation(mutationRange);
		return new FacilitiesPlanMutateTimeAllocation(mutationRange);
	}

}

