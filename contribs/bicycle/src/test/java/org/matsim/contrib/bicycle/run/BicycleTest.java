/* *********************************************************************** *
 * project: org.matsim.*                                                   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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
package org.matsim.contrib.bicycle.run;

import static org.junit.Assert.assertEquals;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

/**
 * @author dziemke
 */
public class BicycleTest {
	private static final Logger log = Logger.getLogger(BicycleTest.class);

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Ignore
	// After several alterations in bicycle code, this test needs to be redone
	@Test
	public final void test() {
		
		// This works when the data is stored under "/matsim/contribs/bicycle/src/main/resources/bicycle_example"
		Config config = ConfigUtils.loadConfig("bicycle_example/config.xml", new BicycleConfigGroup());
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		
		try {
			new RunBicycleExample().run(config);
		} catch (Exception e) {
			e.printStackTrace();
			Assert.fail("Something did not work") ;
		}

		log.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", EventsFileComparator.compareAndReturnInt(eventsFilenameReference, eventsFilenameNew), 0);
	}
}