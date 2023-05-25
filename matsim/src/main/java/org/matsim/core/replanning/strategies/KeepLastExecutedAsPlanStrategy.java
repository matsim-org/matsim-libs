/* *********************************************************************** *
 * project: org.matsim.*												   *
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

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.controler.ControlerListenerManager;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.PlanStrategyImpl.Builder;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.withinday.controller.ExecutedPlansServiceImpl;

/**
 * @author nagel
 *
 */
public class KeepLastExecutedAsPlanStrategy implements Provider<PlanStrategy> {
	@Inject Config config ;
	@Inject ControlerListenerManager cm ;
	@Inject ExecutedPlansServiceImpl executedPlans ;

	@Override public PlanStrategy get() {
		Builder builder = new PlanStrategyImpl.Builder(new RandomPlanSelector<Plan,Person>()) ;
		builder.addStrategyModule(new org.matsim.core.replanning.modules.KeepLastExecuted(config, executedPlans) ) ;
		return builder.build() ;
	}
}
