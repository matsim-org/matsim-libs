/* *********************************************************************** *
 * project: org.matsim.*
 * PopWithHeterogeneousPlanChoiceSet.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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
package playground.yu.newPlans;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManagerImpl;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.replanning.selectors.ExpBetaPlanChanger;

import playground.yu.replanning.selectors.RandomPlanSelectorWithPlanType;

/**
 * generate a populationfile, in which each agent has a plan choice set with
 * distinguishing plans. Man should have set
 * strategy/Module__1/2=ReRoute/TimeAllocationMutator,
 * strategy/ModuleProbability_1/2=xxx,
 * strategy/ModuleDisableAfterIteration_1/2=xxx
 * 
 * @author yu
 * 
 */
public class PopWithHeterogeneousPlanChoiceSet extends Controler {

	private int strategyChangerIteration = -1000000;

	/**
	 * @param args
	 *            configfilename
	 */
	public PopWithHeterogeneousPlanChoiceSet(String args) {
		super(args);
	}

	/**
	 * @param strategyChangerIteration
	 *            ths absolute iteration number for the changeRequest
	 */
	public void setStrategyChangerIteration(int strategyChangerIteration) {
		this.strategyChangerIteration = strategyChangerIteration;
	}

	protected StrategyManagerImpl loadStrategyManager() {
		StrategyManagerImpl manager = new StrategyManagerImpl();
		StrategyManagerConfigLoader.load(this, manager);
		manager.setPlanSelectorForRemoval(new RandomPlanSelectorWithPlanType()
		// RandomPlanSelector()
				);

		if (this.strategyChangerIteration > 0) {
			PlanStrategyImpl expBetaPlanChanger = new PlanStrategyImpl(
					new ExpBetaPlanChanger(config.charyparNagelScoring()
							.getBrainExpBeta()));
			manager.addStrategy(expBetaPlanChanger, 0.0);
			manager.addChangeRequest(this.strategyChangerIteration,
					expBetaPlanChanger, 1.0);
		}

		return manager;
	}

	/**
	 * @param args
	 */
	public static void main(String[] args) {
		PopWithHeterogeneousPlanChoiceSet controler = new PopWithHeterogeneousPlanChoiceSet(
				args[0]/* configfilename */);
		controler.setStrategyChangerIteration(Integer
				.parseInt(args[1]/* strategyChangerIteration */));
		controler.setOverwriteFiles(true);
		controler.setCreateGraphs(false);
		controler.setWriteEventsInterval(100);
		controler.run();
	}

}
