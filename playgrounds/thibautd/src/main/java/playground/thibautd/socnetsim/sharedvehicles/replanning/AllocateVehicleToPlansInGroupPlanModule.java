/* *********************************************************************** *
 * project: org.matsim.*
 * AllocateVehicleToPlansInGroupPlanModule.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package playground.thibautd.socnetsim.sharedvehicles.replanning;

import org.matsim.core.gbl.MatsimRandom;

import playground.thibautd.socnetsim.replanning.GenericPlanAlgorithm;
import playground.thibautd.socnetsim.replanning.grouping.GroupPlans;
import playground.thibautd.socnetsim.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import playground.thibautd.socnetsim.sharedvehicles.VehicleRessources;

/**
 * @author thibautd
 */
public class AllocateVehicleToPlansInGroupPlanModule extends AbstractMultithreadedGenericStrategyModule<GroupPlans> {
	private final VehicleRessources vehicleRessources;
	private final String mode;
	private final boolean allowNullRoutes;
	private final boolean preserveAllocations;

	public AllocateVehicleToPlansInGroupPlanModule(
			final int nThreads,
			final VehicleRessources vehicleRessources,
			final String mode,
			final boolean allowNullRoutes,
			final boolean preserveAllocations) {
		super( nThreads );
		this.vehicleRessources = vehicleRessources;
		this.mode = mode;
		this.allowNullRoutes = allowNullRoutes;
		this.preserveAllocations = preserveAllocations;
	}

	@Override
	public GenericPlanAlgorithm<GroupPlans> createAlgorithm() {
		return new AllocateVehicleToPlansInGroupPlanAlgorithm(
				MatsimRandom.getLocalInstance(),
				vehicleRessources,
				mode,
				allowNullRoutes,
				preserveAllocations);
	}
}

