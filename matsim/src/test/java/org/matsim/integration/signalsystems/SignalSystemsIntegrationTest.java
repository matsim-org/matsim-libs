/* *********************************************************************** *
 * project: org.matsim.*
 * SignalSystemIntegrationTest
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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
package org.matsim.integration.signalsystems;

import java.io.File;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.Controler;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.signalsystems.data.SignalsScenarioWriter;
import org.matsim.testcases.MatsimTestUtils;


/**
 * @author dgrether
 *
 */
public class SignalSystemsIntegrationTest {
  
  private static final Logger log = Logger.getLogger(SignalSystemsIntegrationTest.class);
  
	private final static String CONFIG_FILE_NAME = "signalSystemsIntegrationConfig.xml";

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public void testSignalSystems() {
		Config config = testUtils.loadConfig(testUtils.getClassInputDirectory() + CONFIG_FILE_NAME);
		config.controler().setOutputDirectory(testUtils.getOutputDirectory());
		config.setQSimConfigGroup(new QSimConfigGroup());
		Controler c = new Controler(config);
		c.setCreateGraphs(false);
		c.run();
		
			//iteration 0 
		String iterationOutput = testUtils.getOutputDirectory() + "ITERS/it.0/";
		String inputDirectory = testUtils.getInputDirectory();
		
		Assert.assertEquals("different events files after iteration 0 ", 
				CRCChecksum.getCRCFromFile(inputDirectory + "0.events.xml.gz"), 
				CRCChecksum.getCRCFromFile(iterationOutput + "0.events.xml.gz"));

		Assert.assertEquals("different population files after iteration 0 ", 
				CRCChecksum.getCRCFromFile(testUtils.getInputDirectory() + "0.plans.xml.gz"), 
				CRCChecksum.getCRCFromFile(iterationOutput + "0.plans.xml.gz"));

		//iteration 10 
		iterationOutput = testUtils.getOutputDirectory() + "ITERS/it.10/";
		
		Assert.assertEquals("different events files after iteration 10 ", 
				CRCChecksum.getCRCFromFile(inputDirectory + "10.events.xml.gz"), 
				CRCChecksum.getCRCFromFile(iterationOutput + "10.events.xml.gz"));

		Assert.assertEquals("different population files after iteration 10 ", 
				CRCChecksum.getCRCFromFile(testUtils.getInputDirectory() + "10.plans.xml.gz"), 
				CRCChecksum.getCRCFromFile(iterationOutput + "10.plans.xml.gz"));
		
		SignalsScenarioWriter writer = new SignalsScenarioWriter(c.getControlerIO().getOutputPath());
		File file = new File(writer.getSignalSystemsOutputFilename());
		Assert.assertTrue(file.exists());
		file = new File(writer.getSignalGroupsOutputFilename());
		Assert.assertTrue(file.exists());
		file = new File(writer.getSignalControlOutputFilename());
		Assert.assertTrue(file.exists());
		file = new File(writer.getAmberTimesOutputFilename());
		Assert.assertTrue(file.exists());
		
	}
	
	
}
