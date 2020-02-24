package org.matsim.contrib.parking.parkingproxy.run;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.population.PopulationUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.Assert.fail;

public class RunWithParkingProxyTest{
        private static final Logger log = Logger.getLogger( RunWithParkingProxyTest.class ) ;
        @Rule public MatsimTestUtils utils = new MatsimTestUtils();

        @Test
        public void testMain(){

                try{
                        RunWithParkingProxy.main( new String []{ IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" ).toString()
                                        , "--config:controler.outputDirectory=" + utils.getOutputDirectory()
                                        , "--config:controler.lastIteration=2"
//                                                                               ,"--config:parkingProxy....=..."
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
