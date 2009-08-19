/* *********************************************************************** *
 * project: org.matsim.*
 * TravelTimeTestFourWay
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

package org.matsim.signalsystems;

import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.events.Events;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.queuesim.QueueSimulation;
import org.matsim.core.scenario.ScenarioLoader;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author aneumann
 * @author dgrether
 */
public class TravelTimeFourWaysTest extends MatsimTestCase {

	private static final String EVENTSFILE = "events.xml.gz";
	
	public void testTrafficLightIntersection4arms() {
		Config conf = loadConfig(this.getClassInputDirectory() + "config.xml");
		String laneDefinitions = this.getClassInputDirectory()
				+ "testLaneDefinitions_v1.1.xml";
		String lsaDefinition = this.getClassInputDirectory()
				+ "testSignalSystems_v1.1.xml";
		String lsaConfig = this.getClassInputDirectory()
				+ "testSignalSystemConfigurations_v1.1.xml";
		conf.network().setLaneDefinitionsFile(laneDefinitions);
		conf.signalSystems().setSignalSystemFile(lsaDefinition);
		conf.signalSystems().setSignalSystemConfigFile(lsaConfig);

		conf.scenario().setUseLanes(true);
		conf.scenario().setUseSignalSystems(true);
		ScenarioImpl data = new ScenarioImpl(conf);
		ScenarioLoader loader = new ScenarioLoader(data);
		loader.loadScenario();
		
		String eventsOut = this.getOutputDirectory() + EVENTSFILE;
		Events events = new Events();
		EventWriterXML eventsXmlWriter = new EventWriterXML(eventsOut);
		events.addHandler(eventsXmlWriter);
		QueueSimulation sim = new QueueSimulation(data.getNetwork(), data.getPopulation(), events);
		sim.setLaneDefinitions(data.getLaneDefinitions());
		sim.setSignalSystems(data.getSignalSystems(), data.getSignalSystemConfigurations());
		sim.run();
		eventsXmlWriter.closeFile();
		assertEquals("different events files", CRCChecksum.getCRCFromFile(this.getInputDirectory() + EVENTSFILE), CRCChecksum.getCRCFromFile(eventsOut));
	}

	public void testTrafficLightIntersection4armsWithUTurn() {
		Config conf = loadConfig(this.getClassInputDirectory() + "config.xml");
		String laneDefinitions = this.getClassInputDirectory()
				+ "testLaneDefinitions_v1.1.xml";
		String lsaDefinition = this.getClassInputDirectory()
				+ "testSignalSystems_v1.1.xml";
		String lsaConfig = this.getClassInputDirectory()
				+ "testSignalSystemConfigurations_v1.1.xml";
		conf.network().setLaneDefinitionsFile(laneDefinitions);
		conf.signalSystems().setSignalSystemFile(lsaDefinition);
		conf.signalSystems().setSignalSystemConfigFile(lsaConfig);
		conf.plans().setInputFile(this.getClassInputDirectory() + "plans_uturn.xml.gz");
		conf.scenario().setUseLanes(true);
		conf.scenario().setUseSignalSystems(true);
		ScenarioImpl data = new ScenarioImpl(conf);
		ScenarioLoader loader = new ScenarioLoader(data);
		loader.loadScenario();

		String eventsOut = this.getOutputDirectory() + EVENTSFILE;
		Events events = new Events();
		EventWriterXML eventsXmlWriter = new EventWriterXML(eventsOut);
		events.addHandler(eventsXmlWriter);
		
		QueueSimulation sim = new QueueSimulation(data.getNetwork(), data.getPopulation(), events);
		sim.setLaneDefinitions(data.getLaneDefinitions());
		sim.setSignalSystems(data.getSignalSystems(), data.getSignalSystemConfigurations());
		sim.run();
		eventsXmlWriter.closeFile();
		assertEquals("different events files", CRCChecksum.getCRCFromFile(this.getInputDirectory() + EVENTSFILE), CRCChecksum.getCRCFromFile(eventsOut));
	}
}
