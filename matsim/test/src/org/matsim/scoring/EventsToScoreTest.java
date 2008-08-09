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

package org.matsim.scoring;

import org.matsim.basic.v01.IdImpl;
import org.matsim.events.AgentUtilityEvent;
import org.matsim.events.Events;
import org.matsim.population.Act;
import org.matsim.population.Leg;
import org.matsim.population.Person;
import org.matsim.population.Plan;
import org.matsim.population.Population;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class EventsToScoreTest extends MatsimTestCase {

	/**
	 * Tests that an AgentUtilityEvent is handled by calling the method addUtility() of a scoring function.
	 *
	 * @throws Exception
	 */
	public void testAddUtility() throws Exception {
		Population population = new Population(Population.NO_STREAMING);
		Person person = new Person(new IdImpl(1));
		population.addPerson(person);
		MockScoringFunctionFactory sfFactory = new MockScoringFunctionFactory();
		EventsToScore e2s = new EventsToScore(population, sfFactory, 1.0);
		Events events = new Events();
		events.addHandler(e2s);

		events.processEvent(new AgentUtilityEvent(3600.0, person, 3.4));

		assertEquals("exactly one instance should have been requested.", 1, sfFactory.counter);
		assertEquals(0, sfFactory.sf.cntEndAct);
		assertEquals(0, sfFactory.sf.cntStartAct);
		assertEquals(0, sfFactory.sf.cntEndLeg);
		assertEquals(0, sfFactory.sf.cntStartLeg);
		assertEquals(0, sfFactory.sf.cntFinish);
		assertEquals(0, sfFactory.sf.cntGetScore);
		assertEquals(0, sfFactory.sf.cntReset);
		assertEquals(0, sfFactory.sf.cntStuck);
		assertEquals(1, sfFactory.sf.cntUtility);
	}

	private static class MockScoringFunctionFactory implements ScoringFunctionFactory {

		public final MockScoringFunction sf = new MockScoringFunction();
		public int counter = 0;

		public MockScoringFunctionFactory() {
			// empty public constructor for private inner class
		}

		public ScoringFunction getNewScoringFunction(Plan plan) {
			counter++;
			return sf;
		}

	}

	private static class MockScoringFunction implements ScoringFunction {

		public int cntUtility = 0;
		public int cntStuck = 0;
		public int cntEndAct = 0;
		public int cntEndLeg = 0;
		public int cntStartLeg = 0;
		public int cntStartAct = 0;
		public int cntFinish = 0;
		public int cntGetScore = 0;
		public int cntReset = 0;

		public MockScoringFunction() {
			// empty public constructor for private inner class
		}

		public void addUtility(double amount) {
			this.cntUtility++;
		}

		public void agentStuck(double time) {
			this.cntStuck++;
		}

		public void endActivity(double time) {
			this.cntEndAct++;
		}

		public void endLeg(double time) {
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

		public void startActivity(double time, Act act) {
			this.cntStartAct++;
		}

		public void startLeg(double time, Leg leg) {
			this.cntStartLeg++;
		}

	}
}
