/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2015 by the members listed in the COPYING,        *
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

package playground.ikaddoura.agentSpecificActivityScheduling;

import static org.junit.Assert.assertEquals;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.ActivityStartEvent;
import org.matsim.api.core.v01.population.Activity;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Plan;
import org.matsim.api.core.v01.population.PlanElement;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.TypicalDurationScoreComputation;
import org.matsim.core.population.PersonUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.scoring.ScoringFunction;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.testcases.MatsimTestUtils;

/**
* @author ikaddoura
*/

public class AgentSpecificActivityScoringTest {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	// late arrival penalty = 0.
	@Test
	public final void test1() {
		
		// Activities
		
		double firstActEnd = 7 * 3600.;
		double secondActEnd = 16 * 3600;
		double travelTime = 900.;
		
		Person person = PopulationUtils.getFactory().createPerson(Id.create("1", Person.class));
		person.getAttributes().putAttribute("OpeningClosingTimes", Double.NEGATIVE_INFINITY + ";" + Double.NEGATIVE_INFINITY + ";26100.0;57600.0;" + Double.NEGATIVE_INFINITY + ";" + Double.NEGATIVE_INFINITY);					

		Plan plan1 = PersonUtils.createAndAddPlan(person, true);;
		Activity firstActivity1 = PopulationUtils.createAndAddActivity(plan1, "h");
		firstActivity1.setEndTime(firstActEnd);
		Activity secondActivity1 = PopulationUtils.createAndAddActivity(plan1, "w");
		secondActivity1.setStartTime(firstActEnd + travelTime);
		secondActivity1.setEndTime(secondActEnd);
		Activity thirdActivity1 = PopulationUtils.createAndAddActivity(plan1, "h");
		thirdActivity1.setStartTime(secondActEnd + travelTime);
		
		Plan plan2 = PersonUtils.createAndAddPlan(person, false);;
		Activity firstActivity2 = PopulationUtils.createAndAddActivity(plan2, "h");
		firstActivity2.setEndTime(firstActEnd);
		Activity secondActivity2 = PopulationUtils.createAndAddActivity(plan2, "w");
		secondActivity2.setStartTime(firstActEnd + travelTime + 1800.);
		secondActivity2.setEndTime(secondActEnd + 1800.);
		Activity thirdActivity2 = PopulationUtils.createAndAddActivity(plan2, "h");
		thirdActivity2.setStartTime(secondActEnd + travelTime);
		
		// Activity parameters
		
		Config config = ConfigUtils.createConfig(new AgentSpecificActivitySchedulingConfigGroup());
		AgentSpecificActivitySchedulingConfigGroup asasConfigGroup = (AgentSpecificActivitySchedulingConfigGroup) config.getModules().get(AgentSpecificActivitySchedulingConfigGroup.GROUP_NAME);
		
		ActivityParams actParamsH = new ActivityParams("h");
		actParamsH.setTypicalDuration(12 * 3600.);
		actParamsH.setLatestStartTime(Double.NEGATIVE_INFINITY);
		actParamsH.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
		config.planCalcScore().addActivityParams(actParamsH);
		
		ActivityParams actParamsW = new ActivityParams("w");
		config.planCalcScore().addActivityParams(actParamsW);
		actParamsW.setTypicalDuration(8 * 3600.);
		actParamsW.setLatestStartTime(Double.NEGATIVE_INFINITY);
		actParamsW.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
		
		config.planCalcScore().setPerforming_utils_hr(6.);
		config.planCalcScore().setLateArrival_utils_hr(0.);
		
		// Scenario
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getPopulation().addPerson(person);
		
		// test
		
		double defaultScorePlan1 = calcScoreDefault(scenario, plan1);
		assertEquals("default activity-related score of plan1 has changed", 139.15760679294863, defaultScorePlan1, MatsimTestUtils.EPSILON);
		
		{
			CountActEventHandler actCounter = new CountActEventHandler();	
			asasConfigGroup.setTolerance(0.);
			AgentSpecificScoringFunctionFactory agentSpecificScoringFunctionFactory = new AgentSpecificScoringFunctionFactory(scenario, actCounter);
			ScoringFunction scoringFunction = agentSpecificScoringFunctionFactory.createNewScoringFunction(plan1.getPerson());
			scoringFunction.handleActivity(firstActivity1);
			actCounter.handleEvent(new ActivityStartEvent(secondActivity1.getStartTime(), plan1.getPerson().getId(), null, null, "w"));
			scoringFunction.handleActivity(secondActivity1);
			actCounter.handleEvent(new ActivityStartEvent(thirdActivity1.getStartTime(), plan1.getPerson().getId(), null, null, "h"));
			scoringFunction.handleActivity(thirdActivity1);
			scoringFunction.finish();
			assertEquals("agent-specific activity-score of plan1 should result in the same score as default scoring", defaultScorePlan1, scoringFunction.getScore(), MatsimTestUtils.EPSILON);
		}
	
		double defaultScorePlan2 = calcScoreDefault(scenario, plan2);
		assertEquals("default activity-related score of plan2 has changed", 139.15760679294863, defaultScorePlan2, MatsimTestUtils.EPSILON);
		
		{
			CountActEventHandler actCounter = new CountActEventHandler();
			asasConfigGroup.setTolerance(3600.);
			AgentSpecificScoringFunctionFactory agentSpecificScoringFunctionFactory = new AgentSpecificScoringFunctionFactory(scenario, actCounter);
			ScoringFunction scoringFunction = agentSpecificScoringFunctionFactory.createNewScoringFunction(plan2.getPerson());
			scoringFunction.handleActivity(firstActivity2);
			actCounter.handleEvent(new ActivityStartEvent(secondActivity2.getStartTime(), plan2.getPerson().getId(), null, null, "w"));
			scoringFunction.handleActivity(secondActivity2);
			actCounter.handleEvent(new ActivityStartEvent(thirdActivity2.getStartTime(), plan2.getPerson().getId(), null, null, "h"));
			scoringFunction.handleActivity(thirdActivity2);
			scoringFunction.finish();
			assertEquals("agent-specific activity-score of plan2 should result in the same score as default scoring (tolerance: 3600.)", defaultScorePlan2, scoringFunction.getScore(), MatsimTestUtils.EPSILON);
		}
		
		{
			CountActEventHandler actCounter = new CountActEventHandler();
			asasConfigGroup.setTolerance(0.);
			AgentSpecificScoringFunctionFactory agentSpecificScoringFunctionFactory = new AgentSpecificScoringFunctionFactory(scenario, actCounter);
			ScoringFunction scoringFunction = agentSpecificScoringFunctionFactory.createNewScoringFunction(plan2.getPerson());
			scoringFunction.handleActivity(firstActivity2);
			actCounter.handleEvent(new ActivityStartEvent(secondActivity2.getStartTime(), plan2.getPerson().getId(), null, null, "w"));
			scoringFunction.handleActivity(secondActivity2);
			actCounter.handleEvent(new ActivityStartEvent(thirdActivity2.getStartTime(), plan2.getPerson().getId(), null, null, "h"));
			scoringFunction.handleActivity(thirdActivity2);
			scoringFunction.finish();
			assertEquals("agent-specific activity-score of plan2 should result in a lower score compared to default scoring (tolerance: 0.)", true, defaultScorePlan2 > scoringFunction.getScore());
			assertEquals("wrong agent-specific activity-score of plan2", 136.33326279184783, scoringFunction.getScore(), MatsimTestUtils.EPSILON);
		}
	
	}
	
	// late arrival penalty != 0.
	@Test
	public final void test2() {
		
		// Activities
		
		double firstActEnd = 7 * 3600.;
		double secondActEnd = 16 * 3600;
		double travelTime = 900.;
		
		Person person = PopulationUtils.getFactory().createPerson(Id.create("1", Person.class));
		person.getAttributes().putAttribute("OpeningClosingTimes", Double.NEGATIVE_INFINITY + ";" + Double.NEGATIVE_INFINITY + ";26100.0;57600.0;" + Double.NEGATIVE_INFINITY + ";" + Double.NEGATIVE_INFINITY);					

		Plan plan1 = PersonUtils.createAndAddPlan(person, true);;
		Activity firstActivity1 = PopulationUtils.createAndAddActivity(plan1, "h");
		firstActivity1.setEndTime(firstActEnd);
		Activity secondActivity1 = PopulationUtils.createAndAddActivity(plan1, "w");
		secondActivity1.setStartTime(firstActEnd + travelTime);
		secondActivity1.setEndTime(secondActEnd);
		Activity thirdActivity1 = PopulationUtils.createAndAddActivity(plan1, "h");
		thirdActivity1.setStartTime(secondActEnd + travelTime);
		
		Plan plan2 = PersonUtils.createAndAddPlan(person, false);;
		Activity firstActivity2 = PopulationUtils.createAndAddActivity(plan2, "h");
		firstActivity2.setEndTime(firstActEnd);
		Activity secondActivity2 = PopulationUtils.createAndAddActivity(plan2, "w");
		secondActivity2.setStartTime(firstActEnd + travelTime + 1800.);
		secondActivity2.setEndTime(secondActEnd + 1800.);
		Activity thirdActivity2 = PopulationUtils.createAndAddActivity(plan2, "h");
		thirdActivity2.setStartTime(secondActEnd + travelTime);
		
		// Activity parameters
		
		Config config = ConfigUtils.createConfig(new AgentSpecificActivitySchedulingConfigGroup());
		AgentSpecificActivitySchedulingConfigGroup asasConfigGroup = (AgentSpecificActivitySchedulingConfigGroup) config.getModules().get(AgentSpecificActivitySchedulingConfigGroup.GROUP_NAME);

		ActivityParams actParamsH = new ActivityParams("h");
		actParamsH.setTypicalDuration(12 * 3600.);
		actParamsH.setLatestStartTime(Double.NEGATIVE_INFINITY);
		actParamsH.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
		config.planCalcScore().addActivityParams(actParamsH);
		
		ActivityParams actParamsW = new ActivityParams("w");
		config.planCalcScore().addActivityParams(actParamsW);
		actParamsW.setTypicalDuration(8 * 3600.);
		actParamsW.setLatestStartTime(Double.NEGATIVE_INFINITY);
		actParamsW.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
		
		config.planCalcScore().setPerforming_utils_hr(6.);
		config.planCalcScore().setLateArrival_utils_hr(-18.);
		
		// Scenario
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getPopulation().addPerson(person);
		
		// test
		
		double defaultScorePlan1 = calcScoreDefault(scenario, plan1);
		assertEquals("default activity-related score of plan1 has changed", 139.15760679294863, defaultScorePlan1, MatsimTestUtils.EPSILON);
		
		{
			CountActEventHandler actCounter = new CountActEventHandler();
			asasConfigGroup.setTolerance(0.);

			AgentSpecificScoringFunctionFactory agentSpecificScoringFunctionFactory = new AgentSpecificScoringFunctionFactory(scenario, actCounter);
			ScoringFunction scoringFunction = agentSpecificScoringFunctionFactory.createNewScoringFunction(plan1.getPerson());
			scoringFunction.handleActivity(firstActivity1);
			actCounter.handleEvent(new ActivityStartEvent(secondActivity1.getStartTime(), plan1.getPerson().getId(), null, null, "w"));
			scoringFunction.handleActivity(secondActivity1);
			actCounter.handleEvent(new ActivityStartEvent(thirdActivity1.getStartTime(), plan1.getPerson().getId(), null, null, "h"));
			scoringFunction.handleActivity(thirdActivity1);
			scoringFunction.finish();
			assertEquals("agent-specific activity-score of plan1 should result in the same score as default scoring", defaultScorePlan1, scoringFunction.getScore(), MatsimTestUtils.EPSILON);
		}
	
		double defaultScorePlan2 = calcScoreDefault(scenario, plan2);
		assertEquals("default activity-related score of plan2 has changed", 139.15760679294863, defaultScorePlan2, MatsimTestUtils.EPSILON);
		
		{
			CountActEventHandler actCounter = new CountActEventHandler();	
			asasConfigGroup.setTolerance(3600.);

			AgentSpecificScoringFunctionFactory agentSpecificScoringFunctionFactory = new AgentSpecificScoringFunctionFactory(scenario, actCounter);
			ScoringFunction scoringFunction = agentSpecificScoringFunctionFactory.createNewScoringFunction(plan2.getPerson());
			scoringFunction.handleActivity(firstActivity2);
			actCounter.handleEvent(new ActivityStartEvent(secondActivity2.getStartTime(), plan2.getPerson().getId(), null, null, "w"));
			scoringFunction.handleActivity(secondActivity2);
			actCounter.handleEvent(new ActivityStartEvent(thirdActivity2.getStartTime(), plan2.getPerson().getId(), null, null, "h"));
			scoringFunction.handleActivity(thirdActivity2);
			scoringFunction.finish();
			assertEquals("agent-specific activity-score of plan2 should result in the same score as default scoring (tolerance: 3600.)", defaultScorePlan2, scoringFunction.getScore(), MatsimTestUtils.EPSILON);
		}
		
		{
			CountActEventHandler actCounter = new CountActEventHandler();	
			asasConfigGroup.setTolerance(0.);

			AgentSpecificScoringFunctionFactory agentSpecificScoringFunctionFactory = new AgentSpecificScoringFunctionFactory(scenario, actCounter);
			ScoringFunction scoringFunction = agentSpecificScoringFunctionFactory.createNewScoringFunction(plan2.getPerson());
			scoringFunction.handleActivity(firstActivity2);
			actCounter.handleEvent(new ActivityStartEvent(secondActivity2.getStartTime(), plan2.getPerson().getId(), null, null, "w"));
			scoringFunction.handleActivity(secondActivity2);
			actCounter.handleEvent(new ActivityStartEvent(thirdActivity2.getStartTime(), plan2.getPerson().getId(), null, null, "h"));
			scoringFunction.handleActivity(thirdActivity2);
			scoringFunction.finish();
			assertEquals("agent-specific activity-score of plan2 should result in a lower score compared to default scoring (tolerance: 0.)", true, defaultScorePlan2 > scoringFunction.getScore());
			assertEquals("wrong agent-specific activity-score of plan2", 127.3332627918783, scoringFunction.getScore(), MatsimTestUtils.EPSILON);
		}
	
	}
	
	// late arrival penalty != 0.; beta_performing = 0.
	@Test
	public final void test3() {
		
		// Activities
		
		double firstActEnd = 7 * 3600.;
		double secondActEnd = 16 * 3600;
		double travelTime = 900.;
		
		Person person = PopulationUtils.getFactory().createPerson(Id.create("1", Person.class));
		person.getAttributes().putAttribute("OpeningClosingTimes", Double.NEGATIVE_INFINITY + ";" + Double.NEGATIVE_INFINITY + ";26100.0;57600.0;" + Double.NEGATIVE_INFINITY + ";" + Double.NEGATIVE_INFINITY);					

		Plan plan1 = PersonUtils.createAndAddPlan(person, true);;
		Activity firstActivity1 = PopulationUtils.createAndAddActivity(plan1, "h");
		firstActivity1.setEndTime(firstActEnd);
		Activity secondActivity1 = PopulationUtils.createAndAddActivity(plan1, "w");
		secondActivity1.setStartTime(firstActEnd + travelTime);
		secondActivity1.setEndTime(secondActEnd);
		Activity thirdActivity1 = PopulationUtils.createAndAddActivity(plan1, "h");
		thirdActivity1.setStartTime(secondActEnd + travelTime);
		
		Plan plan2 = PersonUtils.createAndAddPlan(person, false);;
		Activity firstActivity2 = PopulationUtils.createAndAddActivity(plan2, "h");
		firstActivity2.setEndTime(firstActEnd);
		Activity secondActivity2 = PopulationUtils.createAndAddActivity(plan2, "w");
		secondActivity2.setStartTime(firstActEnd + travelTime + 1800.);
		secondActivity2.setEndTime(secondActEnd + 1800.);
		Activity thirdActivity2 = PopulationUtils.createAndAddActivity(plan2, "h");
		thirdActivity2.setStartTime(secondActEnd + travelTime);
		
		// Activity parameters
		
		Config config = ConfigUtils.createConfig(new AgentSpecificActivitySchedulingConfigGroup());
		AgentSpecificActivitySchedulingConfigGroup asasConfigGroup = (AgentSpecificActivitySchedulingConfigGroup) config.getModules().get(AgentSpecificActivitySchedulingConfigGroup.GROUP_NAME);
		
		ActivityParams actParamsH = new ActivityParams("h");
		actParamsH.setTypicalDuration(12 * 3600.);
		actParamsH.setLatestStartTime(Double.NEGATIVE_INFINITY);
		actParamsH.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
		config.planCalcScore().addActivityParams(actParamsH);
		
		ActivityParams actParamsW = new ActivityParams("w");
		config.planCalcScore().addActivityParams(actParamsW);
		actParamsW.setTypicalDuration(8 * 3600.);
		actParamsW.setLatestStartTime(Double.NEGATIVE_INFINITY);
		actParamsW.setTypicalDurationScoreComputation(TypicalDurationScoreComputation.relative);
		
		config.planCalcScore().setPerforming_utils_hr(0.);
		config.planCalcScore().setLateArrival_utils_hr(-18.);
		
		// Scenario
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.getPopulation().addPerson(person);
		
		// test
		
		double defaultScorePlan1 = calcScoreDefault(scenario, plan1);
		assertEquals("default activity-related score of plan1 has changed", 0., defaultScorePlan1, MatsimTestUtils.EPSILON);
		
		{
			CountActEventHandler actCounter = new CountActEventHandler();	
			asasConfigGroup.setTolerance(0.);

			AgentSpecificScoringFunctionFactory agentSpecificScoringFunctionFactory = new AgentSpecificScoringFunctionFactory(scenario, actCounter);
			ScoringFunction scoringFunction = agentSpecificScoringFunctionFactory.createNewScoringFunction(plan1.getPerson());
			scoringFunction.handleActivity(firstActivity1);
			actCounter.handleEvent(new ActivityStartEvent(secondActivity1.getStartTime(), plan1.getPerson().getId(), null, null, "w"));
			scoringFunction.handleActivity(secondActivity1);
			actCounter.handleEvent(new ActivityStartEvent(thirdActivity1.getStartTime(), plan1.getPerson().getId(), null, null, "h"));
			scoringFunction.handleActivity(thirdActivity1);
			scoringFunction.finish();
			assertEquals("agent-specific activity-score of plan1 should result in the same score as default scoring", defaultScorePlan1, scoringFunction.getScore(), MatsimTestUtils.EPSILON);
		}
	
		double defaultScorePlan2 = calcScoreDefault(scenario, plan2);
		assertEquals("default activity-related score of plan2 has changed", 0, defaultScorePlan2, MatsimTestUtils.EPSILON);
		
		{
			CountActEventHandler actCounter = new CountActEventHandler();	
			asasConfigGroup.setTolerance(3600.);

			AgentSpecificScoringFunctionFactory agentSpecificScoringFunctionFactory = new AgentSpecificScoringFunctionFactory(scenario, actCounter);
			ScoringFunction scoringFunction = agentSpecificScoringFunctionFactory.createNewScoringFunction(plan2.getPerson());
			scoringFunction.handleActivity(firstActivity2);
			actCounter.handleEvent(new ActivityStartEvent(secondActivity2.getStartTime(), plan2.getPerson().getId(), null, null, "w"));
			scoringFunction.handleActivity(secondActivity2);
			actCounter.handleEvent(new ActivityStartEvent(thirdActivity2.getStartTime(), plan2.getPerson().getId(), null, null, "h"));
			scoringFunction.handleActivity(thirdActivity2);
			scoringFunction.finish();
			assertEquals("agent-specific activity-score of plan2 should result in the same score as default scoring (tolerance: 3600.)", defaultScorePlan2, scoringFunction.getScore(), MatsimTestUtils.EPSILON);
		}
		
		{
			CountActEventHandler actCounter = new CountActEventHandler();	
			asasConfigGroup.setTolerance(0.);
			AgentSpecificScoringFunctionFactory agentSpecificScoringFunctionFactory = new AgentSpecificScoringFunctionFactory(scenario, actCounter);
			ScoringFunction scoringFunction = agentSpecificScoringFunctionFactory.createNewScoringFunction(plan2.getPerson());
			scoringFunction.handleActivity(firstActivity2);
			actCounter.handleEvent(new ActivityStartEvent(secondActivity2.getStartTime(), plan2.getPerson().getId(), null, null, "w"));
			scoringFunction.handleActivity(secondActivity2);
			actCounter.handleEvent(new ActivityStartEvent(thirdActivity2.getStartTime(), plan2.getPerson().getId(), null, null, "h"));
			scoringFunction.handleActivity(thirdActivity2);
			scoringFunction.finish();
			assertEquals("wrong agent-specific activity-score of plan2", -9., scoringFunction.getScore(), MatsimTestUtils.EPSILON);
		}
	
	}
	
	private double calcScoreDefault(Scenario scenario, Plan plan) {
		CharyparNagelScoringFunctionFactory charyparNagelScoringFunctionFactory = new CharyparNagelScoringFunctionFactory(scenario);
		ScoringFunction scoringFunction = charyparNagelScoringFunctionFactory.createNewScoringFunction(plan.getPerson());
		
		for (PlanElement planElement : plan.getPlanElements()) {
			if (planElement instanceof Activity) {
				scoringFunction.handleActivity((Activity) planElement);
			}
		}
		
		scoringFunction.finish();
		return scoringFunction.getScore();
	}
	
}

