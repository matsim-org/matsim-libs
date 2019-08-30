/* *********************************************************************** *
 * project: org.matsim.*
 * OptimizeVehicleAllocationAtTourLevelModule.java
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
package org.matsim.contrib.socnetsim.sharedvehicles.replanning;

import java.util.Collection;
import java.util.Set;

import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.replanning.ReplanningContext;

import org.matsim.contrib.socnetsim.framework.replanning.GenericPlanAlgorithm;
import org.matsim.contrib.socnetsim.framework.replanning.grouping.GroupPlans;
import org.matsim.contrib.socnetsim.framework.replanning.modules.AbstractMultithreadedGenericStrategyModule;
import org.matsim.contrib.socnetsim.sharedvehicles.VehicleRessources;

/**
 * @author thibautd
 */
public class OptimizeVehicleAllocationAtTourLevelModule  extends AbstractMultithreadedGenericStrategyModule<GroupPlans> {
	private final Collection<String> vehicularModes;
	private final boolean allowNullRoutes;
	private final Set<String> stageActs;
	private final VehicleRessources vehicleRessources;

	public OptimizeVehicleAllocationAtTourLevelModule(
			final int nThreads,
			final Set<String> stageActivitiesForSubtourDetection,
			final VehicleRessources vehicleRessources,
			final Collection<String> modes,
			final boolean allowNullRoutes) {
		super( nThreads );
		this.vehicularModes = modes;
		this.allowNullRoutes = allowNullRoutes;
		this.stageActs = stageActivitiesForSubtourDetection;
		this.vehicleRessources = vehicleRessources;
	}	

	@Override
	public GenericPlanAlgorithm<GroupPlans> createAlgorithm(ReplanningContext replanningContext) {
		return new OptimizeVehicleAllocationAtTourLevelAlgorithm(
			stageActs,
			MatsimRandom.getLocalInstance(),
			vehicleRessources,
			vehicularModes,
			allowNullRoutes);
	}
}

