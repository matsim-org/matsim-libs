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
public class BicycleTest {
	private static final Logger LOG = Logger.getLogger(BicycleTest.class);

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testA() {
		String configFile = "./src/main/resources/bicycle_example/config-a.xml";
		Config config = ConfigUtils.loadConfig(configFile, new BicycleConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		new RunBicycleExample().run(config);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew), 0);
	}
	
	@Test
	public void testB() {
		String configFile = "./src/main/resources/bicycle_example/config-b.xml";
		Config config = ConfigUtils.loadConfig(configFile, new BicycleConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		new RunBicycleExample().run(config);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew), 0);
	}
	
	@Test
	public void testC() {
		String configFile = "./src/main/resources/bicycle_example/config-c.xml";
		Config config = ConfigUtils.loadConfig(configFile, new BicycleConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		new RunBicycleExample().run(config);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew), 0);
	}
	
	@Test
	public void testD() {
		String configFile = "./src/main/resources/bicycle_example/config-d.xml";
		Config config = ConfigUtils.loadConfig(configFile, new BicycleConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		new RunBicycleExample().run(config);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew), 0);
	}
	
	@Test
	public void testE() {
		String configFile = "./src/main/resources/bicycle_example/config-e.xml";
		Config config = ConfigUtils.loadConfig(configFile, new BicycleConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		new RunBicycleExample().run(config);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew), 0);
	}
	
	@Test
	public void testF() {
		String configFile = "./src/main/resources/bicycle_example/config-f.xml";
		Config config = ConfigUtils.loadConfig(configFile, new BicycleConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		new RunBicycleExample().run(config);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew), 0);
	}
	
	@Test
	public void testA10It() {
		String configFile = "./src/main/resources/bicycle_example/config-a-10it.xml";
		Config config = ConfigUtils.loadConfig(configFile, new BicycleConfigGroup());
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(10);
		
		new RunBicycleExample().run(config);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew), 0);
	}
}