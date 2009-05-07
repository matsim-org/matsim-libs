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

	public void testMainDefault() {
		this.runControlerTest(this.loadConfig(this.getInputDirectory() + "config.xml"));
	}

	public void testMainCarPt() {
		this.runControlerTest(this.loadConfig(this.getInputDirectory() + "config.xml"));
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
