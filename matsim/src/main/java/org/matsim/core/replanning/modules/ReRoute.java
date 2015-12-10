/* *********************************************************************** *
 * project: org.matsim.*
 * ReRoute.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.router.PlanRouter;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;
import org.matsim.population.algorithms.PlanAlgorithm;

import javax.inject.Provider;

/**
 * Uses the routing algorithm provided by the {@linkplain Controler} for 
 * calculating the routes of plans during Replanning.
 *
 * @author mrieser
 */
public class ReRoute extends AbstractMultithreadedModule {
	
	private ActivityFacilities facilities;

	private final Provider<TripRouter> tripRouterProvider;

	ReRoute(Config config, ActivityFacilities facilities, Provider<TripRouter> tripRouterProvider) {
		super(config.global());
		this.facilities = facilities;
		this.tripRouterProvider = tripRouterProvider;
	}

	public ReRoute(Scenario scenario, Provider<TripRouter> tripRouterProvider) {
		this(scenario.getConfig(), scenario.getActivityFacilities(), tripRouterProvider);
	}

	@Override
	public final PlanAlgorithm getPlanAlgoInstance() {
			return new PlanRouter(
					tripRouterProvider.get(),
					facilities);
	}

}
