/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

/**
 *
 */
package org.matsim.contrib.noise;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.groups.RoutingConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 *
 */

public class NoiseConfigGroupIT {

	@RegisterExtension
	private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	final void test0(){

		String configFile = testUtils.getPackageInputDirectory() + "NoiseConfigGroupTest/config0.xml";
		Config config = ConfigUtils.loadConfig(configFile, new NoiseConfigGroup());
		config.controller().setOutputDirectory(testUtils.getOutputDirectory());

		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModule("noise");

		// test the config parameters
		Assertions.assertEquals(12345., noiseParameters.getReceiverPointGap(), MatsimTestUtils.EPSILON, "wrong config parameter");

		String actForRecPtGrid = noiseParameters.getConsideredActivitiesForReceiverPointGridArray()[0] + "," + noiseParameters.getConsideredActivitiesForReceiverPointGridArray()[1] + "," + noiseParameters.getConsideredActivitiesForReceiverPointGridArray()[2];
		Assertions.assertEquals("home,sleep,eat", actForRecPtGrid, "wrong config parameter");

		String actForSpatFct = noiseParameters.getConsideredActivitiesForDamageCalculationArray()[0] + "," + noiseParameters.getConsideredActivitiesForDamageCalculationArray()[1] + "," + noiseParameters.getConsideredActivitiesForDamageCalculationArray()[2];
		Assertions.assertEquals("work,leisure,other", actForSpatFct, "wrong config parameter");

		Assertions.assertEquals(12345789., noiseParameters.getRelevantRadius(), MatsimTestUtils.EPSILON, "wrong config parameter");
		Assertions.assertFalse(noiseParameters.isComputeNoiseDamages(), "wrong config parameter");

		String hgvIdPrefixes = noiseParameters.getHgvIdPrefixesArray()[0] + "," + noiseParameters.getHgvIdPrefixesArray()[1] + "," + noiseParameters.getHgvIdPrefixesArray()[2] + "," + noiseParameters.getHgvIdPrefixesArray()[3];
		Assertions.assertEquals("lkw,LKW,HGV,hgv", hgvIdPrefixes, "wrong config parameter");

		String tunnelLinkIds = noiseParameters.getTunnelLinkIDsSet().toArray()[0] + "," + noiseParameters.getTunnelLinkIDsSet().toArray()[1];
		Assertions.assertEquals("link1,link2", tunnelLinkIds, "wrong config parameter");
	}

	@Test
	final void test1(){

		String configFile = testUtils.getPackageInputDirectory() + "NoiseConfigGroupTest/config1.xml";
		Config config = ConfigUtils.loadConfig(configFile, new NoiseConfigGroup());
		config.controller().setOutputDirectory(testUtils.getOutputDirectory());
		config.routing().setNetworkRouteConsistencyCheck(RoutingConfigGroup.NetworkRouteConsistencyCheck.disable);

		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModule("noise");

		// see if the custom config group is written into the output config file
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		controler.addOverridingModule(new NoiseModule());
		controler.getConfig().controller().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();

		Config outputConfig = ConfigUtils.loadConfig(controler.getConfig().controller().getOutputDirectory() + "/output_config.xml", new NoiseConfigGroup());
		NoiseConfigGroup outputNoiseParameters = (NoiseConfigGroup) outputConfig.getModule("noise");

		Assertions.assertEquals(noiseParameters.toString(), outputNoiseParameters.toString(), "input and output config parameters are not the same");
	}

}
