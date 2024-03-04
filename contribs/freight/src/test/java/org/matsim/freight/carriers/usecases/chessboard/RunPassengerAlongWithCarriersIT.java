/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C)  by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.freight.carriers.usecases.chessboard;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.testcases.MatsimTestUtils;

public class RunPassengerAlongWithCarriersIT {

    @RegisterExtension
	public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	void runChessboard() {
        try{
        	RunPassengerAlongWithCarriers abc = new RunPassengerAlongWithCarriers();
            // ---
            Config config = abc.prepareConfig();
            config.controller().setLastIteration( 1 );
            config.controller().setOutputDirectory( utils.getOutputDirectory() );
            // ---
            abc.run();
        } catch (Exception ee ) {
            ee.printStackTrace();
            Assertions.fail("something went wrong: " + ee.getMessage());
        }
    }

}
