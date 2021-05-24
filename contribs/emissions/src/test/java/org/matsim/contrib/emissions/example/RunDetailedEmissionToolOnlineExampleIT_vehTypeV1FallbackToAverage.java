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

import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

import static org.junit.Assert.fail;

/**
 * @author nagel
 *
 */
public class RunDetailedEmissionToolOnlineExampleIT_vehTypeV1FallbackToAverage {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	/**
	 * Test method for {@link RunDetailedEmissionToolOnlineExample#main(java.lang.String[])}.
	 */

	/*
	 *
	 * Fallback to Average
	 * this was the previous behaviour.
	 *
	 * */
	@Test
	public final void testDetailed_vehTypeV1_FallbackToAverage() {
		try {
			//			Config config = onlineExample.prepareConfig( new String[]{"./scenarios/sampleScenario/testv2_Vehv1/config_detailed.xml"} ) ;
			var scenarioUrl = ExamplesUtils.getTestScenarioURL( "emissions-sampleScenario/testv2_Vehv1" );
			var configUrl = IOUtils.extendUrl( scenarioUrl, "config_detailed.xml" );
			Config config = RunDetailedEmissionToolOnlineExample.prepareConfig( new String [] { configUrl.toString() } );

			config.controler().setOutputDirectory( utils.getOutputDirectory() );
			config.controler().setLastIteration( 1 );
			EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
			emissionsConfig.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.fromVehicleTypeDescription );
			emissionsConfig.setDetailedVsAverageLookupBehavior(
					EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable ); //This is the previous behaviour -> Test only pass, if falling back to average table :(
			Scenario scenario = RunDetailedEmissionToolOnlineExample.prepareScenario( config ) ;
			RunDetailedEmissionToolOnlineExample.run( scenario ) ;
		} catch ( Exception ee ) {
			ee.printStackTrace();
			fail("something did not work" ) ;
		}
	}

}
