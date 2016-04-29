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

package playground.singapore.springcalibration.run.replanning;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.ChangeLegModeConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.SubtourModeChoiceConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TripsToLegsModule;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.facilities.ActivityFacilities;

import playground.singapore.springcalibration.run.SingaporeConfigGroup;
import playground.singapore.springcalibration.run.TaxiUtils;

import javax.inject.Inject;
import javax.inject.Provider;

public class SingaporeLegModeChoicePlanStrategyProvider implements Provider<PlanStrategy> {

	private final GlobalConfigGroup globalConfigGroup;
	private final ChangeLegModeConfigGroup changeLegModeConfigGroup;
	private final SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup;
	private Provider<TripRouter> tripRouterProvider;
	private ActivityFacilities activityFacilities;
	private Population population;
	private TaxiUtils taxiUtils;

	@Inject
	SingaporeLegModeChoicePlanStrategyProvider(
			GlobalConfigGroup globalConfigGroup, 
			ChangeLegModeConfigGroup changeLegModeConfigGroup, 
			SubtourModeChoiceConfigGroup subtourModeChoiceConfigGroup,
			ActivityFacilities activityFacilities, 
			Provider<TripRouter> tripRouterProvider,
			Population population,
			SingaporeConfigGroup config) {
		this.globalConfigGroup = globalConfigGroup;
		this.changeLegModeConfigGroup = changeLegModeConfigGroup;
		this.activityFacilities = activityFacilities;
		this.tripRouterProvider = tripRouterProvider;
		this.population = population;
		this.subtourModeChoiceConfigGroup = subtourModeChoiceConfigGroup;
		this.taxiUtils = new TaxiUtils(config);
	}

    @Override
	public PlanStrategy get() {
		PlanStrategyImpl strategy = new PlanStrategyImpl(new BestPlanSelector());
		strategy.addStrategyModule(new TripsToLegsModule(tripRouterProvider, globalConfigGroup));
		strategy.addStrategyModule(new SingaporeModeChangeAlgoGenerator(globalConfigGroup, changeLegModeConfigGroup, subtourModeChoiceConfigGroup, population, taxiUtils));
		strategy.addStrategyModule(new ReRoute(activityFacilities, tripRouterProvider, globalConfigGroup));
		return strategy;
	}

}
