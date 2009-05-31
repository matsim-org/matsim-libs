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

import org.apache.log4j.Logger;
import org.matsim.core.api.population.Person;
import org.matsim.core.api.population.Plan;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class PersonImplTest extends MatsimTestCase {

	private final static Logger log = Logger.getLogger(PersonImplTest.class);

	/**
	 * @author mrieser
	 */
	public void testGetRandomUnscoredPlan() {
		PopulationImpl population = new PopulationImpl();
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
		population.getPersons().put(person.getId(), person);

		// now test if we all for plans without score are returned
		Plan plan = person.getRandomUnscoredPlan();
		assertNull(plan.getScore());
		plan.setScore(1.0);
		plan = person.getRandomUnscoredPlan();
		assertNull(plan.getScore());
		plan.setScore(2.0);
		plan = person.getRandomUnscoredPlan();
		assertNull(plan.getScore());
		plan.setScore(3.0);
		plan = person.getRandomUnscoredPlan();
		assertNull(plan.getScore());
		plan.setScore(4.0);
		plan = person.getRandomUnscoredPlan();
		assertNull(plan);
		for (int i = 0; i < plans.length; i++) {
			assertNotNull(plans[i].getScore());
		}
	}

	/**
	 * @author mrieser
	 */
	public void testRemoveUnselectedPlans() {
		Person person = new PersonImpl(new IdImpl(1));
		person.createPlan(false);
		person.createPlan(false);
		Plan selPlan = person.createPlan(true);
		person.createPlan(false);

		assertEquals("person should have 4 plans.", 4, person.getPlans().size());

		person.removeUnselectedPlans();

		assertEquals("person should have 1 plan.", 1, person.getPlans().size());
		assertEquals("remaining plan should be selPlan.", selPlan, person.getPlans().get(0));
	}
	
	public void testRemovePlan() {
		Person person = new PersonImpl(new IdImpl(5));
		Plan p1 = person.createPlan(false);
		Plan p2 = person.createPlan(true);
		Plan p3 = person.createPlan(false);
		Plan p4 = person.createPlan(false);
		Plan p5 = new PlanImpl(null);
		
		assertEquals("wrong number of plans.", 4, person.getPlans().size());
		assertEquals("expected different selected plan.", p2, person.getSelectedPlan());
		assertTrue(person.removePlan(p3));
		assertEquals("wrong number of plans.", 3, person.getPlans().size());
		assertEquals("expected different selected plan.", p2, person.getSelectedPlan());
		assertFalse(person.removePlan(p5));
		assertEquals("wrong number of plans.", 3, person.getPlans().size());
		assertTrue(person.removePlan(p2));
		assertEquals("wrong number of plans.", 2, person.getPlans().size());
		assertNotSame("removed plan still set as selected.", p2, person.getSelectedPlan());
		assertFalse("plan cannot be removed twice.", person.removePlan(p2));
		assertEquals("wrong number of plans.", 2, person.getPlans().size());
		assertTrue(person.removePlan(p1));
		assertTrue(person.removePlan(p4));
		assertEquals("wrong number of plans.", 0, person.getPlans().size());
	}
	
	public void testSetSelectedPlan() {
		Person person = new PersonImpl(new IdImpl(11));
		Plan p1 = person.createPlan(false);
		assertEquals(p1, person.getSelectedPlan());
		Plan p2 = person.createPlan(false);
		assertEquals(p1, person.getSelectedPlan());
		Plan p3 = person.createPlan(true);
		assertEquals(p3, person.getSelectedPlan());
		person.setSelectedPlan(p2);
		assertEquals(p2, person.getSelectedPlan());
		Plan p4 = new PlanImpl(null);
		try {
			person.setSelectedPlan(p4);
			fail("expected Exception when setting a plan as selected that is not part of person.");
		} catch (IllegalStateException e) {
			log.info("catched expected exception: " + e.getMessage());
		}
	}

}
