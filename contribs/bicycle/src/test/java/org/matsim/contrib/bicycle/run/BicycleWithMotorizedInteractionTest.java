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
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

/**
 * @author dziemke
 */
public class BicycleWithMotorizedInteractionTest {
	private static final Logger LOG = Logger.getLogger(BicycleWithMotorizedInteractionTest.class);

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void test() {
		String configFile = "./src/main/resources/bicycle_example/config-a-motor.xml";
		Config config = ConfigUtils.loadConfig(configFile, new BicycleConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(10);
		
		new RunBicycleWithMotorizedInteraction().run(config);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", EventsFileComparator.compareAndReturnInt(eventsFilenameReference, eventsFilenameNew), 0);
	}
}