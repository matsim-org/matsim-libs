/* *********************************************************************** *
 * project: org.matsim.*
 * ChangeLegModeWithParkLocationRemoveOldestPlanControler.java
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

/**
 * 
 */
package playground.yu.test;

import org.matsim.api.core.v01.ScenarioLoader;
import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.replanning.modules.ReRoute;
import org.matsim.core.replanning.modules.TimeAllocationMutator;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

import playground.yu.replanning.StrategyManagerWithRemoveOldestPlan;
import playground.yu.scoring.CharyparNagelScoringFunctionFactoryWithWalk;

/**
 * @author yu
 * 
 */
public class ChangeLegModeWithParkLocationRemoveOldestPlanControler extends
		Controler {

	public ChangeLegModeWithParkLocationRemoveOldestPlanControler(String[] args) {
		super(args);
	}

	@Override
	protected StrategyManager loadStrategyManager() {
		StrategyManager manager = new StrategyManagerWithRemoveOldestPlan();
		StrategyManagerConfigLoader.load(this, this.config, manager);

		manager.setMaxPlansPerAgent(4);

		// ChangeExpBeta
		PlanStrategy strategy1 = new PlanStrategy(new ExpBetaPlanChanger());
		manager.addStrategy(strategy1, 0.7);

		// ChangeLegModeWithParkLocation
		PlanStrategy strategy2 = new PlanStrategy(new RandomPlanSelector());
		strategy2.addStrategyModule(new ChangeLegModeWithParkLocation(
				this.config));
		strategy2.addStrategyModule(new ReRoute(this));
		manager.addStrategy(strategy2, 0.1);

		// ReRoute
		PlanStrategy strategy3 = new PlanStrategy(new RandomPlanSelector());
		strategy3.addStrategyModule(new ReRoute(this));
		manager.addStrategy(strategy3, 0.1);

		// TimeAllocationMutator
		PlanStrategy strategy4 = new PlanStrategy(new RandomPlanSelector());
		strategy4.addStrategyModule(new TimeAllocationMutator());
		manager.addStrategy(strategy4, 0.1);

		return manager;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		Controler ctl = new ChangeLegModeWithParkLocationRemoveOldestPlanControler(
				args);
		ctl
				.addControlerListener(new ChangeLegModeWithParkLocation.LegChainModesListener());
		// ctl.setCreateGraphs(false);
		ctl.setWriteEventsInterval(0);
		ctl
				.setScoringFunctionFactory(new CharyparNagelScoringFunctionFactoryWithWalk(
						new ScenarioLoader(args[0]).loadScenario().getConfig()
								.charyparNagelScoring()));
		ctl.run();
	}

}
