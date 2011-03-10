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

package playground.yu.test;

import java.io.IOException;

import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.utils.misc.ConfigUtils;

import playground.yu.scoring.CharyparNagelScoringFunctionFactoryWithWalk;

public class SubTourModeChoicePlanStrategy implements PlanStrategy {
	// the reason why this class needs to be here is that this is defined in the
	// config file

	PlanStrategy planStrategyDelegate = null;

	public SubTourModeChoicePlanStrategy(Controler controler) {
		// also possible: MyStrategy( Scenario scenario ). But then I do not
		// have events. kai, aug'10

		// A PlanStrategy is something that can be applied to a person(!).

		// It first selects one of the plans:
		planStrategyDelegate = new PlanStrategyImpl(new RandomPlanSelector());

		// the plan selector may, at the same time, collect events:
		controler.getEvents().addHandler((EventHandler) this.getPlanSelector());

		// if you just want to select plans, you can stop here.

		// Otherwise, to do something with that plan, one needs to add modules
		// into the strategy. If there is at least
		// one module added here, then the plan is copied and then modified.
		SubTourModeChoice stmc = new SubTourModeChoice(controler.getConfig(),
				controler.getFacilities(), controler.getNetwork());
		addStrategyModule(stmc);

		addStrategyModule(new ReRoute(controler));
		// these modules may, at the same time, be events listeners (so that
		// they can collect information):
		// controler.getEvents().addHandler(stmc);

	}

	public void addStrategyModule(PlanStrategyModule module) {
		planStrategyDelegate.addStrategyModule(module);
	}

	public void finish() {
		planStrategyDelegate.finish();
	}

	public int getNumberOfStrategyModules() {
		return planStrategyDelegate.getNumberOfStrategyModules();
	}

	public PlanSelector getPlanSelector() {
		return planStrategyDelegate.getPlanSelector();
	}

	public void init() {
		planStrategyDelegate.init();
	}

	public void run(Person person) {
		planStrategyDelegate.run(person);
	}

	public String toString() {
		return planStrategyDelegate.toString();
	}

	public static void main(String[] args) {
		Config config = ConfigUtils.loadConfig(args[0]);
		Controler controler = new Controler(config);
		/* <module name="strategy">
		 * 	<param name="maxAgentPlanMemorySize" value="x" /> <!-- 0 means unlimited -->
		 * 	<param name="ModuleProbability_y" value="0.1" />
		 * 	<param name="Module_y" value="playground.yu.test.SubTourModeChoicePlanStrategy" />
		 * </module>*/
		controler
				.setScoringFunctionFactory(new CharyparNagelScoringFunctionFactoryWithWalk(
						config.planCalcScore(), config
//									.vspExperimental().getOffsetWalk()));
						.planCalcScore().getConstantWalk() )) ;
		// controler.addControlerListener(new MZComparisonListener());
		controler.setWriteEventsInterval(Integer.parseInt(args[1]));
		controler.setCreateGraphs(Boolean.parseBoolean(args[2]));
		controler.setOverwriteFiles(true);
		controler.run();

	}
}
