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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.contrib.signals.builder.DefaultSignalModelFactory;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEvent;
import org.matsim.contrib.signals.events.SignalGroupStateChangedEventHandler;
import org.matsim.contrib.signals.mobsim.QSimSignalEngine;
import org.matsim.contrib.signals.mobsim.SignalEngine;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.api.experimental.events.LaneEnterEvent;
import org.matsim.core.api.experimental.events.LaneLeaveEvent;
import org.matsim.core.api.experimental.events.handler.LaneEnterEventHandler;
import org.matsim.core.api.experimental.events.handler.LaneLeaveEventHandler;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Simple test case for the Controler and or QSim and the default signal system implementation.
 * One agent drives one round a simple test
 * network.
 *
 * @author dgrether
 */
public class QSimTest implements
		LinkEnterEventHandler, SignalGroupStateChangedEventHandler, LaneEnterEventHandler, LaneLeaveEventHandler {

	private static final Logger log = Logger.getLogger(QSimTest.class);

	private double link2EnterTime = Double.NaN;

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	
	/**
	 * Tests the setup with a traffic light that shows all the time green
	 */
	@Test
	public void testTrafficLightIntersection2arms1AgentV20() {
		// configure and load standard scenario
		Scenario scenario = new Fixture().createAndLoadTestScenario(true);
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(this);
		this.link2EnterTime = 38.0;
		
		FromDataBuilder builder = new FromDataBuilder(scenario, new DefaultSignalModelFactory(), events);
		SignalSystemsManager manager = builder.createAndInitializeSignalSystemsManager();
		SignalEngine engine = new QSimSignalEngine(manager);

		QSim qsim = (QSim) QSimUtils.createDefaultQSim(scenario, events);
		qsim.addQueueSimulationListeners(engine);
		qsim.run();
	}
	

	/**
	 * Tests the setup with a traffic light that shows red up to second 99 then in sec 100 green. 
	 */
	@Test
	public void testSignalSystems1AgentGreenAtSec100() {
		// configure and load standard scenario
		Scenario scenario = new Fixture().createAndLoadTestScenario(false);
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
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(this);
		this.link2EnterTime = 100.0;
		
		FromDataBuilder builder = new FromDataBuilder(scenario, new DefaultSignalModelFactory(), events);
		SignalSystemsManager manager = builder.createAndInitializeSignalSystemsManager();
		SignalEngine engine = new QSimSignalEngine(manager);

		QSim qsim = (QSim) QSimUtils.createDefaultQSim(scenario, events);
		qsim.addQueueSimulationListeners(engine);
		qsim.run();
		
//		// configure and load standard scenario
//		Scenario scenario = new Fixture().createAndLoadTestScenario(false);
//
//		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
//		SignalSystemControllerData controllerData = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(Fixture.signalSystemId2);
//		SignalPlanData planData = controllerData.getSignalPlanData().get(Fixture.signalPlanId2);
//		planData.setStartTime(0.0);
//		planData.setEndTime(0.0);
//		planData.setCycleTime(5 * 3600);
//		SignalGroupSettingsData groupData = planData.getSignalGroupSettingsDataByGroupId().get(Fixture.signalGroupId100);
//		groupData.setDropping(1);
//		groupData.setOnset(100);
//
//		com.google.inject.Injector injector = createInjector(scenario);
//
//		EventsManager events = injector.getInstance(EventsManager.class);
//		events.addHandler(this);
//		this.link2EnterTime = 100.0;
//		events.initProcessing();
//		
//		injector.getInstance(Mobsim.class).run();
//		// TODO why are there no signal events?
	}
	
	private com.google.inject.Injector createInjector(final Scenario scenario) {
		return Injector.createInjector(scenario.getConfig(), new AbstractModule() {
			@Override
			public void install() {
				// defaults
				install(new NewControlerModule());
				install(new ControlerDefaultCoreListenersModule());
				install(new ControlerDefaultsModule());
				install(new ScenarioByInstanceModule(scenario));
				// signal specific module
				install(new SignalsModule());
			}
		});
	}
	
	/**
	 * Tests the setup with a traffic light that shows red less than the specified intergreen time of five seconds.
	 */
	@Test
	public void testIntergreensAbortOneAgentDriving() {
		//configure and load standard scenario
		Scenario scenario = new Fixture().createAndLoadTestScenario(true);
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
		
		EventsManager events = EventsUtils.createEventsManager();

		FromDataBuilder builder = new FromDataBuilder(scenario, new DefaultSignalModelFactory(), events);
		SignalSystemsManager manager = builder.createAndInitializeSignalSystemsManager();
		SignalEngine engine = new QSimSignalEngine(manager);

		QSim qsim = (QSim) QSimUtils.createDefaultQSim(scenario, events);
		qsim.addQueueSimulationListeners(engine);
		Exception ex = null;
		try{
			qsim.run();
		} catch (Exception e){
			log.info(e.getMessage());
			ex = e;
		}
		Assert.assertNotNull(ex);
	}
	
	/**
	 * Tests the setup with a traffic light which red time corresponds to the specified intergreen time of five seconds.
	 */
	@Test
	public void testIntergreensNoAbortOneAgentDriving() {
		//configure and load standard scenario
		Scenario scenario = new Fixture().createAndLoadTestScenario(true);
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
		
		EventsManager events = EventsUtils.createEventsManager();
		events.addHandler(this);
		this.link2EnterTime = 38.0;
		
		FromDataBuilder builder = new FromDataBuilder(scenario, new DefaultSignalModelFactory(), events);
		SignalSystemsManager manager = builder.createAndInitializeSignalSystemsManager();
		SignalEngine engine = new QSimSignalEngine(manager);

		QSim qsim = (QSim) QSimUtils.createDefaultQSim(scenario, events);
		qsim.addQueueSimulationListeners(engine);
		qsim.run();
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
