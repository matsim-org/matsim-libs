/* *********************************************************************** *
 * project: org.matsim.*
 * ReRoutingTest.java
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

package org.matsim.core.replanning;

import org.apache.log4j.Logger;
import org.matsim.core.config.Config;
import org.matsim.core.controler.Controler;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.utils.CRCChecksum;

public class ReRoutingTest extends MatsimTestCase {

	/*package*/ static final Logger log = Logger.getLogger(ReRoutingTest.class);

	public void testReRouting() {
		Config config = loadConfig(getInputDirectory() + "config.xml");
		config.simulation().setTimeStepSize(10.0);

		TestControler controler = new TestControler(config);
		controler.setCreateGraphs(false);
		controler.run();

		long checksum1 = CRCChecksum.getCRCFromFile(getInputDirectory() + "0.events.txt.gz");
		long checksum2 = CRCChecksum.getCRCFromFile(getOutputDirectory() + "ITERS/it.0/0.events.txt.gz");
		assertEquals("different event files", checksum1, checksum2);

		checksum1 = CRCChecksum.getCRCFromFile(getInputDirectory() + "plans.xml.gz");
		checksum2 = CRCChecksum.getCRCFromFile(getOutputDirectory() + "ITERS/it.1/1.plans.xml.gz");
		assertEquals("different plans files", checksum1, checksum2);
	}

	static public class TestControler extends Controler {

		public TestControler(final Config config) {
			super(config);
		}

		@Override
		protected void setup() {
			super.setup();

			// do some test to ensure the scenario is correct
			int lastIter = this.config.controler().getLastIteration();
			if (lastIter < 1) {
				throw new IllegalArgumentException("Controler.lastIteration must be at least 1. Current value is " + lastIter);
			}
			if (lastIter > 1) {
				log.error("Controler.lastIteration is currently set to " + lastIter + ". Only the first iteration will be analyzed.");
			}
		}

		@Override
		protected void runMobSim() {
			if (getIteration() == 0) {
				/* only run mobsim in iteration 0, afterwards we're no longer interested
				 * in it as we have our plans-file to compare against to check the
				 * replanning.
				 */
				super.runMobSim();
			} else {
				log.info("skipping mobsim, as it is not of interest in this iteration.");
			}
		}

	}
}
