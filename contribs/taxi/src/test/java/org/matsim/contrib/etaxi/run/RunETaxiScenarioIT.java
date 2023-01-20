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

import org.apache.logging.log4j.LogManager;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

/**
 * @author michalm
 */
public class RunETaxiScenarioIT {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void testOneTaxi() {
		try {
			String configPath = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("dvrp-grid"), "one_etaxi_config.xml").toString();
			String [] args = {configPath,
					"--config:controler.outputDirectory", utils.getOutputDirectory()
//					,"--config:controler.lastIteration", "1"
			} ;
			RunETaxiScenario.run(args, false);


			{
				Population expected = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
				PopulationUtils.readPopulation( expected, utils.getInputDirectory() + "/output_plans.xml.gz" );

				Population actual = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
				PopulationUtils.readPopulation( actual, utils.getOutputDirectory() + "/output_plans.xml.gz" );

				boolean result = PopulationUtils.comparePopulations( expected, actual );
				Assert.assertTrue( result );
			}
			{
				String expected = utils.getInputDirectory() + "/output_events.xml.gz" ;
				String actual = utils.getOutputDirectory() + "/output_events.xml.gz" ;
				EventsFileComparator.Result result = EventsUtils.compareEventsFiles( expected, actual );
				Assert.assertEquals( EventsFileComparator.Result.FILES_ARE_EQUAL, result );
			}

		} catch ( Exception ee ) {
			LogManager.getLogger(this.getClass() ).fatal("there was an exception: \n" + ee ) ;

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}

	}

	@Test
	public void testRuleBased() {
		try {
			String configPath = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_etaxi_config.xml").toString();
			String [] args = {configPath,
					"--config:controler.outputDirectory", utils.getOutputDirectory()
//				,"--config:controler.lastIteration", "1"
			} ;
			RunETaxiScenario.run(args, false);
			{
				String expected = utils.getInputDirectory() + "/output_events.xml.gz" ;
				String actual = utils.getOutputDirectory() + "/output_events.xml.gz" ;
				EventsFileComparator.Result result = EventsUtils.compareEventsFiles( expected, actual );
				Assert.assertEquals( EventsFileComparator.Result.FILES_ARE_EQUAL, result );
			}
			{
				Population expected = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
				PopulationUtils.readPopulation( expected, utils.getInputDirectory() + "/output_plans.xml.gz" );

				Population actual = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
				PopulationUtils.readPopulation( actual, utils.getOutputDirectory() + "/output_plans.xml.gz" );

				boolean result = PopulationUtils.comparePopulations( expected, actual );
				Assert.assertTrue( result );
			}

		} catch ( Exception ee ) {
			LogManager.getLogger(this.getClass() ).fatal("there was an exception: \n" + ee ) ;

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}

	}

	@Test
	public void testAssignment() {
		try {
			String configPath = IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("mielec"), "mielec_etaxi_config.xml").toString();
			String [] args = {configPath,
					"--config:controler.outputDirectory", utils.getOutputDirectory()
//				,"--config:controler.lastIteration", "1"
			} ;
			RunETaxiScenario.run(args, false);
			{
				Population expected = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
				PopulationUtils.readPopulation( expected, utils.getInputDirectory() + "/output_plans.xml.gz" );

				Population actual = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
				PopulationUtils.readPopulation( actual, utils.getOutputDirectory() + "/output_plans.xml.gz" );

				boolean result = PopulationUtils.comparePopulations( expected, actual );
				Assert.assertTrue( result );
			}
			{
				String expected = utils.getInputDirectory() + "/output_events.xml.gz" ;
				String actual = utils.getOutputDirectory() + "/output_events.xml.gz" ;
				EventsFileComparator.Result result = EventsUtils.compareEventsFiles( expected, actual );
				Assert.assertEquals( EventsFileComparator.Result.FILES_ARE_EQUAL, result );
			}

		} catch ( Exception ee ) {
			LogManager.getLogger(this.getClass() ).fatal("there was an exception: \n" + ee ) ;

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}
}
