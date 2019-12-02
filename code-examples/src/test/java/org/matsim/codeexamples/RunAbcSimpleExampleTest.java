package org.matsim.codeexamples;

import com.jogamp.common.util.SyncedRingbuffer;
import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.Assert.*;

public class RunAbcSimpleExampleTest{
	private static final Logger log = Logger.getLogger(RunAbcSimpleExampleTest.class) ;

	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public void testMain(){
		try{
			RunAbcSimpleExample.main( new String []{ IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" ).toString()
				  , "--config:controler.outputDirectory=" + utils.getOutputDirectory()
				  , "--config:controler.lastIteration=2"
			} );
			{
				String expected = utils.getInputDirectory() + "/output_events.xml.gz" ;
				String actual = utils.getOutputDirectory() + "/output_events.xml.gz" ;
				EventsUtils.compareEventsFiles( expected, actual );
			}
			{
				final Population expected = PopulationUtils.createPopulation( ConfigUtils.createConfig() );
				PopulationUtils.readPopulation( expected, utils.getInputDirectory() + "/output_plans.xml.gz" );
				final Population actual = PopulationUtils.createPopulation( ConfigUtils.createConfig() );
				PopulationUtils.readPopulation( actual, utils.getOutputDirectory() + "/output_plans.xml.gz" );
				PopulationUtils.comparePopulations( expected, actual ) ;
			}


		} catch ( Exception ee ) {
			log.fatal(ee) ;
			fail() ;
		}
	}

}
