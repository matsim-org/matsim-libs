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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.signals.builder.Signals;
import org.matsim.contrib.signals.data.SignalsData;
import org.matsim.contrib.signals.data.SignalsDataLoader;
import org.matsim.contrib.signals.data.SignalsScenarioWriter;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.network.io.MatsimNetworkReader;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.io.PopulationReader;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

import java.io.File;


/**
 * @author dgrether
 *
 */
public class SignalSystemsIT {

	private final static String CONFIG_FILE_NAME = "signalSystemsIntegrationConfig.xml";

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void testSignalSystems() {
		Config config = testUtils.loadConfig(testUtils.getClassInputDirectory() + CONFIG_FILE_NAME);
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.minOfDurationAndEndTime);
		String controlerOutputDir = testUtils.getOutputDirectory() + "controlerOutput/";

		config.network().setLaneDefinitionsFile("testLaneDefinitions_v2.0.xml");
		config.controller().setWriteEventsInterval(10);
		config.controller().setWritePlansInterval(10);

		config.controller().setOutputDirectory(controlerOutputDir);
		config.controller().setCreateGraphs(false);
		config.controller().setDumpDataAtEnd(false);

		config.qsim().setStartTime(1.5*3600);
		config.qsim().setEndTime(5.5*3600);
		config.qsim().setUsingFastCapacityUpdate(false);

		config.controller().setLastIteration(10);

		// ---

		Scenario scenario = ScenarioUtils.loadScenario(config) ;
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

		// ---

		Controler c = new Controler(scenario);
		Signals.configure(c);

		c.run();

		String inputDirectory = testUtils.getInputDirectory();
		{
			//iteration 0
			String iterationOutput = controlerOutputDir + "ITERS/it.0/";

			Assertions.assertEquals(EventsFileComparator.Result.FILES_ARE_EQUAL,
					new EventsFileComparator().setIgnoringCoordinates( true ).runComparison( inputDirectory + "0.events.xml.gz",
							iterationOutput + "0.events.xml.gz"),
					"different events files after iteration 0 "
					   );

			Scenario expectedPopulation = ScenarioUtils.createScenario(c.getConfig());
			new MatsimNetworkReader(expectedPopulation.getNetwork()).parse(c.getConfig().network().getInputFileURL(c.getConfig().getContext()));
			new PopulationReader(expectedPopulation).readFile(inputDirectory + "0.plans.xml.gz");

			Scenario actualPopulation = ScenarioUtils.createScenario(c.getConfig());
			new PopulationReader(actualPopulation).readFile(iterationOutput + "0.plans.xml.gz");

			boolean works = PopulationUtils.equalPopulation(expectedPopulation.getPopulation(), actualPopulation.getPopulation());

			if (!works){
				new org.matsim.api.core.v01.population.PopulationWriter(expectedPopulation.getPopulation()).write(testUtils.getOutputDirectory()+"/expected_plans_it0.xml.gz");
				new org.matsim.api.core.v01.population.PopulationWriter(actualPopulation.getPopulation()).write(testUtils.getOutputDirectory()+"/actual_plans_it0.xml.gz");
			}
			Assertions.assertTrue(works, "different population files after iteration 0 ");
		}
		{
			//iteration 10
			String iterationOutput = controlerOutputDir + "ITERS/it.10/";

			Assertions.assertEquals(EventsFileComparator.Result.FILES_ARE_EQUAL,
					new EventsFileComparator().setIgnoringCoordinates( true ).runComparison( inputDirectory + "10.events.xml.gz", iterationOutput + "10.events.xml.gz" ),
					"different event files after iteration 10"
					   );

			Scenario expectedPopulation = ScenarioUtils.createScenario(c.getConfig());
			new MatsimNetworkReader(expectedPopulation.getNetwork()).parse(c.getConfig().network().getInputFileURL(c.getConfig().getContext()));
			new PopulationReader(expectedPopulation).readFile(inputDirectory + "10.plans.xml.gz");

			Scenario actualPopulation = ScenarioUtils.createScenario(c.getConfig());
			new PopulationReader(actualPopulation).readFile(iterationOutput + "10.plans.xml.gz");

			boolean works = PopulationUtils.equalPopulation(expectedPopulation.getPopulation(), actualPopulation.getPopulation());

			if (!works){
				new org.matsim.api.core.v01.population.PopulationWriter(expectedPopulation.getPopulation()).write(testUtils.getOutputDirectory()+"/expected_plans_it10.xml.gz");
				new org.matsim.api.core.v01.population.PopulationWriter(actualPopulation.getPopulation()).write(testUtils.getOutputDirectory()+"/actual_plans_it10.xml.gz");
			}
			Assertions.assertTrue(works, "different population files after iteration 10 ");
		}
		SignalsScenarioWriter writer = new SignalsScenarioWriter(c.getControlerIO());
		File file = new File(writer.getSignalSystemsOutputFilename());
		Assertions.assertTrue(file.exists());
		file = new File(writer.getSignalGroupsOutputFilename());
		Assertions.assertTrue(file.exists());
		file = new File(writer.getSignalControlOutputFilename());
		Assertions.assertTrue(file.exists());
		file = new File(writer.getAmberTimesOutputFilename());
		Assertions.assertTrue(file.exists());
		file = new File(writer.getIntergreenTimesOutputFilename());
		Assertions.assertTrue(file.exists());

	}

	@Test
	void testSignalSystemsWTryEndTimeThenDuration() {
		Config config = testUtils.loadConfig(testUtils.getClassInputDirectory() + CONFIG_FILE_NAME);
		// tryEndTimeThenDuration currently is the default
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration);
		String controlerOutputDir = testUtils.getOutputDirectory() + "controlerOutput/";

		config.network().setLaneDefinitionsFile("testLaneDefinitions_v2.0.xml");
		config.controller().setWriteEventsInterval(10);
		config.controller().setWritePlansInterval(10);

		config.controller().setOutputDirectory(controlerOutputDir);
		config.controller().setCreateGraphs(false);
		config.controller().setDumpDataAtEnd(false);

		config.qsim().setStartTime(1.5*3600);
		config.qsim().setEndTime(5*3600);
		config.qsim().setUsingFastCapacityUpdate(false);

		config.controller().setLastIteration(10);

		Scenario scenario = ScenarioUtils.loadScenario(config);
		scenario.addScenarioElement(SignalsData.ELEMENT_NAME, new SignalsDataLoader(config).loadSignalsData());

		Controler c = new Controler(scenario);
		Signals.configure( c );
		c.run();


		String inputDirectory = testUtils.getInputDirectory();
		{
			//iteration 0
			String iterationOutput = controlerOutputDir + "ITERS/it.0/";

			Assertions.assertEquals(EventsFileComparator.Result.FILES_ARE_EQUAL,
					new EventsFileComparator().setIgnoringCoordinates( true ).runComparison( inputDirectory + "0.events.xml.gz",
							iterationOutput + "0.events.xml.gz"),
					"different events files after iteration 0 "
					   );

			Scenario expectedPopulation = ScenarioUtils.createScenario(c.getConfig());
			new MatsimNetworkReader(expectedPopulation.getNetwork()).parse(c.getConfig().network().getInputFileURL(c.getConfig().getContext()));
			new PopulationReader(expectedPopulation).readFile(testUtils.getInputDirectory() + "0.plans.xml.gz");

			Scenario actualPopulation = ScenarioUtils.createScenario(c.getConfig());
			new PopulationReader(actualPopulation).readFile(iterationOutput + "0.plans.xml.gz");

			boolean works = PopulationUtils.equalPopulation(expectedPopulation.getPopulation(), actualPopulation.getPopulation());

			if (!works){
				new org.matsim.api.core.v01.population.PopulationWriter(expectedPopulation.getPopulation()).write(testUtils.getOutputDirectory()+"/expected_plans_it0.xml.gz");
				new org.matsim.api.core.v01.population.PopulationWriter(actualPopulation.getPopulation()).write(testUtils.getOutputDirectory()+"/actual_plans_it0.xml.gz");
			}
			Assertions.assertTrue(works, "different population files after iteration 0 ");
		}
		{
			//iteration 10
			String iterationOutput = controlerOutputDir + "ITERS/it.10/";

			Assertions.assertEquals(EventsFileComparator.Result.FILES_ARE_EQUAL,
					new EventsFileComparator().setIgnoringCoordinates( true ).runComparison( inputDirectory + "10.events.xml.gz", iterationOutput + "10.events.xml.gz"),
					"different event files after iteration 10"
					   );


			Scenario expectedPopulation = ScenarioUtils.createScenario(c.getConfig());
			new MatsimNetworkReader(expectedPopulation.getNetwork()).parse(c.getConfig().network().getInputFileURL(c.getConfig().getContext()));
			new PopulationReader(expectedPopulation).readFile(inputDirectory + "10.plans.xml.gz");

			Scenario actualPopulation = ScenarioUtils.createScenario(c.getConfig());
			new PopulationReader(actualPopulation).readFile(iterationOutput + "10.plans.xml.gz");

			boolean works = PopulationUtils.equalPopulation(expectedPopulation.getPopulation(), actualPopulation.getPopulation());

			if (!works){
				new org.matsim.api.core.v01.population.PopulationWriter(expectedPopulation.getPopulation()).write(testUtils.getOutputDirectory()+"/expected_plans_it10.xml.gz");
				new org.matsim.api.core.v01.population.PopulationWriter(actualPopulation.getPopulation()).write(testUtils.getOutputDirectory()+"/actual_plans_it10.xml.gz");
			}
			Assertions.assertTrue(works, "different population files after iteration 10 ");
		}
		SignalsScenarioWriter writer = new SignalsScenarioWriter(c.getControlerIO());
		File file = new File(writer.getSignalSystemsOutputFilename());
		Assertions.assertTrue(file.exists());
		file = new File(writer.getSignalGroupsOutputFilename());
		Assertions.assertTrue(file.exists());
		file = new File(writer.getSignalControlOutputFilename());
		Assertions.assertTrue(file.exists());
		file = new File(writer.getAmberTimesOutputFilename());
		Assertions.assertTrue(file.exists());
		file = new File(writer.getIntergreenTimesOutputFilename());
		Assertions.assertTrue(file.exists());

	}


}
