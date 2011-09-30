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
package playground.tnicolai.matsim4opus.warmStart;

import org.apache.log4j.Logger;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.api.core.v01.population.Population;
import org.matsim.core.facilities.ActivityFacilitiesImpl;
import org.matsim.core.scenario.ScenarioImpl;
import org.matsim.core.scenario.ScenarioLoaderImpl;
import org.matsim.core.scenario.ScenarioUtils;
import org.matsim.core.utils.misc.ConfigUtils;
import org.matsim.testcases.MatsimTestCase;
import org.matsim.testcases.MatsimTestUtils;

import playground.tnicolai.matsim4opus.matsimTestData.GenerateOPUSTestEnvironment;
import playground.tnicolai.matsim4opus.utils.InitMATSimScenario;
import playground.tnicolai.matsim4opus.utils.JAXBUnmaschal;
import playground.tnicolai.matsim4opus.utils.io.ReadFromUrbansimParcelModel;
import playground.tnicolai.matsim4opus.utils.io.ReadFromUrbansimParcelModel.PopulationCounter;
import playground.tnicolai.matsim4opus.utils.io.TempDirectoryUtil;


/**
 * @author thomas
 *
 */
public class WarmStartTest extends MatsimTestCase{
	
	private static final Logger log = Logger.getLogger(WarmStartTest.class);
	
	private GenerateOPUSTestEnvironment gote = null;
	private String warmStartPlansFile = "warm-start-input-plans-file.xml";
	
	@Rule 
	public MatsimTestUtils utils = new MatsimTestUtils();
	
	@Test
	public void testNoInitalPlansFile(){
		log.info("Starting testNoInitalPlansFile run: Testing if MATSim regognizes that there is no initial input plans file (with a sampling rate of 100%).");
		double samplingRate = 1.;
		PopulationCounter result = prepareTest(false,samplingRate);
		
		evaluate(false, result, samplingRate);
		
		// clean up temp directories
		TempDirectoryUtil.cleaningUpOPUSDirectories();

		log.info("End of testMATSimConfig.");
	}
	
	@Test
	public void testWithInitalPlansFile(){
		log.info("Starting testWithInitalPlansFile run: Testing if MATSim correctly merges the old population (plans file) and new popululation (from UrbanSim output) with a sampling rate of 100%.");
		double samplingRate = 1.;
		PopulationCounter result = prepareTest(true,samplingRate);
		
		evaluate(true, result, samplingRate);
		
		// clean up temp directories
		TempDirectoryUtil.cleaningUpOPUSDirectories();
		
		log.info("End of testMATSimConfig.");
	}
	
	@Test
	public void testWithInitalPlansFileAndLowerSamplingRate(){
		log.info("Starting testWithInitalPlansFile run: Testing if MATSim correctly merges the old population (plans file) and new popululation (from UrbanSim output) with a lower sampling rate of 50%.");
		double samplingRate = 0.5;
		PopulationCounter result = prepareTest(true,samplingRate);
		
		evaluate(true, result, samplingRate);
		
		// clean up temp directories
		TempDirectoryUtil.cleaningUpOPUSDirectories();
		
		log.info("End of testMATSimConfig.");
	}
	
	/**
	 * preparing MATSim test run
	 * @param configName name of MATSim config file
	 */
	@SuppressWarnings("deprecation")
	private PopulationCounter prepareTest(boolean isWarmStart, double samplingRate){
		
		// this generates an entire matsim4opus testing environment including 
		// typical matsim input files (e. g. matsim config, urbansim outputs, network)
		// and the matsim4opus folder structure ...
		gote = new GenerateOPUSTestEnvironment( Boolean.TRUE );
		// the matsim config contains all information about the matsim4opus test environment -> see next step
		String matsimConfigPath = gote.createWarmStartOPUSTestEnvironment( this.warmStartPlansFile );
		
		// the information about the matsim4opus test environment (see previous step) will be imported into the scenario 
		ScenarioImpl scenario = (ScenarioImpl)ScenarioUtils.createScenario(ConfigUtils.createConfig());
		// 1 Step: assign information to jaxb binding classes 
		JAXBUnmaschal jaxbU = new JAXBUnmaschal(matsimConfigPath);
		// 2 Step: import/extract into MATSim scenario 
		InitMATSimScenario ims = new InitMATSimScenario(scenario, jaxbU.unmaschalMATSimConfig());
		ims.init();

		// 3 Step: init/loading scenario
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadScenario();
		
		// init class ReadFromUrbansimParcelModel
		ReadFromUrbansimParcelModel readFromUrbansim = new ReadFromUrbansimParcelModel( 2001 );
		
		// create facilities -> needed for Population generation
		ActivityFacilitiesImpl parcels = new ActivityFacilitiesImpl("urbansim locations (gridcells _or_ parcels _or_ ...)");
		ActivityFacilitiesImpl zones   = new ActivityFacilitiesImpl("urbansim zones");
		readFromUrbansim.readFacilities(parcels, zones);

		Population oldPopulation = null;
//		Population newPopulation = ((ScenarioImpl) ScenarioUtils.createScenario(ConfigUtils.createConfig())).getPopulation();
		if(isWarmStart){
			// get old population (from input plans file)
			oldPopulation = scenario.getPopulation();
		}
		
		Population newPopulation = readFromUrbansim.readPersons(oldPopulation, parcels, scenario.getNetwork(), samplingRate);
		return readFromUrbansim.getPopulationCounter();
	}
	
	/**
	 * 
	 * @param isWarmStart
	 */
	private void evaluate(boolean isWarmStart, PopulationCounter result, double samplingRate){
		
		if(isWarmStart && samplingRate == 1.){
			
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
			
			// the Population counter must eqal this counts
			assertTrue(
					result.identifiedCnt == 5 &&
					result.newPersonCnt == 1 &&
					result.fromBackupCnt ==1 &&
					result.homelocationChangedCnt == 1 &&
					result.worklocationChangedCnt == 1 && 
					result.employmentChangedCnt == 1 && 
					result.jobLocationIdNullCnt == 1 &&
					result.populationMergeTotal == 9);
			
		}
		else if(isWarmStart && samplingRate < 1.){
			assertTrue(result.populationMergeTotal == (10 * samplingRate) );
		}
		else if(! isWarmStart)
			assertTrue(result.identifiedCnt == 0 &&
					result.NUrbansimPersons == 10 &&
					result.populationMergeTotal == 9);
	}

}

