/* *********************************************************************** *
 * project: org.matsim.*
 * MATSim4UrbanSimTest.java
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

/**
 * 
 */
package playground.tnicolai.urbansim.matsimTest;

import java.net.URL;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;

import playground.tnicolai.urbansim.MATSim4Urbansim;
import playground.tnicolai.urbansim.constants.Constants;
import playground.tnicolai.urbansim.testUtils.TempDirectoryUtil;
import playground.tnicolai.urbansim.utils.io.FileCopy;

/**
 * @author thomas
 *
 */
public class MATSim4UrbanSimTest extends MatsimTestCase{
	
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimTest.class);
	
	private static String destinationDir = null;
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testMATSimConfig(){
		log.info("Starting testMATSimConfig run: Testing if MATSim config is valid.");
		prepareTest("matsim_config_test_run.xml", "testMATSimConfig");
	}
	
	@Test
	public void testMATSimRun(){
		log.info("Starting testMATSimRun run: Testing if MATSim run is passes through.");
		prepareTest("matsim_config_normal_run.xml", "testMATSimRun");
	}
	
	/**
	 * preparing MATSim test run
	 * @param configName name of MATSim config file
	 * @param testRunName name of current test case
	 */
	private void prepareTest(String configName, String testRunName){

		String matsimConfigDir = getMATSimConfigDir();
		String matsimConfigName = configName;
		String urbanSimDataDir = getUrbanSimInputDataDir();
		
		log.info("Set path to MATSim config: " + matsimConfigDir);
		log.info("Set MATSim config to: " + matsimConfigName);
		log.info("Set path to UrbanSim data: " + urbanSimDataDir);
		
		// copy UrbanSim data into the OPUS tmp directory
		// these files serve as input for MATSim.
		allocateUrbanSimDataForMATSimRun(urbanSimDataDir);
		
		// running MATSim4UrbanSim
		testRun( matsimConfigDir+matsimConfigName );
		
		// remove temp directories
		TempDirectoryUtil.cleaningUp();
		log.info("End of " + testRunName + ".");
	}
	
	/**
	 * running MATSim with differend configuration files
	 * @param matsimConfigFileLocation poits to le location of the MATSim config
	 */
	private void testRun(String matsimConfigFileLocation){
		log.info("Starting MATSim4UrbanSim with args = " + matsimConfigFileLocation);
		String [] args = new String[]{matsimConfigFileLocation};
		
		MATSim4Urbansim.main(args);
	}
	
	/**
	 * copying UrbanSim data into the OPUS tmp directory
	 * @param urbanSimDataPath path to the UrbanSim input data for MATSim 
	 */
	private void allocateUrbanSimDataForMATSimRun(String urbanSimDataPath){
		
		Constants.setOpusHomeDirectory(System.getProperty("java.io.tmpdir"));
		// create temp directories
		TempDirectoryUtil.createDirectories();
		// set outpur directory for UrbanSim data to OPUS_HOME/opus_matsim/tmp
		destinationDir = Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY;

		// copy UrbanSim data to destination path
		if( !FileCopy.copyTree(urbanSimDataPath, destinationDir) ){
			log.error("Error while copying UrbanSim data.");
			System.exit(-1);
		}
	}
	
	/**
	 * returns the directory to the MATSim config file
	 * @return path to MATSim config
	 */
	private String getMATSimConfigDir(){
		
		String currentDir = getCurrentPath();
		int index = currentDir.lastIndexOf("/");
		return currentDir.substring(0, index) + "Data/xmlMATSimConfig/";
	}
	
	/**
	 * returns the directory to the UrbanSim input data for MATSim
	 * @return directory to the UrbanSim input data
	 */
	private String getUrbanSimInputDataDir(){
		String currentDir = getCurrentPath();
		int index = currentDir.lastIndexOf("matsimTest");
		return currentDir.substring(0, index) + "matsimTestData/urbanSimOutput/";
	}
	
	/**
	 * returns the path of the current directory
	 * @return
	 */
	private String getCurrentPath(){
		try{
			URL dirUrl = MATSim4UrbanSimTest.class.getResource("./"); // get my directory
			return dirUrl.getFile();
		}catch(Exception e){
			e.printStackTrace();
			return null;
		}
	}

}

