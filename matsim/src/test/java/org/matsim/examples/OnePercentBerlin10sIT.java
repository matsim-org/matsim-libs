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

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
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
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

public class OnePercentBerlin10sIT {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();


	private static final Logger log = LogManager.getLogger(OnePercentBerlin10sIT.class);

	@Test
	void testOnePercent10sQSim() {
		Config config = utils.loadConfig((String)null);
		// input files are in the main directory in the resource path!
		String netFileName = "test/scenarios/berlin/network.xml";
		String popFileName = "test/scenarios/berlin/plans_hwh_1pct.xml.gz";

		String eventsFileName = utils.getOutputDirectory() + "events.xml.gz";
		String referenceEventsFileName = utils.getInputDirectory() + "events.xml.gz";

		MatsimRandom.reset(7411L);

		config.qsim().setTimeStepSize(10.0);
		config.qsim().setFlowCapFactor(0.01);
		config.qsim().setStorageCapFactor(0.04);
		config.qsim().setRemoveStuckVehicles(false);
		config.qsim().setStuckTime(10.0);
		config.scoring().setLearningRate(1.0);

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
		assertEquals( ComparisonResult.FILES_ARE_EQUAL,
				new EventsFileComparator().setIgnoringCoordinates( true ).runComparison( referenceEventsFileName, eventsFileName ),
				"different event files" );

	}

	@Test
	void testOnePercent10sQSimTryEndTimeThenDuration() {
		Config config = utils.loadConfig((String)null);
		String netFileName = "test/scenarios/berlin/network.xml";
		String popFileName = "test/scenarios/berlin/plans_hwh_1pct.xml.gz";
		String eventsFileName = utils.getOutputDirectory() + "events.xml.gz";
		String referenceEventsFileName = utils.getInputDirectory() + "events.xml.gz";

		MatsimRandom.reset(7411L);

		config.qsim().setTimeStepSize(10.0);
		config.qsim().setFlowCapFactor(0.01);
		config.qsim().setStorageCapFactor(0.04);
		config.qsim().setRemoveStuckVehicles(false);
		config.qsim().setStuckTime(10.0);
		config.scoring().setLearningRate(1.0);

		config.controller().setOutputDirectory(utils.getOutputDirectory());

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

		assertEquals( ComparisonResult.FILES_ARE_EQUAL,
				new EventsFileComparator().setIgnoringCoordinates( true ).runComparison( referenceEventsFileName, eventsFileName ),
				"different event files" );

	}

}
