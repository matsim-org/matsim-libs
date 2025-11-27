/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.usecases.chessboard;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.population.Population;
import org.matsim.application.ApplicationUtils;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.PopulationComparison;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

import java.nio.file.Path;

public class RunChessboardIT {

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils() ;

	private static final Logger log = LogManager.getLogger(RunChessboardIT.class);

	@Test
	void runChessboard() {
		String [] args = { IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "freight-chessboard-9x9" ), "config.xml" ).toString()
				, "--config:controler.outputDirectory", utils.getOutputDirectory()
				, "--config:controler.lastIteration", "1",
				"--config:controler.runId", "itTest"
		};

		try{
			RunChessboard.main( args );
			{
				Population expected = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
				PopulationUtils.readPopulation(expected, ApplicationUtils.globFile(Path.of(utils.getInputDirectory()), "*output_plans.xml*").toString());

				Population actual = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
				PopulationUtils.readPopulation(actual, ApplicationUtils.globFile(Path.of(utils.getOutputDirectory()), "*output_plans.xml*").toString());

				PopulationComparison.Result result = PopulationComparison.compare(expected, actual);
				Assertions.assertSame(PopulationComparison.Result.equal, result);
			}
			{
				String expected = ApplicationUtils.globFile(Path.of(utils.getInputDirectory()), "*output_events.xml*").toString();
				String actual = ApplicationUtils.globFile(Path.of(utils.getOutputDirectory()), "*output_events.xml*").toString();
				ComparisonResult result = EventsUtils.compareEventsFiles( expected, actual );
				Assertions.assertEquals( ComparisonResult.FILES_ARE_EQUAL, result );
			}
		} catch (Exception ee ) {
			log.error("Exception while running example", ee);
			Assertions.fail("something went wrong");
		}

	}

}
