/* *********************************************************************** *
 * project: org.matsim.*
 * PlanStrategyTest.java
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

package org.matsim.core.replanning;

import org.matsim.core.api.replanning.PlanStrategyModule;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.PlanStrategy;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.testcases.MatsimTestCase;

public class PlanStrategyTest extends MatsimTestCase {

	/**
	 * @author mrieser
	 */
	public void testGetNumberOfStrategyModules() {
		final PlanStrategy strategy = new PlanStrategy(new RandomPlanSelector());
		assertEquals(0, strategy.getNumberOfStrategyModules());
		strategy.addStrategyModule(new DummyStrategyModule());
		assertEquals(1, strategy.getNumberOfStrategyModules());
		strategy.addStrategyModule(new DummyStrategyModule());
		assertEquals(2, strategy.getNumberOfStrategyModules());
	}

	/**
	 * A dummy strategy module for use in tests, without any functionality
	 *
	 * @author mrieser
	 */
	private static class DummyStrategyModule implements PlanStrategyModule {

		/*package*/ DummyStrategyModule() {
			// empty constructor with default visibility for a private inner class
		}

		public void finishReplanning() {
		}

		public void handlePlan(final PlanImpl plan) {
		}

		public void prepareReplanning() {
		}

	}

}
