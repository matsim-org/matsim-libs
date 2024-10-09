package org.matsim.contrib.ev.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.population.routes.PopulationComparison;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.ComparisonResult;

public class RunEvExampleWithLTHConsumptionModelTest{

	private static final Logger log = LogManager.getLogger(RunEvExample.class );

	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	void runTest(){
		try {
			String [] args = { RunEvExampleWithLTHConsumptionModel.DEFAULT_CONFIG_FILE
					,"--config:controler.outputDirectory", utils.getOutputDirectory()
			};

			new RunEvExampleWithLTHConsumptionModel().run( args );
			{
				Population expected = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
				PopulationUtils.readPopulation( expected, utils.getInputDirectory() + "/output_plans.xml.gz" );

				Population actual = PopulationUtils.createPopulation( ConfigUtils.createConfig() ) ;
				PopulationUtils.readPopulation( actual, utils.getOutputDirectory() + "/output_plans.xml.gz" );

				PopulationComparison.Result result = PopulationComparison.compare(expected, actual);
				Assertions.assertEquals(PopulationComparison.Result.equal, result);
			}
			{
				String expected = utils.getInputDirectory() + "/output_events.xml.gz" ;
				String actual = utils.getOutputDirectory() + "/output_events.xml.gz" ;
				ComparisonResult result = EventsUtils.compareEventsFiles( expected, actual );
				Assertions.assertEquals( ComparisonResult.FILES_ARE_EQUAL, result );
			}

		} catch ( Exception ee ) {
			log.fatal("there was an exception: \n" + ee ) ;

			// if one catches an exception, then one needs to explicitly fail the test:
			Assertions.fail();
		}

	}

}
