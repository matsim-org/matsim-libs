/* *********************************************************************** *
 * project: org.matsim.*
 * StrategyManagerTest.java
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

package playground.marcel;

import org.matsim.population.Population;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.modules.TimeAllocationMutator;
import org.matsim.replanning.selectors.KeepSelected;
import org.matsim.replanning.selectors.RandomPlanSelector;

public class StrategyManagerTest {

	
	
	static public void main(String[] args) {
		StrategyManager manager = new StrategyManager();
		
		PlanStrategy strategy1 = new PlanStrategy(new RandomPlanSelector());
		PlanStrategy strategy2 = new PlanStrategy(new RandomPlanSelector());
		PlanStrategy strategy3 = new PlanStrategy(new KeepSelected());
		
		// strategy1: TAM only
		strategy1.addStrategyModule(new TimeAllocationMutator());
		
		// strategy2: TAM first, after that ReRoute
		strategy2.addStrategyModule(new TimeAllocationMutator());
//		strategy2.addStrategyModule(new ReRoute());
		
		// strategy3: keep selected, don't change anything
		
		// add the strategies to the manager
		manager.addStrategy(strategy1, 0.10);
		manager.addStrategy(strategy2, 0.10);
		manager.addStrategy(strategy3, 0.80);
		
		// run it
		manager.run(new Population(), 1);
	}
}
