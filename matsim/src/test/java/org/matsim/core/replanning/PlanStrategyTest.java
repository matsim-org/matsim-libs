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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.testcases.MatsimTestUtils;

public class PlanStrategyTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	/**
	 * @author mrieser
	 */
	@Test
	void testGetNumberOfStrategyModules() {
		final PlanStrategyImpl strategy = new PlanStrategyImpl(new RandomPlanSelector());
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

		public void handlePlan(final Plan plan) {
		}

		public void prepareReplanning(ReplanningContext replanningContext) {
		}

	}

}
