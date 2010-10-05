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
package org.matsim.signalsystems;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.SignalSystemsConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.signalsystems.builder.FromDataBuilder;
import org.matsim.signalsystems.config.PlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.SignalGroupSettings;
import org.matsim.signalsystems.config.SignalSystemConfiguration;
import org.matsim.signalsystems.config.SignalSystemConfigurations;
import org.matsim.signalsystems.config.SignalSystemPlan;
import org.matsim.signalsystems.data.SignalsData;
import org.matsim.signalsystems.data.SignalsScenarioLoader;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalGroupSettingsData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalPlanData;
import org.matsim.signalsystems.data.signalcontrol.v20.SignalSystemControllerData;
import org.matsim.signalsystems.model.QSimSignalEngine;
import org.matsim.signalsystems.model.SignalEngine;
import org.matsim.signalsystems.model.SignalSystemsManager;
import org.matsim.testcases.MatsimTestUtils;

/**
 * Simple test case for the QueueSim signal system implementation.
 * One agent drives one round in the signal system default simple test
 * network.
 *
 * @author dgrether
 */
public class SignalSystemsOneAgentTest implements
		LinkEnterEventHandler {

	private static final Logger log = Logger.getLogger(SignalSystemsOneAgentTest.class);

	private Id id1 = new IdImpl(1);
	private Id id2 = new IdImpl(2);
	private Id id100 = new IdImpl(100);
	
	private double link2EnterTime = Double.NaN;

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
//	@Test
	public void testTrafficLightIntersection2arms1Agent() {
		String plansFile = testUtils.getClassInputDirectory() + "plans1Agent.xml";
		String laneDefinitions = testUtils.getClassInputDirectory() + "testLaneDefinitions_v1.1.xml";
		String lsaDefinition = testUtils.getClassInputDirectory() + "testSignalSystems_v1.1.xml";
		String lsaConfig = testUtils.getClassInputDirectory() + "testSignalSystemConfigurations_v1.1.xml";

		ScenarioImpl scenario = new ScenarioImpl();
		Config conf = scenario.getConfig();
		conf.network().setInputFile(testUtils.getClassInputDirectory() + "network.xml.gz");
		conf.network().setLaneDefinitionsFile(laneDefinitions);
		conf.signalSystems().setSignalSystemFile(lsaDefinition);
		conf.signalSystems().setSignalSystemConfigFile(lsaConfig);
		conf.plans().setInputFile(plansFile);
		conf.scenario().setUseLanes(true);
		conf.scenario().setUseSignalSystems(true);
		conf.setQSimConfigGroup(new QSimConfigGroup());
		conf.getQSimConfigGroup().setStuckTime(1000);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);

		loader.loadScenario();

		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(this);
		this.link2EnterTime = 38.0;
		
		SignalSystemConfigurations lssConfigs = scenario.getSignalSystemConfigurations();
		for (SignalSystemConfiguration lssConfig : lssConfigs.getSignalSystemConfigurations().values()) {
			PlanBasedSignalSystemControlInfo controlInfo = (PlanBasedSignalSystemControlInfo) lssConfig
					.getControlInfo();
			SignalSystemPlan p = controlInfo.getPlans().get(new IdImpl("2"));
			p.setCycleTime(60);
			SignalGroupSettings group = p.getGroupConfigs().get(new IdImpl("100"));
			group.setDropping(60);
		}

		new QSim(scenario, events).run();
	}
	
	private Scenario createAndLoadTestScenario(){
		String plansFile = testUtils.getClassInputDirectory() + "plans1Agent.xml";
		String laneDefinitions = testUtils.getClassInputDirectory() + "testLaneDefinitions_v1.1.xml";
		ScenarioImpl scenario = new ScenarioImpl();
		Config conf = scenario.getConfig();
		conf.network().setInputFile(testUtils.getClassInputDirectory() + "network.xml.gz");
		conf.network().setLaneDefinitionsFile(laneDefinitions);
		conf.plans().setInputFile(plansFile);
		conf.scenario().setUseLanes(true);
		//as signals are configured below we don't need signals on
		conf.scenario().setUseSignalSystems(false);
		conf.setQSimConfigGroup(new QSimConfigGroup());
		conf.getQSimConfigGroup().setStuckTime(1000);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadScenario();
		return scenario;
	}
	
	private void setSignalSystemConfigValues(SignalSystemsConfigGroup signalsConfig){
		String signalSystemsFile = testUtils.getClassInputDirectory() + "testSignalSystems_v2.0.xml";
		String signalGroupsFile = testUtils.getClassInputDirectory() + "testSignalGroups_v2.0.xml";
		String signalControlFile = testUtils.getClassInputDirectory() + "testSignalControl_v2.0.xml";
		String amberTimesFile = testUtils.getClassInputDirectory() + "testAmberTimes_v1.0.xml";
		signalsConfig.setSignalSystemFile(signalSystemsFile);
		signalsConfig.setSignalGroupsFile(signalGroupsFile);
		signalsConfig.setSignalControlFile(signalControlFile);
		signalsConfig.setAmberTimesFile(amberTimesFile);
	}

	/**
	 * Tests the setup with a traffic light that shows all the time green
	 */
	@Test
	public void testTrafficLightIntersection2arms1AgentV20() {
		//configure and load standard scenario
		Scenario scenario = this.createAndLoadTestScenario();
		SignalSystemsConfigGroup signalsConfig = scenario.getConfig().signalSystems();
		this.setSignalSystemConfigValues(signalsConfig);
		
		SignalsScenarioLoader signalsLoader = new SignalsScenarioLoader(signalsConfig);
		SignalsData signalsData = signalsLoader.loadSignalsData();
		
		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(this);
		this.link2EnterTime = 38.0;
		
		FromDataBuilder builder = new FromDataBuilder(signalsData, events);
		SignalSystemsManager manager = builder.createAndInitializeSignalSystemsManager();
		SignalEngine engine = new QSimSignalEngine(manager);
		
		QSim qsim = new QSim(scenario, events);
		qsim.addQueueSimulationListeners(engine);
		qsim.run();
	}
	

	/**
	 * Tests the setup with a traffic light that shows all the time green
	 */
	@Test
	public void testSignalSystems1AgentGreenAtSec100() {
		//configure and load standard scenario
		Scenario scenario = this.createAndLoadTestScenario();
		SignalSystemsConfigGroup signalsConfig = scenario.getConfig().signalSystems();
		this.setSignalSystemConfigValues(signalsConfig);
		
		SignalsScenarioLoader signalsLoader = new SignalsScenarioLoader(signalsConfig);
		SignalsData signalsData = signalsLoader.loadSignalsData();
		
		SignalSystemControllerData controllerData = signalsData.getSignalControlData().getSignalSystemControllerDataBySystemId().get(id2);
		SignalPlanData planData = controllerData.getSignalPlanData().get(id2);
		planData.setCycleTime(5 * 3600);
		SignalGroupSettingsData groupData = planData.getSignalGroupSettingsDataByGroupId().get(id100);
		groupData.setDropping(0);
		groupData.setOnset(100);
		
		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(this);
		this.link2EnterTime = 100.0;
		
		FromDataBuilder builder = new FromDataBuilder(signalsData, events);
		SignalSystemsManager manager = builder.createAndInitializeSignalSystemsManager();
		SignalEngine engine = new QSimSignalEngine(manager);
		
		QSim qsim = new QSim(scenario, events);
		qsim.addQueueSimulationListeners(engine);
		qsim.run();
	}

	
	
	@Override
	public void handleEvent(LinkEnterEvent e) {
		log.info("Link id: " + e.getLinkId().toString() + " enter time: " + e.getTime());
		if (e.getLinkId().equals(id1)){
			Assert.assertEquals(1.0, e.getTime(), MatsimTestUtils.EPSILON);
		}
		else if (e.getLinkId().equals(id2)){
			Assert.assertEquals(this.link2EnterTime, e.getTime(), MatsimTestUtils.EPSILON);
		}
	}

	@Override
	public void reset(int iteration) {
	}

}
