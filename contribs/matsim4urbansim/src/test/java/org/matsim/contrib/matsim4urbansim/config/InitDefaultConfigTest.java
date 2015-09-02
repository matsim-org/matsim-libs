/* *********************************************************************** *
 * project: org.matsim.													   *
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,     *
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
package org.matsim.contrib.matsim4urbansim.config;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.contrib.matrixbasedptrouter.utils.TempDirectoryUtil;
import org.matsim.contrib.matsim4urbansim.utils.OPUSDirectoryUtil;
import org.matsim.core.config.Config;

/**
 * @author thomas
 *
 */
public class InitDefaultConfigTest /*extends MatsimTestCase*/{
	
	private static final Logger log = Logger.getLogger(InitDefaultConfigTest.class);
	
	/**
	 * This test makes sure that the MATSim4UrbanSim config file will be correctly written, 
	 * correctly converted into standard MATSim format and that all values are recognized correctly
	 */
	@Test
	//@Ignore // found this as ignored as of 22/jan/14. kai
	public void testLoadDefaultMATSim4UrbanSimWithExternalConfigFile(){
		TempDirectoryUtil tempDirectoryUtil = new TempDirectoryUtil() ;
		
		// MATSim4UrbanSim configuration converter
		M4UConfigurationConverterV4 connector = null;
		
		try{
			String path = tempDirectoryUtil.createCustomTempDirectory("tmp");
			
			log.info("Creating a matsim4urbansim config file and writing it on hand disk");
			
			// this creates a default external configuration file, some parameters overlap with the MATSim4UrbanSim configuration
			CreateTestExternalMATSimConfig testExternalConfig = new CreateTestExternalMATSimConfig(CreateTestM4UConfig.COLD_START, path);
			String externalConfigLocation = testExternalConfig.generateMATSimConfig();
			
			// this creates a default MATSim4UrbanSim configuration including an external config
			CreateTestM4UConfig testConfig = new CreateTestM4UConfig(CreateTestM4UConfig.COLD_START, path, externalConfigLocation);
			String configLocation = testConfig.generateConfigV3();
			
			log.info("Reading the matsim4urbansim config file ("+configLocation+") and converting it into matsim format");
			if( !(connector = new M4UConfigurationConverterV4( configLocation )).init() ){
				log.error("An error occured while initializing MATSim scenario ...");
				Assert.assertFalse(true);
			}
			
			log.info("Getting config settings in matsim format");
			Config config = connector.getConfig();
			
			// the process was not aborted 
			Assert.assertTrue(config != null);
			
		} catch(Exception e){
			e.printStackTrace();
			Assert.assertFalse(true);
		}
		tempDirectoryUtil.cleanUpCustomTempDirectories();
	}

}
