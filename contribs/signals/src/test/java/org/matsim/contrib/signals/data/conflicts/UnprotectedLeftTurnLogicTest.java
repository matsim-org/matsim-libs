/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package org.matsim.contrib.signals.data.conflicts;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.SignalSystemsConfigGroup.IntersectionLogic;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author tthunig
 */
public class UnprotectedLeftTurnLogicTest {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public void testSingleIntersectionScenarioWithLeftTurns() {
		// run scenarios from files
		AnalyzeSingleIntersectionLeftTurnDelays restrictedLeftTurns = createControler(IntersectionLogic.CONFLICTING_DIRECTIONS_AND_TURN_RESTRICTIONS);
		AnalyzeSingleIntersectionLeftTurnDelays unrestrictedLeftTurns = createControler(IntersectionLogic.CONFLICTING_DIRECTIONS_NO_TURN_RESTRICTIONS);
		AnalyzeSingleIntersectionLeftTurnDelays noLogic = createControler(IntersectionLogic.NONE);
		
		double leftTurnDelayWTurnRestriction = restrictedLeftTurns.getLeftTurnDelay();
		double leftTurnDelayWoTurnRestriction = unrestrictedLeftTurns.getLeftTurnDelay();
		double leftTurnDelayWithoutLogic = noLogic.getLeftTurnDelay();
		System.out.println("delay wTurn: " + leftTurnDelayWTurnRestriction);
		System.out.println("delay w/oTurn: " + leftTurnDelayWoTurnRestriction);
		System.out.println("delay w/oLogic: " + leftTurnDelayWithoutLogic);
		Assert.assertTrue("Delay without restriction should be less than with restricted left turns.", 2 * leftTurnDelayWoTurnRestriction < leftTurnDelayWTurnRestriction);
		Assert.assertEquals("Delay without turn restriction should be equal to the case without conflicting data.", leftTurnDelayWoTurnRestriction, leftTurnDelayWithoutLogic, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Delay value for the case without turn restrictions is not as expected!", 21120, leftTurnDelayWoTurnRestriction, MatsimTestUtils.EPSILON);
		Assert.assertEquals("Delay value for the case with turn restrictions is not as expected!", 80845, leftTurnDelayWTurnRestriction, MatsimTestUtils.EPSILON);
	}

	private AnalyzeSingleIntersectionLeftTurnDelays createControler(IntersectionLogic conflictingDirectionsAndTurnRestrictions) {
//		Config config = ConfigUtils.loadConfig(testUtils.getPackageInputDirectory() + "singleCrossing/config.xml") ;
		Config config = ConfigUtils.loadConfig("./examples/tutorial/singleCrossingScenario/config.xml");
		SignalSystemsConfigGroup signalsConfigGroup = ConfigUtils.addOrGetModule(config,
				SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		signalsConfigGroup.setSignalControlFile("signalControlFixedTime.xml");
		signalsConfigGroup.setIntersectionLogic(conflictingDirectionsAndTurnRestrictions);
		config.controler().setOutputDirectory(testUtils.getOutputDirectory() + conflictingDirectionsAndTurnRestrictions + "/");
		
		Scenario scenario = ScenarioUtils.loadScenario( config ) ;
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());
		
		Controler controler = new Controler( scenario );
        
		// add the signals module
		controler.addOverridingModule(new SignalsModule());
				
		// add analysis tools
		AnalyzeSingleIntersectionLeftTurnDelays handler = new AnalyzeSingleIntersectionLeftTurnDelays();
		controler.addOverridingModule(new AbstractModule() {			
			@Override
			public void install() {
				this.addEventHandlerBinding().toInstance(handler);
			}
		});
		
		// run the simulation
		controler.run();
		
		return handler;
	}
	
}
