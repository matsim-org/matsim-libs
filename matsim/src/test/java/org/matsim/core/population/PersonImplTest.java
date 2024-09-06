/* *********************************************************************** *
 * project: org.matsim.*
 * PersonTest.java
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

package org.matsim.core.population;

import static org.junit.jupiter.api.Assertions.*;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.replanning.selectors.BestPlanSelector;
import org.matsim.core.replanning.selectors.RandomUnscoredPlanSelector;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class PersonImplTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	private final static Logger log = LogManager.getLogger(PersonImplTest.class);

	/**
	 * @author mrieser
	 */
	@Test
	void testGetRandomUnscoredPlan() {
		Population population = ScenarioUtils.createScenario(ConfigUtils.createConfig()).getPopulation();
		Person person = null;
		Plan[] plans = new Plan[10];
		// create a person with 4 unscored plans
		person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		plans[0] = PersonUtils.createAndAddPlan(person, false);
		plans[1] = PersonUtils.createAndAddPlan(person, false);
		plans[1].setScore(0.0);
		plans[2] = PersonUtils.createAndAddPlan(person, false);
		plans[3] = PersonUtils.createAndAddPlan(person, false);
		plans[3].setScore(-50.0);
		plans[4] = PersonUtils.createAndAddPlan(person, false);
		plans[4].setScore(50.0);
		plans[5] = PersonUtils.createAndAddPlan(person, false);
		plans[5].setScore(50.0);
		plans[6] = PersonUtils.createAndAddPlan(person, false);
		plans[6].setScore(60.0);
		plans[7] = PersonUtils.createAndAddPlan(person, false);
		plans[8] = PersonUtils.createAndAddPlan(person, false);
		plans[8].setScore(-10.0);
		plans[9] = PersonUtils.createAndAddPlan(person, false);
		population.addPerson(person);

		// now test if we all for plans without score are returned
		Plan plan = new RandomUnscoredPlanSelector<Plan, Person>().selectPlan(person);
		assertNull(plan.getScore());
		plan.setScore(1.0);
		plan = new RandomUnscoredPlanSelector<Plan, Person>().selectPlan(person);
		assertNull(plan.getScore());
		plan.setScore(2.0);
		plan = new RandomUnscoredPlanSelector<Plan, Person>().selectPlan(person);
		assertNull(plan.getScore());
		plan.setScore(3.0);
		plan = new RandomUnscoredPlanSelector<Plan, Person>().selectPlan(person);
		assertNull(plan.getScore());
		plan.setScore(4.0);
		plan = new RandomUnscoredPlanSelector<Plan, Person>().selectPlan(person);
		assertNull(plan);
		for (int i = 0; i < plans.length; i++) {
			assertNotNull(plans[i].getScore());
		}
	}

	/**
	 * @author mrieser
	 */
	@Test
	void testRemoveUnselectedPlans() {
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		PersonUtils.createAndAddPlan(person, false);
		PersonUtils.createAndAddPlan(person, false);
		Plan selPlan = PersonUtils.createAndAddPlan(person, true);
		PersonUtils.createAndAddPlan(person, false);

		assertEquals(4, person.getPlans().size(), "person should have 4 plans.");

		PersonUtils.removeUnselectedPlans(person);

		assertEquals(1, person.getPlans().size(), "person should have 1 plan.");
		assertEquals(selPlan, person.getPlans().get(0), "remaining plan should be selPlan.");
	}

	@Test
	void testRemovePlan() {
		Person person = PopulationUtils.getFactory().createPerson(Id.create(5, Person.class));
		Plan p1 = PersonUtils.createAndAddPlan(person, false);
		Plan p2 = PersonUtils.createAndAddPlan(person, true);
		Plan p3 = PersonUtils.createAndAddPlan(person, false);
		Plan p4 = PersonUtils.createAndAddPlan(person, false);
		Plan p5 = PopulationUtils.createPlan(null);

		assertEquals(4, person.getPlans().size(), "wrong number of plans.");
		assertEquals(p2, person.getSelectedPlan(), "expected different selected plan.");
		assertTrue(person.removePlan(p3));
		assertEquals(3, person.getPlans().size(), "wrong number of plans.");
		assertEquals(p2, person.getSelectedPlan(), "expected different selected plan.");
		assertFalse(person.removePlan(p5));
		assertEquals(3, person.getPlans().size(), "wrong number of plans.");
		assertTrue(person.removePlan(p2));
		assertEquals(2, person.getPlans().size(), "wrong number of plans.");
		assertNotSame(p2, person.getSelectedPlan(), "removed plan still set as selected.");
		assertFalse(person.removePlan(p2), "plan cannot be removed twice.");
		assertEquals(2, person.getPlans().size(), "wrong number of plans.");
		assertTrue(person.removePlan(p1));
		assertTrue(person.removePlan(p4));
		assertEquals(0, person.getPlans().size(), "wrong number of plans.");
	}

	@Test
	void testSetSelectedPlan() {
		Person person = PopulationUtils.getFactory().createPerson(Id.create(11, Person.class));
		Plan p1 = PersonUtils.createAndAddPlan(person, false);
		assertEquals(p1, person.getSelectedPlan());
		Plan p2 = PersonUtils.createAndAddPlan(person, false);
		assertEquals(p1, person.getSelectedPlan());
		Plan p3 = PersonUtils.createAndAddPlan(person, true);
		assertEquals(p3, person.getSelectedPlan());
		person.setSelectedPlan(p2);
		assertEquals(p2, person.getSelectedPlan());
		Plan p4 = PopulationUtils.createPlan(null);
		try {
			person.setSelectedPlan(p4);
			fail("expected Exception when setting a plan as selected that is not part of person.");
		} catch (IllegalStateException e) {
			log.info("catched expected exception: " + e.getMessage());
		}
	}

	/**
	 * @author mrieser
	 */
	@Test
	void testGetBestPlan() {
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan p1 = PopulationUtils.createPlan();
		p1.setScore(90.0);
		Plan p2 = PopulationUtils.createPlan();
		p2.setScore(89.0);
		person.addPlan(p1);
		person.addPlan(p2);
		Plan p = new BestPlanSelector<Plan, Person>().selectPlan(person);
		assertEquals(p1, p);
	}

	/**
	 * @author mrieser
	 */
	@Test
	void testGetBestPlan_multipleBest() {
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan p1 = PopulationUtils.createPlan();
		p1.setScore(11.0);
		Plan p2 = PopulationUtils.createPlan();
		p2.setScore(5.0);
		Plan p3 = PopulationUtils.createPlan();
		p3.setScore(11.0);
		person.addPlan(p1);
		person.addPlan(p2);
		person.addPlan(p3);
		Plan p = new BestPlanSelector<Plan, Person>().selectPlan(person);
		assertTrue(p == p1 || p == p3);
	}

	/**
	 * @author mrieser
	 */
	@Test
	void testGetBestPlan_oneWithoutScore() {
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan p1 = PopulationUtils.createPlan();
		Plan p2 = PopulationUtils.createPlan();
		p2.setScore(80.0);
		person.addPlan(p1);
		person.addPlan(p2);
		Plan p = new BestPlanSelector<Plan, Person>().selectPlan(person);
		assertEquals(p2, p);
	}

	/**
	 * @author mrieser
	 */
	@Test
	void testGetBestPlan_allWithoutScore() {
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan p1 = PopulationUtils.createPlan();
		Plan p2 = PopulationUtils.createPlan();
		person.addPlan(p1);
		person.addPlan(p2);
		Plan p = new BestPlanSelector<Plan, Person>().selectPlan(person);
		assertTrue(p == p1 || p == p2);
	}
}
