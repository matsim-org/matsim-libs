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
import org.matsim.api.basic.v01.Id;
import org.matsim.api.basic.v01.events.BasicLinkEnterEvent;
import org.matsim.api.basic.v01.events.handler.BasicLinkEnterEventHandler;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.EventsImpl;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.scenario.ScenarioLoader;
import org.matsim.lanes.basic.BasicLaneDefinitions;
import org.matsim.signalsystems.basic.BasicSignalSystems;
import org.matsim.signalsystems.config.BasicPlanBasedSignalSystemControlInfo;
import org.matsim.signalsystems.config.BasicSignalGroupSettings;
import org.matsim.signalsystems.config.BasicSignalSystemConfiguration;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurations;
import org.matsim.signalsystems.config.BasicSignalSystemConfigurationsImpl;
import org.matsim.signalsystems.config.BasicSignalSystemPlan;
import org.matsim.testcases.MatsimTestCase;

/**
 * Simple test case for the QueueSim signal system implementation.
 * One agent drives one round in the signal system default simple test
 * network.
 * 
 * @author dgrether
 */
public class SignalSystemsOneAgentTest extends MatsimTestCase implements
		BasicLinkEnterEventHandler {

	
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
		ScenarioImpl data = new ScenarioImpl(conf);
		ScenarioLoader loader = new ScenarioLoader(data);
		loader.loadScenario();
		
		BasicLaneDefinitions lanedefs = data.getLaneDefinitions();
		BasicSignalSystems signalSystems = data.getSignalSystems();

		EventsImpl events = new EventsImpl();
		events.addHandler(this);

		BasicSignalSystemConfigurations lssConfigs = new BasicSignalSystemConfigurationsImpl();
  	MatsimSignalSystemConfigurationsReader reader = new MatsimSignalSystemConfigurationsReader(lssConfigs);
		reader.readFile(lsaConfig);
		for (BasicSignalSystemConfiguration lssConfig : lssConfigs.getSignalSystemConfigurations().values()) {
			BasicPlanBasedSignalSystemControlInfo controlInfo = (BasicPlanBasedSignalSystemControlInfo) lssConfig
					.getControlInfo();
			BasicSignalSystemPlan p = controlInfo.getPlans()
					.get(new IdImpl("2"));
			p.setCycleTime(60);
			BasicSignalGroupSettings group = p.getGroupConfigs().get(
					new IdImpl("100"));
			group.setDropping(60);
		}

		QueueSimulation sim = new QueueSimulation(data.getNetwork(), data.getPopulation(), events);
		sim.setLaneDefinitions(lanedefs);
		sim.setSignalSystems(signalSystems, lssConfigs);
		sim.run();
		
		sim = new QueueSimulation(data.getNetwork(), data.getPopulation(), events);
		sim.run();
	}

	public void handleEvent(BasicLinkEnterEvent e) {
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
