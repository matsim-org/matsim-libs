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

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import com.google.inject.Inject;
import org.matsim.core.router.TripRouter;

import javax.inject.Provider;

/**
 * 
 * Adds a rerouting strategy that will only reroute pt trips.
 * 
 * @author aneumann
 *
 */
public final class PReRoute implements PlanStrategy {
	private PlanStrategyImpl strategy = null ;

	@Inject
	public PReRoute(Scenario scenario, Provider<TripRouter> tripRouterProvider) {
		this.strategy = new PlanStrategyImpl(new RandomPlanSelector());
		this.strategy.addStrategyModule(new PReRouteStrategyModule(tripRouterProvider, scenario)) ;
	}

	@Override
	public void finish() {
		this.strategy.finish();
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		this.strategy.init(replanningContext);
	}

	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		this.strategy.run(person);
	}

}
