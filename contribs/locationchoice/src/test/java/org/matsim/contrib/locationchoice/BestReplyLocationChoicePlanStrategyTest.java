/* *********************************************************************** *
 * project: org.matsim.*
 * BestReplyLocationChoicePlanStrategyTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

package org.matsim.contrib.locationchoice;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.*;
import org.matsim.core.router.costcalculators.RandomizingTimeDistanceTravelDisutility;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.testcases.MatsimTestUtils;

import javax.inject.Provider;

public class BestReplyLocationChoicePlanStrategyTest {
	
	@Rule
	public MatsimTestUtils utils = new MatsimTestUtils();
		
	@Test
	public void testOne() {
		
//		Config config = ConfigUtils.loadConfig(this.utils.getPackageInputDirectory() + "config.xml");
//		Scenario scenario = ScenarioUtils.loadScenario(config);
//		
//		// joint context (based on scenario):
//		final DestinationChoiceBestResponseContext lcContext = new DestinationChoiceBestResponseContext(scenario) ;
//		lcContext.init();
//
//		// CONTROL(L)ER - only used to pass the config to ReadOrComputeMaxDCScore
//		Controler controler = new Controler(scenario);
//
//		ReadOrComputeMaxDCScore computer = new ReadOrComputeMaxDCScore(lcContext);
//  		computer.readOrCreateMaxDCScore(controler, lcContext.kValsAreRead());
//  		final ObjectAttributes personsMaxDCScoreUnscaled = computer.getPersonsMaxEpsUnscaled();
//		
//		MaxDCScoreWrapper dcScore = new MaxDCScoreWrapper();
//		dcScore.setPersonsMaxDCScoreUnscaled(personsMaxDCScoreUnscaled);
//		controler.getScenario().addScenarioElement(DestinationChoiceBestResponseContext.ELEMENT_NAME, lcContext);
//		controler.getScenario().addScenarioElement(MaxDCScoreWrapper.ELEMENT_NAME, dcScore);
//		
//		BestReplyLocationChoicePlanStrategy planStrategy = new BestReplyLocationChoicePlanStrategy(scenario);	
//		
//		planStrategy.init(new ReplanningContextImpl(scenario));
//
//		for (Person person : scenario.getPopulation().getPersons().values()) planStrategy.run(person);
	}
	
	private static class ReplanningContextImpl implements ReplanningContext {
		
		private final TravelTime travelTime;
		private final TravelDisutility travelDisutility;
		private final ScoringFunctionFactory scoringFunctionFactory;
		private final Provider<TripRouter> tripRouterFactory;
		
		public ReplanningContextImpl(Scenario scenario) {
			this.travelTime = new FreeSpeedTravelTime();
			this.travelDisutility = new RandomizingTimeDistanceTravelDisutility.Builder().createTravelDisutility(this.travelTime, scenario.getConfig().planCalcScore());
			this.scoringFunctionFactory = new CharyparNagelScoringFunctionFactory( scenario );
			this.tripRouterFactory = new TripRouterFactoryBuilderWithDefaults().build(scenario);
		}
				
		@Override
		public TravelDisutility getTravelDisutility() {
			return this.travelDisutility;
		}

		@Override
		public TravelTime getTravelTime() {
			return this.travelTime;
		}

		@Override
		public ScoringFunctionFactory getScoringFunctionFactory() {
			return this.scoringFunctionFactory;
		}

		@Override
		public int getIteration() {
			return 0;
		}

		@Override
		public TripRouter getTripRouter() {
			return this.tripRouterFactory.get();
		}
	}
}
