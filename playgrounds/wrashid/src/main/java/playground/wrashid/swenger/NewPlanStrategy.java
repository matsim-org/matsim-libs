/* *********************************************************************** *
 * project: org.matsim.*
 * TemplatePlanStrategy.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.wrashid.swenger;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.router.TripRouter;

import javax.inject.Provider;

public class NewPlanStrategy implements PlanStrategy {
	
	PlanStrategyImpl planStrategyDelegate = null ;

	/**
	 * @param scenario needs to be there because of the class loader
	 * @param tripRouterProvider
	 */
	public NewPlanStrategy(Scenario scenario, Provider<TripRouter> tripRouterProvider) {
		this.planStrategyDelegate = new PlanStrategyImpl( new RandomPlanSelector() ) ;
		this.addStrategyModule(new NewStrategyModule(tripRouterProvider));
	}

	public void addStrategyModule(PlanStrategyModule module) {
		this.planStrategyDelegate.addStrategyModule(module);
	}

	@Override
	public void finish() {
		this.planStrategyDelegate.finish();
	}

	public int getNumberOfStrategyModules() {
		return this.planStrategyDelegate.getNumberOfStrategyModules();
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		this.planStrategyDelegate.init(replanningContext);
	}

	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		this.planStrategyDelegate.run(person);
	}

	@Override
	public String toString() {
		return this.planStrategyDelegate.toString();
	}


}
