/* *********************************************************************** *
 * project: org.matsim.*
 * BseControler.java
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

package playground.yu.replanning;

import org.matsim.core.controler.Controler;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManagerImpl;
import org.matsim.core.replanning.StrategyManagerConfigLoader;
import org.matsim.core.replanning.selectors.RandomPlanSelector;

public class RandomPlanSelectorControler extends Controler {

	public RandomPlanSelectorControler(String[] args) {
		super(args);
	}

	@Override
	protected StrategyManagerImpl loadStrategyManager() {
		StrategyManagerImpl manager = new StrategyManagerImpl();
		StrategyManagerConfigLoader.load(this, manager);

		PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
		manager.addStrategy(strategy, 0.0);
		manager.addChangeRequest(601, strategy, 1.0);

		return manager;
	}

	public static void main(final String args[]) {
		Controler controler = new RandomPlanSelectorControler(args);
		controler.setWriteEventsInterval(10);
		controler.run();
	}
}
