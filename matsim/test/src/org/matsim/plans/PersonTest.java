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

package org.matsim.plans;

import org.matsim.plans.Person;
import org.matsim.plans.Plan;
import org.matsim.testcases.MatsimTestCase;

public class PersonTest extends MatsimTestCase {

	/**
	 * Tests {@link org.matsim.plans.Person#removeWorstPlans(int)} when all
	 * plans have the type <code>null</code>.
	 *
	 * @author mrieser
	 */
	public void testRemoveWorstPlans_nullType() {
		Person person = new Person("1", "m", "35", "yes", "yes", "yes");

		Plan plan1 = new Plan("15.0", null, person);
		Plan plan2 = new Plan("22.0", null, person);
		Plan plan3 = new Plan(null, null, person);
		Plan plan4 = new Plan("1.0", null, person);
		Plan plan5 = new Plan("18.0", null, person);
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
	 * Tests {@link org.matsim.plans.Person#removeWorstPlans(int)} when the
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
		Person person = new Person("1", "m", "35", "yes", "yes", "yes");

		Plan plan1 = new Plan("15.0", null, person);
		plan1.setType("a");
		Plan plan2 = new Plan("22.0", null, person);
		plan2.setType("b");
		Plan plan3 = new Plan(null, null, person);
		plan3.setType("a");
		Plan plan4 = new Plan("1.0", null, person);
		plan4.setType("b");
		Plan plan5 = new Plan("18.0", null, person);
		plan5.setType("a");
		Plan plan6 = new Plan("21.0", null, person);
		plan6.setType("b");
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

}
