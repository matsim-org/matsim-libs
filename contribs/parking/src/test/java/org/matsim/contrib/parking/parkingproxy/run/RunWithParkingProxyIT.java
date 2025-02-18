/* *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2020 by the members listed in the COPYING,        *
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

package org.matsim.contrib.parking.parkingproxy.run;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.PopulationComparison;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

public class RunWithParkingProxyIT {
	private static final Logger log = LogManager.getLogger(RunWithParkingProxyIT.class);

	@RegisterExtension
	private MatsimTestUtils utils = new MatsimTestUtils();

	/**
	 * This is only a regression test! The test was disabled with commit 740c0cd84 in jan'22.
	 * I reactivate this test and update the event/experienced plans files. But still, somebody needs to check the results manually. paul, feb'25
	 */
	@Test
	void testMain() {
		RunWithParkingProxy.main(new String[]{IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("chessboard"), "config.xml").toString()
			, "--config:controller.outputDirectory=" + utils.getOutputDirectory()
			, "--config:controller.lastIteration=2"
			, "--config:controller.writePlansInterval=1"
			, "--config:global.numberOfThreads=1"
		});
		{
			String expected = utils.getInputDirectory() + "/output_events.xml.gz";
			String actual = utils.getOutputDirectory() + "/output_events.xml.gz";
			ComparisonResult result = EventsUtils.compareEventsFiles(expected, actual);
			if (!result.equals(ComparisonResult.FILES_ARE_EQUAL)) {
				throw new RuntimeException("Events comparison ended with result " + result.name());
			}
		}
		{
			final Population expected = PopulationUtils.createPopulation(ConfigUtils.createConfig());
			PopulationUtils.readPopulation(expected, utils.getInputDirectory() + "/output_experienced_plans.xml.gz");
			final Population actual = PopulationUtils.createPopulation(ConfigUtils.createConfig());
			PopulationUtils.readPopulation(actual, utils.getOutputDirectory() + "/output_experienced_plans.xml.gz");
			PopulationComparison.Result result = PopulationComparison.compare(expected, actual);
			Assertions.assertEquals(PopulationComparison.Result.equal, result);
		}
	}
}
