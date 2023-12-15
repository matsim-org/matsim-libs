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
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;

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

				boolean result = PopulationUtils.comparePopulations( expected, actual );
				Assertions.assertTrue( result );
			}
			{
				String expected = utils.getInputDirectory() + "/output_events.xml.gz" ;
				String actual = utils.getOutputDirectory() + "/output_events.xml.gz" ;
				EventsFileComparator.Result result = EventsUtils.compareEventsFiles( expected, actual );
				Assertions.assertEquals( EventsFileComparator.Result.FILES_ARE_EQUAL, result );
			}

		} catch ( Exception ee ) {
			log.fatal("there was an exception: \n" + ee ) ;

			// if one catches an exception, then one needs to explicitly fail the test:
			Assertions.fail();
		}

	}

}
