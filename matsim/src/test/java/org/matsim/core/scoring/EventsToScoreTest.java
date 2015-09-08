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

import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Leg;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
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
		Person person = PersonImpl.createPerson(Id.create(1, Person.class));
		population.addPerson(person);
		MockScoringFunctionFactory sfFactory = new MockScoringFunctionFactory();
		EventsToScore e2s = new EventsToScore(scenario, sfFactory, 1.0);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(e2s);

		events.processEvent(new PersonMoneyEvent(3600.0, person.getId(), 3.4));

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
		
		config.controler().setFirstIteration(10);
		config.controler().setLastIteration(110);
		
		config.planCalcScore().setMarginalUtilityOfMoney(1.);

		config.planCalcScore().setFractionOfIterationsToStartScoreMSA(0.9);
		
		Scenario scenario = ScenarioUtils.createScenario(config);
        Population population = scenario.getPopulation();
		Person person = PersonImpl.createPerson(Id.create(1, Person.class));
		population.addPerson(person);
		PlanImpl plan = new PlanImpl() ;
		person.addPlan(plan);
		
		ScoringFunctionFactory sfFactory = new CharyparNagelScoringFunctionFactory(config.planCalcScore(), config.scenario(), null);
		EventsToScore e2s = new EventsToScore(scenario, sfFactory, 1.0);
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(e2s);
		
		for ( int mockIteration = config.controler().getFirstIteration() ; mockIteration <= config.controler().getLastIteration() ; mockIteration++ ) {

			events.resetHandlers(mockIteration) ;

			// generating a money event with amount mockIteration-98 (i.e. 1, 2, 3, 4):
			events.processEvent(new PersonMoneyEvent(3600.0, person.getId(), mockIteration-98 ));
			
			e2s.finish() ;
			
			System.out.println( "score: " + person.getSelectedPlan().getScore() ) ;
			
			switch(mockIteration){
			case 99:
				assertEquals(1.0, person.getSelectedPlan().getScore() ) ;
				break ;
			case 100:
				// first MSA iteration; plain score should be ok:
				assertEquals(2.0, person.getSelectedPlan().getScore() ) ;
				break ;
			case 101:
				// second MSA iteration
				assertEquals(2.5, person.getSelectedPlan().getScore() ) ; // (2+3)/2 = 2.5
				break ;
			case 102:
				// 3rd MSA iteration
				assertEquals(3.0, person.getSelectedPlan().getScore() ) ; // (2+3+4)/3 = 3
				break ;
			case 103:
				assertEquals(3.5, person.getSelectedPlan().getScore() ) ; // (2+3+4+5)/4 = 3.5
				break ;
			case 104:
				// 3rd MSA iteration
				assertEquals(4.0, person.getSelectedPlan().getScore() ) ;
				break ;
			case 105:
				// 3rd MSA iteration
				assertEquals(4.5, person.getSelectedPlan().getScore() ) ;
				break ;
			case 106:
				// 3rd MSA iteration
				assertEquals(5.0, person.getSelectedPlan().getScore() ) ;
				break ;
			case 107:
				// 3rd MSA iteration
				assertEquals(5.5, person.getSelectedPlan().getScore() ) ;
				break ;
			case 108:
				// 3rd MSA iteration
				assertEquals(6.0, person.getSelectedPlan().getScore() ) ;
				break ;
			case 109:
				// 3rd MSA iteration
				assertEquals(6.5, person.getSelectedPlan().getScore() ) ;
				break ;
			case 110:
				// 3rd MSA iteration
				assertEquals(7.0, person.getSelectedPlan().getScore() ) ;
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

		@Override
		public ScoringFunction createNewScoringFunction(final Person person) {
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

		@Override
		public void addMoney(final double amount) {
			this.cntMoney++;
		}

		@Override
		public void agentStuck(final double time) {
			this.cntStuck++;
		}

		@Override
		public void endLeg(final double time) {
			this.cntEndLeg++;
		}

		@Override
		public void finish() {
			this.cntFinish++;
		}

		@Override
		public double getScore() {
			this.cntGetScore++;
			return 0;
		}

        public void reset() {
			this.cntReset++;
		}

		@Override
		public void startActivity(final double time, final Activity act) {
			this.cntStartAct++;
		}

		@Override
		public void startLeg(final double time, final Leg leg) {
			this.cntStartLeg++;
		}

		@Override
		public void endActivity(double time, Activity activity) {
			this.cntEndAct++;
		}

		@Override
		public void handleEvent(Event event) {
			// TODO Auto-generated method stub
			
		}

	}
}
