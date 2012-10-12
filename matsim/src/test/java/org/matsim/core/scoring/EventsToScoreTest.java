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

import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.AgentMoneyEvent;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspExperimentalConfigKey;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PersonImpl;
import org.matsim.core.population.PlanImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author mrieser
 */
public class EventsToScoreTest extends MatsimTestCase {

	/**
	 * Tests that an AgentUtilityEvent is handled by calling the method addUtility() of a scoring function.
	 */
	public void testAddMoney() {
        ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Population population = scenario.getPopulation();
		PersonImpl person = new PersonImpl(new IdImpl(1));
		population.addPerson(person);
		MockScoringFunctionFactory sfFactory = new MockScoringFunctionFactory();
		EventsToScore e2s = new EventsToScore(scenario, sfFactory, 1.0);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(e2s);

		events.processEvent(new AgentMoneyEvent(3600.0, person.getId(), 3.4));

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
	public void testMsaAveraging() {
		Config config = ConfigUtils.createConfig() ;
		config.vspExperimental().addParam(VspExperimentalConfigKey.scoreMSAStartsAtIteration, "100") ;
		config.planCalcScore().setMarginalUtilityOfMoney(1.);

		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(config);
        Population population = scenario.getPopulation();
		PersonImpl person = new PersonImpl(new IdImpl(1));
		population.addPerson(person);
		PlanImpl plan = new PlanImpl() ;
		person.addPlan(plan);
		
		ScoringFunctionFactory sfFactory = new CharyparNagelScoringFunctionFactory(config.planCalcScore(), null);
		EventsToScore e2s = new EventsToScore(scenario, sfFactory, 1.0);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(e2s);
		
		for ( int mockIteration = 99 ; mockIteration <= 102 ; mockIteration++ ) {

			events.resetHandlers(mockIteration) ;

			events.processEvent(new AgentMoneyEvent(3600.0, person.getId(), mockIteration-98 ));
			
			e2s.finish() ;
			
			System.out.println( "score: " + person.getSelectedPlan().getScore() ) ;
			
			switch(mockIteration){
			case 99:
				assertEquals(1.0, person.getSelectedPlan().getScore() ) ;
				break ;
			case 100:
				assertEquals(2.0, person.getSelectedPlan().getScore() ) ;
				break ;
			case 101:
				assertEquals(3.0, person.getSelectedPlan().getScore() ) ;
				break ;
			case 102:
				assertEquals(3.5, person.getSelectedPlan().getScore() ) ;
				break ;
			}
			
		}
	}

	private static class MockScoringFunctionFactory implements ScoringFunctionFactory {

		protected final MockScoringFunction sf = new MockScoringFunction();
		protected int counter = 0;

		public MockScoringFunctionFactory() {
			// empty public constructor for private inner class
		}

		public ScoringFunction createNewScoringFunction(final Plan plan) {
			this.counter++;
			return this.sf;
		}

	}

	private static class MockScoringFunction extends ScoringFunctionAdapter {

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

		public void startActivity(final double time, final Activity act) {
			this.cntStartAct++;
		}

		public void startLeg(final double time, final Leg leg) {
			this.cntStartLeg++;
		}

		@Override
		public void endActivity(double time, Activity activity) {
			this.cntEndAct++;
		}

	}
}
