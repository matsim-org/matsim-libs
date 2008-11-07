/* *********************************************************************** *
 * project: org.matsim.*
 * StrategyManagerTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

import org.matsim.basic.v01.IdImpl;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.replanning.selectors.PlanSelector;
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

		Population population = new Population(Population.NO_STREAMING);
		for (int i = 0; i < 1000; i++) {
			population.addPerson(new PersonImpl(new IdImpl(i)));
		}

		// setup StrategyManager
		StrategyManager manager = new StrategyManager();
		StrategyCounter strategy1 = new StrategyCounter(new RandomPlanSelector());
		StrategyCounter strategy2 = new StrategyCounter(new RandomPlanSelector());
		StrategyCounter strategy3 = new StrategyCounter(new RandomPlanSelector());
		StrategyCounter strategy4 = new StrategyCounter(new RandomPlanSelector());

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
	 * Tests the removal of strategies. Ensures that after removal, no plan is given to the removed strategy.
	 * Also checks that the removal of strategies not known to the StrategyManager doesn't have any side-effects.
	 *
	 * @author mrieser
	 */
	public void testRemoveStrategy() {

		Population population = new Population(Population.NO_STREAMING);
		for (int i = 0; i < 100; i++) {
			population.addPerson(new PersonImpl(new IdImpl(i)));
		}

		// setup StrategyManager
		StrategyManager manager = new StrategyManager();
		StrategyCounter strategy1 = new StrategyCounter(new RandomPlanSelector());
		StrategyCounter strategy2 = new StrategyCounter(new RandomPlanSelector());

		manager.addStrategy(strategy1, 0.10);
		manager.addStrategy(strategy2, 0.20);

		// run iteration 1
		manager.run(population, 1);

		// ensure all strategies were called
		assertEquals(34, strategy1.getCounter());
		assertEquals(66, strategy2.getCounter());

		strategy1.resetCounter();
		strategy2.resetCounter();
		
		// remove 2nd strategy
		manager.removeStrategy(strategy2);

		// run iteration 2
		manager.run(population, 2);

		// ensure only strategy1 got plans to handle
		assertEquals(100, strategy1.getCounter());
		assertEquals(0, strategy2.getCounter());

		strategy1.resetCounter();
		strategy2.resetCounter();

		// try to remove strategy2 again
		manager.removeStrategy(strategy2);

		// run iteration 3
		manager.run(population, 3);

		// ensure that strategey1 still gets all plans
		assertEquals(100, strategy1.getCounter());
		assertEquals(0, strategy2.getCounter());
	}

	/**
	 * This method tests that the StrategyManager uses a so-called "optimistic behavior"
	 * when selecting plans for replanning/execution. Optimistic Behavior means that plans
	 * with undefined score are chosen before any other plan with defined score.
	 *
	 * @author mrieser
	 */
	public void testOptimisticBehavior() {

		Population population = new Population(Population.NO_STREAMING);
		Person person = null;
		Plan[] plans = new Plan[10];
		// create a person with 4 unscored plans
		person = new PersonImpl(new IdImpl(1));
		plans[0] = person.createPlan(false);
		plans[1] = person.createPlan(false);
		plans[1].setScore(0.0);
		plans[2] = person.createPlan(false);
		plans[3] = person.createPlan(false);
		plans[3].setScore(-50.0);
		plans[4] = person.createPlan(false);
		plans[4].setScore(50.0);
		plans[5] = person.createPlan(false);
		plans[5].setScore(50.0);
		plans[6] = person.createPlan(false);
		plans[6].setScore(60.0);
		plans[7] = person.createPlan(false);
		plans[8] = person.createPlan(false);
		plans[8].setScore(-10.0);
		plans[9] = person.createPlan(false);
		population.addPerson(person);

		StrategyManager manager = new StrategyManager();
		PlanStrategy strategy = new PlanStrategy(new TestPlanSelector());
		manager.addStrategy(strategy, 1.0);

		// in each "iteration", an unscored plans should be selected
		for (int i = 0; i < 4; i++) {
			manager.run(population, i);
			Plan plan = person.getSelectedPlan();
			assertTrue("plan has not undefined score in iteration " + i, plan.hasUndefinedScore());
			plan.setScore(i);
		}

		/* There are no more unscored plans now, so in the next "iteration" our
		 * bad PlanSelector should be called. */
		try {
			manager.run(population, 5);
			fail("expected UnsupportedOperationException.");
		}
		catch (UnsupportedOperationException expected) {
			// expected Exception catched
		}

	}

	/**
	 * A simple extension to the PlanStrategy which counts how often it was
	 * called.
	 *
	 * @author mrieser
	 */
	static private class StrategyCounter extends PlanStrategy {

		int counter = 0;

		public StrategyCounter(final PlanSelector selector) {
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

	/**
	 * A simple PlanSelector that throws an UnsupportedOperationException whenever
	 * it should select a plan.
	 *
	 * @author mrieser
	 */
	static private class TestPlanSelector implements PlanSelector {

		public TestPlanSelector() {
		}
		public Plan selectPlan(final Person person) {
			throw new UnsupportedOperationException();
		}

	}

}
