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

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.contrib.matsim4opus.config.modules.AccessibilityConfigModule;
import org.matsim.contrib.matsim4opus.config.modules.M4UControlerConfigModuleV3;
import org.matsim.contrib.matsim4opus.config.modules.UrbanSimParameterConfigModuleV3;
import org.matsim.contrib.matsim4opus.utils.CreateTestExternalMATSimConfig;
import org.matsim.contrib.matsim4opus.utils.CreateTestMATSimConfig;
import org.matsim.contrib.matsim4opus.utils.io.Paths;
import org.matsim.contrib.matsim4opus.utils.io.TempDirectoryUtil;
import org.matsim.core.basic.v01.IdImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.Module;
import org.matsim.core.config.consistency.VspConfigConsistencyCheckerImpl;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

import difflib.Delta;
import difflib.DiffUtils;
import difflib.Patch;

/**
 * @author thomas
 *
 */
public class ConfigReadWriteOverwriteTest /*extends MatsimTestCase*/{
	
	private static final Logger log = Logger.getLogger(ConfigReadWriteOverwriteTest.class);
	
	@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	
	/**
	 * This test makes sure that the MATSim4UrbanSim config file will be correctly written, 
	 * correctly converted into standard MATSim format and that all values are recognized correctly
	 */
	@Test
	public void testLoadMATSim4UrbanSimMinimalConfig(){
		
		// MATSim4UrbanSim configuration converter
		M4USimConfigurationConverterV4 connector = null;
		
		try{
			String path = TempDirectoryUtil.createCustomTempDirectory("tmp");
			
			log.info("Creating a matsim4urbansim config file and writing it on hand disk");
			
			CreateTestMATSimConfig testConfig = new CreateTestMATSimConfig(CreateTestMATSimConfig.COLD_START, path);
			String configLocation = testConfig.generateMinimalConfig();
			
			log.info("Reading the matsim4urbansim config file ("+configLocation+") and converting it into matsim format");
			if( !(connector = new M4USimConfigurationConverterV4( configLocation )).init() ){
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
//		TempDirectoryUtil.cleaningUpCustomTempDirectories();
		log.info("done") ;
	}
	
	/**
	 * This test makes sure that the MATSim4UrbanSim config file will be correctly written, 
	 * correctly converted into standard MATSim format and that all values are recognized correctly
	 */
	@Test
	public void testLoadMATSim4UrbanSimConfigOnly(){
		
		// MATSim4UrbanSim configuration converter
		M4USimConfigurationConverterV4 connector = null;
		
		try{
			String path = TempDirectoryUtil.createCustomTempDirectory("tmp");
			
			log.info("Creating a matsim4urbansim config file and writing it on hand disk");
			
			CreateTestMATSimConfig testConfig = new CreateTestMATSimConfig(CreateTestMATSimConfig.COLD_START, path);
			String configLocation = testConfig.generate();
			
			log.info("Reading the matsim4urbansim config file ("+configLocation+") and converting it into matsim format");
			if( !(connector = new M4USimConfigurationConverterV4( configLocation )).init() ){
				log.error("An error occured while initializing MATSim scenario ...");
				Assert.assertTrue(false);
			}
			
			log.info("Getting config settings in matsim format");
			Config config = connector.getConfig();
			
			checkCoreModuleSettings(testConfig, config);
			
			config.addConfigConsistencyChecker(new VspConfigConsistencyCheckerImpl() ) ;
			config.checkConsistency() ;
			
		} catch(Exception e){
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		TempDirectoryUtil.cleaningUpCustomTempDirectories();
		log.info("done") ;
	}
	
	/**
	 * This test makes sure that settings made in an external config
	 * file will always overwrite MATSim4UrbanSim settings. 
	 * 
	 * In this test, some parameters settings in the external config are
	 * overlapping with settings made in the MATSim4UrbanSim config. To detect
	 * this, different parameter settings are used in the external config.
	 */
	@Test
	public void testExternalMATSimConfig(){
		
		// MATSim4UrbanSim configuration converter
		M4USimConfigurationConverterV4 converter = null;
		
		try{
//			String path = TempDirectoryUtil.createCustomTempDirectory("tmp");
			String path = utils.getOutputDirectory() + "/tmp" ;
			IOUtils.createDirectory(path) ;
			
			log.info("Creating a matsim4urbansim config file and writing it on hard disk");
			
			// this creates an external configuration file, some parameters overlap with the MATSim4UrbanSim configuration
			CreateTestExternalMATSimConfig testExternalConfig = new CreateTestExternalMATSimConfig(CreateTestExternalMATSimConfig.COLD_START, path);
			String externalConfigLocation = testExternalConfig.generate();
			
			// this creates a MATSim4UrbanSim configuration
			CreateTestMATSimConfig testConfig = new CreateTestMATSimConfig(CreateTestMATSimConfig.COLD_START, path, externalConfigLocation);
			String configLocation = testConfig.generate();
			
			// This converts the MATSim4UrbanSim configuration into MATSim format, 
			// i.e. puts the settings into the MATSim config groups or adds new MATSim modules
			// An important task is that settings from the external config file are overwriting the settings from MATSim4UrbanSim
			log.info("Reading the matsim4urbansim config file ("+configLocation+") and converting it into matsim format");
			if( !(converter = new M4USimConfigurationConverterV4( configLocation )).init() ){
				log.error("An error occured while initializing MATSim scenario ...");
				Assert.assertTrue(false);
			}
			
			log.info("Getting config settings in matsim format");
			Config config = converter.getConfig();
//			config.controler().setFirstIteration(20) ;

			String revisedFileName = utils.getOutputDirectory()+"/config.xml"  ;
			log.info( "new: " + revisedFileName ) ;
			new ConfigWriter(config).write( revisedFileName ) ;
			final long revisedCheckSum = CRCChecksum.getCRCFromFile(revisedFileName) ;

			String originalFileName = utils.getClassInputDirectory()+"/config.xml" ;
			log.info( "old: " + originalFileName ) ;
			final long originalCheckSum = CRCChecksum.getCRCFromFile(originalFileName);

			if ( revisedCheckSum != originalCheckSum ) {

				List<String> original = fileToLines(originalFileName);
				List<String> revised  = fileToLines(revisedFileName);

				Patch patch = DiffUtils.diff(original, revised);

				for (Delta delta: patch.getDeltas()) {
					System.out.flush() ;
					System.err.println(delta.getOriginal());
					System.err.println(delta.getRevised());
					System.err.flush() ;
				}

			}
            
			// checking if overlapping parameter settings are overwritten by the external config
			checkCoreModulesAndExternalConfigSettings(testExternalConfig, testConfig, config);
			
			// following test is too tough for regular tests (because of default changes) but can be made operational before refactorings.
			Assert.assertEquals( "config files are different", originalCheckSum, revisedCheckSum	 ) ;
			
			
		} catch(Exception e){
			e.printStackTrace();
			Assert.assertTrue(false);
		}
		TempDirectoryUtil.cleaningUpCustomTempDirectories();
		
	}

	// Helper method for get the file content
    private static List<String> fileToLines(String filename) {
            List<String> lines = new LinkedList<String>();
            String line = "";
            try {
                    BufferedReader in = new BufferedReader(new FileReader(filename));
                    while ((line = in.readLine()) != null) {
                            lines.add(line);
                    }
            } catch (IOException e) {
                    e.printStackTrace();
            }
            return lines;
    }

	/**
	 * This tests if overlapping parameter settings are overwritten by the external config.
	 * Settings that are overlapping in the MATSim4UrbanSimConfig and the external config 
	 * should be overwritten by parameter settings from the external config.
	 * 
	 * @param testExternalConfig contains all parameter settings from the external config 
	 * @param testConfig contains all parameter from the MATSim4UrbanSim config
	 * @param config is the merged config in standard MATSim format.
	 */
	private void checkCoreModulesAndExternalConfigSettings(
			CreateTestExternalMATSimConfig testExternalConfig,
			CreateTestMATSimConfig testConfig, Config config) {
		
		Assert.assertTrue(config.controler().getFirstIteration() == testExternalConfig.firstIteration.intValue());
		Assert.assertTrue(config.controler().getLastIteration() == testExternalConfig.lastIteration.intValue());
		Assert.assertTrue( Paths.checkPathEnding( config.controler().getOutputDirectory() ).equalsIgnoreCase( Paths.checkPathEnding( testExternalConfig.matsim4opusOutput ) ));
		
		Assert.assertTrue( Paths.checkPathEnding( config.network().getInputFile() ).equalsIgnoreCase( Paths.checkPathEnding( testExternalConfig.networkInputFile ) ));
		
		if(testConfig.getStartMode() != testExternalConfig.COLD_START){
			Assert.assertTrue( Paths.checkPathEnding( config.plans().getInputFile() ).equalsIgnoreCase( Paths.checkPathEnding( testExternalConfig.inputPlansFile ) ));
		}
		
		Iterator<StrategySettings> iteratorStrategyCG = config.strategy().getStrategySettings().iterator();
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
		
		
		///////////////////////////////////////////////////
		// Here, additional parameters from the external config, that are not overlapping, are tested!
		///////////////////////////////////////////////////
		
		Module matsim4UrbanSimModule = config.getModule(M4UConfigUtils.MATSIM4URBANSIM_MODULE_EXTERNAL_CONFIG);
		
		///////////////////////////////////////////////////
		// MATSim4UrbanSim Controler Config Module Settings
		///////////////////////////////////////////////////
		M4UControlerConfigModuleV3 matsim4UrbanSimControlerModule = M4UConfigUtils.getMATSim4UrbaSimControlerConfigAndPossiblyConvert(config) ;
		AccessibilityConfigModule acm = M4UAccessibilityConfigUtils.getConfigModuleAndPossiblyConvert(config) ;
		
		// time of day
		Assert.assertTrue( acm.getTimeOfDay() == testExternalConfig.timeOfDay );
		// use pt stops flag
		boolean usePtStopsFlagFromConfig = matsim4UrbanSimModule.getValue( M4UConfigUtils.PT_STOPS_SWITCH ) != null && matsim4UrbanSimModule.getValue( M4UConfigUtils.PT_STOPS_SWITCH ).equalsIgnoreCase("TRUE");
		Assert.assertTrue( usePtStopsFlagFromConfig == testExternalConfig.usePtStops.equalsIgnoreCase("TRUE") );
		// pt stops
		if(testExternalConfig.usePtStops.equalsIgnoreCase("TRUE"))
			Assert.assertTrue( Paths.checkPathEnding( matsim4UrbanSimControlerModule.getPtStopsInputFile()  ).equalsIgnoreCase( Paths.checkPathEnding( testExternalConfig.ptStops ) ));
		// use pt travel times and distances
		boolean usePtTimesAndDiastances = matsim4UrbanSimModule.getValue( M4UConfigUtils.PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH ) != null && matsim4UrbanSimModule.getValue( M4UConfigUtils.PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH ).equalsIgnoreCase("TRUE");
		Assert.assertTrue( usePtTimesAndDiastances == testExternalConfig.useTravelTimesAndDistances.equalsIgnoreCase("TRUE") );
		
		if( testExternalConfig.useTravelTimesAndDistances.equalsIgnoreCase("TRUE") ){
			// pt travel times
			Assert.assertTrue( Paths.checkPathEnding( matsim4UrbanSimControlerModule.getPtTravelTimesInputFile() ).equalsIgnoreCase( testExternalConfig.ptTravelTimes ));
			// pt travel distances
			Assert.assertTrue(Paths.checkPathEnding( matsim4UrbanSimControlerModule.getPtTravelDistancesInputFile() ).equalsIgnoreCase( testExternalConfig.ptTravelDistances ));
		
		}
		
		///////////////////////////////////////////////////
		// UrbanSim Parameter Config Module Settings
		///////////////////////////////////////////////////
		UrbanSimParameterConfigModuleV3 urbansimParameterConfigModule = testExternalConfig.getUrbanSimParameterConfig(config);
		// shape file for population distribution (zone)
		Assert.assertTrue( Paths.checkPathEnding( urbansimParameterConfigModule.getUrbanSimZoneShapefileLocationDistribution() ).equalsIgnoreCase( Paths.checkPathEnding( testExternalConfig.urbanSimZoneShapefileLocationDistribution )));
		
//		// Accessibility Parameter Config Module settings
//		AccessibilityParameterConfigModule accessibilityParameterModule = testExternalConfig.getAccessibilityParameterConfig(config);
//		// bike parameter
//		Assert.assertTrue( accessibilityParameterModule.getBetaBikeTravelTime() 	== testExternalConfig.betaBikeTravelTime );
//		Assert.assertTrue( accessibilityParameterModule.getBetaBikeTravelTimePower2()== testExternalConfig.betaBikeTravelTimePower2 );
//		Assert.assertTrue( accessibilityParameterModule.getBetaBikeLnTravelTime()  	== testExternalConfig.betaBikeLnTravelTime );
//		Assert.assertTrue( accessibilityParameterModule.getBetaBikeTravelDistance() == testExternalConfig.betaBikeTravelDistance );
//		Assert.assertTrue( accessibilityParameterModule.getBetaBikeTravelDistancePower2() == testExternalConfig.betaBikeTravelDistancePower2 );
//		Assert.assertTrue( accessibilityParameterModule.getBetaBikeLnTravelDistance()== testExternalConfig.betaBikeLnTravelDistance );
//		Assert.assertTrue( accessibilityParameterModule.getBetaBikeTravelMonetaryCost()  	== testExternalConfig.betaBikeTravelCost );
//		Assert.assertTrue( accessibilityParameterModule.getBetaBikeTravelMonetaryCostPower2() == testExternalConfig.betaBikeTravelCostPower2 );
//		Assert.assertTrue( accessibilityParameterModule.getBetaBikeLnTravelMonetaryCost()  	== testExternalConfig.betaBikeLnTravelCost );
//		// pt parameter
//		Assert.assertTrue( accessibilityParameterModule.getBetaPtTravelTime() 		== testExternalConfig.betaPtTravelTime );
//		Assert.assertTrue( accessibilityParameterModule.getBetaPtTravelTimePower2() == testExternalConfig.betaPtTravelTimePower2 );
//		Assert.assertTrue( accessibilityParameterModule.getBetaPtLnTravelTime()  	== testExternalConfig.betaPtLnTravelTime );
//		Assert.assertTrue( accessibilityParameterModule.getBetaPtTravelDistance()  	== testExternalConfig.betaPtTravelDistance );
//		Assert.assertTrue( accessibilityParameterModule.getBetaPtTravelDistancePower2() == testExternalConfig.betaPtTravelDistancePower2 );
//		Assert.assertTrue( accessibilityParameterModule.getBetaPtLnTravelDistance() == testExternalConfig.betaPtLnTravelDistance );
//		Assert.assertTrue( accessibilityParameterModule.getBetaPtTravelMonetaryCost()  		== testExternalConfig.betaPtTravelCost );
//		Assert.assertTrue( accessibilityParameterModule.getBetaPtTravelMonetaryCostPower2() == testExternalConfig.betaPtTravelCostPower2 );
//		Assert.assertTrue( accessibilityParameterModule.getBetaPtLnTravelMonetaryCost()  	== testExternalConfig.betaPtLnTravelCost );
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
