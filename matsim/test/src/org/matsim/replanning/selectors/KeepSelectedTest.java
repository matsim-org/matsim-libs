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

package org.matsim.replanning.selectors;

import org.matsim.basic.v01.IdImpl;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;

/**
 * Test for {@link KeepSelected}
 *
 * @author mrieser
 */
public class KeepSelectedTest extends AbstractPlanSelectorTest {

	@Override
	protected PlanSelector getPlanSelector() {
		return new KeepSelected();
	}

	/**
	 * Test that really the already selected plan is returned.
	 *
	 * @author mrieser
	 */
	public void testSelected() {
		Person person = new PersonImpl(new IdImpl(1));
		Plan plan1 = person.createPlan(false);
		Plan plan2 = person.createPlan(true);
		plan2.setScore(10.0);
		Plan plan3 = person.createPlan(false);
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
