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

import org.matsim.basic.v01.BasicPlan;
import org.matsim.basic.v01.Id;
import org.matsim.plans.Person;
import org.matsim.plans.Plan;

/**
 * Test for {@link BestPlanSelector}.
 *
 * @author mrieser
 */
public class BestPlanSelectorTest extends AbstractPlanSelectorTest {

	@Override
	protected PlanSelectorI getPlanSelector() {
		return new BestPlanSelector();
	}

	/**
	 * Test the {@link BestPlanSelector} indeed returns the plans with the highest score.
	 *
	 * @author mrieser
	 */
	public void testBestPlan() {
		Person person = new Person(new Id(1), "m", 40, null, null, null);
		person.createPlan(null, "no");
		person.createPlan("10.0", "no");
		person.createPlan("-50.0", "no");
		person.createPlan("40.0", "no");
		person.createPlan("30.0", "no");
		person.createPlan("-20.0", "no");

		BestPlanSelector selector = new BestPlanSelector();

		Plan plan = selector.selectPlan(person);
		assertEquals(40.0, plan.getScore(), 0.0);
		plan.setScore(BasicPlan.UNDEF_SCORE);

		plan = selector.selectPlan(person);
		assertEquals(30.0, plan.getScore(), 0.0);
		plan.setScore(BasicPlan.UNDEF_SCORE);

		plan = selector.selectPlan(person);
		assertEquals(10.0, plan.getScore(), 0.0);
		plan.setScore(BasicPlan.UNDEF_SCORE);

		plan = selector.selectPlan(person);
		assertEquals(-20.0, plan.getScore(), 0.0);
		plan.setScore(BasicPlan.UNDEF_SCORE);

		plan = selector.selectPlan(person);
		assertEquals(-50.0, plan.getScore(), 0.0);
		plan.setScore(BasicPlan.UNDEF_SCORE);

		plan = selector.selectPlan(person);
		assertTrue(plan.hasUndefinedScore());
	}

}
