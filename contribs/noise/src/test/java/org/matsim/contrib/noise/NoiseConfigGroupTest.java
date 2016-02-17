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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.controler.Controler;
import org.matsim.core.controler.OutputDirectoryHierarchy;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author ikaddoura
 *
 */

public class NoiseConfigGroupTest {
	
	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public final void test0(){
		
		String configFile = testUtils.getPackageInputDirectory() + "NoiseConfigGroupTest/config0.xml";
		Config config = ConfigUtils.loadConfig(configFile, new NoiseConfigGroup());
				
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModule("noise");

		// test the config parameters
		Assert.assertEquals("wrong config parameter", 12345., noiseParameters.getReceiverPointGap(), MatsimTestUtils.EPSILON);
		
		String actForRecPtGrid = noiseParameters.getConsideredActivitiesForReceiverPointGridArray()[0] + "," + noiseParameters.getConsideredActivitiesForReceiverPointGridArray()[1] + "," + noiseParameters.getConsideredActivitiesForReceiverPointGridArray()[2];
		Assert.assertEquals("wrong config parameter", "home,sleep,eat", actForRecPtGrid);		
		
		String actForSpatFct = noiseParameters.getConsideredActivitiesForDamageCalculationArray()[0] + "," + noiseParameters.getConsideredActivitiesForDamageCalculationArray()[1] + "," + noiseParameters.getConsideredActivitiesForDamageCalculationArray()[2];
		Assert.assertEquals("wrong config parameter", "work,leisure,other", actForSpatFct);	
		
		Assert.assertEquals("wrong config parameter", 12345789., noiseParameters.getRelevantRadius(), MatsimTestUtils.EPSILON);
		Assert.assertFalse("wrong config parameter", noiseParameters.isComputeNoiseDamages());

		String hgvIdPrefixes = noiseParameters.getHgvIdPrefixesArray()[0] + "," + noiseParameters.getHgvIdPrefixesArray()[1] + "," + noiseParameters.getHgvIdPrefixesArray()[2] + "," + noiseParameters.getHgvIdPrefixesArray()[3];
		Assert.assertEquals("wrong config parameter", "lkw,LKW,HGV,hgv", hgvIdPrefixes);		
		
		String tunnelLinkIds = noiseParameters.getTunnelLinkIDsSet().toArray()[0] + "," + noiseParameters.getTunnelLinkIDsSet().toArray()[1];
		Assert.assertEquals("wrong config parameter", "link1,link2", tunnelLinkIds);
	}
	
	@Test
	public final void test1(){
		
		String configFile = testUtils.getPackageInputDirectory() + "NoiseConfigGroupTest/config1.xml";
		Config config = ConfigUtils.loadConfig(configFile, new NoiseConfigGroup());
				
		NoiseConfigGroup noiseParameters = (NoiseConfigGroup) config.getModule("noise");
		
		// see if the custom config group is written into the output config file
		Scenario scenario = ScenarioUtils.loadScenario(config);

		Controler controler = new Controler(scenario);
		controler.addControlerListener(new NoiseCalculationOnline(controler));
		controler.getConfig().controler().setOverwriteFileSetting(OutputDirectoryHierarchy.OverwriteFileSetting.deleteDirectoryIfExists);
		controler.run();
		
		Config outputConfig = ConfigUtils.loadConfig(controler.getConfig().controler().getOutputDirectory() + "/output_config.xml.gz", new NoiseConfigGroup());
		NoiseConfigGroup outputNoiseParameters = (NoiseConfigGroup) outputConfig.getModule("noise");
		
		Assert.assertEquals("input and output config parameters are not the same", noiseParameters.toString(), outputNoiseParameters.toString());
	}

}
