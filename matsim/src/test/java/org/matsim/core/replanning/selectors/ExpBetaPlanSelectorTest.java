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
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.core.config.Config;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;

/**
 * Tests for {@link ExpBetaPlanSelector}.
 *
 * @author mrieser
 */
public class ExpBetaPlanSelectorTest extends AbstractPlanSelectorTest {

	private final static Logger log = LogManager.getLogger(ExpBetaPlanSelectorTest.class);
	private Config config = null;

	@BeforeEach public void setUp() {
		this.config = utils.loadConfig((String)null); // required for planCalcScore.beta to be defined
	}

	@AfterEach public void tearDown() {
		this.config = null;
	}

	@Override
	protected ExpBetaPlanSelector<Plan, Person> getPlanSelector() {
		return new ExpBetaPlanSelector<Plan, Person>(this.config.scoring());
	}

	/**
	 * Test that plans are selected depending on their weight, use beta = 2.0.
	 */
	@Test
	void testExpBeta2() {
		this.config.scoring().setBrainExpBeta(2.0);
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		// weight = Math.exp(this.beta * (plan.getScore() - maxScore));
		Plan plan1 = PersonUtils.createAndAddPlan(person, false); // weight: 0.0003.35462627902512
		plan1.setScore(96.0);
		Plan plan2 = PersonUtils.createAndAddPlan(person, false); // weight: 0.0024787521766663594
		plan2.setScore(97.0);
		Plan plan3 = PersonUtils.createAndAddPlan(person, false); // weight: 0.018315638888734186
		plan3.setScore(98.0);
		Plan plan4 = PersonUtils.createAndAddPlan(person, false); // weight: 0.1353352832366127
		plan4.setScore(99.0);
		Plan plan5 = PersonUtils.createAndAddPlan(person, false);// weight: 1
		plan5.setScore(100.0);

		ExpBetaPlanSelector<Plan, Person> selector = new ExpBetaPlanSelector<Plan, Person>(this.config.scoring());
		int cnt1 = 0;
		int cnt2 = 0;
		int cnt3 = 0;
		int cnt4 = 0;
		int cnt5 = 0;

		for (int i = 0; i < 10000; i++) {
			Plan plan = selector.selectPlan(person);
			if (plan == plan1) cnt1++;
			if (plan == plan2) cnt2++;
			if (plan == plan3) cnt3++;
			if (plan == plan4) cnt4++;
			if (plan == plan5) cnt5++;
		}

		log.info("Plan 1 was returned " + cnt1 + " times.");
		log.info("Plan 2 was returned " + cnt2 + " times.");
		log.info("Plan 3 was returned " + cnt3 + " times.");
		log.info("Plan 4 was returned " + cnt4 + " times.");
		log.info("Plan 5 was returned " + cnt5 + " times.");

		/* The fixed values here must correspond to the weights.
		 * The numbers will never match exactly because of the randomness, but
		 * they should still be feasible. In this example, the numbers should
		 * differ by a factor of ~9 from each other, according to the weights.
		 */
		assertEquals(4, cnt1);
		assertEquals(17, cnt2);
		assertEquals(141, cnt3);
		assertEquals(1115, cnt4);
		assertEquals(8723, cnt5);
	}

	/**
	 * Test that plans are selected depending on their weight, use beta = 2.0.
	 */
	@Test
	void testExpBeta1() {
		this.config.scoring().setBrainExpBeta(1.0);
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		// weight = Math.exp(this.beta * (plan.getScore() - maxScore));
		// weight: 0.018315638888734186
		Plan plan1 = PersonUtils.createAndAddPlan(person, false);
		plan1.setScore(96.0);
		// weight: 0.04978706836786395
		Plan plan2 = PersonUtils.createAndAddPlan(person, false);
		plan2.setScore(97.0);
		// weight: 0.1353352832366127
		Plan plan3 = PersonUtils.createAndAddPlan(person, false);
		plan3.setScore(98.0);
		// weight: 0.3678794411714423
		Plan plan4 = PersonUtils.createAndAddPlan(person, false);
		plan4.setScore(99.0);
		// weight: 1
		Plan plan5 = PersonUtils.createAndAddPlan(person, false);
		plan5.setScore(100.0);



		ExpBetaPlanSelector<Plan, Person> selector = new ExpBetaPlanSelector<Plan, Person>(this.config.scoring());
		int cnt1 = 0;
		int cnt2 = 0;
		int cnt3 = 0;
		int cnt4 = 0;
		int cnt5 = 0;

		for (int i = 0; i < 10000; i++) {
			Plan plan = selector.selectPlan(person);
			if (plan == plan1) cnt1++;
			if (plan == plan2) cnt2++;
			if (plan == plan3) cnt3++;
			if (plan == plan4) cnt4++;
			if (plan == plan5) cnt5++;
		}

		log.info("Plan 1 was returned " + cnt1 + " times.");
		log.info("Plan 2 was returned " + cnt2 + " times.");
		log.info("Plan 3 was returned " + cnt3 + " times.");
		log.info("Plan 4 was returned " + cnt4 + " times.");
		log.info("Plan 5 was returned " + cnt5 + " times.");

		/* The fixed values here must correspond to the weights.
		 * The numbers will never match exactly because of the randomness, but
		 * they should still be feasible. In this example, the numbers should
		 * differ by a factor of ~3 from each other, according to the weights.
		 */
		assertEquals(101, cnt1);
		assertEquals(310, cnt2);
		assertEquals(816, cnt3);
		assertEquals(2313, cnt4);
		assertEquals(6460, cnt5);
	}

	@Test
	void testGetSelectionProbability() {

		/*
		 * the expected results were computed with R. The standard output of double precision numbers in R has 7 digits.
		 */
		final double EPSILON_R = 1e-7;

		this.config.scoring().setBrainExpBeta(2.0);
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		Plan plan1 = PersonUtils.createAndAddPlan(person, false);
		plan1.setScore(180.0);
		Plan plan2 = PersonUtils.createAndAddPlan(person, false);
		plan2.setScore(180.1);
		Plan plan3 = PersonUtils.createAndAddPlan(person, false);
		plan3.setScore(180.5);
		Plan plan4 = PersonUtils.createAndAddPlan(person, false);
		plan4.setScore(169.9);

		ExpBetaPlanSelector testee = new ExpBetaPlanSelector(this.config.scoring());

		assertEquals(0.2024421, ExpBetaPlanSelector.getSelectionProbability(testee, person, plan1), EPSILON_R);
		assertEquals(0.2472634, ExpBetaPlanSelector.getSelectionProbability(testee, person, plan2), EPSILON_R);
		assertEquals(0.5502947, ExpBetaPlanSelector.getSelectionProbability(testee, person, plan3), EPSILON_R);
		assertEquals(6.208075e-10, ExpBetaPlanSelector.getSelectionProbability(testee, person, plan4), EPSILON_R);
	}

}
