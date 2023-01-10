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

import org.junit.Assert;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.PersonMoneyEvent;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.MutableScenario;
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
	@org.junit.Test public void testAddMoney() {
        MutableScenario scenario = (MutableScenario) ScenarioUtils.createScenario(ConfigUtils.createConfig());
        Population population = scenario.getPopulation();
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		population.addPerson(person);
		MockScoringFunctionFactory sfFactory = new MockScoringFunctionFactory();
		EventsManager events = EventsUtils.createEventsManager();
		EventsToScore e2s = EventsToScore.createWithoutScoreUpdating(scenario, sfFactory, events);
		e2s.beginIteration(0, false);
		events.initProcessing();
		events.processEvent(new PersonMoneyEvent(3600.0, person.getId(), 3.4, "tollRefund", "motorwayOperator"));
		events.finishProcessing();
		e2s.finish();
		Assert.assertEquals(3.4, e2s.getAgentScore(person.getId()), 0);
	}

	@org.junit.Test public void testMsaAveraging() {
		Config config = ConfigUtils.createConfig() ;

		config.controler().setFirstIteration(10);
		config.controler().setLastIteration(110);

		config.planCalcScore().setMarginalUtilityOfMoney(1.);

		config.planCalcScore().setFractionOfIterationsToStartScoreMSA(0.9);

		Scenario scenario = ScenarioUtils.createScenario(config);
        Population population = scenario.getPopulation();
		Person person = PopulationUtils.getFactory().createPerson(Id.create(1, Person.class));
		population.addPerson(person);
		Plan plan = PopulationUtils.createPlan() ;
		person.addPlan(plan);

		ScoringFunctionFactory sfFactory = new CharyparNagelScoringFunctionFactory( scenario );
		EventsManager events = EventsUtils.createEventsManager();
		EventsToScore e2s = EventsToScore.createWithScoreUpdating(scenario, sfFactory, events);

		for ( int mockIteration = config.controler().getFirstIteration() ; mockIteration <= config.controler().getLastIteration() ; mockIteration++ ) {

			e2s.beginIteration(mockIteration, false); ;
			events.initProcessing();

			// generating a money event with amount mockIteration-98 (i.e. 1, 2, 3, 4):
			events.processEvent(new PersonMoneyEvent(3600.0, person.getId(), mockIteration-98, "bribe", "contractor" ));

			events.finishProcessing();
			e2s.finish() ;

			System.out.println( "score: " + person.getSelectedPlan().getScore() ) ;

			switch(mockIteration){
			case 99:
				Assert.assertEquals(1.0, person.getSelectedPlan().getScore(), 0);
				break ;
			case 100:
				// first MSA iteration; plain score should be ok:
				Assert.assertEquals(2.0, person.getSelectedPlan().getScore(), 0);
				break ;
			case 101:
				// second MSA iteration
				// (2+3)/2 = 2.5
				Assert.assertEquals(2.5, person.getSelectedPlan().getScore(), 0);
				break ;
			case 102:
				// 3rd MSA iteration
				// (2+3+4)/3 = 3
				Assert.assertEquals(3.0, person.getSelectedPlan().getScore(), 0);
				break ;
			case 103:
				// (2+3+4+5)/4 = 3.5
				Assert.assertEquals(3.5, person.getSelectedPlan().getScore(), 0);
				break ;
			case 104:
				// 3rd MSA iteration
				Assert.assertEquals(4.0, person.getSelectedPlan().getScore(), 0);
				break ;
			case 105:
				// 3rd MSA iteration
				Assert.assertEquals(4.5, person.getSelectedPlan().getScore(), 0);
				break ;
			case 106:
				// 3rd MSA iteration
				Assert.assertEquals(5.0, person.getSelectedPlan().getScore(), 0);
				break ;
			case 107:
				// 3rd MSA iteration
				Assert.assertEquals(5.5, person.getSelectedPlan().getScore(), 0);
				break ;
			case 108:
				// 3rd MSA iteration
				Assert.assertEquals(6.0, person.getSelectedPlan().getScore(), 0);
				break ;
			case 109:
				// 3rd MSA iteration
				Assert.assertEquals(6.5, person.getSelectedPlan().getScore(), 0);
				break ;
			case 110:
				// 3rd MSA iteration
				Assert.assertEquals(7.0, person.getSelectedPlan().getScore(), 0);
				break ;
			}

		}
	}

	private static class MockScoringFunctionFactory implements ScoringFunctionFactory {

		@Override
		public ScoringFunction createNewScoringFunction(final Person person) {
			SumScoringFunction sumScoringFunction = new SumScoringFunction();
			sumScoringFunction.addScoringFunction(new SumScoringFunction.MoneyScoring() {
				double money = 0.0;
				@Override
				public void addMoney(double amount) {
					money += amount;
				}
				@Override
				public void finish() {}
				@Override
				public double getScore() {
					return money;
				}
			});
			return sumScoringFunction;
		}

	}
}
