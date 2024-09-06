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

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;

/**
 * Test for {@link RandomPlanSelector}.
 *
 * @author mrieser
 */
public class RandomPlanSelectorTest extends AbstractPlanSelectorTest {

	private final static Logger log = LogManager.getLogger(RandomPlanSelectorTest.class);

	@Override
	protected PlanSelector<Plan, Person> getPlanSelector() {
		return new RandomPlanSelector<Plan, Person>();
	}

	/**
	 * Test that each of a person's plans is randomly selected.
	 */
	@Test
	void testRandom() {
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan plan1 = PersonUtils.createAndAddPlan(person, false);
		Plan plan2 = PersonUtils.createAndAddPlan(person, false);
		plan2.setScore(10.0);
		Plan plan3 = PersonUtils.createAndAddPlan(person, false);
		plan3.setScore(-50.0);
		Plan plan4 = PersonUtils.createAndAddPlan(person, false);
		plan4.setScore(0.0);

		RandomPlanSelector<Plan, Person> selector = new RandomPlanSelector<Plan, Person>();
		int cnt1 = 0;
		int cnt2 = 0;
		int cnt3 = 0;
		int cnt4 = 0;

		for (int i = 0; i < 4000; i++) {
			Plan plan = selector.selectPlan(person);
			if (plan == plan1) cnt1++;
			if (plan == plan2) cnt2++;
			if (plan == plan3) cnt3++;
			if (plan == plan4) cnt4++;
		}

		log.info("Plan 1 was returned " + cnt1 + " times.");
		log.info("Plan 2 was returned " + cnt2 + " times.");
		log.info("Plan 3 was returned " + cnt3 + " times.");
		log.info("Plan 4 was returned " + cnt4 + " times.");

		assertEquals(966, cnt1);
		assertEquals(1014, cnt2);
		assertEquals(1039, cnt3);
		assertEquals(981, cnt4);
	}

}
