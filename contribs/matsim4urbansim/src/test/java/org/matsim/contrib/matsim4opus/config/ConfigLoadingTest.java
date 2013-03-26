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
package org.matsim.contrib.matsim4opus.config;

import java.util.Iterator;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Test;
import org.matsim.contrib.matsim4opus.utils.CreateTestExternalMATSimConfig;
import org.matsim.contrib.matsim4opus.utils.CreateTestMATSimConfig;
import org.matsim.contrib.matsim4opus.utils.io.Paths;
import org.matsim.contrib.matsim4opus.utils.io.TempDirectoryUtil;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.testcases.MatsimTestCase;

/**
 * @author thomas
 *
 */
public class ConfigLoadingTest extends MatsimTestCase{
	
	private static final Logger log = Logger.getLogger(ConfigLoadingTest.class);
	
	/**
	 * This test makes sure that the MATSim4UrbanSim config file will be correctly written, 
	 * correctly converted into standard MATSim format and that all values are recognized correctly
	 */
	@Test
	public void testLoadMATSim4UrbanSimConfigOnly(){
		
		// MATSim4UrbanSim configuration converter
		MATSim4UrbanSimConfigurationConverterV4 connector = null;
		
		try{
			String path = TempDirectoryUtil.createCustomTempDirectory("tmp");
			
			log.info("Creating a matsim4urbansim config file and writing it on hand disk");
			
			CreateTestMATSimConfig testConfig = new CreateTestMATSimConfig(CreateTestMATSimConfig.COLD_START, path);
			String configLocation = testConfig.generate();
			
			log.info("Reading the matsim4urbansim config file ("+configLocation+") and converting it into matsim format");
			if( !(connector = new MATSim4UrbanSimConfigurationConverterV4( configLocation )).init() ){
				log.error("An error occured while initializing MATSim scenario ...");
				Assert.assertTrue(false);
			}
			
			log.info("Getting config settings in matsim format");
			Config config = connector.getConfig();
			
			checkCoreModuleSettings(testConfig, config);
			
		} catch(Exception e){
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		TempDirectoryUtil.cleaningUpCustomTempDirectories();
	}
	
	/**
	 * This test makes sure that settings made in an external config
	 * file will always overwrite MATSim4UrbanSim settings
	 */
	@Test
	public void testExternalMATSimConfig(){
		
		// MATSim4UrbanSim configuration converter
		MATSim4UrbanSimConfigurationConverterV4 connector = null;
		
		try{
			String path = TempDirectoryUtil.createCustomTempDirectory("tmp");
			
			log.info("Creating a matsim4urbansim config file and writing it on hand disk");
			
			CreateTestExternalMATSimConfig testExternalConfig = new CreateTestExternalMATSimConfig(CreateTestExternalMATSimConfig.COLD_START, path);
			String externalConfigLocation = testExternalConfig.generate();
			
			CreateTestMATSimConfig testConfig = new CreateTestMATSimConfig(CreateTestMATSimConfig.COLD_START, path, externalConfigLocation);
			String configLocation = testConfig.generate();
			
			log.info("Reading the matsim4urbansim config file ("+configLocation+") and converting it into matsim format");
			if( !(connector = new MATSim4UrbanSimConfigurationConverterV4( configLocation )).init() ){
				log.error("An error occured while initializing MATSim scenario ...");
				Assert.assertTrue(false);
			}
			
			log.info("Getting config settings in matsim format");
			Config config = connector.getConfig();

			// check all settings
			checkCoreModulesAndExternalConfigSettings(testExternalConfig, testConfig, config);
			
		} catch(Exception e){
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		TempDirectoryUtil.cleaningUpCustomTempDirectories();
		
	}

	/**
	 * @param testExternalConfig
	 * @param testConfig
	 * @param config
	 */
	void checkCoreModulesAndExternalConfigSettings(
			CreateTestExternalMATSimConfig testExternalConfig,
			CreateTestMATSimConfig testConfig, Config config) {
		
		MATSim4UrbanSimControlerConfigModuleV3 matsim4UrbanSimControlerModule = testExternalConfig.getMATSim4UrbaSimControlerConfig(config);
		Assert.assertTrue( matsim4UrbanSimControlerModule.getTimeOfDay() == testExternalConfig.timeOfDay );
		// Assert.assertTrue( Paths.checkPathEnding( matsim4UrbanSimControlerModule.getShapeFileCellBasedAccessibility() ).equalsIgnoreCase( Paths.checkPathEnding( testExternalConfig.urbanSimZoneShapefileLocationDistribution )));
		// Assert.assertTrue( Paths.checkPathEnding( matsim4UrbanSimControlerModule.getPtStopsInputFile()  ).equalsIgnoreCase( Paths.checkPathEnding( testExternalConfig.ptStops ) ));
		
		
		ControlerConfigGroup contolerCG = (ControlerConfigGroup) config.getModule(ControlerConfigGroup.GROUP_NAME);
		Assert.assertTrue(contolerCG.getFirstIteration() == testExternalConfig.firstIteration.intValue());
		Assert.assertTrue(contolerCG.getLastIteration() == testExternalConfig.lastIteration.intValue());
		Assert.assertTrue( Paths.checkPathEnding( contolerCG.getOutputDirectory() ).equalsIgnoreCase( Paths.checkPathEnding( testExternalConfig.matsim4opusOutput ) ));
		
		NetworkConfigGroup networkCG = (NetworkConfigGroup) config.getModule(NetworkConfigGroup.GROUP_NAME);
		Assert.assertTrue( Paths.checkPathEnding( networkCG.getInputFile() ).equalsIgnoreCase( Paths.checkPathEnding( testExternalConfig.networkInputFile ) ));
		
		if(testConfig.getStartMode() != testConfig.COLD_START){
			PlansConfigGroup plansCG = (PlansConfigGroup) config.getModule(PlansConfigGroup.GROUP_NAME);
			Assert.assertTrue( Paths.checkPathEnding( plansCG.getInputFile() ).equalsIgnoreCase( Paths.checkPathEnding( testExternalConfig.inputPlansFile ) ));
		}
		
		StrategyConfigGroup strategyCG = (StrategyConfigGroup) config.getModule(StrategyConfigGroup.GROUP_NAME);
		Iterator<StrategySettings> iteratorStrategyCG = strategyCG.getStrategySettings().iterator();
		while(iteratorStrategyCG.hasNext()){
			StrategySettings strategySettings = iteratorStrategyCG.next();
			
			if(strategySettings.getId() == new IdImpl(1))
				Assert.assertTrue(strategySettings.getProbability() == testExternalConfig.timeAllocationMutatorProbability);
			else if(strategySettings.getId() == new IdImpl(2))
				Assert.assertTrue(strategySettings.getProbability() == testExternalConfig.changeExpBetaProbability);
			else if(strategySettings.getId() == new IdImpl(3))
				Assert.assertTrue(strategySettings.getProbability() == testExternalConfig.reRouteDijkstraProbability);
		}
		
		// tnicolai: times and durations in testExternalConfig are given as String, so they are not comparable with double values
		ActivityParams homeActivity = config.planCalcScore().getActivityParams(testExternalConfig.activityType_0);
		ActivityParams workActivity = config.planCalcScore().getActivityParams(testExternalConfig.activityType_1);
		Assert.assertTrue(homeActivity.getType().equalsIgnoreCase( testExternalConfig.activityType_0 ));
		// Assert.assertTrue(homeActivity.getTypicalDuration() == testExternalConfig.homeActivityTypicalDuration.intValue());
		Assert.assertTrue(workActivity.getType().equalsIgnoreCase( testExternalConfig.activityType_1 ));
		// Assert.assertTrue(workActivity.getOpeningTime() == testExternalConfig.workActivityOpeningTime.intValue());
		// Assert.assertTrue(workActivity.getLatestStartTime() == testExternalConfig.workActivityLatestStartTime.intValue());
	}

	/**
	 * @param testConfig
	 * @param config
	 */
	void checkCoreModuleSettings(CreateTestMATSimConfig testConfig, Config config) {
		log.info("Checking settings");
		// the following checks all settings that are (i) part of the core modules and that are (ii) set in CreateTestMATSimConfig
		
		ControlerConfigGroup contolerCG = (ControlerConfigGroup) config.getModule(ControlerConfigGroup.GROUP_NAME);
		Assert.assertTrue(contolerCG.getFirstIteration() == testConfig.firstIteration.intValue());
		Assert.assertTrue(contolerCG.getLastIteration() == testConfig.lastIteration.intValue());
		Assert.assertTrue( Paths.checkPathEnding( contolerCG.getOutputDirectory() ).equalsIgnoreCase( Paths.checkPathEnding( testConfig.matsim4opusOutput ) ));

		NetworkConfigGroup networkCG = (NetworkConfigGroup) config.getModule(NetworkConfigGroup.GROUP_NAME);
		Assert.assertTrue( Paths.checkPathEnding( networkCG.getInputFile() ).equalsIgnoreCase( Paths.checkPathEnding( testConfig.networkInputFile ) ));
		
		if(testConfig.getStartMode() != testConfig.COLD_START){
			PlansConfigGroup plansCG = (PlansConfigGroup) config.getModule(PlansConfigGroup.GROUP_NAME);
			Assert.assertTrue( Paths.checkPathEnding( plansCG.getInputFile() ).equalsIgnoreCase( Paths.checkPathEnding( testConfig.inputPlansFile ) ));
		}
		
		StrategyConfigGroup strategyCG = (StrategyConfigGroup) config.getModule(StrategyConfigGroup.GROUP_NAME);
		Iterator<StrategySettings> iteratorStrategyCG = strategyCG.getStrategySettings().iterator();
		while(iteratorStrategyCG.hasNext()){
			StrategySettings strategySettings = iteratorStrategyCG.next();
			
			if(strategySettings.getId() == new IdImpl(1))
				Assert.assertTrue(strategySettings.getProbability() == testConfig.timeAllocationMutatorProbability);
			else if(strategySettings.getId() == new IdImpl(2))
				Assert.assertTrue(strategySettings.getProbability() == testConfig.changeExpBetaProbability);
			else if(strategySettings.getId() == new IdImpl(3))
				Assert.assertTrue(strategySettings.getProbability() == testConfig.reRouteDijkstraProbability);
		}
		
		ActivityParams homeActivity = config.planCalcScore().getActivityParams(testConfig.activityType_0);
		ActivityParams workActivity = config.planCalcScore().getActivityParams(testConfig.activityType_1);
		Assert.assertTrue(homeActivity.getType().equalsIgnoreCase( testConfig.activityType_0 ));
		Assert.assertTrue(homeActivity.getTypicalDuration() == testConfig.homeActivityTypicalDuration.intValue());
		Assert.assertTrue(workActivity.getType().equalsIgnoreCase( testConfig.activityType_1 ));
		Assert.assertTrue(workActivity.getOpeningTime() == testConfig.workActivityOpeningTime.intValue());
		Assert.assertTrue(workActivity.getLatestStartTime() == testConfig.workActivityLatestStartTime.intValue());
	}
	

}
