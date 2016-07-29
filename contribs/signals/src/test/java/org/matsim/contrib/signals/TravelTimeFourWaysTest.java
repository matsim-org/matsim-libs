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

package org.matsim.contrib.signals;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.builder.FromDataBuilder;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.mobsim.QSimSignalEngine;
import org.matsim.contrib.signals.mobsim.SignalEngine;
import org.matsim.contrib.signals.model.SignalSystemsManager;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.lanes.data.v11.LaneDefinitonsV11ToV20Converter;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

/**
 * @author aneumann
 * @author dgrether
 */
public class TravelTimeFourWaysTest {

	private static final String EVENTSFILE = "events.xml.gz";
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	private Scenario createTestScenario(){
		Config conf = ConfigUtils.createConfig(testUtils.classInputResourcePath());
		conf.controler().setMobsim("qsim");
		conf.network().setInputFile("network.xml.gz");
		String laneDefinitions = this.testUtils.getClassInputDirectory()
				+ "testLaneDefinitions_v1.1.xml";
		String lanes20 = testUtils.getOutputDirectory() + "testLaneDefinitions_v2.0.xml";
		new LaneDefinitonsV11ToV20Converter().convert(laneDefinitions,lanes20, conf.network().getInputFileURL(conf.getContext()).getFile());
		conf.network().setLaneDefinitionsFile(lanes20);
		conf.qsim().setUseLanes(true);
		ConfigUtils.addOrGetModule(conf, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class).setUseSignalSystems(false);
		Scenario scenario = ScenarioUtils.createScenario(conf);

		SignalSystemsConfigGroup signalsConfig = ConfigUtils.addOrGetModule(conf, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		String signalSystemsFile = testUtils.getClassInputDirectory() + "testSignalSystems_v2.0.xml";
		String signalGroupsFile = testUtils.getClassInputDirectory() + "testSignalGroups_v2.0.xml";
		String signalControlFile = testUtils.getClassInputDirectory() + "testSignalControl_v2.0.xml";
		String amberTimesFile = testUtils.getClassInputDirectory() + "testAmberTimes_v1.0.xml";
		signalsConfig.setSignalSystemFile(signalSystemsFile);
		signalsConfig.setSignalGroupsFile(signalGroupsFile);
		signalsConfig.setSignalControlFile(signalControlFile);
		signalsConfig.setUseAmbertimes(true);
		signalsConfig.setAmberTimesFile(amberTimesFile);

		return scenario;
	}
	
	private SignalEngine initSignalEngine(Scenario scenario, EventsManager events) {
		SignalSystemsConfigGroup signalsConfig = ConfigUtils.addOrGetModule(scenario.getConfig(), SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class);
		SignalsScenarioLoader signalsLoader = new SignalsScenarioLoader(signalsConfig);
		SignalsData signalsData = signalsLoader.loadSignalsData();
		scenario.addScenarioElement( SignalsData.ELEMENT_NAME , signalsData);
		
		FromDataBuilder builder = new FromDataBuilder(scenario, events);
		SignalSystemsManager manager = builder.createAndInitializeSignalSystemsManager();
		SignalEngine engine = new QSimSignalEngine(manager);
		return engine;
	}
	
	@Test
	public void testTrafficLightIntersection4arms() {
		Scenario scenario = this.createTestScenario();
		scenario.getConfig().plans().setInputFile("plans.xml.gz");
		
		ScenarioUtils.loadScenario(scenario);
		String eventsOut = this.testUtils.getOutputDirectory() + EVENTSFILE;
		EventsManager events = EventsUtils.createEventsManager();
		EventWriterXML eventsXmlWriter = new EventWriterXML(eventsOut);
		events.addHandler(eventsXmlWriter);
		
		SignalEngine signalEngine = this.initSignalEngine(scenario, events);

		QSim sim = QSimUtils.createDefaultQSim(scenario, events);
		sim.addQueueSimulationListeners(signalEngine);
		sim.run();
		eventsXmlWriter.closeFile();
        Assert.assertEquals("different events files", EventsFileComparator.compare(this.testUtils.getInputDirectory() + EVENTSFILE, eventsOut), 0);
	}

	@Test
	public void testTrafficLightIntersection4armsWithUTurn() {
		Scenario scenario = this.createTestScenario();
		scenario.getConfig().plans().setInputFile("plans_uturn.xml.gz");
		ScenarioUtils.loadScenario(scenario);

		String eventsOut = this.testUtils.getOutputDirectory() + EVENTSFILE;
		EventsManager events = EventsUtils.createEventsManager();
		EventWriterXML eventsXmlWriter = new EventWriterXML(eventsOut);
		events.addHandler(eventsXmlWriter);
		
		SignalEngine signalEngine = this.initSignalEngine(scenario, events);

		QSim sim = QSimUtils.createDefaultQSim(scenario, events);
		sim.addQueueSimulationListeners(signalEngine);
		sim.run();
		eventsXmlWriter.closeFile();
		Assert.assertEquals("different events files", CRCChecksum.getCRCFromFile(this.testUtils.getInputDirectory() + EVENTSFILE), CRCChecksum.getCRCFromFile(eventsOut));
	}
	
}
