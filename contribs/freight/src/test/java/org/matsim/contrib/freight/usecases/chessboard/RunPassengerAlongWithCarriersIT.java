package org.matsim.contrib.freight.usecases.chessboard;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.testcases.MatsimTestUtils;

public class RunPassengerAlongWithCarriersIT {
   
    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    public void runChessboard() {
        try{
        	RunPassengerAlongWithCarriers abc = new RunPassengerAlongWithCarriers();
            // ---
            Config config = abc.prepareConfig();
            config.controler().setLastIteration( 1 );
            config.controler().setOutputDirectory( utils.getOutputDirectory() );
            // ---
            abc.run();
        } catch (Exception ee ) {
            ee.printStackTrace();
            Assert.fail("something went wrong");
        }
    }

}
