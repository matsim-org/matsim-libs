/*
 * *********************************************************************** *
 * project: org.matsim.*
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2019 by the members listed in the COPYING,        *
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
 * *********************************************************************** *
 */

package org.matsim.contrib.etaxi.run;

import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.run.RunMatsim;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

/**
 * @author Michal Maciejewski (michalm)
 */
public class RunETaxiBenchmarkTest {
	private static final Logger log = LogManager.getLogger(RunETaxiBenchmarkTest.class );

	@Rule public final MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testRuleBased() {
		try {
			String configPath = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("dvrp-grid"), "one_etaxi_benchmark_config.xml").toString();
			String [] args = {configPath
					,"--config:controler.outputDirectory", utils.getOutputDirectory()
//					,"--config:controler.writeEventsInterval","1"
//					,"--config:controler.writePlansInterval","1"
//					,"--config:controler.lastIteration", "1"
			} ;
			// the config file suppresses most writing of output.  Presumably, since it is to be run as a benchmark.  One can override it here, but it is again overwritten later.  So
			// I guess that the authors really mean it.  In consequence, cannot test regression on the functionality of the benchmark.  kai, nov'22

			RunETaxiBenchmark.run(args, 2);

//			{
//				Population expected = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
//				PopulationUtils.readPopulation( expected, utils.getInputDirectory() + "/output_plans.xml.gz" );
//
//				Population actual = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
//				PopulationUtils.readPopulation( actual, utils.getOutputDirectory() + "/output_plans.xml.gz" );
//
//				boolean result = PopulationUtils.comparePopulations( expected, actual );
//				Assert.assertTrue( result );
//			}
//			{
//				String expected = utils.getInputDirectory() + "/output_events.xml.gz" ;
//				String actual = utils.getOutputDirectory() + "/output_events.xml.gz" ;
//				EventsFileComparator.Result result = EventsUtils.compareEventsFiles( expected, actual );
//				Assert.assertEquals( EventsFileComparator.Result.FILES_ARE_EQUAL, result );
//			}

		} catch ( Exception ee ) {
			log.fatal("there was an exception: \n" + ee ) ;

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}




	}
}
