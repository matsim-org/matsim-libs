/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2018 by the members listed in the COPYING,        *
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

package org.matsim.contrib.etaxi.run;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.PopulationComparison;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

/**
 * @author michalm
 */
public class RunETaxiScenarioIT {
	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	void testOneTaxi() {
		String configPath = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("dvrp-grid"), "one_etaxi_config.xml").toString();
		runScenario(configPath);
	}

	@Test
	void testRuleBased() {
		String configPath = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_etaxi_config.xml").toString();
		runScenario(configPath);
	}

	@Test
	void testAssignment() {
		String configPath = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_etaxi_config.xml").toString();
		runScenario(configPath);
	}

	private void runScenario(String configPath) {
		Id.resetCaches();
		String[] args = { configPath, "--config:controler.outputDirectory", utils.getOutputDirectory() };
		RunETaxiScenario.run(args, false);
		{
			Population expected = PopulationUtils.createPopulation(ConfigUtils.createConfig());
			PopulationUtils.readPopulation(expected, utils.getInputDirectory() + "/output_plans.xml.gz");

			Population actual = PopulationUtils.createPopulation(ConfigUtils.createConfig());
			PopulationUtils.readPopulation(actual, utils.getOutputDirectory() + "/output_plans.xml.gz");

			PopulationComparison.Result result = PopulationComparison.compare(expected, actual);
			Assertions.assertEquals(PopulationComparison.Result.equal, result);
		}
		{
			String expected = utils.getInputDirectory() + "/output_events.xml.gz";
			String actual = utils.getOutputDirectory() + "/output_events.xml.gz";
			ComparisonResult result = EventsUtils.compareEventsFiles(expected, actual);
			Assertions.assertEquals(ComparisonResult.FILES_ARE_EQUAL, result);
		}
	}
}
