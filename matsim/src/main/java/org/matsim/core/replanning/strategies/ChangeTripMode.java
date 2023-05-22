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

package org.matsim.core.replanning.strategies;

import org.matsim.core.config.groups.ChangeModeConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ChangeLegMode;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TripsToLegsModule;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;

import jakarta.inject.Inject;
import jakarta.inject.Provider;

public class ChangeTripMode implements Provider<PlanStrategy> {

	private final GlobalConfigGroup globalConfigGroup;
	private final ChangeModeConfigGroup changeLegModeConfigGroup;
	private Provider<TripRouter> tripRouterProvider;
	private ActivityFacilities activityFacilities;
	private final TimeInterpretation timeInterpretation;

	@Inject
    protected ChangeTripMode(GlobalConfigGroup globalConfigGroup, ChangeModeConfigGroup changeLegModeConfigGroup, ActivityFacilities activityFacilities, Provider<TripRouter> tripRouterProvider, TimeInterpretation timeInterpretation) {
		this.globalConfigGroup = globalConfigGroup;
		this.changeLegModeConfigGroup = changeLegModeConfigGroup;
		this.activityFacilities = activityFacilities;
		this.tripRouterProvider = tripRouterProvider;
		this.timeInterpretation = timeInterpretation;
	}

    @Override
	public PlanStrategy get() {
		PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<>());
		builder.addStrategyModule(new TripsToLegsModule(tripRouterProvider, globalConfigGroup));
		builder.addStrategyModule(new ChangeLegMode(globalConfigGroup, changeLegModeConfigGroup));
		builder.addStrategyModule(new ReRoute(activityFacilities, tripRouterProvider, globalConfigGroup, timeInterpretation));
		return builder.build();
	}

}
