/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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
package org.matsim.codeexamples.programming.multipleSubpopulations;

import static org.junit.Assert.*;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.codeexamples.RunAbcSimpleExample;
import org.matsim.codeexamples.programming.multipleSubpopulations.RunSubpopulationsExample;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

/**
 * @author nagel
 *
 */
public class SubpopulationsExampleTest {

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	/**
	 * Test method for {@link RunSubpopulationsExample#main(java.lang.String[])}.
	 */
	@SuppressWarnings("static-method")
	@Test
	public final void testMain() {
		try {
//			RunSubpopulationsExample.main( new String []{ IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil-extended" ), "config-with-subpopulation.xml" ).toString()
//					, "--config:controler.outputDirectory=" + utils.getOutputDirectory()
//					, "--config:controler.lastIteration=1"
//			} );
			RunSubpopulationsExample.main( new String[]{"scenarios/equil-extended/config-with-subpopulation.xml"
					, "--config:controler.outputDirectory=" + utils.getOutputDirectory()
					, "--config:controler.lastIteration=1"
			} );
//			{
//				String expected = utils.getInputDirectory() + "/output_events.xml.gz" ;
//				String actual = utils.getOutputDirectory() + "/output_events.xml.gz" ;
//				EventsFileComparator.Result result = EventsUtils.compareEventsFiles( expected, actual );
//				Assert.assertEquals( result, EventsFileComparator.Result.FILES_ARE_EQUAL );
//			}
			// (important that the subpopulations undergo different replanning, which is visible in the plans file.  The events file would
			// also check for smallish other changes, which is (IMO) not needed here.  kai, jul'23)

			{
				final Population expected = PopulationUtils.createPopulation( ConfigUtils.createConfig() );
				PopulationUtils.readPopulation( expected, utils.getInputDirectory() + "/output_plans.xml.gz" );
				final Population actual = PopulationUtils.createPopulation( ConfigUtils.createConfig() );
				PopulationUtils.readPopulation( actual, utils.getOutputDirectory() + "/output_plans.xml.gz" );
				boolean result = PopulationUtils.equalPopulation( expected, actual );
				Assert.assertTrue( "populations are different", result );

				for( Person expectedPerson : expected.getPersons().values() ){
					Person actualPerson = actual.getPersons().get( Id.createPersonId( expectedPerson.getId() ) );
					Assert.assertEquals( expectedPerson.getSelectedPlan().getScore(), actualPerson.getSelectedPlan().getScore() );
				}
			}


		} catch ( Exception ee ) {
			ee.printStackTrace();
			fail( "Got an exception while running subpopulation example: "+ee ) ;
		}
	}

}
