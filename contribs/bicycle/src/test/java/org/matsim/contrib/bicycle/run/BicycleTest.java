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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Person;
import org.matsim.contrib.bicycle.BicycleConfigGroup;
import org.matsim.contrib.bicycle.BicycleConfigGroup.BicycleScoringType;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.OutputDirectoryHierarchy.OverwriteFileSetting;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.matsim.utils.eventsfilecomparison.EventsFileComparator.Result.FILES_ARE_EQUAL;

/**
 * @author dziemke
 */
public class BicycleTest {
	private static final Logger LOG = Logger.getLogger(BicycleTest.class);

	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testNormal() {
		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);
		
		// Normal network
		config.network().setInputFile("network_normal.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", FILES_ARE_EQUAL, EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew));
		
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
		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);
		
		// Links 4-8 and 13-17 have cobblestones
		config.network().setInputFile("network_cobblestone.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		new RunBicycleExample().run(config, false);
		{
			Scenario scenarioReference = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
			Scenario scenarioCurrent = ScenarioUtils.createScenario( ConfigUtils.createConfig() );
			new PopulationReader( scenarioReference ).readFile( utils.getInputDirectory() + "output_plans.xml.gz" );
			new PopulationReader( scenarioCurrent ).readFile( utils.getOutputDirectory() + "output_plans.xml.gz" );
			assertTrue( "Populations are different", PopulationUtils.equalPopulation( scenarioReference.getPopulation(), scenarioCurrent.getPopulation() ) );
		}
		{
			LOG.info( "Checking MATSim events file ..." );
			final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
			final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
			assertEquals( "Different event files.", FILES_ARE_EQUAL, EventsFileComparator.compare( eventsFilenameReference, eventsFilenameNew ));
		}
	}
	
	@Test
	public void testPedestrian() {
		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);
		
		// Links 4-8 and 13-17 are pedestrian zones
		config.network().setInputFile("network_pedestrian.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", FILES_ARE_EQUAL, EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew));
		
		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		assertTrue("Populations are different", PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()));
	}
	
	@Test
	public void testLane() {
		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);
		
		// Links 2-4/8-10 and 11-13/17-19 have cycle lanes (cycleway=lane)
		config.network().setInputFile("network_lane.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", FILES_ARE_EQUAL, EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew));
		
		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		assertTrue("Populations are different", PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()));
	}
	
	@Test
	public void testGradient() {
		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);
		
		// Nodes 5-9 have a z-coordinate > 0, i.e. the links leading to those nodes have a slope
		config.network().setInputFile("network_gradient.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", FILES_ARE_EQUAL, EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew));
		
		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		assertTrue("Populations are different", PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()));
	}
	
	@Test
	public void testGradientLane() {
		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);
		
		// Nodes 5-9 have a z-coordinate > 0, i.e. the links leading to those nodes have a slope
		// and links 4-5 and 13-14 have cycle lanes
		config.network().setInputFile("network_gradient_lane.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(0);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", FILES_ARE_EQUAL, EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew));
		
		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		assertTrue("Populations are different", PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()));
	}
	
	@Test
	public void testNormal10It() {
		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);
		
		// Normal network
		config.network().setInputFile("network_normal.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		// 10 iterations
		config.controler().setLastIteration(10);
		
		new RunBicycleExample().run(config, false);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", FILES_ARE_EQUAL, EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew));
		
		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		assertTrue("Populations are different", PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()));
	}
	
	@Test
	public void testMotorizedInteraction() {
		Config config = ConfigUtils.createConfig("./src/main/resources/bicycle_example/");
		config.addModule(new BicycleConfigGroup());
		RunBicycleExample.fillConfigWithBicycleStandardValues(config);
		
		// Normal network
		config.network().setInputFile("network_normal.xml");
		config.plans().setInputFile("population_1200.xml");
		config.controler().setOverwriteFileSetting(OverwriteFileSetting.deleteDirectoryIfExists);
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		config.controler().setLastIteration(10);
		
		// Activate link-based scoring
		BicycleConfigGroup bicycleConfigGroup = (BicycleConfigGroup) config.getModules().get("bicycle");
		bicycleConfigGroup.setBicycleScoringType(BicycleScoringType.linkBased);
		
		// Interaction with motor vehicles
		new RunBicycleExample().run(config, true);

		LOG.info("Checking MATSim events file ...");
		final String eventsFilenameReference = utils.getInputDirectory() + "output_events.xml.gz";
		final String eventsFilenameNew = utils.getOutputDirectory() + "output_events.xml.gz";
		assertEquals("Different event files.", FILES_ARE_EQUAL, EventsFileComparator.compare(eventsFilenameReference, eventsFilenameNew));
		
		Scenario scenarioReference = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		Scenario scenarioCurrent = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		new PopulationReader(scenarioReference).readFile(utils.getInputDirectory() + "output_plans.xml.gz");
		new PopulationReader(scenarioCurrent).readFile(utils.getOutputDirectory() + "output_plans.xml.gz");
		assertTrue("Populations are different", PopulationUtils.equalPopulation(scenarioReference.getPopulation(), scenarioCurrent.getPopulation()));
	}
}
