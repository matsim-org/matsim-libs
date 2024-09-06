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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.api.core.v01.replanning.PlanStrategyModule;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.PlanSelector;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class StrategyManagerTest {

	/**
	 * This method tests, if adding strategies and strategyRequest get correctly
	 * executed and if, after changes are performed, still all required strategies
	 * are called according to their weights.
	 *
	 * @author mrieser
	 */
	@Test
	void testChangeRequests() {
		MatsimRandom.reset(4711);
		
		Population population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		for (int i = 0; i < 1000; i++) {
			Person p = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			population.addPerson(p);
		}

		// setup StrategyManager
		StrategyManager manager = new StrategyManager();
		StrategyCounter strategy1 = new StrategyCounter(new RandomPlanSelector<Plan, Person>());
		StrategyCounter strategy2 = new StrategyCounter(new RandomPlanSelector<Plan, Person>());
		StrategyCounter strategy3 = new StrategyCounter(new RandomPlanSelector<Plan, Person>());
		StrategyCounter strategy4 = new StrategyCounter(new RandomPlanSelector<Plan, Person>());

		manager.addStrategy( strategy1, null, 0.10 );
		manager.addStrategy( strategy2, null, 0.20 );
		manager.addStrategy( strategy3, null, 0.30 );
		manager.addStrategy( strategy4, null, 0.40 );


		// add ChangeRequests
		manager.addChangeRequest( 11, strategy2, null, 0.0 );
		manager.addChangeRequest( 11, strategy3, null, 0.0 );
		manager.addChangeRequest( 12, strategy4, null, 0.1 );

		// run iteration 1
		manager.run(population, 1, null);

		assertEquals(92, strategy1.getCounter());
		assertEquals(199, strategy2.getCounter());
		assertEquals(297, strategy3.getCounter());
		assertEquals(412, strategy4.getCounter());

		strategy1.resetCounter();
		strategy2.resetCounter();
		strategy3.resetCounter();
		strategy4.resetCounter();

		// run iteration 10
		manager.run(population, 10, null);

		assertEquals(95, strategy1.getCounter());
		assertEquals(197, strategy2.getCounter());
		assertEquals(279, strategy3.getCounter());
		assertEquals(429, strategy4.getCounter());

		strategy1.resetCounter();
		strategy2.resetCounter();
		strategy3.resetCounter();
		strategy4.resetCounter();

		// run iteration 11, strategy2 and strategy3 should now be disabled
		manager.run(population, 11, null);

		assertEquals(173, strategy1.getCounter());
		assertEquals(0, strategy2.getCounter());
		assertEquals(0, strategy3.getCounter());
		assertEquals(827, strategy4.getCounter());

		strategy1.resetCounter();
		strategy2.resetCounter();
		strategy3.resetCounter();
		strategy4.resetCounter();

		// run iteration 12, strategy4 should now have the same weight as strategy1
		manager.run(population, 12, null);

		assertEquals(502, strategy1.getCounter());
		assertEquals(0, strategy2.getCounter());
		assertEquals(0, strategy3.getCounter());
		assertEquals(498, strategy4.getCounter());
	}

	@Test
	void testAddTwiceStrategy() {
		assertThrows(IllegalStateException.class, () -> {
			final StrategyManager manager = new StrategyManager();
			final PlanStrategy s = new PlanStrategyImpl.Builder( new RandomPlanSelector() ).build();
			manager.addStrategy(s, null, 1);
			manager.addStrategy(s, null, 10);
		});
	}

	/**
	 * Tests the removal of strategies. Ensures that after removal, no plan is given to the removed strategy.
	 * Also checks that the removal of strategies not known to the StrategyManager doesn't have any side-effects.
	 *
	 * @author mrieser
	 */
	@Test
	void testRemoveStrategy() {
		MatsimRandom.reset(4711);
		
		Population population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		for (int i = 0; i < 100; i++) {
			Person p = PopulationUtils.getFactory().createPerson(Id.create(i, Person.class));
			population.addPerson(p);
		}

		// setup StrategyManager
		StrategyManager manager = new StrategyManager();
		StrategyCounter strategy1 = new StrategyCounter(new RandomPlanSelector());
		StrategyCounter strategy2 = new StrategyCounter(new RandomPlanSelector());

		manager.addStrategy( strategy1, null, 0.10 );
		manager.addStrategy( strategy2, null, 0.20 );

		// run iteration 1
		manager.run(population, 1, null);

		// ensure all strategies were called
		assertEquals(34, strategy1.getCounter());
		assertEquals(66, strategy2.getCounter());

		strategy1.resetCounter();
		strategy2.resetCounter();

		// remove 2nd strategy
		manager.removeStrategy( strategy2, null );

		// run iteration 2
		manager.run(population, 2, null);

		// ensure only strategy1 got plans to handle
		assertEquals(100, strategy1.getCounter());
		assertEquals(0, strategy2.getCounter());

		strategy1.resetCounter();
		strategy2.resetCounter();

		// try to remove strategy2 again
		manager.removeStrategy( strategy2, null );

		// run iteration 3
		manager.run(population, 3, null);

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
	@Test
	void testOptimisticBehavior() {

		Population population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		Person person = null;
		Plan[] plans = new Plan[10];
		// create a person with 4 unscored plans
		person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		plans[0] = PersonUtils.createAndAddPlan(person, false);
		plans[1] = PersonUtils.createAndAddPlan(person, false);
		plans[1].setScore(Double.valueOf(0.0));
		plans[2] = PersonUtils.createAndAddPlan(person, false);
		plans[3] = PersonUtils.createAndAddPlan(person, false);
		plans[3].setScore(Double.valueOf(-50.0));
		plans[4] = PersonUtils.createAndAddPlan(person, false);
		plans[4].setScore(Double.valueOf(50.0));
		plans[5] = PersonUtils.createAndAddPlan(person, false);
		plans[5].setScore(Double.valueOf(50.0));
		plans[6] = PersonUtils.createAndAddPlan(person, false);
		plans[6].setScore(Double.valueOf(60.0));
		plans[7] = PersonUtils.createAndAddPlan(person, false);
		plans[8] = PersonUtils.createAndAddPlan(person, false);
		plans[8].setScore(Double.valueOf(-10.0));
		plans[9] = PersonUtils.createAndAddPlan(person, false);
		population.addPerson(person);

		StrategyManager manager = new StrategyManager();
		PlanStrategyImpl strategy = new PlanStrategyImpl(new TestPlanSelector());
		manager.addStrategy( strategy, null, 1.0 );

		// in each "iteration", an unscored plans should be selected
		for (int i = 0; i < 4; i++) {
			manager.run(population, i, null);
			Plan plan = person.getSelectedPlan();
			assertNull(plan.getScore(), "plan has not undefined score in iteration " + i);
			plan.setScore(Double.valueOf(i));
		}

		/* There are no more unscored plans now, so in the next "iteration" our
		 * bad PlanSelector should be called. */
		try {
			manager.run(population, 5, null);
			fail("expected UnsupportedOperationException.");
		}
		catch (UnsupportedOperationException expected) {
			// expected Exception catched
		}

	}

	@Test
	void testSetPlanSelectorForRemoval() {
		// init StrategyManager
		StrategyManager manager = new StrategyManager();
		manager.addStrategy( new PlanStrategyImpl(new RandomPlanSelector()), null, 1.0 );

		// init Population
		Person p = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan[] plans = new Plan[7];
		for (int i = 0; i < plans.length; i++) {
			plans[i] = PersonUtils.createAndAddPlan(p, false);
			plans[i].setScore(Double.valueOf(i*10));
		}
		Population pop = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		pop.addPerson(p);

		// run with default settings
		manager.setMaxPlansPerAgent(plans.length - 2);
		manager.run(pop, -1,null);

		assertEquals(5, p.getPlans().size(), "wrong number of plans.");
		// default of StrategyManager is to remove worst plans:
		assertFalse(p.getPlans().contains(plans[0]), "plan should have been removed.");
		assertFalse(p.getPlans().contains(plans[1]), "plan should have been removed.");
		assertTrue(p.getPlans().contains(plans[2]), "plan should not have been removed.");

		// change plan selector for removal and run again
		manager.setPlanSelectorForRemoval(new BestPlanSelector<Plan, Person>());
		manager.setMaxPlansPerAgent(plans.length - 4);
		manager.run(pop, -1,null);

		assertEquals(3, p.getPlans().size(), "wrong number of plans.");
		// default of StrategyManager is to remove worst plans:
		assertFalse(p.getPlans().contains(plans[plans.length - 1]), "plan should have been removed.");
		assertFalse(p.getPlans().contains(plans[plans.length - 2]), "plan should have been removed.");
		assertTrue(p.getPlans().contains(plans[plans.length - 3]), "plan should not have been removed.");
	}

	@Test
	void testGetStrategies() {
		// init StrategyManager
		StrategyManager manager = new StrategyManager();
		PlanStrategy str1 = new PlanStrategyImpl(new RandomPlanSelector());
		PlanStrategy str2 = new PlanStrategyImpl(new RandomPlanSelector());
		PlanStrategy str3 = new PlanStrategyImpl(new RandomPlanSelector());

		manager.addStrategy( str1, null, 1.0 );
		manager.addStrategy( str2, null, 2.0 );
		manager.addStrategy( str3, null, 0.5 );

		List<GenericPlanStrategy<Plan, Person>> strategies = manager.getStrategies( null );
		Assertions.assertEquals(3, strategies.size());

		Assertions.assertEquals(str1, strategies.get(0));
		Assertions.assertEquals(str2, strategies.get(1));
		Assertions.assertEquals(str3, strategies.get(2));
	}

	@Test
	void testGetWeights() {
		// init StrategyManager
		StrategyManager manager = new StrategyManager();
		PlanStrategy str1 = new PlanStrategyImpl(new RandomPlanSelector());
		PlanStrategy str2 = new PlanStrategyImpl(new RandomPlanSelector());
		PlanStrategy str3 = new PlanStrategyImpl(new RandomPlanSelector());

		manager.addStrategy( str1, null, 1.0 );
		manager.addStrategy( str2, null, 2.0 );
		manager.addStrategy( str3, null, 0.5 );

		List<Double> weights = manager.getWeights( null );
		Assertions.assertEquals(3, weights.size());

		Assertions.assertEquals(1.0, weights.get(0), 1e-8);
		Assertions.assertEquals(2.0, weights.get(1), 1e-8);
		Assertions.assertEquals(0.5, weights.get(2), 1e-8);
	}

	@Test
	void testGetWeights_ChangeRequests() {
		// init StrategyManager
		StrategyManager manager = new StrategyManager();
		PlanStrategy str1 = new PlanStrategyImpl(new RandomPlanSelector());
		PlanStrategy str2 = new PlanStrategyImpl(new RandomPlanSelector());
		PlanStrategy str3 = new PlanStrategyImpl(new RandomPlanSelector());

		manager.addStrategy( str1, null, 1.0 );
		manager.addStrategy( str2, null, 2.0 );
		manager.addStrategy( str3, null, 0.5 );

		manager.addChangeRequest( 5, str2, null, 3.0 );
		manager.addChangeRequest( 10, str3, null, 1.0 );

		Population pop = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
	
		manager.run(pop, 1, null);

		List<Double> weights = manager.getWeights( null );
		Assertions.assertEquals(3, weights.size());
		
		Assertions.assertEquals(1.0, weights.get(0), 1e-8);
		Assertions.assertEquals(2.0, weights.get(1), 1e-8);
		Assertions.assertEquals(0.5, weights.get(2), 1e-8);

		manager.run(pop, 5, null);

		weights = manager.getWeights( null );
		Assertions.assertEquals(3, weights.size());
		
		Assertions.assertEquals(1.0, weights.get(0), 1e-8);
		Assertions.assertEquals(3.0, weights.get(1), 1e-8);
		Assertions.assertEquals(0.5, weights.get(2), 1e-8);

		manager.run(pop, 10, null);

		weights = manager.getWeights( null );
		Assertions.assertEquals(3, weights.size());
		
		Assertions.assertEquals(1.0, weights.get(0), 1e-8);
		Assertions.assertEquals(3.0, weights.get(1), 1e-8);
		Assertions.assertEquals(1.0, weights.get(2), 1e-8);
	}
	
	/**
	 * A simple extension to the PlanStrategy which counts how often it was
	 * called.
	 *
	 * @author mrieser
	 */
	static private class StrategyCounter implements PlanStrategy {
		
		private PlanStrategyImpl planStrategyDelegate = null ;

		private int counter = 0;

		protected StrategyCounter(final PlanSelector<Plan, Person> selector) {
			planStrategyDelegate = new PlanStrategyImpl( selector ) ;
		}

		@Override
		public void run(final HasPlansAndId<Plan, Person> person) {
			this.counter++;
			planStrategyDelegate.run(person);
		}

		public int getCounter() {
			return this.counter;
		}

		protected void resetCounter() {
			this.counter = 0;
		}

		public void addStrategyModule(PlanStrategyModule module) {
			planStrategyDelegate.addStrategyModule(module);
		}

		public int getNumberOfStrategyModules() {
			return planStrategyDelegate.getNumberOfStrategyModules();
		}

		@Override
		public void init(ReplanningContext replanningContext) {
			planStrategyDelegate.init(replanningContext);
		}

		@Override
		public void finish() {
			planStrategyDelegate.finish();
		}

		@Override
		public String toString() {
			return planStrategyDelegate.toString();
		}

	}

	/**
	 * A simple PlanSelector that throws an UnsupportedOperationException whenever
	 * it should select a plan.
	 *
	 * @author mrieser
	 */
	static private class TestPlanSelector implements PlanSelector<Plan, Person> {

		public TestPlanSelector() {
		}
		@Override
		public Plan selectPlan(final HasPlansAndId<Plan, Person> person) {
			throw new UnsupportedOperationException();
		}

	}

}
