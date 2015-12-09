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

package playground.andreas.virginia;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;
import org.matsim.pt.replanning.TransitActsRemoverStrategy;

import javax.inject.Provider;

/**
 * 
 * {@link TimeAllocationMutator} without an immediate {@link ReRoute} yields invalid routes.
 * Thus, agents tend to be stuck and cannot probe successfully for alternative departures.
 * Same argument holds true for individual transport (car), but implications are not that clear.
 * 
 * @author aneumann
 *
 */
public class TimeAllocationWithReroute implements PlanStrategy {


	private final Provider<TripRouter> tripRouterProvider;
	private PlanStrategyImpl strategy;

	public TimeAllocationWithReroute(Scenario scenario, Provider<TripRouter> tripRouterProvider) {
		this.tripRouterProvider = tripRouterProvider;
		PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
		strategy.addStrategyModule(new TransitActsRemoverStrategy(scenario.getConfig()));
		strategy.addStrategyModule(new TimeAllocationMutator(scenario.getConfig(), this.tripRouterProvider));
		strategy.addStrategyModule(new ReRoute(scenario, this.tripRouterProvider));
		this.strategy = strategy;
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
