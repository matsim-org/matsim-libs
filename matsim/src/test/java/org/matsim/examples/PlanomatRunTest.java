/* *********************************************************************** *
 * project: org.matsim.*
 * PlanomatControlerTest.java
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

package org.matsim.examples;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestCase;

public class PlanomatRunTest extends MatsimTestCase {

	private Config config;

	@Override
	protected void setUp() throws Exception {
		super.setUp();
		this.config = super.loadConfig(this.getInputDirectory() + "config.xml");
	}

	@Override
	protected void tearDown() throws Exception {
		super.tearDown();
		this.config = null;
	}

	public void testMainDefault() {
		this.runControlerTest(this.config);
	}

	/**
	 * TODO this test always fails for an unknown reason.
	 * checksums of actual test results are not stable even in repeated runs of this test on one machine
	 */
	public void testMainCarPt() {
//		this.runControlerTest(this.config); // commented this out because I don't really like non-deterministic tests. kai, aug09
	}

	private void runControlerTest(final Config config) {

		final Logger logger = Logger.getLogger(PlanomatRunTest.class);

		config.controler().setOutputDirectory(this.getOutputDirectory());
		Controler testee = new Controler(config);
		testee.setCreateGraphs(false);
		testee.setWriteEventsInterval(0);
		testee.run();

		// actual test: compare checksums of the files
		final long expectedChecksum = CRCChecksum.getCRCFromFile(this.getInputDirectory() + "plans.xml.gz");
		final long actualChecksum = CRCChecksum.getCRCFromFile(this.getOutputDirectory() + "output_plans.xml.gz");
		logger.info("Actual checksum: " + Long.toString(actualChecksum));
		assertEquals("different plans files.", expectedChecksum, actualChecksum);
	}

}
