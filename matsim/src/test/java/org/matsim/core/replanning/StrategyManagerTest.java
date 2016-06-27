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

import org.junit.Assert;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.HasPlansAndId;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
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

import static org.junit.Assert.*;

public class StrategyManagerTest {

	/**
	 * This method tests, if adding strategies and strategyRequest get correctly
	 * executed and if, after changes are performed, still all required strategies
	 * are called according to their weights.
	 *
	 * @author mrieser
	 */
	@Test
	public void testChangeRequests() {
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

		manager.addStrategyForDefaultSubpopulation(strategy1, 0.10);
		manager.addStrategyForDefaultSubpopulation(strategy2, 0.20);
		manager.addStrategyForDefaultSubpopulation(strategy3, 0.30);
		manager.addStrategyForDefaultSubpopulation(strategy4, 0.40);


		// add ChangeRequests
		manager.addChangeRequestForDefaultSubpopulation(11, strategy2, 0.0);
		manager.addChangeRequestForDefaultSubpopulation(11, strategy3, 0.0);
		manager.addChangeRequestForDefaultSubpopulation(12, strategy4, 0.1);

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

	@Test( expected=IllegalStateException.class )
	public void testAddTwiceStrategy() {
		final StrategyManager manager = new StrategyManager();
		final PlanStrategy s = new PlanStrategyImpl.Builder( new RandomPlanSelector() ).build();
		manager.addStrategy( s , null , 1 );
		manager.addStrategy( s , null , 10 );
	}

	/**
	 * Tests the removal of strategies. Ensures that after removal, no plan is given to the removed strategy.
	 * Also checks that the removal of strategies not known to the StrategyManager doesn't have any side-effects.
	 *
	 * @author mrieser
	 */
	@Test
	public void testRemoveStrategy() {
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

		manager.addStrategyForDefaultSubpopulation(strategy1, 0.10);
		manager.addStrategyForDefaultSubpopulation(strategy2, 0.20);

		// run iteration 1
		manager.run(population, 1, null);

		// ensure all strategies were called
		assertEquals(34, strategy1.getCounter());
		assertEquals(66, strategy2.getCounter());

		strategy1.resetCounter();
		strategy2.resetCounter();

		// remove 2nd strategy
		manager.removeStrategyForDefaultSubpopulation(strategy2);

		// run iteration 2
		manager.run(population, 2, null);

		// ensure only strategy1 got plans to handle
		assertEquals(100, strategy1.getCounter());
		assertEquals(0, strategy2.getCounter());

		strategy1.resetCounter();
		strategy2.resetCounter();

		// try to remove strategy2 again
		manager.removeStrategyForDefaultSubpopulation(strategy2);

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
	public void testOptimisticBehavior() {

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
		manager.addStrategyForDefaultSubpopulation(strategy, 1.0);

		// in each "iteration", an unscored plans should be selected
		for (int i = 0; i < 4; i++) {
			manager.run(population, i, null);
			Plan plan = person.getSelectedPlan();
			assertNull("plan has not undefined score in iteration " + i, plan.getScore());
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
	public void testSetPlanSelectorForRemoval() {
		// init StrategyManager
		StrategyManager manager = new StrategyManager();
		manager.addStrategyForDefaultSubpopulation(new PlanStrategyImpl(new RandomPlanSelector()), 1.0);

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
		manager.run(pop, null);

		assertEquals("wrong number of plans.", 5, p.getPlans().size());
		// default of StrategyManager is to remove worst plans:
		assertFalse("plan should have been removed.", p.getPlans().contains(plans[0]));
		assertFalse("plan should have been removed.", p.getPlans().contains(plans[1]));
		assertTrue("plan should not have been removed.", p.getPlans().contains(plans[2]));

		// change plan selector for removal and run again
		manager.setPlanSelectorForRemoval(new BestPlanSelector<Plan, Person>());
		manager.setMaxPlansPerAgent(plans.length - 4);
		manager.run(pop, null);

		assertEquals("wrong number of plans.", 3, p.getPlans().size());
		// default of StrategyManager is to remove worst plans:
		assertFalse("plan should have been removed.", p.getPlans().contains(plans[plans.length - 1]));
		assertFalse("plan should have been removed.", p.getPlans().contains(plans[plans.length - 2]));
		assertTrue("plan should not have been removed.", p.getPlans().contains(plans[plans.length - 3]));
	}

	@Test
	public void testGetStrategies() {
		// init StrategyManager
		StrategyManager manager = new StrategyManager();
		PlanStrategy str1 = new PlanStrategyImpl(new RandomPlanSelector());
		PlanStrategy str2 = new PlanStrategyImpl(new RandomPlanSelector());
		PlanStrategy str3 = new PlanStrategyImpl(new RandomPlanSelector());
		
		manager.addStrategyForDefaultSubpopulation(str1, 1.0);
		manager.addStrategyForDefaultSubpopulation(str2, 2.0);
		manager.addStrategyForDefaultSubpopulation(str3, 0.5);
		
		List<GenericPlanStrategy<Plan, Person>> strategies = manager.getStrategiesOfDefaultSubpopulation();
		Assert.assertEquals(3, strategies.size());

		Assert.assertEquals(str1, strategies.get(0));
		Assert.assertEquals(str2, strategies.get(1));
		Assert.assertEquals(str3, strategies.get(2));
	}
	
	@Test
	public void testGetWeights() {
		// init StrategyManager
		StrategyManager manager = new StrategyManager();
		PlanStrategy str1 = new PlanStrategyImpl(new RandomPlanSelector());
		PlanStrategy str2 = new PlanStrategyImpl(new RandomPlanSelector());
		PlanStrategy str3 = new PlanStrategyImpl(new RandomPlanSelector());
		
		manager.addStrategyForDefaultSubpopulation(str1, 1.0);
		manager.addStrategyForDefaultSubpopulation(str2, 2.0);
		manager.addStrategyForDefaultSubpopulation(str3, 0.5);
		
		List<Double> weights = manager.getWeightsOfDefaultSubpopulation();
		Assert.assertEquals(3, weights.size());

		Assert.assertEquals(1.0, weights.get(0), 1e-8);
		Assert.assertEquals(2.0, weights.get(1), 1e-8);
		Assert.assertEquals(0.5, weights.get(2), 1e-8);
	}
	
	@Test
	public void testGetWeights_ChangeRequests() {
		// init StrategyManager
		StrategyManager manager = new StrategyManager();
		PlanStrategy str1 = new PlanStrategyImpl(new RandomPlanSelector());
		PlanStrategy str2 = new PlanStrategyImpl(new RandomPlanSelector());
		PlanStrategy str3 = new PlanStrategyImpl(new RandomPlanSelector());
		
		manager.addStrategyForDefaultSubpopulation(str1, 1.0);
		manager.addStrategyForDefaultSubpopulation(str2, 2.0);
		manager.addStrategyForDefaultSubpopulation(str3, 0.5);

		manager.addChangeRequestForDefaultSubpopulation(5, str2, 3.0);
		manager.addChangeRequestForDefaultSubpopulation(10, str3, 1.0);

		Population pop = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
	
		manager.run(pop, 1, null);
		
		List<Double> weights = manager.getWeightsOfDefaultSubpopulation();
		Assert.assertEquals(3, weights.size());
		
		Assert.assertEquals(1.0, weights.get(0), 1e-8);
		Assert.assertEquals(2.0, weights.get(1), 1e-8);
		Assert.assertEquals(0.5, weights.get(2), 1e-8);

		manager.run(pop, 5, null);
		
		weights = manager.getWeightsOfDefaultSubpopulation();
		Assert.assertEquals(3, weights.size());
		
		Assert.assertEquals(1.0, weights.get(0), 1e-8);
		Assert.assertEquals(3.0, weights.get(1), 1e-8);
		Assert.assertEquals(0.5, weights.get(2), 1e-8);

		manager.run(pop, 10, null);
		
		weights = manager.getWeightsOfDefaultSubpopulation();
		Assert.assertEquals(3, weights.size());
		
		Assert.assertEquals(1.0, weights.get(0), 1e-8);
		Assert.assertEquals(3.0, weights.get(1), 1e-8);
		Assert.assertEquals(1.0, weights.get(2), 1e-8);
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
