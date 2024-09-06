/* *********************************************************************** *
 * project: org.matsim.*
 * BestPlanSelectorTest.java
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

package org.matsim.core.replanning.selectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;

/**
 * Test for {@link BestPlanSelector}.
 *
 * @author mrieser
 */
public class BestPlanSelectorTest extends AbstractPlanSelectorTest {

	@Override
	protected PlanSelector<Plan, Person> getPlanSelector() {
		return new BestPlanSelector();
	}

	/**
	 * Test the {@link BestPlanSelector} indeed returns the plans with the highest score.
	 *
	 * @author mrieser
	 */
	@Test
	void testBestPlan() {
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan plan;
		PersonUtils.createAndAddPlan(person, false);
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore(10.0);
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore(-50.0);
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore(40.0);
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore(30.0);
		plan = PersonUtils.createAndAddPlan(person, false);
		plan.setScore(-20.0);

		PlanSelector<Plan, Person> selector = new BestPlanSelector<Plan, Person>();

		plan = selector.selectPlan(person);
		assertEquals(40.0, plan.getScore().doubleValue(), 0.0);
		plan.setScore(null);

		plan = selector.selectPlan(person);
		assertEquals(30.0, plan.getScore().doubleValue(), 0.0);
		plan.setScore(null);

		plan = selector.selectPlan(person);
		assertEquals(10.0, plan.getScore().doubleValue(), 0.0);
		plan.setScore(null);

		plan = selector.selectPlan(person);
		assertEquals(-20.0, plan.getScore().doubleValue(), 0.0);
		plan.setScore(null);

		plan = selector.selectPlan(person);
		assertEquals(-50.0, plan.getScore().doubleValue(), 0.0);
		plan.setScore(null);

		plan = selector.selectPlan(person);
		assertNull(plan.getScore());
	}

}
