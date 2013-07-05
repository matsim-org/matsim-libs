/* *********************************************************************** *
 * project: org.matsim.*
 * ControlerTests
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
package org.matsim.signalsystems.oneagent;

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.EventsUtils;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.testcases.MatsimTestUtils;


/**
 * @author dgrether
 *
 */
public class ControlerTests {
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	/**
	 * Tests the setup with a traffic light that shows all the time green
	 */
	@Test
	public void testModifySignalControlDataOnsetOffset() {
		//configure and load standard scenario
		Fixture fixture = new Fixture();
		Scenario scenario = fixture.createAndLoadTestScenario(false);
		scenario.getConfig().controler().setFirstIteration(0);
		scenario.getConfig().controler().setLastIteration(1);
		scenario.getConfig().controler().setOutputDirectory(testUtils.getOutputDirectory());
		
		SignalsData signalsData = scenario.getScenarioElement(SignalsData.class);
		
//		SignalSystemControllerData controllerData = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(id2);
//		SignalPlanData planData = controllerData.getSignalPlanData().get(id2);
//		planData.setStartTime(0.0);
//		planData.setEndTime(0.0);
//		planData.setCycleTime(5 * 3600);
//		SignalGroupSettingsData groupData = planData.getSignalGroupSettingsDataByGroupId().get(id100);
//		groupData.setDropping(0);
//		groupData.setOnset(100);
		
		EventsManager events = EventsUtils.createEventsManager();
//		events.addHandler(this);
//		this.link2EnterTime = 100.0;
		
		Controler controler = new Controler(scenario);
		controler.run();
		
	}
}
