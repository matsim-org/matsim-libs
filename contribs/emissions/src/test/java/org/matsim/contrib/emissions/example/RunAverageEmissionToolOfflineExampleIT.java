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
import org.matsim.contrib.emissions.EmissionUtils;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup;
import org.matsim.contrib.emissions.utils.EmissionsConfigGroup.HbefaVehicleDescriptionSource;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.events.EventsUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;
import org.matsim.utils.eventsfilecomparison.EventsFileComparator.Result;

import java.net.URL;

/**
 * @author ihab
 *
 */
public class RunAverageEmissionToolOfflineExampleIT{
	@RegisterExtension private MatsimTestUtils utils = new MatsimTestUtils() ;

	@Test
	final void testAverage_vehTypeV1() {
		RunAverageEmissionToolOfflineExample offlineExample = new RunAverageEmissionToolOfflineExample();

//		Config config = offlineExample.prepareConfig("./scenarios/sampleScenario/testv2_Vehv1/config_average.xml");
		URL scenarioUrl = ExamplesUtils.getTestScenarioURL( "emissions-sampleScenario/testv2_Vehv1" );
		URL configUrl = IOUtils.extendUrl( scenarioUrl, "config_average.xml" );
		Config config = offlineExample.prepareConfig( new String [] {configUrl.toString()} );
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
		emissionsConfig.setHbefaVehicleDescriptionSource( HbefaVehicleDescriptionSource.fromVehicleTypeDescription );

		offlineExample.run();

		String expected = utils.getInputDirectory() + RunAverageEmissionToolOfflineExample.emissionEventsFilename;
		String actual = utils.getOutputDirectory() + RunAverageEmissionToolOfflineExample.emissionEventsFilename;
		Result result = EventsUtils.compareEventsFiles( expected, actual );
		Assertions.assertEquals( Result.FILES_ARE_EQUAL, result);
	}

	@Test
	final void testAverage_vehTypeV2() {
		RunAverageEmissionToolOfflineExample offlineExample = new RunAverageEmissionToolOfflineExample();

//		Config config = offlineExample.prepareConfig("./scenarios/sampleScenario/testv2_Vehv2/config_average.xml");
		URL scenarioUrl = ExamplesUtils.getTestScenarioURL( "emissions-sampleScenario/testv2_Vehv2" );
		URL configUrl = IOUtils.extendUrl( scenarioUrl, "config_average.xml" );
		Config config = offlineExample.prepareConfig( new String [] {configUrl.toString()} );
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
		emissionsConfig.setHbefaVehicleDescriptionSource( HbefaVehicleDescriptionSource.asEngineInformationAttributes );
		// (this is currently the default.  kai, apr'21)

		offlineExample.run();

		String expected = utils.getInputDirectory() + RunAverageEmissionToolOfflineExample.emissionEventsFilename;
		String actual = utils.getOutputDirectory() + RunAverageEmissionToolOfflineExample.emissionEventsFilename;
		Result result = EventsUtils.compareEventsFiles( expected, actual );
		Assertions.assertEquals( Result.FILES_ARE_EQUAL, result);
	}

	/**
	 * It is a bit odd that this test works: fromVehicleTypeDescription points to vehicles v1, but it actually reads v2.  Has to do with the fact
	 * that the normal vehicles reader, where this could be checked, has no way to access the emissions config group.  And {@link EmissionUtils},
	 * where this is used, has no way to know which file format was originally read.  See some discussion there.  :-(
	 */
	@Test
	final void testAverage_vehTypeV2b() {
		RunAverageEmissionToolOfflineExample offlineExample = new RunAverageEmissionToolOfflineExample();

//		Config config = offlineExample.prepareConfig("./scenarios/sampleScenario/testv2_Vehv2/config_average.xml");
		URL scenarioUrl = ExamplesUtils.getTestScenarioURL( "emissions-sampleScenario/testv2_Vehv2" );
		URL configUrl = IOUtils.extendUrl( scenarioUrl, "config_average.xml" );
		Config config = offlineExample.prepareConfig( new String [] {configUrl.toString()} );
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
		emissionsConfig.setHbefaVehicleDescriptionSource( HbefaVehicleDescriptionSource.fromVehicleTypeDescription );

		offlineExample.run();

		String expected = utils.getInputDirectory() + RunAverageEmissionToolOfflineExample.emissionEventsFilename;
		String actual = utils.getOutputDirectory() + RunAverageEmissionToolOfflineExample.emissionEventsFilename;
		Result result = EventsUtils.compareEventsFiles( expected, actual );
		Assertions.assertEquals( Result.FILES_ARE_EQUAL, result);

	}

	@Test
	final void testAverage_vehTypeV2_HBEFA4() {
		RunAverageEmissionToolOfflineExample offlineExample = new RunAverageEmissionToolOfflineExample();

//		Config config = offlineExample.prepareConfig("./scenarios/sampleScenario/testv2_Vehv2/config_average.xml");
		URL scenarioUrl = ExamplesUtils.getTestScenarioURL( "emissions-sampleScenario/testv2_Vehv2" );
		URL configUrl = IOUtils.extendUrl( scenarioUrl, "config_average.xml" );
		Config config = offlineExample.prepareConfig( new String [] {configUrl.toString()} );
		config.controller().setOutputDirectory(utils.getOutputDirectory());

		EmissionsConfigGroup emissionsConfig = ConfigUtils.addOrGetModule( config, EmissionsConfigGroup.class );
		emissionsConfig.setAverageColdEmissionFactorsFile("../sample_41_EFA_ColdStart_vehcat_2020average.csv");
		emissionsConfig.setAverageWarmEmissionFactorsFile("../sample_41_EFA_HOT_vehcat_2020average.csv");
		emissionsConfig.setHbefaVehicleDescriptionSource( HbefaVehicleDescriptionSource.asEngineInformationAttributes );

		offlineExample.run();

		String expected = utils.getInputDirectory() + RunAverageEmissionToolOfflineExample.emissionEventsFilename;
		String actual = utils.getOutputDirectory() + RunAverageEmissionToolOfflineExample.emissionEventsFilename;
		Result result = EventsUtils.compareEventsFiles( expected, actual );
		Assertions.assertEquals( Result.FILES_ARE_EQUAL, result);
	}
}
