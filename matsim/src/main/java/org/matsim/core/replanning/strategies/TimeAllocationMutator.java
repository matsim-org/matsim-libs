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

import javax.inject.Inject;
import javax.inject.Provider;

import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.TimeAllocationMutatorConfigGroup;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

public class TimeAllocationMutator implements Provider<PlanStrategy> {

	@Inject private GlobalConfigGroup globalConfigGroup;
	@Inject private TimeAllocationMutatorConfigGroup timeAllocationMutatorConfigGroup;
	@Inject private PlansConfigGroup plansConfigGroup;
	@Inject private Provider<org.matsim.core.router.TripRouter> tripRouterProvider;
	@Inject private Population population;
	
	@Override
	public PlanStrategy get() {
		PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
		org.matsim.core.replanning.modules.TimeAllocationMutator tam = new org.matsim.core.replanning.modules.TimeAllocationMutator(this.tripRouterProvider, 
				this.plansConfigGroup, this.timeAllocationMutatorConfigGroup, this.globalConfigGroup, population);
		strategy.addStrategyModule(tam);
		return strategy;
	}
}