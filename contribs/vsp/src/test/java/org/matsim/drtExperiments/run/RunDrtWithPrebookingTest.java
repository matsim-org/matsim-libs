package org.matsim.drtExperiments.run;

import com.ibm.icu.impl.Assert;
import org.apache.logging.log4j.LogManager;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Id;
import org.matsim.api.core.v01.population.Person;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class RunDrtWithPrebookingTest{
	@RegisterExtension public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testMain(){
		try {
			String [] args = {"--output", utils.getOutputDirectory()
					, "--config", "scenarios/mielec/mielec_drt_config.xml"
			} ;
			new RunDrtWithPrebooking().execute( args );

			{
				Population expected = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
				PopulationUtils.readPopulation( expected, utils.getInputDirectory() + "/output_plans.xml.gz" );

				Population actual = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
				PopulationUtils.readPopulation( actual, utils.getOutputDirectory() + "/output_plans.xml.gz" );

				for ( Id<Person> personId : expected.getPersons().keySet()) {
					double scoreReference = expected.getPersons().get(personId).getSelectedPlan().getScore();
					double scoreCurrent = actual.getPersons().get(personId).getSelectedPlan().getScore();
					assertEquals(scoreReference, scoreCurrent, 0.001, "Scores of person=" + personId + " are different");
				}


//				boolean result = PopulationUtils.comparePopulations( expected, actual );
//				Assert.assertTrue( result );
				// (There are small differences in the score.  Seems that there were some floating point changes in Java 17, and the
				// differ by JDK (e.g. oracle vs. ...).   So not testing this any more for the time being.  kai, jul'23
			}
			{
				String expected = utils.getInputDirectory() + "/output_events.xml.gz" ;
				String actual = utils.getOutputDirectory() + "/output_events.xml.gz" ;
				ComparisonResult result = EventsUtils.compareEventsFiles( expected, actual );
				assertEquals( ComparisonResult.FILES_ARE_EQUAL, result );
			}

		} catch ( Exception ee ) {
			LogManager.getLogger(this.getClass() ).fatal("there was an exception: \n" + ee ) ;

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}

	}
}
