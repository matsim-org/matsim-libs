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

package org.matsim.core.replanning;

import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.api.population.Population;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
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

		Population population = new PopulationImpl();
		for (int i = 0; i < 1000; i++) {
			Person p = new PersonImpl(new IdImpl(i));
			population.getPersons().put(p.getId(), p);
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

		Population population = new PopulationImpl();
		for (int i = 0; i < 100; i++) {
			Person p = new PersonImpl(new IdImpl(i));
			population.getPersons().put(p.getId(), p);
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

		Population population = new PopulationImpl();
		Person person = null;
		Plan[] plans = new Plan[10];
		// create a person with 4 unscored plans
		person = new PersonImpl(new IdImpl(1));
		plans[0] = person.createPlan(false);
		plans[1] = person.createPlan(false);
		plans[1].setScore(Double.valueOf(0.0));
		plans[2] = person.createPlan(false);
		plans[3] = person.createPlan(false);
		plans[3].setScore(Double.valueOf(-50.0));
		plans[4] = person.createPlan(false);
		plans[4].setScore(Double.valueOf(50.0));
		plans[5] = person.createPlan(false);
		plans[5].setScore(Double.valueOf(50.0));
		plans[6] = person.createPlan(false);
		plans[6].setScore(Double.valueOf(60.0));
		plans[7] = person.createPlan(false);
		plans[8] = person.createPlan(false);
		plans[8].setScore(Double.valueOf(-10.0));
		plans[9] = person.createPlan(false);
		population.getPersons().put(person.getId(), person);

		StrategyManager manager = new StrategyManager();
		PlanStrategy strategy = new PlanStrategy(new TestPlanSelector());
		manager.addStrategy(strategy, 1.0);

		// in each "iteration", an unscored plans should be selected
		for (int i = 0; i < 4; i++) {
			manager.run(population, i);
			Plan plan = person.getSelectedPlan();
			assertNull("plan has not undefined score in iteration " + i, plan.getScore());
			plan.setScore(Double.valueOf(i));
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
	
	public void testSetPlanSelectorForRemoval() {
		// init StrategyManager
		StrategyManager manager = new StrategyManager();
		manager.addStrategy(new PlanStrategy(new RandomPlanSelector()), 1.0);
		
		// init Population
		Person p = new PersonImpl(new IdImpl(1));
		Plan[] plans = new Plan[7];
		for (int i = 0; i < plans.length; i++) {
			plans[i] = p.createPlan(false);
			plans[i].setScore(Double.valueOf(i*10));
		}
		Population pop = new PopulationImpl();
		pop.getPersons().put(p.getId(), p);
		
		// run with default settings
		manager.setMaxPlansPerAgent(plans.length - 2);
		manager.run(pop);
		
		assertEquals("wrong number of plans.", 5, p.getPlans().size());
		// default of StrategyManager is to remove worst plans:
		assertFalse("plan should have been removed.", p.getPlans().contains(plans[0]));
		assertFalse("plan should have been removed.", p.getPlans().contains(plans[1]));
		assertTrue("plan should not have been removed.", p.getPlans().contains(plans[2]));

		// change plan selector for removal and run again
		manager.setPlanSelectorForRemoval(new BestPlanSelector());
		manager.setMaxPlansPerAgent(plans.length - 4);
		manager.run(pop);
		
		assertEquals("wrong number of plans.", 3, p.getPlans().size());
		// default of StrategyManager is to remove worst plans:
		assertFalse("plan should have been removed.", p.getPlans().contains(plans[plans.length - 1]));
		assertFalse("plan should have been removed.", p.getPlans().contains(plans[plans.length - 2]));
		assertTrue("plan should not have been removed.", p.getPlans().contains(plans[plans.length - 3]));
	}

	/**
	 * A simple extension to the PlanStrategy which counts how often it was
	 * called.
	 *
	 * @author mrieser
	 */
	static private class StrategyCounter extends PlanStrategy {

		private int counter = 0;

		protected StrategyCounter(final PlanSelector selector) {
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

		protected void resetCounter() {
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
