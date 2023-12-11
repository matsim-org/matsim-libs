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
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.replanning.ReplanningContext;
import org.matsim.core.router.*;
import org.matsim.core.router.util.TravelDisutility;
import org.matsim.core.router.util.TravelTime;
import org.matsim.core.scoring.ScoringFunctionFactory;
import org.matsim.core.scoring.functions.CharyparNagelScoringFunctionFactory;
import org.matsim.core.trafficmonitoring.FreeSpeedTravelTime;
import org.matsim.testcases.MatsimTestUtils;

import jakarta.inject.Provider;

public class BestReplyLocationChoicePlanStrategyTest {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testOne() {

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
//		controler.getTestScenarioURL().addScenarioElement(DestinationChoiceBestResponseContext.ELEMENT_NAME, lcContext);
//		controler.getTestScenarioURL().addScenarioElement(MaxDCScoreWrapper.ELEMENT_NAME, dcScore);
//
//		BestReplyLocationChoicePlanStrategy planStrategy = new BestReplyLocationChoicePlanStrategy(scenario);
//
//		planStrategy.init(new ReplanningContextImpl(scenario));
//
//		for (Person person : scenario.getPopulation().getPersons().values()) planStrategy.run(person);
	}

}
