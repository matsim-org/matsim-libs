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
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.basic.network.BasicLaneDefinitions;
import org.matsim.basic.signalsystems.BasicSignalSystems;
import org.matsim.basic.signalsystemsconfig.BasicPlanBasedSignalSystemControlInfo;
import org.matsim.basic.signalsystemsconfig.BasicSignalGroupSettings;
import org.matsim.basic.signalsystemsconfig.BasicSignalSystemConfiguration;
import org.matsim.basic.signalsystemsconfig.BasicSignalSystemConfigurations;
import org.matsim.basic.signalsystemsconfig.BasicSignalSystemConfigurationsImpl;
import org.matsim.basic.signalsystemsconfig.BasicSignalSystemPlan;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.events.Events;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.testcases.MatsimTestCase;

/**
 * Simple test case for the QueueSim signal system implementation.
 * One agent drives one round in the signal system default simple test
 * network.
 * @author dgrether
 * 
 */
public class SignalSystemsOneAgentTest extends MatsimTestCase implements
		LinkEnterEventHandler {

	
	private static final Logger log = Logger
			.getLogger(SignalSystemsOneAgentTest.class);
	
	private Id id1 = new IdImpl(1);
	private Id id2 = new IdImpl(2);
//	private Id id3 = new IdImpl(3);
//	private Id id4 = new IdImpl(4);
//	private Id id5 = new IdImpl(5);
	
	
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
		ScenarioImpl data = new ScenarioImpl(conf);
		BasicLaneDefinitions lanedefs = data.getLaneDefinitions();
		BasicSignalSystems signalSystems = data.getSignalSystems();

		Events events = new Events();
		events.addHandler(this);

		BasicSignalSystemConfigurations lssConfigs = new BasicSignalSystemConfigurationsImpl();
  	MatsimSignalSystemConfigurationsReader reader = new MatsimSignalSystemConfigurationsReader(lssConfigs);
		reader.readFile(lsaConfig);
		for (BasicSignalSystemConfiguration lssConfig : lssConfigs.getSignalSystemConfigurations().values()) {
			BasicPlanBasedSignalSystemControlInfo controlInfo = (BasicPlanBasedSignalSystemControlInfo) lssConfig
					.getControlInfo();
			BasicSignalSystemPlan p = controlInfo.getPlans()
					.get(new IdImpl("2"));
			p.setCirculationTime(60);
			BasicSignalGroupSettings group = p.getGroupConfigs().get(
					new IdImpl("100"));
			group.setDropping(60);
		}

		QueueSimulation sim = new QueueSimulation(data.getNetwork(), data
				.getPopulation(), events);
		sim.setLaneDefinitions(lanedefs);
		sim.setSignalSystems(signalSystems, lssConfigs);
		sim.run();
		
		
		sim = new QueueSimulation(data.getNetwork(), data
				.getPopulation(), events);
		sim.run();
		
		
	}

	public void handleEvent(LinkEnterEvent e) {
		log.info("LinkEnter: " + e.linkId + " time: " + e.getTime());
		if (e.link.getId().equals(id1)){
			assertEquals(1.0, e.getTime(), EPSILON);
		}
		else if (e.link.getId().equals(id2)){
			assertEquals(38.0, e.getTime(), EPSILON);
		}
	}

	public void reset(int iteration) {
	}

}
