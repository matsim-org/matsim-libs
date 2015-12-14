/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2014 by the members listed in the COPYING,        *
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

package org.matsim.contrib.minibus.performance;

import com.google.inject.Inject;
import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.replanning.modules.AbstractMultithreadedModule;
import org.matsim.core.router.TripRouter;
import org.matsim.population.algorithms.PlanAlgorithm;

import javax.inject.Provider;

/**
 * 
 * @author aneumann
 *
 */
final class PReRouteStrategyModule extends AbstractMultithreadedModule{

	private Provider<TripRouter> tripRouterProvider;

	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger(PReRouteStrategyModule.class);

	private final Scenario scenario;

	public PReRouteStrategyModule(Provider<TripRouter> tripRouterProvider, Scenario scenario) {
		super(scenario.getConfig().global());
		this.tripRouterProvider = tripRouterProvider;
		this.scenario = scenario;
	}

	@Override
	public PlanAlgorithm getPlanAlgoInstance() {
		return new PPlanRouter(
				tripRouterProvider.get(),
				this.scenario.getActivityFacilities());
	}

}
