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

import org.apache.log4j.Logger;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.interfaces.core.v01.Person;
import org.matsim.interfaces.core.v01.Plan;
import org.matsim.population.PersonImpl;

/**
 * Tests for {@link ExpBetaPlanSelector}.
 *
 * @author mrieser
 */
public class ExpBetaPlanSelectorTest extends AbstractPlanSelectorTest {

	private final static Logger log = Logger.getLogger(RandomPlanSelectorTest.class);
	private Config config = null;

	@Override
	public void setUp() throws Exception {
		super.setUp();
		this.config = loadConfig(null); // required for planCalcScore.beta to be defined
	}

	@Override
	protected PlanSelector getPlanSelector() {
		return new ExpBetaPlanSelector();
	}

	/**
	 * Test that plans are selected depending on their weight, use beta = 2.0.
	 */
	public void testExpBeta2() {
		this.config.charyparNagelScoring().setBrainExpBeta(2.0);
		Person person = new PersonImpl(new IdImpl(1));
		// weight = Math.exp(this.beta * (plan.getScore() - maxScore));
		Plan plan1 = person.createPlan(false); // weight: 0.0003.35462627902512
		plan1.setScore(96.0);
		Plan plan2 = person.createPlan(false); // weight: 0.0024787521766663594
		plan2.setScore(97.0);
		Plan plan3 = person.createPlan(false); // weight: 0.018315638888734186
		plan3.setScore(98.0);
		Plan plan4 = person.createPlan(false); // weight: 0.1353352832366127
		plan4.setScore(99.0);
		Plan plan5 = person.createPlan(false);// weight: 1
		plan5.setScore(100.0);
		
		ExpBetaPlanSelector selector = new ExpBetaPlanSelector();
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
	public void testExpBeta1() {
		this.config.charyparNagelScoring().setBrainExpBeta(1.0);
		Person person = new PersonImpl(new IdImpl(1));
		// weight = Math.exp(this.beta * (plan.getScore() - maxScore));
		// weight: 0.018315638888734186
		Plan plan1 = person.createPlan(false); 
		plan1.setScore(96.0);
		// weight: 0.04978706836786395
		Plan plan2 = person.createPlan(false); 
		plan2.setScore(97.0);
		// weight: 0.1353352832366127
		Plan plan3 = person.createPlan(false); 
		plan3.setScore(98.0);
		// weight: 0.3678794411714423
		Plan plan4 = person.createPlan(false); 
		plan4.setScore(99.0);
		// weight: 1
		Plan plan5 = person.createPlan(false);
		plan5.setScore(100.0);

		
		
		ExpBetaPlanSelector selector = new ExpBetaPlanSelector();
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

}
