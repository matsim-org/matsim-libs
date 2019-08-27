/* *********************************************************************** *
 * project: org.matsim.*
 * TripsToLegModule.java
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
package org.matsim.core.replanning.modules;

import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.population.algorithms.PlanAlgorithm;
import org.matsim.core.population.algorithms.TripsToLegsAlgorithm;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;

import javax.inject.Provider;

/**
 * Removes trips and replaces them by legs.
 * The aim is to simplify the plan before passing it to plan algorithms
 * unable to handle multi-planElement trips.
 * The plan must be re-routed before execution!
 * @author thibautd
 */
public class TripsToLegsModule extends AbstractMultithreadedModule {

	private final Provider<TripRouter> tripRouterProvider;

	/**
	 * Initializes an instance using the stage activity types from the controler
	 */
	public TripsToLegsModule(Provider<TripRouter> tripRouterProvider, GlobalConfigGroup globalConfigGroup) {
		this(null, tripRouterProvider, globalConfigGroup);
	}

	/**
	 * Initializes an instance, allowing to specify additional activity types to
	 * consider as stage activities.
	 * @param additionalBlackList a {@link StageActivityTypes} instance identifying
	 * @param tripRouterProvider
	 * @param globalConfigGroup
	 */
	public TripsToLegsModule(final StageActivityTypes additionalBlackList, Provider<TripRouter> tripRouterProvider, GlobalConfigGroup globalConfigGroup) {
		super(globalConfigGroup);
		this.tripRouterProvider = tripRouterProvider;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		TripRouter router = tripRouterProvider.get();

		return new TripsToLegsAlgorithm( 
				router.getMainModeIdentifier() );
	}
}

