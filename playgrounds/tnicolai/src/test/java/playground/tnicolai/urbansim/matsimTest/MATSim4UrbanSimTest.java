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

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.population.PopulationReaderMatsimV4;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;

import playground.tnicolai.urbansim.MATSim4Urbansim;
import playground.tnicolai.urbansim.constants.Constants;
import playground.tnicolai.urbansim.utils.CommonUtilities;
import playground.tnicolai.urbansim.utils.io.FileCopy;
import playground.tnicolai.urbansim.utils.io.ReadFromUrbansimParcelModel;
import playground.tnicolai.urbansim.utils.io.TempDirectoryUtil;

/**
 * @author thomas
 *
 */
public class MATSim4UrbanSimTest extends MatsimTestCase{
	
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimTest.class);
	
	private static String destinationDir = null;
	
	@Rule 
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testMATSimConfig(){
		
//		utils.starting(method)
//		System.out.println( utils.getInputDirectory() );
		
		log.info("Starting testMATSimConfig run: Testing if MATSim config is valid.");
		prepareTest("matsim_config_test_run.xml");
		// remove temp directories
		TempDirectoryUtil.cleaningUpOPUSDirectories();
		log.info("End of testMATSimConfig.");
	}
	
	@Test
	public void testMATSimRun(){
		log.info("Starting testMATSimRun run: Testing if MATSim run is passes through.");
		prepareTest("matsim_config_normal_run.xml");
		
		Assert.assertTrue( postProgressing() );
		// remove temp directories
		TempDirectoryUtil.cleaningUpOPUSDirectories();
		
		log.info("End of testMATSimRun.");
	}
	
	/**
	 * preparing MATSim test run
	 * @param configName name of MATSim config file
	 */
	private void prepareTest(String configName){

		String matsimConfigDir = CommonUtilities.getTestMATSimConfigDir(MATSim4UrbanSimTest.class);
		String matsimConfigName = configName;
		String urbanSimDataDir = CommonUtilities.getTestUrbanSimInputDataDir(MATSim4UrbanSimTest.class);
		
		log.info("Set path to MATSim config: " + matsimConfigDir);
		log.info("Set MATSim config to: " + matsimConfigName);
		log.info("Set path to UrbanSim data: " + urbanSimDataDir);
		
		// copy UrbanSim data into the OPUS tmp directory
		// these files serve as input for MATSim.
		allocateUrbanSimDataForMATSimRun(urbanSimDataDir);
		
		// running MATSim4UrbanSim
		testRun( matsimConfigDir+matsimConfigName );
	}
	
	private boolean postProgressing(){
		
		int inputPopulation= -1;
		Population outputPopulation = null;
		
		// get population size from MATSim input file
		ReadFromUrbansimParcelModel readFromUrbanSim = new ReadFromUrbansimParcelModel(2001, null);
		inputPopulation = readFromUrbanSim.countPersons();
		
		// population size from MATSim output file
		ScenarioImpl scenario = (ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig());
		PopulationReaderMatsimV4 populationReader = new PopulationReaderMatsimV4(scenario);
		populationReader.readFile( Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY + "pop.xml.gz" );
//		populationReader.readFile( Constants.OPUS_MATSIM_OUTPUT_DIRECTORY + "output_plans.xml.gz" );
		outputPopulation = scenario.getPopulation();
		
		log.info("Population size in inputput file : " + inputPopulation);
		log.info("Population size in output file : " + outputPopulation.getPersons().size());
		
		return outputPopulation.getPersons().size() == inputPopulation;
	}
	
	/**
	 * running MATSim with differend configuration files
	 * @param matsimConfigFileLocation poits to le location of the MATSim config
	 */
	private void testRun(String matsimConfigFileLocation){
		log.info("Starting MATSim4UrbanSim with args = " + matsimConfigFileLocation);
		String [] args = new String[]{matsimConfigFileLocation}; // create progam arguments for MATSim
		
		MATSim4Urbansim.main(args);
	}
	
	/**
	 * copying UrbanSim data into the OPUS tmp directory
	 * @param urbanSimDataPath path to the UrbanSim input data for MATSim 
	 */
	private void allocateUrbanSimDataForMATSimRun(String urbanSimDataPath){
		
		// set temp directory as opus_home
		// Constants.setOpusHomeDirectory(System.getProperty("java.io.tmpdir")); // moved to TempDirectoryUtil
		// create temp directories
		TempDirectoryUtil.createOPUSDirectories();
		// set outpur directory for UrbanSim data to OPUS_HOME/opus_matsim/tmp
		destinationDir = Constants.OPUS_MATSIM_TEMPORARY_DIRECTORY;

		// copy UrbanSim data to destination path
		if( !FileCopy.copyTree(urbanSimDataPath, destinationDir) ){
			log.error("Error while copying UrbanSim data.");
			System.exit(-1);
		}
	}

}

