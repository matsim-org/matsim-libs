package org.matsim.contrib.freight.usecases.chessboard;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.testcases.MatsimTestUtils;

public class RunChessboardIT {

    @Rule
    public MatsimTestUtils utils = new MatsimTestUtils() ;

    @Test
    public void runChessboard() {
        try{
            RunChessboard runChessboard = new RunChessboard();
            // ---
            Config config = runChessboard.prepareConfig();
            config.controler().setLastIteration( 1 );
            config.controler().setOutputDirectory( utils.getOutputDirectory() );
            // ---
            runChessboard.run();
        } catch (Exception ee ) {
            ee.printStackTrace();
            Assert.fail("something went wrong");
        }
    }

}
