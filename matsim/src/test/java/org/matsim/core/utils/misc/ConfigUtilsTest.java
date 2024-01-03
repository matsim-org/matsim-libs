/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2010 by the members listed in the COPYING,        *
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

package org.matsim.core.utils.misc;

import java.io.IOException;
import java.net.URL;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.examples.ExamplesUtils;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author mrieser
 */
public class ConfigUtilsTest {
	private static final Logger log = LogManager.getLogger( ConfigUtilsTest.class ) ;

	@RegisterExtension
	private MatsimTestUtils util = new MatsimTestUtils();

	@Test
	void testLoadConfig_filenameOnly() throws IOException {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		Assertions.assertNotNull(config);
		Assertions.assertEquals("network.xml", config.network().getInputFile());
	}

	@Test
	void testLoadConfig_emptyConfig() throws IOException {
		Config config = new Config();
		Assertions.assertNull(config.network());
		ConfigUtils.loadConfig(config, IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		Assertions.assertNotNull(config.network());
		Assertions.assertEquals("network.xml", config.network().getInputFile());
	}

	@Test
	void testLoadConfig_preparedConfig() throws IOException {
		Config config = new Config();
		config.addCoreModules();
		Assertions.assertNotNull(config.network());
		Assertions.assertNull(config.network().getInputFile());
		ConfigUtils.loadConfig(config, IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		Assertions.assertEquals("network.xml", config.network().getInputFile());
	}

	@Test
	void testModifyPaths_missingSeparator() throws IOException {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		Assertions.assertEquals("network.xml", config.network().getInputFile());
		ConfigUtils.modifyFilePaths(config, "/home/username/matsim");
		Assertions.assertTrue(config.network().getInputFile().equals("/home/username/matsim/network.xml") || config.network().getInputFile().equals("/home/username/matsim\\network.xml"));
	}

	@Test
	void testModifyPaths_withSeparator() throws IOException {
		Config config = ConfigUtils.loadConfig(IOUtils.extendUrl(ExamplesUtils.getTestScenarioURL("equil"), "config.xml"));
		Assertions.assertEquals("network.xml", config.network().getInputFile());
		ConfigUtils.modifyFilePaths(config, "/home/username/matsim/");
		Assertions.assertTrue(config.network().getInputFile().equals("/home/username/matsim/network.xml") || config.network().getInputFile().equals("/home/username/matsim\\network.xml"));
	}

	@Test
	void loadConfigWithTypedArgs(){
		final URL url = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" );
		final String [] typedArgs = {"--config:controler.outputDirectory=abc"} ;
		Config config = ConfigUtils.loadConfig( url, typedArgs );
		Assertions.assertEquals("abc", config.controller().getOutputDirectory());
	}

	@Test
	void loadConfigWithTypedArgsWithTypo(){
		boolean hasFailed = false ;
		try{
			final URL url = IOUtils.extendUrl( ExamplesUtils.getTestScenarioURL( "equil" ), "config.xml" );
			final String[] typedArgs = {"--config:controler.outputDirector=abc"};
			Config config = ConfigUtils.loadConfig( url, typedArgs );
			//		Assert.assertEquals("abc", config.controler().getOutputDirectory());
		} catch (Exception ee ){
			hasFailed = true ;
			log.warn("the above exception was expected") ;
		}
		Assertions.assertTrue( hasFailed );
	}
}
