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
package playground.tnicolai.urbansim.warmstart;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.Scenario;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.network.MatsimNetworkReader;
import org.matsim.core.network.NetworkImpl;
import org.matsim.core.population.MatsimPopulationReader;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;

import playground.tnicolai.urbansim.constants.Constants;
import playground.tnicolai.urbansim.utils.CommonUtilities;
import playground.tnicolai.urbansim.utils.io.FileCopy;
import playground.tnicolai.urbansim.utils.io.ReadFromUrbansimParcelModel;
import playground.tnicolai.urbansim.utils.io.ReadFromUrbansimParcelModel.PopulationCounter;
import playground.tnicolai.urbansim.utils.io.TempDirectoryUtil;

/**
 * @author thomas
 *
 */
public class WarmStartTest extends MatsimTestCase{
	
	private static final Logger log = Logger.getLogger(WarmStartTest.class);
	
	private static String destinationDir = null;
	
	@Rule 
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testNoInitalPlansFile(){
		log.info("Starting testNoInitalPlansFile run: Testing if MATSim regognizes that there is no initial input plans file.");
		PopulationCounter result = prepareTest(false);
		
		// clean up temp directories
		TempDirectoryUtil.cleaningUpOPUSDirectories();
		
		evaluate(false, result);
		log.info("End of testMATSimConfig.");
	}
	
	@Test
	public void testWithInitalPlansFile(){
		log.info("Starting testNoInitalPlansFile run: Testing if MATSim regognizes that there is no initial input plans file.");
		PopulationCounter result = prepareTest(true);
		
		// clean up temp directories
		TempDirectoryUtil.cleaningUpOPUSDirectories();
		
		evaluate(true, result);
		log.info("End of testMATSimConfig.");
	}
	
	/**
	 * preparing MATSim test run
	 * @param configName name of MATSim config file
	 */
	private PopulationCounter prepareTest(boolean isWarmStart){
		
		String inputPlansFileDir = CommonUtilities.getWarmStartInputPlansFile(WarmStartTest.class);
		String urbanSimDataDir = CommonUtilities.getWarmStartUrbanSimInputData(WarmStartTest.class);
		String networkDir = CommonUtilities.getWarmStartNetwork(WarmStartTest.class);
		
		log.info("Set path to Input Plans File (Warm Start): " + inputPlansFileDir);
		log.info("Set path to UrbanSim data (Warm Start: " + urbanSimDataDir);
		log.info("Set path to Network (Warm Start: " + networkDir);
		
		// copy UrbanSim data into the OPUS tmp directory
		// these files serve as input for MATSim.
		allocateUrbanSimDataForMATSimRun(inputPlansFileDir, networkDir, urbanSimDataDir);
		
		
		Scenario scenario = ScenarioUtils.createScenario(ConfigUtils.createConfig());
		MatsimNetworkReader netReader = new MatsimNetworkReader(scenario);
		netReader.readFile(networkDir + "psrc.xml.gz");
		NetworkImpl network = (NetworkImpl) scenario.getNetwork();
		
		
		// init class ReadFromUrbansimParcelModel
		ReadFromUrbansimParcelModel readFromUrbansim = new ReadFromUrbansimParcelModel( 2001, null );
		
		// create facilities -> needed for Population generation
		ActivityFacilitiesImpl parcels = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
		ActivityFacilitiesImpl zones   = new ActivityFacilitiesImpl("urbansim zones");
		readFromUrbansim.readFacilities(parcels, zones);

		Population oldPopulation = null;
		if(isWarmStart){
			
			MatsimPopulationReader popReader = new MatsimPopulationReader(scenario);
			popReader.readFile(inputPlansFileDir + "warm-start-input-plans-file.xml");	// input plans file
			oldPopulation = scenario.getPopulation();
		}
		Population newPopulation = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		
		return readFromUrbansim.readPersons(oldPopulation, newPopulation, parcels, network, 1.0);
				
	}
	
	/**
	 * 
	 * @param isWarmStart
	 */
	private void evaluate(boolean isWarmStart, PopulationCounter result){
		
		if(isWarmStart){
			
			/**
			 * 	input plans file setup (old population)
			
				person_id	parcel_id_home	parcel_id_work
				1	1	11
				2	2	11
				3	3	12
				4	4	12
				5	5	13
				6	6	14
				7	7	15
				8	8	15
				9	9	16
				10	10	16
				parcel_id	x_coord_sp	y_coord_sp	zone_id
				1	1.0	1.0	1
				2	2.0	2.0	1
				3	3.0	3.0	1
				4	4.0	4.0	1
				5	5.0	5.0	1
				6	6.0	6.0	1
				7	7.0	7.0	1
				8	8.0	8.0	1
				9	9.0	9.0	1
				10	10.0	10.0	2
				11	11.0	11.0	2
				12	12.0	12.0	2
				13	13.0	13.0	2
				14	14.0	14.0	2
				15	15.0	15.0	2
				16 	16.0	16.0	2	
			
			Changes in the new population
				- person (ID=1) exists no more
				- new person (ID=11) added				--> notFountCnt = 1 and backupCnt = 1
				- person (ID=2), home location changed 	--> homelocationChangedCnt = 1
				- person (ID=3), work location changed	--> worklocationChangedCnt = 1
				- person (ID=4), employment status changed--> employmentChangedCnt = 1
				- person (ID=5), job location (in parcel_dataset) deleted	--> jobLocationIdNullCnt = 1
			 	
			 In result only 5 person stay the same 		--> identifiedCnt = 5
			 
			 */
			
			assertTrue(
					result.identifiedCnt == 5 &&
					result.notFoundCnt == 1 &&
					result.backupCnt ==1 &&
					result.homelocationChangedCnt == 1 &&
					result.worklocationChangedCnt == 1 && 
					result.employmentChangedCnt == 1 && 
					result.jobLocationIdNullCnt == 1 &&
					result.populationMergeTotal == 9);
			
		}
		else
			assertTrue(result.identifiedCnt == 0 &&
					result.NUrbansimPersons == 10 &&
					result.populationMergeTotal == 9);
	}
	
	/**
	 * copying UrbanSim data into the OPUS tmp directory
	 * @param inputPlansFile path to the input plans file
	 * @param urbanSimDataPath path to the UrbanSim input data for MATSim 
	 */
	private void allocateUrbanSimDataForMATSimRun(String inputPlansFile, String networkDir, String urbanSimDataPath){
		
		// set temp directory as opus_home
		// create temp directories
		TempDirectoryUtil.createOPUSDirectories();
		// set output directory for UrbanSim data to OPUS_HOME/opus_matsim/tmp
		destinationDir = Constants.MATSIM_4_OPUS_TEMP;

		// copy UrbanSim data to destination path
		if( !FileCopy.copyTree(urbanSimDataPath, destinationDir) ){
			log.error("Error while copying UrbanSim data.");
			System.exit(-1);
		}
		
		// copy inputPlansFile data to destination path
		if( !FileCopy.copyTree(inputPlansFile, destinationDir) ){
			log.error("Error while copying inputPlansFile.");
			System.exit(-1);
		}
		
		// copy network data to destination path
		if( !FileCopy.copyTree(networkDir, destinationDir) ){
			log.error("Error while copying inputPlansFile.");
			System.exit(-1);
		}
	}

}

