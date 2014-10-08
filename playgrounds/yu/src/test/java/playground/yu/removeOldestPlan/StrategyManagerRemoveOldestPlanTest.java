/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2012 by the members listed in the COPYING,        *
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
package playground.yu.removeOldestPlan;

import java.util.List;

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.replanning.PlanStrategyImpl;
import org.matsim.core.replanning.StrategyManager;
import org.matsim.core.replanning.selectors.RandomPlanSelector;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;

import playground.yu.replanning.StrategyManagerWithRemoveOldestPlan;

/**
 * testcase for the function in class
 * {@code playground.yu.replanning.StrategyManagerWithRemoveOldestPlan}
 * 
 * @author yu
 * 
 */
public class StrategyManagerRemoveOldestPlanTest extends MatsimTestCase {
	private static class ROPControler extends Controler {

		public ROPControler(String[] args) {
			super(args);
		}

	}

	/**
	 * test remove oldest Plan, who has been very long not used i.e. with
	 * smallest index in choice set, at the same Time, the "makeSelectedYoung"
	 * can also be tested, some codes are copied from
	 * {@code
	 * org.matsim.core.replanning.StrategyManagerTest.
	 * testSetPlanSelectorForRemoval()}
	 */
	public void testRemoveOldestPlan() {
		// init StrategyManager
		StrategyManager manager = new StrategyManager();
		manager.addStrategyForDefaultSubpopulation(new PlanStrategyImpl(new RandomPlanSelector()), 1.0);

		// init Population
		PersonImpl p = new PersonImpl(Id.create(1, Person.class));
		PlanImpl[] plans = new PlanImpl[7];
		for (int i = 0; i < plans.length; i++) {
			plans[i] = p.createAndAddPlan(false);
			plans[i].setScore(Double.valueOf(i * 10));
		}
		Population pop = ((ScenarioImpl) ScenarioUtils
				.createScenario(ConfigUtils.createConfig())).getPopulation();
		pop.addPerson(p);
		{
			// run with default settings
			manager.setMaxPlansPerAgent(plans.length - 2);
			manager.run(pop, null);

			List<Plan> planList = p.getPlans();
			assertEquals("wrong number of plans.", 5, planList.size());
			// default of StrategyManager is to remove worst plans:
			assertFalse("plan should have been removed.",
					planList.contains(plans[0]));
			assertFalse("plan should have been removed.",
					planList.contains(plans[1]));
			for (int i = 2; i < plans.length; i++) {
				assertTrue("plan should not have been removed.",
						planList.contains(plans[i]));
			}
			for (Plan plan : planList) {
				System.out.println("index\t" + planList.indexOf(plan)
						+ "\tplan :\t" + plan);
			}
		}
		// init StrategyManagerWithRemoveOldestPlan
		manager = new StrategyManagerWithRemoveOldestPlan();
		manager.addStrategyForDefaultSubpopulation(new PlanStrategyImpl(new RandomPlanSelector()), 1.0);

		// init Population
		p = new PersonImpl(Id.create(1, Person.class));
		plans = new PlanImpl[7];
		for (int i = 0; i < plans.length; i++) {
			plans[i] = p.createAndAddPlan(false);
			plans[i].setScore(Double.valueOf((7 - i) * 10));
			// p.setSelectedPlan(plans[i]);
		}
		pop.getPersons().clear();
		pop.addPerson(p);

		{
			// run with removeOldestPlan settings
			manager.setMaxPlansPerAgent(plans.length - 3);
			manager.run(pop, null);

			List<Plan> planList = p.getPlans();
			assertEquals("wrong number of plans.", 4, planList.size());
			// by StrategyManagerWithRemoveOldestPlan is to remove oldest plans:
			assertFalse("plan should have been removed.",
					planList.contains(plans[0]/*
											 * score 70, best plan is gonna be
											 * deleted
											 */));
			assertFalse("plan should have been removed.",
					planList.contains(plans[1]/*
											 * score 60, second best plan is
											 * gonna be deleted
											 */));
			assertFalse("plan should have been removed.",
					planList.contains(plans[2]/*
											 * score 50, third best plan is
											 * gonna be deleted
											 */));
			for (int i = 3; i < plans.length; i++) {
				assertTrue("plan should have not been removed.",
						planList.contains(plans[3]));
			}
			assertTrue("the last(newest) plan should be a selected Plan",
					planList.get(manager.getMaxPlansPerAgent() - 1)
							.isSelected());/*
											 * makeSelectedYoung is being also
											 * tested
											 */
			for (Plan plan : planList) {
				System.out.println("index\t" + planList.indexOf(plan)
						+ "\tplan :\t" + plan);
			}
		}
	}
}
