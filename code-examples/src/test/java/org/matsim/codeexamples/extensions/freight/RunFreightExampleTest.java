package org.matsim.codeexamples.extensions.freight;

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
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

public class RunFreightExampleTest{
	private static final Logger log = LogManager.getLogger( RunFreightExampleTest.class );
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();

	@Test
	public void testMain(){
		try{
			String[] args = { IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "freight-chessboard-9x9" ), "config.xml" ).toString() ,
					"--config:controler.outputDirectory", utils.getOutputDirectory(),
					"--config:controler.lastIteration", "0",
					"--config:plans.inputPlansFile", "null",
					"--config:freight.carriersFile", "singleCarrierFiveActivitiesWithoutRoutes.xml",
					"--config:freight.carriersVehicleTypeFile", "vehicleTypes.xml"
			};
			RunFreightExample.run(args, false);
			{
				Population expected = PopulationUtils.createPopulation( ConfigUtils.createConfig() );
				PopulationUtils.readPopulation( expected, utils.getInputDirectory() + "/output_plans.xml.gz" );

				Population actual = PopulationUtils.createPopulation( ConfigUtils.createConfig() );
				PopulationUtils.readPopulation( actual, utils.getOutputDirectory() + "/output_plans.xml.gz" );

				boolean result = PopulationUtils.comparePopulations( expected, actual );
				Assert.assertTrue( result );
			}
			{
				String expected = utils.getInputDirectory() + "/output_events.xml.gz";
				String actual = utils.getOutputDirectory() + "/output_events.xml.gz";
				EventsFileComparator.Result result = EventsUtils.compareEventsFiles( expected, actual );
				Assert.assertEquals( EventsFileComparator.Result.FILES_ARE_EQUAL, result );
			}

		} catch( Exception ee ){
			log.fatal( "there was an exception: \n" + ee );
			ee.printStackTrace();

			// if one catches an exception, then one needs to explicitly fail the test:
			Assert.fail();
		}
	}
}
