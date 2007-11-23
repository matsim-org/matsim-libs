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

package org.matsim.replanning;

import org.matsim.basic.v01.Id;
import org.matsim.gbl.Gbl;
import org.matsim.plans.Person;
import org.matsim.plans.Plans;
import org.matsim.replanning.PlanStrategy;
import org.matsim.replanning.StrategyManager;
import org.matsim.replanning.selectors.PlanSelectorI;
import org.matsim.replanning.selectors.RandomPlanSelector;
import org.matsim.testcases.MatsimTestCase;

public class StrategyManagerTest extends MatsimTestCase {

	/**
	 * This method tests, if adding strategies and strategyRequest get correctly
	 * executed and if, after changes are performed, still all required strategies
	 * are called according to their weights.
	 *
	 * @author mrieser
	 */
	public void testChangeRequests() {

		Gbl.random.setSeed(4711);

		Plans population = new Plans(Plans.NO_STREAMING);
		try {
			for (int i = 0; i < 1000; i++) {
				Person person = new Person(new Id(i), "m", 40, null, null, null);

				population.addPerson(person);
			}
		}
		catch (Exception e) {
			e.printStackTrace();
		}

		// setup StrategyManager
		StrategyManager manager = new StrategyManager();
		TestStrategy strategy1 = new TestStrategy(new RandomPlanSelector());
		TestStrategy strategy2 = new TestStrategy(new RandomPlanSelector());
		TestStrategy strategy3 = new TestStrategy(new RandomPlanSelector());
		TestStrategy strategy4 = new TestStrategy(new RandomPlanSelector());

		manager.addStrategy(strategy1, 0.10);
		manager.addStrategy(strategy2, 0.20);
		manager.addStrategy(strategy3, 0.30);
		manager.addStrategy(strategy4, 0.40);


		// add ChangeRequests
		manager.addChangeRequest(11, strategy2, 0.0);
		manager.addChangeRequest(11, strategy3, 0.0);
		manager.addChangeRequest(12, strategy4, 0.1);

		// run iteration 1
		manager.run(population, 1);

		assertEquals(92, strategy1.getCounter());
		assertEquals(199, strategy2.getCounter());
		assertEquals(297, strategy3.getCounter());
		assertEquals(412, strategy4.getCounter());

		strategy1.resetCounter();
		strategy2.resetCounter();
		strategy3.resetCounter();
		strategy4.resetCounter();

		// run iteration 10
		manager.run(population, 10);

		assertEquals(95, strategy1.getCounter());
		assertEquals(197, strategy2.getCounter());
		assertEquals(279, strategy3.getCounter());
		assertEquals(429, strategy4.getCounter());

		strategy1.resetCounter();
		strategy2.resetCounter();
		strategy3.resetCounter();
		strategy4.resetCounter();

		// run iteration 11, strategy2 and strategy3 should now be disabled
		manager.run(population, 11);

		assertEquals(173, strategy1.getCounter());
		assertEquals(0, strategy2.getCounter());
		assertEquals(0, strategy3.getCounter());
		assertEquals(827, strategy4.getCounter());

		strategy1.resetCounter();
		strategy2.resetCounter();
		strategy3.resetCounter();
		strategy4.resetCounter();

		// run iteration 12, strategy4 should now have the same weight as strategy1
		manager.run(population, 12);

		assertEquals(502, strategy1.getCounter());
		assertEquals(0, strategy2.getCounter());
		assertEquals(0, strategy3.getCounter());
		assertEquals(498, strategy4.getCounter());

	}

	/**
	 * A simple extension to the PlanStrategy which counts how often it was
	 * called.
	 *
	 * @author mrieser
	 */
	static private class TestStrategy extends PlanStrategy {

		int counter = 0;

		private TestStrategy(final PlanSelectorI selector) {
			super(selector);
		}

		@Override
		public void run(final Person person) {
			this.counter++;
			super.run(person);
		}

		public int getCounter() {
			return this.counter;
		}

		public void resetCounter() {
			this.counter = 0;
		}
	}

}
