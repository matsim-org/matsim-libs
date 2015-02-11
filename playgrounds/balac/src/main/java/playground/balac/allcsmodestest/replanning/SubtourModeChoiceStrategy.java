package playground.balac.allcsmodestest.replanning;

/* *********************************************************************** *
 * project: org.matsim.*
 * TimeAllocationMutatorStrategy.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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


import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import com.google.inject.Inject;
/**
 * Uses a TripsToLegModule to simplify trips before running subtour
 * mode choice and re-routing
 * @author thibautd
 */
public class SubtourModeChoiceStrategy implements PlanStrategy {
	private final PlanStrategyImpl strategy;
	@Inject
	public SubtourModeChoiceStrategy(final Scenario scenario) {
		this.strategy = new PlanStrategyImpl( new RandomPlanSelector<Plan, Person>() );

		//addStrategyModule( new TripsToLegsModule(controler.getConfig() ) );   
		SubTourModeChoiceCS smc = new SubTourModeChoiceCS(scenario.getConfig());
		SubTourPermissableModesCalculator cpmc = new SubTourPermissableModesCalculator(scenario);
		smc.setPermissibleModesCalculator(cpmc);
		
		addStrategyModule(smc );
		addStrategyModule( new ReRoute(scenario) );
	}

	public void addStrategyModule(final PlanStrategyModule module) {
		strategy.addStrategyModule(module);
	}

	public int getNumberOfStrategyModules() {
		return strategy.getNumberOfStrategyModules();
	}

	@Override
	public void run(final HasPlansAndId<Plan, Person> person) {
		strategy.run(person);
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		strategy.init(replanningContext);
	}

	@Override
	public void finish() {
		strategy.finish();
	}

	@Override
	public String toString() {
		return strategy.toString();
	}

}

