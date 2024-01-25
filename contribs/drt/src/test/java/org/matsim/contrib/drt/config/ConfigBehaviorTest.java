package org.matsim.contrib.drt.config;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.drt.run.DrtConfigGroup;
import org.matsim.contrib.drt.run.MultiModeDrtConfigGroup;
import org.matsim.contrib.dvrp.run.DvrpConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

public class ConfigBehaviorTest{

        private static final Logger log = LogManager.getLogger(ConfigBehaviorTest.class );
        @RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils() ;


	@Test
	final void testMaterializeAfterReadParameterSets() {
                {
                        // generate a test config that sets two values away from their defaults, and write it to file:
                        Config config = ConfigUtils.createConfig();
                        MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule( config, MultiModeDrtConfigGroup.class );
                        {
                                ConfigGroup abc = multiModeDrtConfigGroup.createParameterSet( DrtConfigGroup.GROUP_NAME );
                                abc.addParam( "mode", "drt20" );
                                multiModeDrtConfigGroup.addParameterSet(abc);
                        }
                        {
                                ConfigGroup abc = multiModeDrtConfigGroup.createParameterSet( DrtConfigGroup.GROUP_NAME );
                                abc.addParam( "mode", "drt20000" );
                                multiModeDrtConfigGroup.addParameterSet(abc);
                        }
                        ConfigUtils.writeConfig( config, utils.getOutputDirectory() + "ad-hoc-config.xml" );
                }

                {
                        // load config file without materializing the drt config group
                        Config config = ConfigUtils.loadConfig( new String[] { utils.getOutputDirectory() + "ad-hoc-config.xml"} );

                        // materialize the config group
                        MultiModeDrtConfigGroup multiModeDrtConfigGroup = ConfigUtils.addOrGetModule( config, MultiModeDrtConfigGroup.class );

                        // this should have two config groups here, but does not:
                        Assertions.assertEquals( 2, multiModeDrtConfigGroup.getModalElements().size() );

                        // check if you are getting back the values from the config file:
                        for( DrtConfigGroup drtConfigGroup : multiModeDrtConfigGroup.getModalElements() ){
                                log.info( drtConfigGroup.getMode() );
                                if ( ! ( drtConfigGroup.getMode().equals( "drt20" ) || drtConfigGroup.getMode().equals( "drt20000" ) ) ) {
                                        Assertions.fail();
                                }
                        }


                }
        }

	@Test
	final void testMaterializeAfterReadStandardParams() {
                {
                        // generate a test config that sets two values away from their defaults, and write it to file:
                        Config config = ConfigUtils.createConfig();
                        DvrpConfigGroup dvrpConfigGroup = ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );
					dvrpConfigGroup.travelTimeEstimationAlpha = 1.23;
					dvrpConfigGroup.travelTimeEstimationBeta = 4.56;
					ConfigUtils.writeConfig( config, utils.getOutputDirectory() + "ad-hoc-config.xml" );
                }

                {
                        // load config file without materializing the drt config group
                        Config config = ConfigUtils.loadConfig( new String[] { utils.getOutputDirectory() + "ad-hoc-config.xml"} );

                        // materialize the config group
                        DvrpConfigGroup dvrpConfig = ConfigUtils.addOrGetModule( config, DvrpConfigGroup.class );

                        // check if you are getting back the values from the config file:
					Assertions.assertEquals( 1.23, dvrpConfig.travelTimeEstimationAlpha, Double.MIN_VALUE );
					Assertions.assertEquals( 4.56, dvrpConfig.travelTimeEstimationBeta, Double.MIN_VALUE );
                }
        }


}
