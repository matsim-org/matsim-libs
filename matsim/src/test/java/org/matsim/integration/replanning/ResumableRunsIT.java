/* *********************************************************************** *
 * project: org.matsim.*
 * ResumableRunsTest.java
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

package org.matsim.integration.replanning;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;
import java.net.MalformedURLException;

/**
 * Tests that a run can be started from a given plan-file and generates
 * the byte-identical events-output from that. This ensures mostly that
 * either no random numbers are used, or that the random numbers are
 * correctly re-initialized every iteration.
 *
 * @author mrieser
 */
public class ResumableRunsIT {

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * Runs a first simulation for 11 iteration, then restarts at iteration 10.
	 * Tests that the events in the 10th and 11th iteration are byte-identical,
	 * to ensure the correct initialization of the simulation as well as the
	 * re-planning, which both could depend on random numbers.
	 */
	@Test
	void testResumableRuns() throws MalformedURLException {
		Config config = utils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		config.controller().setLastIteration(11);
		config.controller().setWriteEventsInterval(1);
		config.global().setNumberOfThreads(1); // only use one thread to rule out other disturbances for the test
		config.controller().setWritePlansInterval(10);

		// run1
		config.controller().setOutputDirectory(utils.getOutputDirectory() + "/run1/");
		/*
		 * The input plans file is not sorted. After switching from TreeMap to LinkedHashMap
		 * to store the persons in the population, we have to sort the population manually.
		 * cdobler, oct'11
		 */
		Scenario scenario1 = ScenarioUtils.loadScenario(config);
		PopulationUtils.sortPersons(scenario1.getPopulation());
		Controler controler1 = new Controler(scenario1);
        controler1.getConfig().controller().setCreateGraphs(false);
		controler1.getConfig().controller().setDumpDataAtEnd(false);
		controler1.run();

		// run2
		config.controller().setOutputDirectory(utils.getOutputDirectory() + "/run2/");
		config.controller().setFirstIteration(10);
		config.plans().setInputFile(new File(utils.getOutputDirectory() + "/run1/ITERS/it.10/10.plans.xml.gz").toURI().toURL().toString());
		Controler controler2 = new Controler(config);
        controler2.getConfig().controller().setCreateGraphs(false);
		controler2.getConfig().controller().setDumpDataAtEnd(false);
		controler2.run();

		// comparison
		long cksum1 = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + "/run1/ITERS/it.10/10.plans.xml.gz");
		long cksum2 = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + "/run2/ITERS/it.10/10.plans.xml.gz");
		Assertions.assertEquals(cksum1, cksum2, "Plans must not be altered just be reading in and writing out again.");

		cksum1 = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + "/run1/ITERS/it.10/10.events.xml.gz");
		cksum2 = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + "/run2/ITERS/it.10/10.events.xml.gz");
		Assertions.assertEquals(cksum1, cksum2, "The checksums of events must be the same when resuming runs.");

		cksum1 = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + "/run1/ITERS/it.11/11.events.xml.gz");
		cksum2 = CRCChecksum.getCRCFromFile(utils.getOutputDirectory() + "/run2/ITERS/it.11/11.events.xml.gz");
		Assertions.assertEquals(cksum1, cksum2, "The checksums of events must be the same when resuming runs.");
	}

}
