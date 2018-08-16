/* *********************************************************************** *
 * project: org.matsim.*
 * OnePercentBerlin10sTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.examples;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.api.experimental.events.EventsManager;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.PrepareForSimUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.events.algorithms.EventWriterXML;
import org.matsim.core.gbl.MatsimRandom;
import org.matsim.core.mobsim.qsim.QSim;
import org.matsim.core.mobsim.qsim.QSimBuilder;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

public class OnePercentBerlin10sIT extends MatsimTestCase {

	private static final Logger log = Logger.getLogger(OnePercentBerlin10sIT.class);

	public void testOnePercent10sQSim() {
		Config config = loadConfig(null);
		// input files are in the main directory in the resource path!
		String netFileName = "test/scenarios/berlin/network.xml"; 
		String popFileName = "test/scenarios/berlin/plans_hwh_1pct.xml.gz";
		
		String eventsFileName = getOutputDirectory() + "events.xml.gz";
		String referenceEventsFileName = getInputDirectory() + "events.xml.gz";

		MatsimRandom.reset(7411L);

		config.qsim().setTimeStepSize(10.0);
		config.qsim().setFlowCapFactor(0.01);
		config.qsim().setStorageCapFactor(0.04);
		config.qsim().setRemoveStuckVehicles(false);
		config.qsim().setStuckTime(10.0);
		config.planCalcScore().setLearningRate(1.0);
		
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime);

		Scenario scenario = ScenarioUtils.createScenario(config);
		
		new MatsimNetworkReader(scenario.getNetwork()).readFile(netFileName);
		new PopulationReader(scenario).readFile(popFileName);

		EventsManager events = EventsUtils.createEventsManager();
		EventWriterXML writer = new EventWriterXML(eventsFileName);
		events.addHandler(writer);

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim qSim = new QSimBuilder(scenario.getConfig()) //
			.useDefaults() //
			.build(scenario, events);

		log.info("START testOnePercent10s SIM");
		qSim.run();
		log.info("STOP testOnePercent10s SIM");

		writer.closeFile();

		System.out.println("reffile: " + referenceEventsFileName);
		assertTrue("different event files", EventsFileComparator.compareAndReturnInt(referenceEventsFileName, eventsFileName) == EventsFileComparator.CODE_FILES_ARE_EQUAL);
		
	}

	public void testOnePercent10sQSimTryEndTimeThenDuration() {
		Config config = loadConfig(null);
		String netFileName = "test/scenarios/berlin/network.xml";
		String popFileName = "test/scenarios/berlin/plans_hwh_1pct.xml.gz";
		String eventsFileName = getOutputDirectory() + "events.xml.gz";
		String referenceEventsFileName = getInputDirectory() + "events.xml.gz";

		MatsimRandom.reset(7411L);

		config.qsim().setTimeStepSize(10.0);
		config.qsim().setFlowCapFactor(0.01);
		config.qsim().setStorageCapFactor(0.04);
		config.qsim().setRemoveStuckVehicles(false);
		config.qsim().setStuckTime(10.0);
		config.planCalcScore().setLearningRate(1.0);
		
		config.controler().setOutputDirectory(this.getOutputDirectory());

		Scenario scenario = ScenarioUtils.createScenario(config);
		
		new MatsimNetworkReader(scenario.getNetwork()).readFile(netFileName);
		new PopulationReader(scenario).readFile(popFileName);

		EventsManager eventsManager = EventsUtils.createEventsManager();
		EventWriterXML writer = new EventWriterXML(eventsFileName);
		eventsManager.addHandler(writer);

		PrepareForSimUtils.createDefaultPrepareForSim(scenario).run();
		QSim qSim = new QSimBuilder(scenario.getConfig()) //
			.useDefaults() //
			.build(scenario, eventsManager);
		
		log.info("START testOnePercent10s SIM");
		qSim.run();
		log.info("STOP testOnePercent10s SIM");

		writer.closeFile();

		assertTrue("different event files", EventsFileComparator.compareAndReturnInt(referenceEventsFileName, eventsFileName) == EventsFileComparator.CODE_FILES_ARE_EQUAL);
		
	}

}
