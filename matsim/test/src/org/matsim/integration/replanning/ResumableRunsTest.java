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

import org.matsim.config.Config;
import org.matsim.controler.Controler;
import org.matsim.gbl.Gbl;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;

/**
 * Tests that a run can be started from a given plan-file and generates
 * the byte-identical events-output from that. This ensures mostly that
 * either no random numbers are used, or that the random numbers are
 * correctly re-initialized every iteration.
 *
 * @author mrieser
 */
public class ResumableRunsTest extends MatsimTestCase {

	/**
	 * Runs a first simulation for 11 iteration, then restarts at iteration 10.
	 * Tests that the events in the 10th and 11th iteration are byte-identical,
	 * to ensure the correct initialization of the simulation as well as the
	 * re-planning, which both could depend on random numbers.
	 */
	public void testResumableRuns() {
		Config config = loadConfig("test/scenarios/equil/config.xml");
		config.controler().setLastIteration(11);
		config.global().setNumberOfThreads(1); // only use one thread to rule out other disturbances for the test

		// run1
		config.controler().setOutputDirectory(getOutputDirectory() + "/run1/");
		Controler controler1 = new Controler(config);
		controler1.setCreateGraphs(false);
		controler1.run();

		// run2
		Gbl.reset();
		config.controler().setOutputDirectory(getOutputDirectory() + "/run2/");
		config.controler().setFirstIteration(10);
		config.plans().setInputFile(getOutputDirectory() + "/run1/ITERS/it.10/10.plans.xml.gz");
		Controler controler2 = new Controler(config);
		controler2.setCreateGraphs(false);
		controler2.run();

		// comparison
		long cksum1 = CRCChecksum.getCRCFromGZFile(getOutputDirectory() + "/run1/ITERS/it.10/10.plans.xml.gz");
		long cksum2 = CRCChecksum.getCRCFromGZFile(getOutputDirectory() + "/run2/ITERS/it.10/10.plans.xml.gz");
		assertEquals("Plans must not be altered just be reading in and writing out again.", cksum1, cksum2);

		cksum1 = CRCChecksum.getCRCFromGZFile(getOutputDirectory() + "/run1/ITERS/it.10/10.events.txt.gz");
		cksum2 = CRCChecksum.getCRCFromGZFile(getOutputDirectory() + "/run2/ITERS/it.10/10.events.txt.gz");
		assertEquals("The checksums of events must be the same when resuming runs.", cksum1, cksum2);

		cksum1 = CRCChecksum.getCRCFromGZFile(getOutputDirectory() + "/run1/ITERS/it.11/11.events.txt.gz");
		cksum2 = CRCChecksum.getCRCFromGZFile(getOutputDirectory() + "/run2/ITERS/it.11/11.events.txt.gz");
		assertEquals("The checksums of events must be the same when resuming runs.", cksum1, cksum2);
	}

}
