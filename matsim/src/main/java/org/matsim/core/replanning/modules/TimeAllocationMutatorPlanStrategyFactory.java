/* *********************************************************************** *
 * project: org.matsim.*
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

import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;

import javax.inject.Inject;
import javax.inject.Provider;

public class TimeAllocationMutatorPlanStrategyFactory implements
		Provider<PlanStrategy> {

    private Config config;
	private javax.inject.Provider<org.matsim.core.router.TripRouter> tripRouterProvider;

	@Inject
    TimeAllocationMutatorPlanStrategyFactory(Config config, Provider<TripRouter> tripRouterProvider) {
        this.config = config;
		this.tripRouterProvider = tripRouterProvider;
	}

    @Override
	public PlanStrategy get() {
		PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
		TimeAllocationMutator tam = new TimeAllocationMutator(config, tripRouterProvider);
		strategy.addStrategyModule(tam);
		return strategy;
	}

}
