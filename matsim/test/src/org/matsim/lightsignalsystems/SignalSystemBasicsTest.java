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
package org.matsim.lightsignalsystems;

import java.util.ArrayList;
import java.util.List;

import org.apache.log4j.Logger;
import org.matsim.basic.signalsystems.BasicLightSignalSystems;
import org.matsim.basic.signalsystemsconfig.BasicLightSignalGroupConfiguration;
import org.matsim.basic.signalsystemsconfig.BasicLightSignalSystemConfiguration;
import org.matsim.basic.signalsystemsconfig.BasicLightSignalSystemPlan;
import org.matsim.basic.signalsystemsconfig.BasicPlanBasedLightSignalSystemControlInfo;
import org.matsim.basic.v01.Id;
import org.matsim.basic.v01.IdImpl;
import org.matsim.config.Config;
import org.matsim.controler.ScenarioData;
import org.matsim.events.Events;
import org.matsim.events.LinkEnterEvent;
import org.matsim.events.handler.LinkEnterEventHandler;
import org.matsim.mobsim.queuesim.QueueNetwork;
import org.matsim.mobsim.queuesim.QueueSimulation;
import org.matsim.signalsystems.MatsimLightSignalSystemConfigurationReader;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author dgrether
 * 
 */
public class SignalSystemBasicsTest extends MatsimTestCase implements
		LinkEnterEventHandler {

	
	private static final Logger log = Logger
			.getLogger(SignalSystemBasicsTest.class);
	
	private Id id1 = new IdImpl(1);
	private Id id2 = new IdImpl(2);
	private Id id3 = new IdImpl(3);
	private Id id4 = new IdImpl(4);
	private Id id5 = new IdImpl(5);
	
	
	@Override
	protected void setUp() throws Exception {
		super.setUp();
		QueueNetwork.setSimulateAllLinks(true);
		QueueNetwork.setSimulateAllNodes(true);
	}

	public void testTrafficLightIntersection2arms1Agent() {
		Config conf = loadConfig(this.getClassInputDirectory() + "config.xml");
		String plansFile = this.getClassInputDirectory() + "plans1Agent.xml";
		String lsaDefinition = this.getClassInputDirectory() + "lsa.xml";
		String lsaConfig = this.getClassInputDirectory() + "lsa_config.xml";
		String tempFile = this.getOutputDirectory() + "__tempFile__.xml";
		conf.signalSystems().setSignalSystemFile(lsaDefinition);
		conf.signalSystems().setSignalSystemConfigFile(lsaConfig);
		conf.plans().setInputFile(plansFile);
		ScenarioData data = new ScenarioData(conf);
		BasicLightSignalSystems signalSystems = data.getSignalSystems();

		Events events = new Events();
		events.addHandler(this);

		List<BasicLightSignalSystemConfiguration> lssConfigs = new ArrayList<BasicLightSignalSystemConfiguration>();
		MatsimLightSignalSystemConfigurationReader reader = new MatsimLightSignalSystemConfigurationReader(
				lssConfigs);
		reader.readFile(lsaConfig);
		for (BasicLightSignalSystemConfiguration lssConfig : lssConfigs) {
			BasicPlanBasedLightSignalSystemControlInfo controlInfo = (BasicPlanBasedLightSignalSystemControlInfo) lssConfig
					.getControlInfo();
			BasicLightSignalSystemPlan p = controlInfo.getPlans()
					.get(new IdImpl("2"));
			p.setCirculationTime(60.0);
			BasicLightSignalGroupConfiguration group = p.getGroupConfigs().get(
					new IdImpl("100"));
			group.setDropping(60);
		}

		QueueSimulation sim = new QueueSimulation(data.getNetwork(), data
				.getPopulation(), events);
		sim.setSignalSystems(signalSystems, lssConfigs);
		sim.run();
		
		
		sim = new QueueSimulation(data.getNetwork(), data
				.getPopulation(), events);
		sim.run();
		
		
	}

	public void handleEvent(LinkEnterEvent e) {
		log.info("LinkEnter: " + e.linkId + " time: " + e.time);
		if (e.link.getId().equals(id1)){
			assertEquals(1.0, e.time, EPSILON);
		}
		else if (e.link.getId().equals(id2)){
			assertEquals(38.0, e.time, EPSILON);
		}
	}

	public void reset(int iteration) {
	}

}
