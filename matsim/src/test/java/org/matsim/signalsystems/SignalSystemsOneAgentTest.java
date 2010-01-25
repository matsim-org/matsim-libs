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
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.api.experimental.events.LinkEnterEvent;
import org.matsim.core.api.experimental.events.handler.LinkEnterEventHandler;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.events.EventsManagerImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.lanes.LaneDefinitions;
import org.matsim.ptproject.qsim.QSim;
import org.matsim.signalsystems.config.PlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.SignalGroupSettings;
import org.matsim.signalsystems.config.SignalSystemConfiguration;
import org.matsim.signalsystems.config.SignalSystemConfigurations;
import org.matsim.signalsystems.config.SignalSystemPlan;
import org.matsim.signalsystems.systems.SignalSystems;
import org.matsim.testcases.MatsimTestCase;

/**
 * Simple test case for the QueueSim signal system implementation.
 * One agent drives one round in the signal system default simple test
 * network.
 * 
 * @author dgrether
 */
public class SignalSystemsOneAgentTest extends MatsimTestCase implements
		LinkEnterEventHandler {

	
	private static final Logger log = Logger.getLogger(SignalSystemsOneAgentTest.class);
	
	private Id id1 = new IdImpl(1);
	private Id id2 = new IdImpl(2);
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
	}
	
	public void testTrafficLightIntersection2arms1Agent() {
		Config conf = loadConfig(this.getClassInputDirectory() + "config.xml");
		String plansFile = this.getClassInputDirectory() + "plans1Agent.xml";
		String laneDefinitions = this.getClassInputDirectory() + "testLaneDefinitions_v1.1.xml";
		String lsaDefinition = this.getClassInputDirectory() + "testSignalSystems_v1.1.xml";
		String lsaConfig = this.getClassInputDirectory() + "testSignalSystemConfigurations_v1.1.xml";
		conf.network().setLaneDefinitionsFile(laneDefinitions);
		conf.signalSystems().setSignalSystemFile(lsaDefinition);
		conf.signalSystems().setSignalSystemConfigFile(lsaConfig);
		conf.plans().setInputFile(plansFile);
		conf.scenario().setUseLanes(true);
		conf.scenario().setUseSignalSystems(true);
		conf.setQSimConfigGroup(new QSimConfigGroup());
		ScenarioImpl scenario = new ScenarioImpl(conf);
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		
		loader.loadScenario();
		
		LaneDefinitions lanedefs = scenario.getLaneDefinitions();
		SignalSystems signalSystems = scenario.getSignalSystems();

		EventsManagerImpl events = new EventsManagerImpl();
		events.addHandler(this);

		SignalSystemConfigurations lssConfigs = scenario.getSignalSystemConfigurations();
		for (SignalSystemConfiguration lssConfig : lssConfigs.getSignalSystemConfigurations().values()) {
			PlanBasedSignalSystemControlInfo controlInfo = (PlanBasedSignalSystemControlInfo) lssConfig
					.getControlInfo();
			SignalSystemPlan p = controlInfo.getPlans()
					.get(new IdImpl("2"));
			p.setCycleTime(60);
			SignalGroupSettings group = p.getGroupConfigs().get(
					new IdImpl("100"));
			group.setDropping(60);
		}

		QSim sim = new QSim(scenario, events);
		sim.run();
		
		sim = new QSim(scenario, events);
		sim.run();
	}

	public void handleEvent(LinkEnterEvent e) {
		log.info("LinkEnter: " + e.getLinkId().toString() + " time: " + e.getTime());
		if (e.getLinkId().equals(id1)){
			assertEquals(1.0, e.getTime(), EPSILON);
		}
		else if (e.getLinkId().equals(id2)){
			assertEquals(38.0, e.getTime(), EPSILON);
		}
	}

	public void reset(int iteration) {
	}

}
