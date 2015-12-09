/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package tutorial.programming.example10PluggablePlanStrategyFromFile;

import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.ReplanningContext;

import javax.inject.Inject;


class MyPlanStrategy implements PlanStrategy {

	private final PlanStrategy delegate;

	@Inject // The constructor must be annotated so that the framework knows which one to use.
	MyPlanStrategy(Scenario scenario, EventsManager eventsManager) {
		// A PlanStrategy is something that can be applied to a Person (not a Plan).
		// Define how to select between existing plans:
		PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(new MyPlanSelector());

		// if you just want to select plans, you can stop here (except for the builder.build() below).


		// Otherwise, to do something with that plan, one needs to add modules into the strategy.  If there is at least
		// one module added here, then the plan is copied and then modified.
		MyPlanStrategyModule mod = new MyPlanStrategyModule(scenario);
		builder.addStrategyModule(mod);

		// these modules may, at the same time, be events listeners (so that they can collect information):
		eventsManager.addHandler(mod);

		delegate = builder.build();
	}

	@Override
	public void finish() {
		delegate.finish();
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		delegate.init(replanningContext);
	}

	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		delegate.run(person);
	}

}
