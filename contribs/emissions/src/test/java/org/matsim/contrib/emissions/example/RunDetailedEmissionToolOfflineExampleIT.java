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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.DetailedVsAverageLookupBehavior;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.HbefaVehicleDescriptionSource;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ihab
 *
 */
public class RunDetailedEmissionToolOfflineExampleIT {
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils() ;

	/*
	 *
	 * Abort if values are not found in detailed table
	 *
	 * */

	// Expecting RuntimeException, because requested values are only in average file. Without fallback, it has to fail!
//	@Test(expected=RuntimeException.class)
	@Test
	final void testDetailed_vehTypeV1() {
		boolean gotAnException = false ;
		try {
			RunDetailedEmissionToolOfflineExample offlineExample = new RunDetailedEmissionToolOfflineExample();

//			Config config = offlineExample.prepareConfig( "./scenarios/sampleScenario/testv2_Vehv1/config_detailed.xml" );
			var scUrl = ExamplesUtils.getTestScenarioURL( "emissions-sampleScenario/testv2_Vehv1" );
			var cfUrl = IOUtils.extendUrl( scUrl, "config_detailed.xml" );
			var config = offlineExample.prepareConfig( new String [] {cfUrl.toString()} );

			EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
			emissionsConfig.setHbefaVehicleDescriptionSource( HbefaVehicleDescriptionSource.fromVehicleTypeDescription );
			emissionsConfig.setDetailedVsAverageLookupBehavior( DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort );
			config.controller().setOutputDirectory( utils.getOutputDirectory() );

			offlineExample.run();
		} catch (Exception ee ) {
			gotAnException = true ;
		}
		Assertions.assertTrue( gotAnException );
	}

	// Expecting RuntimeException, because requested values are only in average file. Without fallback, it has to fail!
//	@Test(expected=RuntimeException.class)
	@Test
	final void testDetailed_vehTypeV2() {
		boolean gotAnException = false ;
		try {
			RunDetailedEmissionToolOfflineExample offlineExample = new RunDetailedEmissionToolOfflineExample();

//			Config config = offlineExample.prepareConfig("./scenarios/sampleScenario/testv2_Vehv2/config_detailed.xml");
			var scUrl = ExamplesUtils.getTestScenarioURL( "emissions-sampleScenario/testv2_Vehv2" );
			var cfUrl = IOUtils.extendUrl( scUrl, "config_detailed.xml" );
			var config = offlineExample.prepareConfig( new String [] {cfUrl.toString()} );

			EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
			emissionsConfig.setDetailedVsAverageLookupBehavior( DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort );
			config.controller().setOutputDirectory(utils.getOutputDirectory());

			offlineExample.run();
		} catch (Exception ee ) {
			gotAnException = true ;
		}
		Assertions.assertTrue( gotAnException );
	}

	// Expecting RuntimeException, because requested values are only in average file. Without fallback, it has to fail!
//	@Test(expected=RuntimeException.class)
	@Test
	final void testDetailed_vehTypeV2_HBEFA4() {
		boolean gotAnException = false ;
		try {
			RunDetailedEmissionToolOfflineExample offlineExample = new RunDetailedEmissionToolOfflineExample();
//			Config config = offlineExample.prepareConfig("./scenarios/sampleScenario/testv2_Vehv2/config_detailed.xml");
			var scUrl = ExamplesUtils.getTestScenarioURL( "emissions-sampleScenario/testv2_Vehv2" );
			var cfUrl = IOUtils.extendUrl( scUrl, "config_detailed.xml" );
			var config = offlineExample.prepareConfig( new String [] {cfUrl.toString()} );

			EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
			emissionsConfig.setDetailedVsAverageLookupBehavior( DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort );
			// --- Change input to hbefa4 sample
			emissionsConfig.setDetailedColdEmissionFactorsFile("../sample_41_EFA_ColdStart_SubSegm_2020detailed.csv");
			emissionsConfig.setDetailedWarmEmissionFactorsFile("../sample_41_EFA_HOT_SubSegm_2020detailed.csv");

			config.controller().setOutputDirectory(utils.getOutputDirectory());
			offlineExample.run();
		} catch (Exception ee ) {
			gotAnException = true ;
		}
		Assertions.assertTrue( gotAnException );
	}


	/*
	 *
	 * Fallback to Average
	 * this was the previous behaviour.
	 *
	 * */

	@Test
	final void testDetailed_vehTypeV1_FallbackToAverage() {
		RunDetailedEmissionToolOfflineExample offlineExample = new RunDetailedEmissionToolOfflineExample();

//		Config config = offlineExample.prepareConfig("./scenarios/sampleScenario/testv2_Vehv1/config_detailed.xml");
		var scUrl = ExamplesUtils.getTestScenarioURL( "emissions-sampleScenario/testv2_Vehv1" );
		var cfUrl = IOUtils.extendUrl( scUrl, "config_detailed.xml" );
		var config = offlineExample.prepareConfig( new String [] {cfUrl.toString()} );

		config.controller().setOutputDirectory(utils.getOutputDirectory());

		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
		emissionsConfig.setAverageColdEmissionFactorsFile("../sample_41_EFA_ColdStart_vehcat_2020average.csv");
		emissionsConfig.setAverageWarmEmissionFactorsFile( "../sample_41_EFA_HOT_vehcat_2020average.csv" );
		emissionsConfig.setHbefaVehicleDescriptionSource( HbefaVehicleDescriptionSource.fromVehicleTypeDescription );
		emissionsConfig.setDetailedVsAverageLookupBehavior( DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable );
		emissionsConfig.setHbefaTableConsistencyCheckingLevel( EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.consistent ); // because we only use a sample hbefa file "allCombinations" is irrelevant.

		offlineExample.run();
	}

	@Test
	final void testDetailed_vehTypeV2_FallbackToAverage() {
		RunDetailedEmissionToolOfflineExample offlineExample = new RunDetailedEmissionToolOfflineExample();

//		Config config = offlineExample.prepareConfig("./scenarios/sampleScenario/testv2_Vehv2/config_detailed.xml");
		var scUrl = ExamplesUtils.getTestScenarioURL( "emissions-sampleScenario/testv2_Vehv2" );
		var cfUrl = IOUtils.extendUrl( scUrl, "config_detailed.xml" );
		var config = offlineExample.prepareConfig( new String [] {cfUrl.toString()} );

		config.controller().setOutputDirectory(utils.getOutputDirectory());

		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
		emissionsConfig.setAverageColdEmissionFactorsFile("../sample_41_EFA_ColdStart_vehcat_2020average.csv");
		emissionsConfig.setAverageWarmEmissionFactorsFile( "../sample_41_EFA_HOT_vehcat_2020average.csv" );
		emissionsConfig.setDetailedVsAverageLookupBehavior( DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable );
		emissionsConfig.setHbefaTableConsistencyCheckingLevel( EmissionsConfigGroup.HbefaTableConsistencyCheckingLevel.consistent ); // because we only use a sample hbefa file "allCombinations" is irrelevant.

		offlineExample.run();
	}

	@Test
	final void testDetailed_vehTypeV2_HBEFA4_FallbackToAverage() {
		RunDetailedEmissionToolOfflineExample offlineExample = new RunDetailedEmissionToolOfflineExample();

//		Config config = offlineExample.prepareConfig("./scenarios/sampleScenario/testv2_Vehv2/config_detailed.xml");
		var scUrl = ExamplesUtils.getTestScenarioURL( "emissions-sampleScenario/testv2_Vehv2" );
		var cfUrl = IOUtils.extendUrl( scUrl, "config_detailed.xml" );
		var config = offlineExample.prepareConfig( new String [] {cfUrl.toString()} );

		config.controller().setOutputDirectory(utils.getOutputDirectory());

		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
		emissionsConfig.setDetailedVsAverageLookupBehavior( DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable );
		emissionsConfig.setAverageColdEmissionFactorsFile("../sample_41_EFA_ColdStart_vehcat_2020average.csv");
		emissionsConfig.setDetailedColdEmissionFactorsFile("../sample_41_EFA_ColdStart_SubSegm_2020detailed.csv");
		emissionsConfig.setAverageWarmEmissionFactorsFile( "../sample_41_EFA_HOT_vehcat_2020average.csv" );
		emissionsConfig.setDetailedWarmEmissionFactorsFile("../sample_41_EFA_HOT_SubSegm_2020detailed.csv");

		offlineExample.run();
	}


}
