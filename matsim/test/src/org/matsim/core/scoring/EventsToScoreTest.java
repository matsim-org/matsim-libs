/* *********************************************************************** *
 * project: org.matsim.*
 * EventsToScoreTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package org.matsim.core.scoring;

import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.events.AgentMoneyEvent;
import org.matsim.core.events.Events;
import org.matsim.core.population.ActivityImpl;
import org.matsim.core.population.LegImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.core.population.PopulationImpl;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class EventsToScoreTest extends MatsimTestCase {

	/**
	 * Tests that an AgentUtilityEvent is handled by calling the method addUtility() of a scoring function.
	 */
	public void testAddMoney() {
		PopulationImpl population = new PopulationImpl();
		PersonImpl person = new PersonImpl(new IdImpl(1));
		population.getPersons().put(person.getId(), person);
		MockScoringFunctionFactory sfFactory = new MockScoringFunctionFactory();
		EventsToScore e2s = new EventsToScore(population, sfFactory, 1.0);
		Events events = new Events();
		events.addHandler(e2s);

		events.processEvent(new AgentMoneyEvent(3600.0, person, 3.4));

		assertEquals("exactly one instance should have been requested.", 1, sfFactory.counter);
		assertEquals(0, sfFactory.sf.cntEndAct);
		assertEquals(0, sfFactory.sf.cntStartAct);
		assertEquals(0, sfFactory.sf.cntEndLeg);
		assertEquals(0, sfFactory.sf.cntStartLeg);
		assertEquals(0, sfFactory.sf.cntFinish);
		assertEquals(0, sfFactory.sf.cntGetScore);
		assertEquals(0, sfFactory.sf.cntReset);
		assertEquals(0, sfFactory.sf.cntStuck);
		assertEquals(1, sfFactory.sf.cntMoney);
	}

	private static class MockScoringFunctionFactory implements ScoringFunctionFactory {

		protected final MockScoringFunction sf = new MockScoringFunction();
		protected int counter = 0;

		public MockScoringFunctionFactory() {
			// empty public constructor for private inner class
		}

		public ScoringFunction getNewScoringFunction(final PlanImpl plan) {
			this.counter++;
			return this.sf;
		}

	}

	private static class MockScoringFunction implements ScoringFunction {

		protected int cntMoney = 0;
		protected int cntStuck = 0;
		protected int cntEndAct = 0;
		protected int cntEndLeg = 0;
		protected int cntStartLeg = 0;
		protected int cntStartAct = 0;
		protected int cntFinish = 0;
		protected int cntGetScore = 0;
		protected int cntReset = 0;

		public MockScoringFunction() {
			// empty public constructor for private inner class
		}

		public void addMoney(final double amount) {
			this.cntMoney++;
		}

		public void agentStuck(final double time) {
			this.cntStuck++;
		}

		public void endActivity(final double time) {
			this.cntEndAct++;
		}

		public void endLeg(final double time) {
			this.cntEndLeg++;
		}

		public void finish() {
			this.cntFinish++;
		}

		public double getScore() {
			this.cntGetScore++;
			return 0;
		}

		public void reset() {
			this.cntReset++;
		}

		public void startActivity(final double time, final ActivityImpl act) {
			this.cntStartAct++;
		}

		public void startLeg(final double time, final LegImpl leg) {
			this.cntStartLeg++;
		}

	}
}
