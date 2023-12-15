/* *********************************************************************** *
 * project: org.matsim.*												   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
 *                   LICENSE and WARRANTY file.                            *
 * email           : info at matsim dot org                                *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 *   This program is free software; you can redistribute it and/or modify  *
 *   it under the terms of the GNU General Public License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */
package org.matsim.contrib.emissions.example;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author nagel
 *
 */
public class RunDetailedEmissionToolOnlineExampleIT_vehTypeV1 {
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils() ;

	/**
	 * Test method for {@link RunDetailedEmissionToolOnlineExample#main(String[])}.
	 */

	/*
	 *
	 * Abort if values are not found in detailed table
	 * This is by now (feb'20) the default. Setting it here for the tests explicitly
	 *
	 * */
//	@Test(expected=RuntimeException.class) // Expecting RuntimeException, because requested values are only in average file. Without fallback it has to fail!
	@Disabled //Ignore this test, because the thrown exception during events handling does not always leads to an abort of the Simulation ->> Maybe a problem in @link{ParallelEventsManagerImpl.class}?
	@Test
	final void testDetailed_vehTypeV1() {
		boolean gotAnException = false ;
		try {
			RunDetailedEmissionToolOnlineExample onlineExample = new RunDetailedEmissionToolOnlineExample();
			Config config = onlineExample.prepareConfig( new String[]{"./scenarios/sampleScenario/testv2_Vehv1/config_detailed.xml"} ) ;
			config.controller().setOutputDirectory( utils.getOutputDirectory() );
			config.controller().setLastIteration( 1 );
			EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
			emissionsConfig.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.fromVehicleTypeDescription );
			emissionsConfig.setDetailedVsAverageLookupBehavior( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort );
			Scenario scenario = onlineExample.prepareScenario( config ) ;
			onlineExample.run( scenario ) ;
		} catch (Exception ee ) {
			gotAnException = true ;
		}
		Assertions.assertTrue( gotAnException );
	}

}
