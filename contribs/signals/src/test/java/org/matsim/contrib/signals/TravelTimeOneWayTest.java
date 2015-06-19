/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeTestOneWay
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
package org.matsim.contrib.signals;

import junit.framework.Assert;
import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.mobsim.QSimSignalEngine;
import org.matsim.contrib.signals.mobsim.SignalEngine;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.SignalSystemsConfigGroup;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.run.LaneDefinitonsV11ToV20Converter;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author aneumann
 * @author dgrether
 */
public class TravelTimeOneWayTest {

	private static final Logger log = Logger.getLogger(TravelTimeOneWayTest.class);

	final static int timeToWaitBeforeMeasure = 498; // Make sure measurement starts with second 0 in signalsystemplan

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	private Scenario loadScenario(boolean useLanes) {
		Config conf = new Config();
		conf.addCoreModules();
		conf.controler().setMobsim("qsim");
		conf.network().setInputFile(this.testUtils.getClassInputDirectory() + "network.xml.gz");
		conf.plans().setInputFile(this.testUtils.getClassInputDirectory() + "plans.xml.gz");
		String signalSystemsFile = null;
		if (useLanes){
				String laneDefinitions = testUtils.getClassInputDirectory() + "testLaneDefinitions_v1.1.xml";
				String lanes20 = testUtils.getOutputDirectory() + "testLaneDefinitions_v2.0.xml";
				new LaneDefinitonsV11ToV20Converter().convert(laneDefinitions,lanes20, conf.network().getInputFile());
				conf.network().setLaneDefinitionsFile(lanes20);
				conf.scenario().setUseLanes(true);
				signalSystemsFile = testUtils.getClassInputDirectory() + "testSignalSystems_v2.0.xml";
		}
		else {
			signalSystemsFile = testUtils.getClassInputDirectory() + "testSignalSystemsNoLanes_v2.0.xml";
		}
		conf.qsim().setStuckTime(1000);
		conf.qsim().setRemoveStuckVehicles(false);
		conf.scenario().setUseSignalSystems(true);

		SignalSystemsConfigGroup signalsConfig = conf.signalSystems();
		signalsConfig.setSignalSystemFile(signalSystemsFile);
		String signalGroupsFile = testUtils.getClassInputDirectory() + "testSignalGroups_v2.0.xml";
		String signalControlFile = testUtils.getClassInputDirectory() + "testSignalControl_v2.0.xml";
		String amberTimesFile = testUtils.getClassInputDirectory() + "testAmberTimes_v1.0.xml";
		signalsConfig.setSignalGroupsFile(signalGroupsFile);
		signalsConfig.setSignalControlFile(signalControlFile);
		signalsConfig.setAmberTimesFile(amberTimesFile);
		
		ScenarioImpl data = (ScenarioImpl) ScenarioUtils.loadScenario(conf);
		data.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsScenarioLoader(signalsConfig).loadSignalsData());
		return data;
	}

	private SignalEngine initSignalEngine(Scenario scenario, EventsManager events) {
		FromDataBuilder builder = new FromDataBuilder(scenario, events);
		SignalSystemsManager manager = builder.createAndInitializeSignalSystemsManager();
		SignalEngine engine = new QSimSignalEngine(manager);
		return engine;
	}

	private void runTrafficLightIntersection2arms_w_TrafficLight_0_60(ScenarioImpl scenario){
		EventsManager events = EventsUtils.createEventsManager();
		StubLinkEnterEventHandler eventHandler = new StubLinkEnterEventHandler();
		events.addHandler(eventHandler);
//		events.addHandler(new LogOutputEventHandler());
		int circulationTime = 60;

		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		
		for (int dropping = 10; dropping <= circulationTime; dropping++) {
			eventHandler.reset(1);

			SignalSystemControllerData controllerData = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(Id.create(2, SignalSystem.class));
			SignalPlanData signalPlan = controllerData.getSignalPlanData().get(Id.create(2, SignalPlan.class));
			signalPlan.setCycleTime(circulationTime);
			signalPlan.getSignalGroupSettingsDataByGroupId().get(Id.create(100, SignalGroup.class)).setDropping(dropping);
			signalPlan.setStartTime(0.0);
			signalPlan.setEndTime(0.0);

			//build the signal model
			FromDataBuilder builder = new FromDataBuilder(scenario, events);
			SignalSystemsManager manager = builder.createAndInitializeSignalSystemsManager();
			SignalEngine signalEngine = new QSimSignalEngine(manager);
			//run the qsim
			QSim sim = (QSim) QSimUtils.createDefaultQSim(scenario, events);
			sim.addQueueSimulationListeners(signalEngine);
			sim.run();
			log.debug("circulationTime: " + circulationTime);
			log.debug("dropping  : " + dropping);

			Assert.assertEquals((dropping * 2000.0 / circulationTime),
					eventHandler.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure, 1.0);
			Assert.assertEquals(5000.0, eventHandler.beginningOfLink2.numberOfVehPassed,
					MatsimTestUtils.EPSILON);
		}
	}
	
	/**
	 * 
	 * @author aneumann
	 * @author dgrether
	 */
	@Test
	public void testTrafficLightIntersection2arms_w_TrafficLight_0_60() {
		ScenarioImpl scenario = (ScenarioImpl) this.loadScenario(false);
		this.runTrafficLightIntersection2arms_w_TrafficLight_0_60(scenario);
		
		scenario = (ScenarioImpl) this.loadScenario(true);
		this.runTrafficLightIntersection2arms_w_TrafficLight_0_60(scenario);
		
	}

	
	private void runTrafficLightIntersection2arms_w_TrafficLight(Scenario scenario){
		//test with signal systems
		EventsManager events = EventsUtils.createEventsManager();
		StubLinkEnterEventHandler eventHandler = new StubLinkEnterEventHandler();
		events.addHandler(eventHandler);

		SignalEngine signalEngine = this.initSignalEngine(scenario, events);

		QSim sim = (QSim) QSimUtils.createDefaultQSim(scenario, events);
		sim.addQueueSimulationListeners(signalEngine);
		sim.run();
		MeasurementPoint qSimMeasurementPoint = eventHandler.beginningOfLink2;
		
	
		//test without signal systems 
		events = EventsUtils.createEventsManager();
		eventHandler = new StubLinkEnterEventHandler();
		events.addHandler(eventHandler);
		QSimUtils.createDefaultQSim(scenario, events).run();
		if (eventHandler.beginningOfLink2 != null) {
			log.debug("tF = 60s, " + eventHandler.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure
					+ ", " + eventHandler.beginningOfLink2.numberOfVehPassed + ", "
					+ eventHandler.beginningOfLink2.firstVehPassTime_s + ", "
					+ eventHandler.beginningOfLink2.lastVehPassTime_s);
		}
		else {
			Assert.fail("seems like no LinkEnterEvent was handled, as this.beginningOfLink2 is not set.");
		}
		MeasurementPoint queueSimulation = eventHandler.beginningOfLink2;
		// circle time is 60s, green 60s
		Assert.assertEquals(5000.0, qSimMeasurementPoint.numberOfVehPassed, MatsimTestUtils.EPSILON);

		Assert.assertEquals(qSimMeasurementPoint.firstVehPassTime_s, queueSimulation.firstVehPassTime_s,
				MatsimTestUtils.EPSILON);
		Assert.assertEquals(qSimMeasurementPoint.numberOfVehPassed, queueSimulation.numberOfVehPassed,
				MatsimTestUtils.EPSILON);
		Assert.assertEquals(qSimMeasurementPoint.numberOfVehPassedDuringTimeToMeasure,
				queueSimulation.numberOfVehPassedDuringTimeToMeasure, MatsimTestUtils.EPSILON);
	}
	
	/**
	 * This tests if a QSim with signals that are all the time green produces the same
	 * result as a QSim without any signals.
	 * 
	 * @author aneumann
	 * @author dgrether
	 */
	@Test
	public void testTrafficLightIntersection2arms_w_TrafficLight() {
		Scenario scenario = this.loadScenario(false);
		this.runTrafficLightIntersection2arms_w_TrafficLight(scenario);
		
		scenario = this.loadScenario(true);
		this.runTrafficLightIntersection2arms_w_TrafficLight(scenario);
	}

	/* package */ static class StubLinkEnterEventHandler implements LinkEnterEventHandler {

		public MeasurementPoint beginningOfLink2 = null;

		@Override
		public void handleEvent(LinkEnterEvent event) {
			// log.info("link enter event id :" + event.linkId);
			if (event.getLinkId().toString().equalsIgnoreCase("2")) {
				if (this.beginningOfLink2 == null) {
					this.beginningOfLink2 = new MeasurementPoint(event.getTime()
							+ TravelTimeOneWayTest.timeToWaitBeforeMeasure);
				}

				this.beginningOfLink2.numberOfVehPassed++;

				if (this.beginningOfLink2.timeToStartMeasurement <= event.getTime()) {

					if (this.beginningOfLink2.firstVehPassTime_s == -1) {
						this.beginningOfLink2.firstVehPassTime_s = event.getTime();
					}

					if (event.getTime() < this.beginningOfLink2.timeToStartMeasurement
							+ MeasurementPoint.timeToMeasure_s) {
						this.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure++;
						this.beginningOfLink2.lastVehPassTime_s = event.getTime();
					}
				}
			}
		}

		@Override
		public void reset(int iteration) {
			this.beginningOfLink2 = null;
		}
	}

	private static class MeasurementPoint {

		static final int timeToMeasure_s = 3600;

		double timeToStartMeasurement;

		double firstVehPassTime_s = -1;

		double lastVehPassTime_s;

		int numberOfVehPassed = 0;

		int numberOfVehPassedDuringTimeToMeasure = 0;

		public MeasurementPoint(double time) {
			this.timeToStartMeasurement = time;
		}
	}


	

}
