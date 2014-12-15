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
	
	private final PlanStrategy planStrategyDelegate;

	@Inject // The constructor must be annotated so that the framework knows which one to use.
    MyPlanStrategy(Scenario scenario, EventsManager eventsManager) {
		// A PlanStrategy is something that can be applied to a Person (not a Plan).
        // It first selects one of the plans:
        MyPlanSelector planSelector = new MyPlanSelector();
        PlanStrategyImpl.Builder builder = new PlanStrategyImpl.Builder(planSelector);

        // the plan selector may, at the same time, collect events:
        eventsManager.addHandler(planSelector);

        // if you just want to select plans, you can stop here.


        // Otherwise, to do something with that plan, one needs to add modules into the strategy.  If there is at least
        // one module added here, then the plan is copied and then modified.
        MyPlanStrategyModule mod = new MyPlanStrategyModule(scenario);
        builder.addStrategyModule(mod);

        // these modules may, at the same time, be events listeners (so that they can collect information):
        eventsManager.addHandler(mod);

        planStrategyDelegate = builder.build();
	}

	@Override
	public void finish() {
		planStrategyDelegate.finish();
	}

	@Override
	public void init(ReplanningContext replanningContext) {
		planStrategyDelegate.init(replanningContext);
	}

	@Override
	public void run(HasPlansAndId<Plan, Person> person) {
		planStrategyDelegate.run(person);
	}

}
