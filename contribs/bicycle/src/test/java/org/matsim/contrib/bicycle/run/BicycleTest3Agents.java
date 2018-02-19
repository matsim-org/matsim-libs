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
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

/**
 * @author dziemke
 */
public class BicycleTest3Agents {
	private static final Logger LOG = Logger.getLogger(BicycleTest3Agents.class);

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testNormal() {
		String configFile = "./src/main/resources/bicycle_example/config_scoring.xml";
		Config config = ConfigUtils.loadConfig(configFile, new BicycleConfigGroup());
		config.network().setInputFile("network_normal.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameCurrent = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", EventsFileComparator.compareAndReturnInt(eventsFilenameReference, eventsFilenameCurrent), 0);
		
		

		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		
		for (Id<Person> personId : scenarioReference.getPopulation().getPersons().keySet()) {
			double scoreReference = scenarioReference.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
			double scoreCurrent = scenarioCurrent.getPopulation().getPersons().get(personId).getSelectedPlan().getScore();
			Assert.assertEquals("Scores of persons " + personId + " are different", scoreReference, scoreCurrent, MatsimTestUtils.EPSILON);
		}
		
		assertTrue("Populations are different", PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()));
	}
	
	@Test
	public void testCobblestone() {
		String configFile = "./src/main/resources/bicycle_example/config_scoring.xml";
		Config config = ConfigUtils.loadConfig(configFile, new BicycleConfigGroup());
		// Links 4-8 and 13-17 have cobblestones
		config.network().setInputFile("network_cobblestone.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", EventsFileComparator.compareAndReturnInt(eventsFilenameReference, eventsFilenameNew), 0);
	}
	
	@Test
	public void testPedestrian() {
		String configFile = "./src/main/resources/bicycle_example/config_scoring.xml";
		Config config = ConfigUtils.loadConfig(configFile, new BicycleConfigGroup());
		// Links 4-8 and 13-17 are pedestrian zones
		config.network().setInputFile("network_pedestrian.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", EventsFileComparator.compareAndReturnInt(eventsFilenameReference, eventsFilenameNew), 0);
	}
	
	@Test
	public void testLane() {
		String configFile = "./src/main/resources/bicycle_example/config_scoring.xml";
		Config config = ConfigUtils.loadConfig(configFile, new BicycleConfigGroup());
		// Links 2-4/8-10 and 11-13/17-19 have cycle lanes (cycleway=lane)
		config.network().setInputFile("network_lane.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", EventsFileComparator.compareAndReturnInt(eventsFilenameReference, eventsFilenameNew), 0);
		
		Scenario sceanrioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(sceanrioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		assertTrue("Populations are different", PopulationUtils.equalPopulation(sceanrioReference.getPopulation(), scenarioCurrent.getPopulation()));
	}
	
	@Test
	public void testGradient() {
		String configFile = "./src/main/resources/bicycle_example/config_scoring.xml";
		Config config = ConfigUtils.loadConfig(configFile, new BicycleConfigGroup());
		// Nodes 5-9 have a z-coordinate > 0, i.e. the links leading to those nodes have a slope
		config.network().setInputFile("network_gradient.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", EventsFileComparator.compareAndReturnInt(eventsFilenameReference, eventsFilenameNew), 0);
		
		Scenario sceanrioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(sceanrioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		assertTrue("Populations are different", PopulationUtils.equalPopulation(sceanrioReference.getPopulation(), scenarioCurrent.getPopulation()));
	}
	
	@Test
	public void testGradientLane() {
		String configFile = "./src/main/resources/bicycle_example/config_scoring.xml";
		Config config = ConfigUtils.loadConfig(configFile, new BicycleConfigGroup());
		// Nodes 5-9 have a z-coordinate > 0, i.e. the links leading to those nodes have a slope
		// and links 4-5 and 13-14 have cycle lanes
		config.network().setInputFile("network_gradient_lane.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", EventsFileComparator.compareAndReturnInt(eventsFilenameReference, eventsFilenameNew), 0);
	}
	
	@Test
	public void testNormal10It() {
		String configFile = "./src/main/resources/bicycle_example/config_scoring.xml";
		Config config = ConfigUtils.loadConfig(configFile, new BicycleConfigGroup());
		config.network().setInputFile("network_normal.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		// 10 iterations
		config.controler().setLastIteration(10);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", EventsFileComparator.compareAndReturnInt(eventsFilenameReference, eventsFilenameNew), 0);
	}
	
	@Test
	public void testMotorizedInteraction() {
		String configFile = "./src/main/resources/bicycle_example/config_scoring.xml";
		Config config = ConfigUtils.loadConfig(configFile, new BicycleConfigGroup());
		config.network().setInputFile("network_normal.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(10);
		
		// interaction with motor vehicles
		new RunBicycleExample().run(config, true);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", EventsFileComparator.compareAndReturnInt(eventsFilenameReference, eventsFilenameNew), 0);
	}
}