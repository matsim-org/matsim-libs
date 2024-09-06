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

import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;

/**
 * Test for {@link KeepSelected}
 *
 * @author mrieser
 */
public class KeepSelectedTest extends AbstractPlanSelectorTest {

	@Override
	protected KeepSelected<Plan, Person> getPlanSelector() {
		return new KeepSelected<Plan, Person>();
	}

	/**
	 * Test that really the already selected plan is returned.
	 *
	 * @author mrieser
	 */
	@Test
	void testSelected() {
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan plan1 = PersonUtils.createAndAddPlan(person, false);
		Plan plan2 = PersonUtils.createAndAddPlan(person, true);
		plan2.setScore(10.0);
		Plan plan3 = PersonUtils.createAndAddPlan(person, false);
		plan3.setScore(-50.0);
		KeepSelected selector = new KeepSelected();

		// test default selected plan
		assertEquals(plan2, selector.selectPlan(person));

		// test selected plan with negative score
		person.setSelectedPlan(plan3);
		assertEquals(plan3, selector.selectPlan(person));

		// test selected plan with undefined score
		person.setSelectedPlan(plan1);
		assertEquals(plan1, selector.selectPlan(person));
	}

}
