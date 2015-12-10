/* *********************************************************************** *
 * project: org.matsim.*
 * TimeAllocationMutatorReRoutePlanStategyFactory.java
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
package org.matsim.core.replanning.modules;

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;

import javax.inject.Inject;
import javax.inject.Provider;

/**
 * @author thibautd
 */
public class TimeAllocationMutatorReRoutePlanStrategyFactory implements Provider<PlanStrategy> {
	private Provider<TripRouter> tripRouterProvider;
	private Config config;
	private ActivityFacilities activityFacilities;

	@Inject
    TimeAllocationMutatorReRoutePlanStrategyFactory(Config config, ActivityFacilities activityFacilities, Provider<TripRouter> tripRouterProvider) {
		this.config = config;
		this.activityFacilities = activityFacilities;
		this.tripRouterProvider = tripRouterProvider;
	}

    @Override
	public PlanStrategy get() {
		final PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
		strategy.addStrategyModule( new TimeAllocationMutator(config, tripRouterProvider) );
		strategy.addStrategyModule( new ReRoute(config, activityFacilities, tripRouterProvider) );
		return strategy;
	}
}

