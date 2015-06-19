/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemIntegrationTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.contrib.signals.integration;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.router.InvertedNetworkTripRouterFactoryModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.run.LaneDefinitonsV11ToV20Converter;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

import java.io.File;


/**
 * @author dgrether
 *
 */
public class SignalSystemsIntegrationTest {

	private static final Logger log = Logger.getLogger(SignalSystemsIntegrationTest.class);

	private final static String CONFIG_FILE_NAME = "signalSystemsIntegrationConfig.xml";

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public void testSignalSystems() {
		Config config = testUtils.loadConfig(testUtils.getClassInputDirectory() + CONFIG_FILE_NAME);
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime);
		String controlerOutputDir = testUtils.getOutputDirectory() + "controlerOutput/";
		//		config.controler().setOutputDirectory(controlerOutputDir);
		//		config.addQSimConfigGroup(new QSimConfigGroup());
		String lanes11 = testUtils.getClassInputDirectory() + "testLaneDefinitions_v1.1.xml";
		String lanes20 = testUtils.getOutputDirectory() + "testLaneDefinitions_v2.0.xml";
		new LaneDefinitonsV11ToV20Converter().convert(lanes11, lanes20, config.network().getInputFile());

		//		config.network().setLaneDefinitionsFile(lanes20);
		config.controler().setWriteEventsInterval(10);
		config.controler().setWritePlansInterval(10);
		Controler c = new Controler(config);
		c.getScenario().addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsScenarioLoader(config.signalSystems()).loadSignalsData());
		c.addOverridingModule(new SignalsModule());
		c.addOverridingModule(new InvertedNetworkTripRouterFactoryModule());
		c.getConfig().controler().setOutputDirectory(controlerOutputDir);
        c.getConfig().controler().setCreateGraphs(false);
        c.setDumpDataAtEnd(false);
		c.run();
		
		String inputDirectory = testUtils.getInputDirectory();
		{
			//iteration 0 
			String iterationOutput = controlerOutputDir + "ITERS/it.0/";
			
			Assert.assertEquals("different events files after iteration 0 ", EventsFileComparator.compare(inputDirectory + "0.events.xml.gz", iterationOutput + "0.events.xml.gz"),0);

			Scenario expectedPopulation = ScenarioUtils.createScenario(c.getConfig());
			new MatsimNetworkReader(expectedPopulation).readFile(c.getConfig().network().getInputFile());
			new MatsimPopulationReader(expectedPopulation).readFile(testUtils.getInputDirectory() + "0.plans.xml.gz");
			Scenario actualPopulation = ScenarioUtils.createScenario(c.getConfig());
			new MatsimPopulationReader(actualPopulation).readFile(iterationOutput + "0.plans.xml.gz");
			
			new org.matsim.core.population.PopulationWriter(expectedPopulation.getPopulation()).write(testUtils.getOutputDirectory()+"/expected_plans.xml.gz");
			new org.matsim.core.population.PopulationWriter(actualPopulation.getPopulation()).write(testUtils.getOutputDirectory()+"/actual_plans.xml.gz");

			Assert.assertTrue("different population files after iteration 0 ", 
					PopulationUtils.equalPopulation(expectedPopulation.getPopulation(), actualPopulation.getPopulation()));
		}
		{
			//iteration 10 
			String iterationOutput = controlerOutputDir + "ITERS/it.10/";

			Assert.assertTrue("different event files after iteration 10", 
					EventsFileComparator.compare(inputDirectory + "10.events.xml.gz", iterationOutput + "10.events.xml.gz") == 
					EventsFileComparator.CODE_FILES_ARE_EQUAL);


			Scenario expectedPopulation = ScenarioUtils.createScenario(c.getConfig());
			new MatsimNetworkReader(expectedPopulation).readFile(c.getConfig().network().getInputFile());
			new MatsimPopulationReader(expectedPopulation).readFile(testUtils.getInputDirectory() + "10.plans.xml.gz");
			Scenario actualPopulation = ScenarioUtils.createScenario(c.getConfig());
			new MatsimPopulationReader(actualPopulation).readFile(iterationOutput + "10.plans.xml.gz");

			Assert.assertTrue("different population files after iteration 10 ", 
					PopulationUtils.equalPopulation(expectedPopulation.getPopulation(), actualPopulation.getPopulation()));
		}
		SignalsScenarioWriter writer = new SignalsScenarioWriter(c.getControlerIO());
		File file = new File(writer.getSignalSystemsOutputFilename());
		Assert.assertTrue(file.exists());
		file = new File(writer.getSignalGroupsOutputFilename());
		Assert.assertTrue(file.exists());
		file = new File(writer.getSignalControlOutputFilename());
		Assert.assertTrue(file.exists());
		file = new File(writer.getAmberTimesOutputFilename());
		Assert.assertTrue(file.exists());
		file = new File(writer.getIntergreenTimesOutputFilename());
		Assert.assertTrue(file.exists());

	}

	@Test
	public void testSignalSystemsWTryEndTimeThenDuration() {
		String configFile = testUtils.getClassInputDirectory() + CONFIG_FILE_NAME;
		Config config = testUtils.loadConfig(testUtils.getClassInputDirectory() + CONFIG_FILE_NAME);
		String controlerOutputDir = testUtils.getOutputDirectory() + "controlerOutput/";
		//		config.controler().setOutputDirectory(controlerOutputDir);
		//		config.addQSimConfigGroup(new QSimConfigGroup());
		String lanes11 = testUtils.getClassInputDirectory() + "testLaneDefinitions_v1.1.xml";
		String lanes20 = testUtils.getOutputDirectory() + "testLaneDefinitions_v2.0.xml";
		new LaneDefinitonsV11ToV20Converter().convert(lanes11, lanes20, config.network().getInputFile());

		//		config.network().setLaneDefinitionsFile(lanes20);

		Controler c = new Controler(configFile);
		c.getScenario().addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsScenarioLoader(config.signalSystems()).loadSignalsData());
		c.addOverridingModule(new SignalsModule());
		c.addOverridingModule(new InvertedNetworkTripRouterFactoryModule());
		c.getConfig().controler().setOutputDirectory(controlerOutputDir);
        c.getConfig().controler().setCreateGraphs(false);
        c.setDumpDataAtEnd(false);
		c.run();

		
		String inputDirectory = testUtils.getInputDirectory();
		{
			//iteration 0 
			String iterationOutput = controlerOutputDir + "ITERS/it.0/";
			
			Assert.assertEquals("different events files after iteration 0 ", 
					EventsFileComparator.compare(inputDirectory + "0.events.xml.gz", iterationOutput + "0.events.xml.gz"),
                    0);

			Scenario expectedPopulation = ScenarioUtils.createScenario(c.getConfig());
			new MatsimNetworkReader(expectedPopulation).readFile(c.getConfig().network().getInputFile());
			new MatsimPopulationReader(expectedPopulation).readFile(testUtils.getInputDirectory() + "0.plans.xml.gz");
			Scenario actualPopulation = ScenarioUtils.createScenario(c.getConfig());
			new MatsimPopulationReader(actualPopulation).readFile(iterationOutput + "0.plans.xml.gz");

			Assert.assertTrue("different population files after iteration 0 ", 
					PopulationUtils.equalPopulation(expectedPopulation.getPopulation(), actualPopulation.getPopulation()));
		}
		{
			//iteration 10 
			String iterationOutput = controlerOutputDir + "ITERS/it.10/";

			Assert.assertTrue("different event files after iteration 10", 
					EventsFileComparator.compare(inputDirectory + "10.events.xml.gz", iterationOutput + "10.events.xml.gz") == 
					EventsFileComparator.CODE_FILES_ARE_EQUAL);


			Scenario expectedPopulation = ScenarioUtils.createScenario(c.getConfig());
			new MatsimNetworkReader(expectedPopulation).readFile(c.getConfig().network().getInputFile());
			new MatsimPopulationReader(expectedPopulation).readFile(testUtils.getInputDirectory() + "10.plans.xml.gz");
			Scenario actualPopulation = ScenarioUtils.createScenario(c.getConfig());
			new MatsimPopulationReader(actualPopulation).readFile(iterationOutput + "10.plans.xml.gz");

			Assert.assertTrue("different population files after iteration 10 ", 
					PopulationUtils.equalPopulation(expectedPopulation.getPopulation(), actualPopulation.getPopulation()));
		}
		SignalsScenarioWriter writer = new SignalsScenarioWriter(c.getControlerIO());
		File file = new File(writer.getSignalSystemsOutputFilename());
		Assert.assertTrue(file.exists());
		file = new File(writer.getSignalGroupsOutputFilename());
		Assert.assertTrue(file.exists());
		file = new File(writer.getSignalControlOutputFilename());
		Assert.assertTrue(file.exists());
		file = new File(writer.getAmberTimesOutputFilename());
		Assert.assertTrue(file.exists());
		file = new File(writer.getIntergreenTimesOutputFilename());
		Assert.assertTrue(file.exists());

	}


}
