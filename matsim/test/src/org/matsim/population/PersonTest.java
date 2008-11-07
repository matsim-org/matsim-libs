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

package org.matsim.population;

import org.matsim.basic.v01.IdImpl;
import org.matsim.testcases.MatsimTestCase;

/**
 *
 * @author cello
 */
public class PersonTest extends MatsimTestCase {

	/**
	 * Tests {@link org.matsim.population.Person#removeWorstPlans(int)} when all
	 * plans have the type <code>null</code>.
	 *
	 * @author mrieser
	 */
	public void testRemoveWorstPlans_nullType() {
		Person person = new PersonImpl(new IdImpl("1"));

		Plan plan1 = new Plan(person);
		plan1.setScore(15.0);
		Plan plan2 = new Plan(person);
		plan2.setScore(22.0);
		Plan plan3 = new Plan(person);
		Plan plan4 = new Plan(person);
		plan4.setScore(1.0);
		Plan plan5 = new Plan(person);
		plan5.setScore(18.0);
		person.addPlan(plan1);
		person.addPlan(plan2);
		person.addPlan(plan3);
		person.addPlan(plan4);
		person.addPlan(plan5);

		assertEquals("test we have all plans we want", 5, person.getPlans().size());

		person.removeWorstPlans(6);
		assertEquals("test that no plans are removed if maxSize > plans.size()", 5, person.getPlans().size());

		person.removeWorstPlans(5);
		assertEquals("test that no plans are removed if maxSize == plans.size()", 5, person.getPlans().size());

		person.removeWorstPlans(4);
		assertEquals("test that a plan was removed", 4, person.getPlans().size());
		assertFalse("test that plan with undefined score was removed.", person.getPlans().contains(plan3));

		person.removeWorstPlans(3);
		assertEquals("test that a plan was removed", 3, person.getPlans().size());
		assertFalse("test that the plan with minimal score was removed", person.getPlans().contains(plan4));

		person.removeWorstPlans(1);
		assertEquals("test that two plans were removed", 1, person.getPlans().size());
		assertTrue("test that the plan left has highest score", person.getPlans().contains(plan2));
	}

	/**
	 * Tests {@link org.matsim.population.Person#removeWorstPlans(int)} when the
	 * plans have different types set.
	 *
	 * @author mrieser
	 */
	public void testRemoveWorstPlans_withTypes() {
		/* The used plans, ordered by score:
		 * plan2: b, 22.0
		 * plan6: b, 21.0
		 * plan5: a, 18.0
		 * plan1: a, 15.0
		 * plan4: b,  1.0
		 * plan3: a, null
		 */
		Person person = new PersonImpl(new IdImpl("1"));

		Plan plan1 = new Plan(person);
		plan1.setScore(15.0);
		Plan plan2 = new Plan(person);
		plan2.setScore(22.0);
		Plan plan3 = new Plan(person);
		Plan plan4 = new Plan(person);
		plan4.setScore(1.0);
		Plan plan5 = new Plan(person);
		plan5.setScore(18.0);
		Plan plan6 = new Plan(person);
		plan6.setScore(21.0);

		plan1.setType(Plan.Type.CAR);
		plan2.setType(Plan.Type.PT);
		plan3.setType(Plan.Type.CAR);
		plan4.setType(Plan.Type.PT);
		plan5.setType(Plan.Type.CAR);
		plan6.setType(Plan.Type.PT);
		person.addPlan(plan1);
		person.addPlan(plan2);
		person.addPlan(plan3);
		person.addPlan(plan4);
		person.addPlan(plan5);
		person.addPlan(plan6);

		assertEquals("test we have all plans we want", 6, person.getPlans().size());

		person.removeWorstPlans(4);
		assertEquals("test that two plans were removed", 4, person.getPlans().size());
		assertFalse("test that plan with undefined score was removed.", person.getPlans().contains(plan3));
		assertFalse("test that plan with worst score was removed.", person.getPlans().contains(plan4));

		person.removeWorstPlans(2);
		assertEquals("test that two plans were removed", 2, person.getPlans().size());
		assertFalse("test that the plan with worst score was removed", person.getPlans().contains(plan1));
		assertTrue("test that the now only plan of type a was not removed", person.getPlans().contains(plan5));
		assertFalse("test that the plan with the 2nd-worst score was removed", person.getPlans().contains(plan6));

		person.removeWorstPlans(1);
		assertEquals("test that no plans were removed", 2, person.getPlans().size());
		assertTrue("test that the plan with highest score of type a was not removed", person.getPlans().contains(plan5));
		assertTrue("test that the plan with highest score of type b was not removed", person.getPlans().contains(plan2));
	}

	/**
	 * Tests that after a call to {@link org.matsim.population.Person#removeWorstPlans(int)}
	 * the person still has a selected plan, even when the previously selected plan was
	 * the one with the worst score.
	 *
	 * @author mrieser
	 */
	public void testRemoveWorstPlans_selectedPlan() {
		Person person = new PersonImpl(new IdImpl("1"));

		Plan plan1 = new Plan(person);
		plan1.setScore(15.0);
		Plan plan2 = new Plan(person);
		plan2.setScore(22.0);
		Plan plan3 = new Plan(person);
		Plan plan4 = new Plan(person);
		plan4.setScore(1.0);
		Plan plan5 = new Plan(person);
		plan5.setScore(18.0);
		Plan plan6 = new Plan(person);
		plan6.setScore(21.0);
		person.addPlan(plan1);
		person.addPlan(plan2);
		person.addPlan(plan3);
		person.addPlan(plan4);
		person.addPlan(plan5);
		person.addPlan(plan6);
		person.setSelectedPlan(plan1);

		// test we have the expected selected plan
		assertEquals(plan1, person.getSelectedPlan());
		// remove one plan, that is not selected
		person.removeWorstPlans(5);
		// the selected plan shouldn't have changed
		assertEquals(plan1, person.getSelectedPlan());
		// remove more plans, now with the selected plan being removed
		person.removeWorstPlans(3);
		// test that the previously selected plan is no longer selected
		assertFalse("plan should no longer be selected!", plan1.isSelected());
		assertNotSame("plan1 should no longer be the selected plan!", plan1, person.getSelectedPlan());
		// test that we have a selected plan again!
		assertNotNull("person has no selected plan!", person.getSelectedPlan());
		// test that the selected plan is one of the remaining plans
		assertTrue((person.getSelectedPlan() == plan2) || (person.getSelectedPlan() == plan5) || (person.getSelectedPlan() == plan6));
		// now furter remove plans, until there is only one left
		person.removeWorstPlans(1);
		assertEquals(plan2, person.getSelectedPlan());
	}

	/**
	 * @author mrieser
	 */
	public void testGetRandomUnscoredPlan() {
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

		// now test if we all for plans without score are returned
		Plan plan = person.getRandomUnscoredPlan();
		assertTrue(plan.hasUndefinedScore());
		plan.setScore(1.0);
		plan = person.getRandomUnscoredPlan();
		assertTrue(plan.hasUndefinedScore());
		plan.setScore(2.0);
		plan = person.getRandomUnscoredPlan();
		assertTrue(plan.hasUndefinedScore());
		plan.setScore(3.0);
		plan = person.getRandomUnscoredPlan();
		assertTrue(plan.hasUndefinedScore());
		plan.setScore(4.0);
		plan = person.getRandomUnscoredPlan();
		assertNull(plan);
		for (int i = 0; i < plans.length; i++) {
			assertFalse(plans[i].hasUndefinedScore());
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

}
