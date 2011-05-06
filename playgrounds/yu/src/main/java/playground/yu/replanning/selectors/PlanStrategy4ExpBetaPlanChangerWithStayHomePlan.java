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

package playground.yu.replanning.selectors;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.selectors.PlanSelector;

public class PlanStrategy4ExpBetaPlanChangerWithStayHomePlan implements
		PlanStrategy {
	// the reason why this class needs to be here is that this is defined in the
	// config file

	PlanStrategy planStrategyDelegate = null;

	public PlanStrategy4ExpBetaPlanChangerWithStayHomePlan(Controler controler) {
		// also possible: MyStrategy( Scenario scenario ). But then I do not
		// have events. kai, aug'10

		// A PlanStrategy is something that can be applied to a person(!).

		// It first selects one of the plans:
		planStrategyDelegate = new PlanStrategyImpl(
				new ExpBetaPlanChangerWithStayHomePlan(controler.getScenario()));

		// the plan selector may, at the same time, collect events:
		controler.getEvents().addHandler((EventHandler) getPlanSelector());

		// if you just want to select plans, you can stop here.

		// Otherwise, to do something with that plan, one needs to add modules
		// into the strategy. If there is at least
		// one module added here, then the plan is copied and then modified.
		// MyPlanStrategyModule mod = new MyPlanStrategyModule( controler ) ;
		// addStrategyModule(mod) ;

		// these modules may, at the same time, be events listeners (so that
		// they can collect information):
		// controler.getEvents().addHandler( mod ) ;

	}

	@Override
	public void addStrategyModule(PlanStrategyModule module) {
		planStrategyDelegate.addStrategyModule(module);
	}

	@Override
	public void finish() {
		planStrategyDelegate.finish();
	}

	@Override
	public int getNumberOfStrategyModules() {
		return planStrategyDelegate.getNumberOfStrategyModules();
	}

	@Override
	public PlanSelector getPlanSelector() {
		return planStrategyDelegate.getPlanSelector();
	}

	@Override
	public void init() {
		planStrategyDelegate.init();
	}

	@Override
	public void run(Person person) {
		planStrategyDelegate.run(person);
	}

	@Override
	public String toString() {
		return planStrategyDelegate.toString();
	}

}
