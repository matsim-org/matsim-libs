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

import com.google.inject.Inject;
import org.matsim.core.config.Config;
import org.matsim.core.router.CompositeStageActivityTypes;
import org.matsim.core.router.StageActivityTypes;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;
import org.matsim.population.algorithms.TripsToLegsAlgorithm;

import javax.inject.Provider;

/**
 * Removes trips and replaces them by legs.
 * The aim is to simplify the plan before passing it to plan algorithms
 * unable to handle multi-planElement trips.
 * The plan must be re-routed before execution!
 * @author thibautd
 */
public class TripsToLegsModule extends AbstractMultithreadedModule {
	@Inject
	Provider<TripRouter> tripRouterProvider;

	private final StageActivityTypes additionalBlackList;

	/**
	 * Initializes an instance using the stage activity types from the controler
	 */
	public TripsToLegsModule(Config config) {
		this( config, null );
	}

	/**
	 * Initializes an instance, allowing to specify additional activity types to
	 * consider as stage activities.
	 * @param controler
	 * @param additionalBlackList a {@link StageActivityTypes} instance identifying
	 * the additionnal types
	 */
	public TripsToLegsModule(final Config config, final StageActivityTypes additionalBlackList) {
		super( config.global() );
		this.additionalBlackList = additionalBlackList;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		TripRouter router = tripRouterProvider.get();
		StageActivityTypes blackListToUse = router.getStageActivityTypes();

		if (additionalBlackList != null) {
			CompositeStageActivityTypes composite = new CompositeStageActivityTypes();
			composite.addActivityTypes( blackListToUse );
			composite.addActivityTypes( additionalBlackList );
			blackListToUse = composite;
		}

		return new TripsToLegsAlgorithm( 
				blackListToUse,
				router.getMainModeIdentifier() );
	}
}

