/* *********************************************************************** *
 * project: org.matsim.*
 * WorstPlanRespectingPlanTypeSelector.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.core.replanning.selectors;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PopulationUtils;

public class WorstPlanForRemovalSelectorTest extends AbstractPlanSelectorTest {

	@Override
	protected PlanSelector<Plan, Person> getPlanSelector() {
		return new WorstPlanForRemovalSelector();
	}

	/**
	 * Tests plan selection when all plans have the type <code>null</code>.
	 *
	 * @author mrieser
	 */
	@Test
	void testRemoveWorstPlans_nullType() {
		PlanSelector<Plan, Person> selector = getPlanSelector();
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));

		Plan plan1 = PopulationUtils.createPlan(person);
		plan1.setScore(15.0);
		Plan plan2 = PopulationUtils.createPlan(person);
		plan2.setScore(22.0);
		Plan plan3 = PopulationUtils.createPlan(person);
		Plan plan4 = PopulationUtils.createPlan(person);
		plan4.setScore(1.0);
		Plan plan5 = PopulationUtils.createPlan(person);
		plan5.setScore(18.0);
		person.addPlan(plan1);
		person.addPlan(plan2);
		person.addPlan(plan3);
		person.addPlan(plan4);
		person.addPlan(plan5);

		assertEquals(5, person.getPlans().size(), "test we have all plans we want");

		person.getPlans().remove(selector.selectPlan(person));
		assertEquals(4, person.getPlans().size(), "test that a plan was removed");
		assertFalse(person.getPlans().contains(plan3), "test that plan with undefined score was removed.");

		person.getPlans().remove(selector.selectPlan(person));
		assertEquals(3, person.getPlans().size(), "test that a plan was removed");
		assertFalse(person.getPlans().contains(plan4), "test that the plan with minimal score was removed");

		person.getPlans().remove(selector.selectPlan(person));
		person.getPlans().remove(selector.selectPlan(person));
		assertEquals(1, person.getPlans().size(), "test that two plans were removed");
		assertTrue(person.getPlans().contains(plan2), "test that the plan left has highest score");
	}

	/**
	 * Tests plan selection when the plans have different types set.
	 *
	 * @author mrieser
	 */
	@Test
	void testRemoveWorstPlans_withTypes() {
		PlanSelector<Plan, Person> selector = getPlanSelector();
		/* The used plans, ordered by score:
		 * plan2: b, 22.0
		 * plan6: b, 21.0
		 * plan5: a, 18.0
		 * plan1: a, 15.0
		 * plan4: b,  1.0
		 * plan3: a, null
		 */
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));

		Plan plan1 = PopulationUtils.createPlan(person);
		plan1.setScore(15.0);
		Plan plan2 = PopulationUtils.createPlan(person);
		plan2.setScore(22.0);
		Plan plan3 = PopulationUtils.createPlan(person);
		Plan plan4 = PopulationUtils.createPlan(person);
		plan4.setScore(1.0);
		Plan plan5 = PopulationUtils.createPlan(person);
		plan5.setScore(18.0);
		Plan plan6 = PopulationUtils.createPlan(person);
		plan6.setScore(21.0);

		plan1.setType("type1");
		plan2.setType("type2");
		plan3.setType("type1");
		plan4.setType("type2");
		plan5.setType("type1");
		plan6.setType("type2");
		person.addPlan(plan1);
		person.addPlan(plan2);
		person.addPlan(plan3);
		person.addPlan(plan4);
		person.addPlan(plan5);
		person.addPlan(plan6);

		assertEquals(6, person.getPlans().size(), "test we have all plans we want");

		person.getPlans().remove(selector.selectPlan(person));
		person.getPlans().remove(selector.selectPlan(person));
		assertEquals(4, person.getPlans().size(), "test that two plans were removed");
		assertFalse(person.getPlans().contains(plan3), "test that plan with undefined score was removed.");
		assertFalse(person.getPlans().contains(plan4), "test that plan with worst score was removed.");

		person.getPlans().remove(selector.selectPlan(person));
		person.getPlans().remove(selector.selectPlan(person));
		assertEquals(2, person.getPlans().size(), "test that two plans were removed");
		assertFalse(person.getPlans().contains(plan1), "test that the plan with worst score was removed");
		assertTrue(person.getPlans().contains(plan5), "test that the now only plan of type a was not removed");
		assertFalse(person.getPlans().contains(plan6), "test that the plan with the 2nd-worst score was removed");

		person.getPlans().remove(selector.selectPlan(person));
		assertEquals(1, person.getPlans().size(), "test that one plan was removed");
		assertTrue(person.getPlans().contains(plan2), "test that the plan with highest score of type b was not removed");
	}

}
