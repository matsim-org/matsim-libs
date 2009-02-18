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
import org.matsim.interfaces.basic.v01.BasicPlan;
import org.matsim.population.Person;
import org.matsim.population.PersonImpl;
import org.matsim.population.Plan;

/**
 * Test for {@link BestPlanSelector}.
 *
 * @author mrieser
 */
public class BestPlanSelectorTest extends AbstractPlanSelectorTest {

	@Override
	protected PlanSelector getPlanSelector() {
		return new BestPlanSelector();
	}

	/**
	 * Test the {@link BestPlanSelector} indeed returns the plans with the highest score.
	 *
	 * @author mrieser
	 */
	public void testBestPlan() {
		Person person = new PersonImpl(new IdImpl(1));
		Plan plan;
		person.createPlan(false);
		plan = person.createPlan(false);
		plan.setScore(10.0);
		plan = person.createPlan(false);
		plan.setScore(-50.0);
		plan = person.createPlan(false);
		plan.setScore(40.0);
		plan = person.createPlan(false);
		plan.setScore(30.0);
		plan = person.createPlan(false);
		plan.setScore(-20.0);

		BestPlanSelector selector = new BestPlanSelector();

		plan = selector.selectPlan(person);
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
