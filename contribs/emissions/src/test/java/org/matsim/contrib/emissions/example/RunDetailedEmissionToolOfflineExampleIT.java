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
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ihab
 *
 */
public class RunDetailedEmissionToolOfflineExampleIT {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	/*
	 *
	 * Abort if values are not found in detailled table
	 *
	 * */

	@Test
	public final void testDetailed_vehTypeV1() {
		RunDetailedEmissionToolOfflineExample offlineExample = new RunDetailedEmissionToolOfflineExample();
		Config config = offlineExample.prepareConfig("./scenarios/sampleScenario/testv2_Vehv1/config_detailed.xml");
		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
		emissionsConfig.setAverageColdEmissionFactorsFile(""); //setting empty to avoid effects from loaded config.
		emissionsConfig.setAverageWarmEmissionFactorsFile(""); //setting empty to avoid effects from loaded config.
		emissionsConfig.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.fromVehicleTypeDescription );
		emissionsConfig.setDetailedFallbackBehaviour( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort );
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		offlineExample.run();
	}

	@Test
	public final void testDetailed_vehTypeV2() {
		RunDetailedEmissionToolOfflineExample offlineExample = new RunDetailedEmissionToolOfflineExample();
		Config config = offlineExample.prepareConfig("./scenarios/sampleScenario/testv2_Vehv2/config_detailed.xml");
		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
		emissionsConfig.setAverageColdEmissionFactorsFile(""); //setting empty to avoid effects from loaded config.
		emissionsConfig.setAverageWarmEmissionFactorsFile(""); //setting empty to avoid effects from loaded config.
		emissionsConfig.setDetailedFallbackBehaviour( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort );
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		offlineExample.run();
	}

	@Test
	public final void testDetailed_vehTypeV2_HBEFA4() {
		RunDetailedEmissionToolOfflineExample offlineExample = new RunDetailedEmissionToolOfflineExample();
		Config config = offlineExample.prepareConfig("./scenarios/sampleScenario/testv2_Vehv2/config_detailed.xml");
		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
		emissionsConfig.setAverageColdEmissionFactorsFile(""); //setting empty to avoid effects from loaded config.
		emissionsConfig.setAverageWarmEmissionFactorsFile(""); //setting empty to avoid effects from loaded config.
		emissionsConfig.setDetailedFallbackBehaviour( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.onlyTryDetailedElseAbort );
		// --- Change input to hbefa4 sample
		emissionsConfig.setDetailedColdEmissionFactorsFile("../sample_41_EFA_ColdStart_SubSegm_2020detailed.txt");
		emissionsConfig.setDetailedWarmEmissionFactorsFile("../sample_41_EFA_HOT_SubSegm_2020detailed.txt");

		config.controler().setOutputDirectory(utils.getOutputDirectory());
		offlineExample.run();
	}


	/*
	*
	* Fallback to Average
	* this was the previous behaviour.
	*
	* */

	@Test
	public final void testDetailed_vehTypeV1_FallbackToAverage() {
		RunDetailedEmissionToolOfflineExample offlineExample = new RunDetailedEmissionToolOfflineExample();
		Config config = offlineExample.prepareConfig("./scenarios/sampleScenario/testv2_Vehv1/config_detailed.xml");
		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
		emissionsConfig.setAverageColdEmissionFactorsFile("../sample_41_EFA_ColdStart_vehcat_2020average.txt");
		emissionsConfig.setAverageWarmEmissionFactorsFile( "../sample_41_EFA_HOT_vehcat_2020average.txt" );
		emissionsConfig.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.fromVehicleTypeDescription );
		emissionsConfig.setDetailedFallbackBehaviour( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable );
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		offlineExample.run();
	}

	@Test
	public final void testDetailed_vehTypeV2_FallbackToAverage() {
		RunDetailedEmissionToolOfflineExample offlineExample = new RunDetailedEmissionToolOfflineExample();
		Config config = offlineExample.prepareConfig("./scenarios/sampleScenario/testv2_Vehv2/config_detailed.xml");
		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
		emissionsConfig.setAverageColdEmissionFactorsFile("../sample_41_EFA_ColdStart_vehcat_2020average.txt");
		emissionsConfig.setAverageWarmEmissionFactorsFile( "../sample_41_EFA_HOT_vehcat_2020average.txt" );
		emissionsConfig.setDetailedFallbackBehaviour( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable );
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		offlineExample.run();
	}

	@Test
	public final void testDetailed_vehTypeV2_HBEFA4_FallbackToAverage() {
		RunDetailedEmissionToolOfflineExample offlineExample = new RunDetailedEmissionToolOfflineExample();
		Config config = offlineExample.prepareConfig("./scenarios/sampleScenario/testv2_Vehv2/config_detailed.xml");
		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
		emissionsConfig.setDetailedFallbackBehaviour( EmissionsConfigGroup.DetailedVsAverageLookupBehavior.tryDetailedThenTechnologyAverageThenAverageTable );
		emissionsConfig.setAverageColdEmissionFactorsFile("../sample_41_EFA_ColdStart_vehcat_2020average.txt");
		emissionsConfig.setDetailedColdEmissionFactorsFile("../sample_41_EFA_ColdStart_SubSegm_2020detailed.txt");
		emissionsConfig.setAverageWarmEmissionFactorsFile( "../sample_41_EFA_HOT_vehcat_2020average.txt" );
		emissionsConfig.setDetailedWarmEmissionFactorsFile("../sample_41_EFA_HOT_SubSegm_2020detailed.txt");
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		offlineExample.run();
	}


}
