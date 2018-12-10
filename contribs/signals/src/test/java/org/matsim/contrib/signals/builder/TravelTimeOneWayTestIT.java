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
package org.matsim.contrib.signals.builder;

import java.util.Collections;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.LinkEnterEvent;
import org.matsim.api.core.v01.events.handler.LinkEnterEventHandler;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalGroupSettingsData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalPlanData;
import org.matsim.contrib.signals.data.signalgroups.v20.SignalSystemControllerData;
import org.matsim.contrib.signals.model.SignalGroup;
import org.matsim.contrib.signals.model.SignalPlan;
import org.matsim.contrib.signals.model.SignalSystem;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.ControlerDefaultsModule;
import org.matsim.core.controler.Injector;
import org.matsim.core.controler.NewControlerModule;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.controler.corelisteners.ControlerDefaultCoreListenersModule;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.handler.EventHandler;
import org.matsim.core.mobsim.framework.Mobsim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.scenario.ScenarioByInstanceModule;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author aneumann
 * @author dgrether
 * @author tthunig
 */
public class TravelTimeOneWayTestIT {

	private static final Logger log = Logger.getLogger(TravelTimeOneWayTestIT.class);

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public void testSignalOutflow_withLanes() {
		runAndTestDifferentGreensplitSignals(this.loadAllGreenScenario(true));
	}

	@Test
	public void testSignalOutflow_woLanes() {
		runAndTestDifferentGreensplitSignals(this.loadAllGreenScenario(false));
	}
	
	@Test
	public void testAllGreenSignalVsNoSignal_withLanes() {
		runAndCompareAllGreenWithNoSignals(this.loadAllGreenScenario(true));
	}
	
	@Test
	public void testAllGreenSignalVsNoSignal_woLanes() {
		runAndCompareAllGreenWithNoSignals(this.loadAllGreenScenario(false));
	}

	private Scenario loadAllGreenScenario(boolean useLanes) {
		Config conf = ConfigUtils.createConfig(testUtils.classInputResourcePath());
		conf.controler().setOverwriteFileSetting(OverwriteFileSetting.overwriteExistingFiles);
		conf.controler().setMobsim("qsim");
		conf.network().setInputFile("network.xml");
		conf.plans().setInputFile("plans.xml.gz");
		conf.qsim().setStuckTime(1000);
		conf.qsim().setRemoveStuckVehicles(false);
		conf.qsim().setUsingFastCapacityUpdate(false);

		SignalSystemsConfigGroup signalsConfig = ConfigUtils.addOrGetModule(conf, SignalSystemsConfigGroup.GROUP_NAME, SignalSystemsConfigGroup.class );
		signalsConfig.setUseSignalSystems(true);
		if (useLanes) {
			conf.network().setLaneDefinitionsFile("testLaneDefinitions_v2.0.xml");
			conf.qsim().setUseLanes(true);
			signalsConfig.setSignalSystemFile("testSignalSystems_v2.0.xml");
		} else {
			signalsConfig.setSignalSystemFile("testSignalSystemsNoLanes_v2.0.xml");
		}
		signalsConfig.setSignalGroupsFile("testSignalGroups_v2.0.xml");
		signalsConfig.setSignalControlFile("testSignalControl_v2.0.xml");
		signalsConfig.setAmberTimesFile("testAmberTimes_v1.0.xml");

		Scenario scenario = ScenarioUtils.loadScenario(conf);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(conf).loadSignalsData());
		return scenario;
	}

	private static void runAndCompareAllGreenWithNoSignals(final Scenario scenario) {
		// with signals
		StubLinkEnterEventHandler stubLinkEnterEventHandler = new StubLinkEnterEventHandler();
		runQsimWithSignals(scenario, stubLinkEnterEventHandler);
		MeasurementPoint resultsWithSignals = stubLinkEnterEventHandler.beginningOfLink2;
		
		// without signals
		EventsManager events = EventsUtils.createEventsManager();
		StubLinkEnterEventHandler eventHandler = new StubLinkEnterEventHandler();
		events.addHandler(eventHandler);

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		new QSimBuilder(scenario.getConfig()) //
			.useDefaults() //
			.build(scenario, events)
			.run();
		MeasurementPoint resultsWoSignals = eventHandler.beginningOfLink2;
		if (resultsWoSignals != null) {
			log.debug("tF = 60s, " + resultsWoSignals.numberOfVehPassedDuringTimeToMeasure + ", " + resultsWoSignals.numberOfVehPassed + ", "
					+ resultsWoSignals.firstVehPassTime_s + ", " + resultsWoSignals.lastVehPassTime_s);
		} else {
			Assert.fail("seems like no LinkEnterEvent was handled, as this.beginningOfLink2 is not set.");
		}
		
		// compare values
		Assert.assertEquals(5000.0, resultsWithSignals.numberOfVehPassed, MatsimTestUtils.EPSILON);
		Assert.assertEquals(resultsWithSignals.firstVehPassTime_s, resultsWoSignals.firstVehPassTime_s, MatsimTestUtils.EPSILON);
		Assert.assertEquals(resultsWithSignals.numberOfVehPassed, resultsWoSignals.numberOfVehPassed, MatsimTestUtils.EPSILON);
		Assert.assertEquals(resultsWithSignals.numberOfVehPassedDuringTimeToMeasure, resultsWoSignals.numberOfVehPassedDuringTimeToMeasure, MatsimTestUtils.EPSILON);
	}

	private static void runAndTestDifferentGreensplitSignals(final Scenario scenario) {
		SignalsData signalsData = (SignalsData) scenario.getScenarioElement(SignalsData.ELEMENT_NAME);
		SignalSystemControllerData controllerData = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(Id.create(2, SignalSystem.class));
		SignalPlanData signalPlan = controllerData.getSignalPlanData().get(Id.create(2, SignalPlan.class));
		SignalGroupSettingsData signalSetting = signalPlan.getSignalGroupSettingsDataByGroupId().get(Id.create(100, SignalGroup.class));

		int circulationTime = signalPlan.getCycleTime();
		double linkCapacity = scenario.getNetwork().getLinks().get(Id.createLinkId(1)).getCapacity();

		// This tests if a QSim with signals creates the correct outflow for all green splits between 1/6 and 1.
		for (int dropping = 10; dropping <= circulationTime; dropping++) {
			signalSetting.setDropping(dropping);

			StubLinkEnterEventHandler stubLinkEnterEventHandler = new StubLinkEnterEventHandler();
			runQsimWithSignals(scenario, stubLinkEnterEventHandler);
			
			log.debug("circulationTime: " + circulationTime);
			log.debug("dropping  : " + dropping);

			Assert.assertEquals((dropping * linkCapacity / circulationTime), stubLinkEnterEventHandler.beginningOfLink2.numberOfVehPassedDuringTimeToMeasure, 1.0);
			Assert.assertEquals(5000.0, stubLinkEnterEventHandler.beginningOfLink2.numberOfVehPassed, MatsimTestUtils.EPSILON);
		}
	}

	private static void runQsimWithSignals(final Scenario scenario, EventHandler... eventHandlers) {
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
		events.initProcessing();
		for (EventHandler handler : eventHandlers){
			events.addHandler(handler);
		}

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		Mobsim mobsim = injector.getInstance(Mobsim.class);
		mobsim.run();
	}

	/* package */ static class StubLinkEnterEventHandler implements LinkEnterEventHandler {

		public MeasurementPoint beginningOfLink2 = null;

		@Override
		public void handleEvent(LinkEnterEvent event) {
			// log.info("link enter event id :" + event.linkId);
			if (event.getLinkId().toString().equalsIgnoreCase("2")) {
				if (this.beginningOfLink2 == null) {
					// Make sure measurement starts with second 0 in signalsystemplan
					double nextSignalCycleStart = event.getTime() + (60 - (event.getTime() % 60));
					this.beginningOfLink2 = new MeasurementPoint(nextSignalCycleStart);
				}

				this.beginningOfLink2.numberOfVehPassed++;

				if (this.beginningOfLink2.timeToStartMeasurement <= event.getTime()) {

					if (this.beginningOfLink2.firstVehPassTime_s == -1) {
						this.beginningOfLink2.firstVehPassTime_s = event.getTime();
					}

					if (event.getTime() < this.beginningOfLink2.timeToStartMeasurement + MeasurementPoint.timeToMeasure_s) {
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

		static final int timeToMeasure_s = 3600; // 1 hour

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
