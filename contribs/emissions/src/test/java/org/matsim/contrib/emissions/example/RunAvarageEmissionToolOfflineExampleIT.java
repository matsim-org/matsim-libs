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
public class RunAvarageEmissionToolOfflineExampleIT {
	@Rule public MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	public final void testAverage_vehTypeV1() {
		RunAverageEmissionToolOfflineExample offlineExample = new RunAverageEmissionToolOfflineExample();
		Config config = offlineExample.prepareConfig("./scenarios/sampleScenario/testv2_Vehv1/config_average.xml");
		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );;
		emissionsConfig.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.fromVehicleTypeDescription );
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		offlineExample.run();
	}

	@Test
	public final void testAverage_vehTypeV2() {
		RunAverageEmissionToolOfflineExample offlineExample = new RunAverageEmissionToolOfflineExample();
		Config config = offlineExample.prepareConfig("./scenarios/sampleScenario/testv2_Vehv2/config_average.xml");
		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );;
		emissionsConfig.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.asEngineInformationAttributes );
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		offlineExample.run();
	}

	@Test
	public final void testAverage_vehTypeV2b() {
		RunAverageEmissionToolOfflineExample offlineExample = new RunAverageEmissionToolOfflineExample();
		Config config = offlineExample.prepareConfig("./scenarios/sampleScenario/testv2_Vehv2/config_average.xml");
		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );;
		emissionsConfig.setHbefaVehicleDescriptionSource( EmissionsConfigGroup.HbefaVehicleDescriptionSource.fromVehicleTypeDescription );
		// (should also be the default)
		config.controler().setOutputDirectory(utils.getOutputDirectory());
		offlineExample.run();
	}

}
