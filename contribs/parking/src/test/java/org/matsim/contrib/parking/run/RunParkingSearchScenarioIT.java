/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2017 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.run;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.contrib.parking.parkingsearch.ParkingSearchStrategy;
import org.matsim.contrib.parking.parkingsearch.RunParkingSearchExample;
import org.matsim.contrib.parking.parkingsearch.sim.ParkingSearchConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

/**
 * @author jbischoff
 */
public class RunParkingSearchScenarioIT {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testRunParkingBenesonStrategy() {
		try {
			String configFile = "./src/main/resources/parkingsearch/config.xml";
			Config config = ConfigUtils.loadConfig(configFile, new ParkingSearchConfigGroup());
			config.controller().setLastIteration(0);
			config.controller().setOutputDirectory(utils.getOutputDirectory());

			ParkingSearchConfigGroup configGroup = (ParkingSearchConfigGroup) config.getModules().get(ParkingSearchConfigGroup.GROUP_NAME);
			configGroup.setParkingSearchStrategy(ParkingSearchStrategy.Benenson);

			new RunParkingSearchExample().run(config, false);

		} catch (Exception e) {
			e.printStackTrace();
			Assertions.fail("something went wrong");
		}
	}

	@Test
	void testRunParkingRandomStrategy() {
		String configFile = "./src/main/resources/parkingsearch/config.xml";
		Config config = ConfigUtils.loadConfig(configFile, new ParkingSearchConfigGroup());
		config.controller().setLastIteration(0);
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		ParkingSearchConfigGroup configGroup = (ParkingSearchConfigGroup) config.getModules().get(ParkingSearchConfigGroup.GROUP_NAME);
		configGroup.setParkingSearchStrategy(ParkingSearchStrategy.Random);

		try {
			new RunParkingSearchExample().run(config, false);
		} catch (Exception e) {
			e.printStackTrace();
			Assertions.fail("something went wrong");
		}
	}

	@Test
	void testRunParkingDistanceMemoryStrategy() {
		try {
			String configFile = "./src/main/resources/parkingsearch/config.xml";
			Config config = ConfigUtils.loadConfig(configFile, new ParkingSearchConfigGroup());
			config.controller().setLastIteration(0);
			config.controller().setOutputDirectory(utils.getOutputDirectory());

			ParkingSearchConfigGroup configGroup = (ParkingSearchConfigGroup) config.getModules().get(ParkingSearchConfigGroup.GROUP_NAME);
			configGroup.setParkingSearchStrategy(ParkingSearchStrategy.DistanceMemory);

			new RunParkingSearchExample().run(config, false);
			{
				Population expected = PopulationUtils.createPopulation(ConfigUtils.createConfig());
				PopulationUtils.readPopulation(expected, utils.getInputDirectory() + "/output_plans.xml.gz");

				Population actual = PopulationUtils.createPopulation(ConfigUtils.createConfig());
				PopulationUtils.readPopulation(actual, utils.getOutputDirectory() + "/output_plans.xml.gz");

				for (Id<Person> personId : expected.getPersons().keySet()) {
					double scoreReference = expected.getPersons().get(personId).getSelectedPlan().getScore();
					double scoreCurrent = actual.getPersons().get(personId).getSelectedPlan().getScore();
					Assertions.assertEquals(scoreReference, scoreCurrent, MatsimTestUtils.EPSILON, "Scores of person=" + personId + " are different");
				}

			}
			{
				String expected = utils.getInputDirectory() + "/output_events.xml.gz";
				String actual = utils.getOutputDirectory() + "/output_events.xml.gz";
				EventsFileComparator.Result result = EventsUtils.compareEventsFiles(expected, actual);
				Assertions.assertEquals(EventsFileComparator.Result.FILES_ARE_EQUAL, result);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assertions.fail("something went wrong");
		}
	}

	@Test
	void testRunParkingNearestParkingSpotStrategy() {
		try {
			String configFile = "./src/main/resources/parkingsearch/config.xml";
			Config config = ConfigUtils.loadConfig(configFile, new ParkingSearchConfigGroup());
			config.controller().setLastIteration(0);
			config.controller().setOutputDirectory(utils.getOutputDirectory());

			ParkingSearchConfigGroup configGroup = (ParkingSearchConfigGroup) config.getModules().get(ParkingSearchConfigGroup.GROUP_NAME);
			configGroup.setParkingSearchStrategy(ParkingSearchStrategy.NearestParkingSpot);

			new RunParkingSearchExample().run(config, false);
			{
				Population expected = PopulationUtils.createPopulation(ConfigUtils.createConfig());
				PopulationUtils.readPopulation(expected, utils.getInputDirectory() + "/output_plans.xml.gz");

				Population actual = PopulationUtils.createPopulation(ConfigUtils.createConfig());
				PopulationUtils.readPopulation(actual, utils.getOutputDirectory() + "/output_plans.xml.gz");

				for (Id<Person> personId : expected.getPersons().keySet()) {
					double scoreReference = expected.getPersons().get(personId).getSelectedPlan().getScore();
					double scoreCurrent = actual.getPersons().get(personId).getSelectedPlan().getScore();
					Assertions.assertEquals(scoreReference, scoreCurrent, MatsimTestUtils.EPSILON, "Scores of person=" + personId + " are different");
				}

			}
			{
				String expected = utils.getInputDirectory() + "/output_events.xml.gz";
				String actual = utils.getOutputDirectory() + "/output_events.xml.gz";
				EventsFileComparator.Result result = EventsUtils.compareEventsFiles(expected, actual);
				Assertions.assertEquals(EventsFileComparator.Result.FILES_ARE_EQUAL, result);
			}
		} catch (Exception e) {
			e.printStackTrace();
			Assertions.fail("something went wrong");
		}
	}
}
