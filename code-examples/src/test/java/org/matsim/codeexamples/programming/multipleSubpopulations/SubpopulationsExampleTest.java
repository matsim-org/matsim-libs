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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.jupiter.api.Assertions.fail;

/**
 * @author nagel
 *
 */
public class SubpopulationsExampleTest {

	@RegisterExtension public MatsimTestUtils utils = new MatsimTestUtils() ;

	/**
	 * Test method for {@link RunSubpopulationsExample#main(java.lang.String[])}.
	 */
	@SuppressWarnings("static-method")
	@Test
	final void testMain() {
		try {
//			RunSubpopulationsExample.main( new String []{ IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil-extended" ), "config-with-subpopulation.xml" ).toString()
//					, "--config:controler.outputDirectory=" + utils.getOutputDirectory()
//					, "--config:controler.lastIteration=1"
//			} );
			RunSubpopulationsExample.main( new String[]{"scenarios/equil-extended/config-with-subpopulation.xml"
					, "--config:controller.outputDirectory=" + utils.getOutputDirectory()
					, "--config:controller.lastIteration=1"
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
				PopulationUtils.readPopulation( expected, utils.getInputDirectory() + "/output_plans.xml.zst" );
				final Population actual = PopulationUtils.createPopulation( ConfigUtils.createConfig() );
				PopulationUtils.readPopulation( actual, utils.getOutputDirectory() + "/output_plans.xml.zst" );
				PopulationUtils.comparePopulations(expected, actual);

				for( Person expectedPerson : expected.getPersons().values() ){
					Person actualPerson = actual.getPersons().get( Id.createPersonId( expectedPerson.getId() ) );
					Assertions.assertEquals(expectedPerson.getSelectedPlan().getScore(), actualPerson.getSelectedPlan().getScore(), MatsimTestUtils.EPSILON);
				}
			}


		} catch ( Exception ee ) {
			ee.printStackTrace();
			fail( "Got an exception while running subpopulation example: "+ee ) ;
		}
	}

}
