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

import java.io.File;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.events.Event;
import org.matsim.contrib.signals.SignalSystemsConfigGroup;
import org.matsim.contrib.signals.controler.SignalsModule;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsScenarioLoader;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.contrib.signals.router.InvertedNetworkRoutingModuleModule;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.AbstractModule;
import org.matsim.core.controler.Controler;
import org.matsim.core.events.handler.BasicEventHandler;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.lanes.data.v11.LaneDefinitonsV11ToV20Converter;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;


/**
 * @author dgrether
 *
 */
public class SignalSystemsIT {

	private final static String CONFIG_FILE_NAME = "signalSystemsIntegrationConfig.xml";

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public void testSignalSystems() {
		Config config = testUtils.loadConfig(testUtils.getClassInputDirectory() + CONFIG_FILE_NAME);
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime);
		String controlerOutputDir = testUtils.getOutputDirectory() + "controlerOutput/";
		String lanes11 = testUtils.getClassInputDirectory() + "testLaneDefinitions_v1.1.xml";
		String lanes20 = testUtils.getOutputDirectory() + "testLaneDefinitions_v2.0.xml";
		new LaneDefinitonsV11ToV20Converter().convert(lanes11, lanes20, config.network().getInputFile());

		config.network().setLaneDefinitionsFile(lanes20);
		config.controler().setWriteEventsInterval(10);
		config.controler().setWritePlansInterval(10);

		config.controler().setOutputDirectory(controlerOutputDir);
		config.controler().setCreateGraphs(false);
		
		config.qsim().setStartTime(1.5*3600);
		config.qsim().setEndTime(5.5*3600);
		
		config.controler().setLastIteration(10);
		
		// ---
		
		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsScenarioLoader(ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class)).loadSignalsData());

		// ---

		Controler c = new Controler(scenario);
		c.addOverridingModule(new SignalsModule());
		c.addOverridingModule(new InvertedNetworkRoutingModuleModule());
		c.getConfig().controler().setDumpDataAtEnd(false);
		
//		c.addOverridingModule( new AbstractModule(){
//			@Override public void install() {
//				this.addEventHandlerBinding().toInstance( new BasicEventHandler(){
//					@Override public void reset(int iteration) { }
//					@Override public void handleEvent(Event event) {
//						Logger.getLogger( SignalSystemsIT.class ).warn( event );
//					}
//				} ) ;
//			}
//		});
		
		c.run();

		String inputDirectory = testUtils.getInputDirectory();
		{
			//iteration 0 
			String iterationOutput = controlerOutputDir + "ITERS/it.0/";

			Assert.assertEquals("different events files after iteration 0 ", EventsFileComparator.compare(inputDirectory + "0.events.xml.gz", iterationOutput + "0.events.xml.gz"),0);

			Scenario expectedPopulation = ScenarioUtils.createScenario(c.getConfig());
			new MatsimNetworkReader(expectedPopulation.getNetwork()).readFile(c.getConfig().network().getInputFile());
			new MatsimPopulationReader(expectedPopulation).readFile(testUtils.getInputDirectory() + "0.plans.xml.gz");

			Scenario actualPopulation = ScenarioUtils.createScenario(c.getConfig());
			new MatsimPopulationReader(actualPopulation).readFile(iterationOutput + "0.plans.xml.gz");

			new org.matsim.core.population.PopulationWriter(expectedPopulation.getPopulation()).write(testUtils.getOutputDirectory()+"/expected_plans_it0.xml.gz");
			new org.matsim.core.population.PopulationWriter(actualPopulation.getPopulation()).write(testUtils.getOutputDirectory()+"/actual_plans_it0.xml.gz");

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
			new MatsimNetworkReader(expectedPopulation.getNetwork()).readFile(c.getConfig().network().getInputFile());
			new MatsimPopulationReader(expectedPopulation).readFile(testUtils.getInputDirectory() + "10.plans.xml.gz");
			
			Scenario actualPopulation = ScenarioUtils.createScenario(c.getConfig());
			new MatsimPopulationReader(actualPopulation).readFile(iterationOutput + "10.plans.xml.gz");
			
			new org.matsim.core.population.PopulationWriter(expectedPopulation.getPopulation()).write(testUtils.getOutputDirectory()+"/expected_plans_it10.xml.gz");
			new org.matsim.core.population.PopulationWriter(actualPopulation.getPopulation()).write(testUtils.getOutputDirectory()+"/actual_plans_it10.xml.gz");

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
		Config config = testUtils.loadConfig(testUtils.getClassInputDirectory() + CONFIG_FILE_NAME);
		String controlerOutputDir = testUtils.getOutputDirectory() + "controlerOutput/";
		String lanes11 = testUtils.getClassInputDirectory() + "testLaneDefinitions_v1.1.xml";
		String lanes20 = testUtils.getOutputDirectory() + "testLaneDefinitions_v2.0.xml";
		new LaneDefinitonsV11ToV20Converter().convert(lanes11, lanes20, config.network().getInputFile());

		config.network().setLaneDefinitionsFile(lanes20);

		config.qsim().setStartTime(1.5*3600);
		config.qsim().setEndTime(5*3600);
		
		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsScenarioLoader(ConfigUtils.addOrGetModule(config, SignalSystemsConfigGroup.GROUPNAME, SignalSystemsConfigGroup.class)).loadSignalsData());

		Controler c = new Controler(scenario);
		c.addOverridingModule(new SignalsModule());
		c.addOverridingModule(new InvertedNetworkRoutingModuleModule());
		c.getConfig().controler().setOutputDirectory(controlerOutputDir);
		c.getConfig().controler().setCreateGraphs(false);
		c.getConfig().controler().setDumpDataAtEnd(false);
		c.run();


		String inputDirectory = testUtils.getInputDirectory();
		{
			//iteration 0 
			String iterationOutput = controlerOutputDir + "ITERS/it.0/";

			Assert.assertEquals("different events files after iteration 0 ", 
					EventsFileComparator.compare(inputDirectory + "0.events.xml.gz", iterationOutput + "0.events.xml.gz"),
					0);

			Scenario expectedPopulation = ScenarioUtils.createScenario(c.getConfig());
			new MatsimNetworkReader(expectedPopulation.getNetwork()).readFile(c.getConfig().network().getInputFile());
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
			new MatsimNetworkReader(expectedPopulation.getNetwork()).readFile(c.getConfig().network().getInputFile());
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
