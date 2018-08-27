/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemBasicsTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.oneagent;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Collections;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.contrib.signals.builder.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEventHandler;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LaneEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LaneLeaveEventHandler;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Simple test case for the Controler and or QSim and the default signal system implementation.
 * One agent drives one round a simple test
 * network.
 *
 * @author dgrether
 */
public class QSimSignalTest implements
		LinkEnterEventHandler, SignalGroupStateChangedEventHandler, LaneEnterEventHandler, LaneLeaveEventHandler {

	private static final Logger log = Logger.getLogger(QSimSignalTest.class);

	private double link2EnterTime = Double.NaN;

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	
	/**
	 * Tests the setup with a traffic light that shows all the time green
	 */
	@Test
	public void testTrafficLightIntersection2arms1AgentV20() {
		// configure and load standard scenario
		Scenario scenario = new Fixture().createAndLoadTestScenarioOneSignal(false);
		
		this.link2EnterTime = 38.0;
		runQSimWithSignals(scenario, true);
	}


	/**
	 * Tests the setup with a traffic light that shows red up to second 99 then in sec 100 green. 
	 */
	@Test
	public void testSignalSystems1AgentGreenAtSec100() {		
		// configure and load standard scenario
		Scenario scenario = new Fixture().createAndLoadTestScenarioOneSignal(false);
		// modify scenario
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemControllerData controllerData = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(Fixture.signalSystemId2);
		SignalPlanData planData = controllerData.getSignalPlanData().get(Fixture.signalPlanId2);
		planData.setStartTime(0.0);
		planData.setEndTime(0.0);
		planData.setCycleTime(5 * 3600);
		SignalGroupSettingsData groupData = planData.getSignalGroupSettingsDataByGroupId().get(Fixture.signalGroupId100);
		groupData.setDropping(0);
		groupData.setOnset(100);

		this.link2EnterTime = 100.0;
		runQSimWithSignals(scenario, true);
	}
	
	/**
	 * Tests the setup with a traffic light that shows red less than the specified intergreen time of five seconds.
	 */
	@Test
	public void testIntergreensAbortOneAgentDriving() { // throws RuntimeException {
		//configure and load standard scenario
		Scenario scenario = new Fixture().createAndLoadTestScenarioOneSignal(true);
		// modify scenario
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemControllerData controllerData = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(Fixture.signalSystemId2);
		SignalPlanData planData = controllerData.getSignalPlanData().get(Fixture.signalPlanId2);
		planData.setStartTime(0.0);
		planData.setEndTime(0.0);
		planData.setCycleTime(60);
		SignalGroupSettingsData groupData = planData.getSignalGroupSettingsDataByGroupId().get(Fixture.signalGroupId100);
		groupData.setOnset(0);
		groupData.setDropping(59);	

		assertThatThrownBy(() -> runQSimWithSignals(scenario, false)) //
			.isExactlyInstanceOf(RuntimeException.class) //
			.hasMessage("Exception while processing events. Cannot guarantee that all events have been fully processed." );
		/* another intergreen specific exception should also be thrown, but I do not know how to test for it. theresa, apr'18 :
		 * "SignalSystem Id 2 SignalGroup Id 100 is switched to green at second 60.0 . "
		 *		+ "This is a intergreen conflict with SignalGroup Id 100 switched red/yellow/off at second 59.0 i.e. the intergreen lasts 1.0 seconds.  "
		 *		+ "The minimal intergreen required is, however, 5 seconds." 
		 */
	}
	
	/**
	 * Tests the setup with a traffic light which red time corresponds to the specified intergreen time of five seconds.
	 */
	@Test
	public void testIntergreensNoAbortOneAgentDriving() {
		//configure and load standard scenario
		Scenario scenario = new Fixture().createAndLoadTestScenarioOneSignal(true);
		// modify scenario
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemControllerData controllerData = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(Fixture.signalSystemId2);
		SignalPlanData planData = controllerData.getSignalPlanData().get(Fixture.signalPlanId2);
		planData.setStartTime(0.0);
		planData.setEndTime(0.0);
		planData.setCycleTime(60);
		SignalGroupSettingsData groupData = planData.getSignalGroupSettingsDataByGroupId().get(Fixture.signalGroupId100);
		groupData.setOnset(30);
		groupData.setDropping(25);	
		
		this.link2EnterTime = 38.0;
		runQSimWithSignals(scenario, true);
	}
	
	/**
	 * Tests the setup with two conflicting directions showing green together
	 */
	@Test(expected = RuntimeException.class)
	public void testConflictingDirectionsAbortOneAgentDriving() {
		//configure and load test scenario with data about conflicting directions
		Scenario scenario = new Fixture().createAndLoadTestScenarioTwoSignals(true);

		runQSimWithSignals(scenario, false);
		
		// if this code is reached, no exception has been thrown
		Assert.fail("The simulation should abort because of intergreens violation.");
	}
	
	/**
	 * Tests the setup with two conflicting directions not showing green together
	 */
	@Test
	public void testConflictingDirectionsNoAbortOneAgentDriving() {
		//configure and load test scenario with data about conflicting directions
		Scenario scenario = new Fixture().createAndLoadTestScenarioTwoSignals(true);
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalGroupSettingsData group100setting = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(Fixture.signalSystemId2).getSignalPlanData().get(Fixture.signalPlanId2).getSignalGroupSettingsDataByGroupId().get(Fixture.signalGroupId100);
		group100setting.setOnset(15);

		runQSimWithSignals(scenario, false);
	}

	
	
	private void runQSimWithSignals(final Scenario scenario, boolean handleEvents) throws RuntimeException{
		com.google.inject.Injector injector = Injector.createInjector(scenario.getConfig(), AbstractModule.override(Collections.singleton(new AbstractModule() {
			@Override
			public void install() {
				// defaults
				install(new NewControlerModule());
				install(new ControlerDefaultCoreListenersModule());
				install(new ControlerDefaultsModule());
				install(new ScenarioByInstanceModule(scenario));
			}
		}), new SignalsModule()));
	
		EventsManager events = injector.getInstance(EventsManager.class);
		if (handleEvents){
			events.addHandler(this);
		}

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();

		Mobsim mobsim = injector.getInstance(Mobsim.class);
		mobsim.run();
	}


	@Override
	public void handleEvent(LinkEnterEvent e) {
		log.info("Link id: " + e.getLinkId().toString() + " enter time: " + e.getTime());
		if (e.getLinkId().equals(Fixture.linkId1)){
			Assert.assertEquals(1.0, e.getTime(), MatsimTestUtils.EPSILON);
		}
		else if (e.getLinkId().equals(Fixture.linkId2)){
			Assert.assertEquals(this.link2EnterTime, e.getTime(), MatsimTestUtils.EPSILON);
		}
	}

	@Override
	public void reset(int iteration) {
	}

	@Override
	public void handleEvent(SignalGroupStateChangedEvent event) {
		log.info("State changed : "  + event.getTime() + " " + event.getSignalSystemId() + " " + event.getSignalGroupId() + " " + event.getNewState());
	}


	@Override
	public void handleEvent(LaneLeaveEvent e) {
		log.info("Leave Lane id: " + e.getLaneId().toString() + " enter time: " + e.getTime());
	}


	@Override
	public void handleEvent(LaneEnterEvent e) {
		log.info("Enter Lane id: " + e.getLaneId().toString() + " enter time: " + e.getTime());
	}

}
