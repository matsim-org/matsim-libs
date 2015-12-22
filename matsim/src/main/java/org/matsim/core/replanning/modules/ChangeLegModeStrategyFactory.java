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

import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ChangeLegModeConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;

import javax.inject.Inject;
import javax.inject.Provider;

public class ChangeLegModeStrategyFactory implements Provider<PlanStrategy> {

	private final GlobalConfigGroup globalConfigGroup;
	private final ChangeLegModeConfigGroup changeLegModeConfigGroup;
	private Provider<TripRouter> tripRouterProvider;
	private ActivityFacilities activityFacilities;

	@Inject
    ChangeLegModeStrategyFactory(GlobalConfigGroup globalConfigGroup, ChangeLegModeConfigGroup changeLegModeConfigGroup, ActivityFacilities activityFacilities, Provider<TripRouter> tripRouterProvider) {
		this.globalConfigGroup = globalConfigGroup;
		this.changeLegModeConfigGroup = changeLegModeConfigGroup;
		this.activityFacilities = activityFacilities;
		this.tripRouterProvider = tripRouterProvider;
	}

    @Override
	public PlanStrategy get() {
		PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
		strategy.addStrategyModule(new ChangeLegMode(globalConfigGroup, changeLegModeConfigGroup));
		strategy.addStrategyModule(new ReRoute(activityFacilities, tripRouterProvider, globalConfigGroup));
		return strategy;
	}

}
