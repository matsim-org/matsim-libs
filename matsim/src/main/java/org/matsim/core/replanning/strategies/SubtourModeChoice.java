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

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.population.algorithms.PermissibleModesCalculator;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.core.utils.timing.TimeInterpretation;
import org.matsim.facilities.ActivityFacilities;

public class SubtourModeChoice implements Provider<PlanStrategy> {

	@Inject private Provider<TripRouter> tripRouterProvider;
	@Inject private GlobalConfigGroup globalConfigGroup;
	@Inject private SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup;
	@Inject private ActivityFacilities facilities;
	@Inject private PermissibleModesCalculator permissibleModesCalculator;
	@Inject private TimeInterpretation timeInterpretation;

	@Override
	public PlanStrategy get() {
		PlanStrategyImpl.Builder builder = new Builder(new RandomPlanSelector<>());
		builder.addStrategyModule(new org.matsim.core.replanning.modules.SubtourModeChoice(globalConfigGroup, subtourModeChoiceConfigGroup, permissibleModesCalculator));
		builder.addStrategyModule(new ReRoute(facilities, tripRouterProvider, globalConfigGroup, timeInterpretation));
		return builder.build();
	}

}
