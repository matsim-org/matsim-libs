/* *********************************************************************** *
 * project: org.matsim.*
 * MATSimConfigObject.java
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
package playground.tnicolai.urbansim.utils;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.ScenarioImpl;
import org.matsim.core.config.Config;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.CharyparNagelScoringConfigGroup.ActivityParams;
import org.matsim.core.scenario.ScenarioLoaderImpl;

import playground.tnicolai.urbansim.com.matsim.config.ConfigType;
import playground.tnicolai.urbansim.com.matsim.config.Matsim4UrbansimType;
import playground.tnicolai.urbansim.com.matsim.config.MatsimConfigType;
import playground.tnicolai.urbansim.constants.Constants;

/**
 * @author thomas
 *
 */
public class MATSimConfigObject {
	
	// logger
	private static final Logger log = Logger.getLogger(MATSimConfigObject.class);
	
	/**
	 * NETWORK
	 */
	// pointer to network file location
	private static String networkFile = null;
	
	/**
	 * CONTROLER
	 */
	// number of first iteration of this run
	private static int firstIteration = -1;
	// number of last iteration of this run
	private static int lastIteration = -1;
	
	/**
	 * PLAN CALC SCORE
	 */
	// identifier activityType_0
	private static String activityType_0 = null;
	// identifier activityType_1
	private static String activityType_1 = null;
	
	/**
	 * URBANSIM PARAMETER
	 */
	// year of this run
	private static int year = -1;
	// denotes the sample rate on which MATSim runs. 0.01 means 1%
	private static double samplingRate = -0.01;
	// points to the directory where urbansim and MATSim exchange data
	private static String tempDirectory = null;
	// points to the OPUS_HOME directory
	private static String opusHomeDirectory = null;
	// flag indicates if this is a test run
	private static boolean isTestRun = false;
	
	/**
	 * OTHER
	 */
	// flag o indicate the fist urbansim run (if always true equals "warm start")
	private static boolean firstRun = true;
	// MATSim configuration
	private static Config config	= null;
	// MATSim scenario
	private static ScenarioImpl scenario = null;
	
	public static String getNetwork(){
		return networkFile;
	}
	public static int getFirstIteration(){
		return firstIteration;
	}
	public static int getLastIteration(){
		return lastIteration;
	}
	public static String getActivityType0(){
		return activityType_0;
	}
	public static String getActivityType1(){
		return activityType_1;
	}
	public static int getYear(){
		return year;
	}
	public static double getSampeRate(){
		return samplingRate;
	}
	public static String getTempDirectory(){
		return tempDirectory;
	}
	public static String getOpusHomeDirectory(){
		return opusHomeDirectory;
	}
	public static boolean isTestRun(){
		return isTestRun;
	}
	public static ScenarioImpl getScenario(){
		return scenario;
	}
	public static Config getConfig(){
		return config;
	}
	
	/**
	 * extracting the values (from the MATSim xml config) into global variables.
	 * creating and initializing a scenario and config.
	 * @return true if init process successful
	 */
	public static boolean initMATSimConfigObject(MatsimConfigType matsimConfig){
		
		try{
			ConfigType matsimParameter = matsimConfig.getConfig();
			Matsim4UrbansimType matsim4UrbanSimParameter = matsimConfig.getMatsim4Urbansim();
			
			// MATSim config parameter
			// network
			networkFile = matsimParameter.getNetwork().getInputFile();
			// controler
			firstIteration = matsimParameter.getControler().getFirstIteration().intValue();
			lastIteration = matsimParameter.getControler().getLastIteration().intValue();
			// planCalcScore
			activityType_0 = matsimParameter.getPlanCalcScore().getActivityType0();
			activityType_1 = matsimParameter.getPlanCalcScore().getActivityType1();
			
			// MATSim 4 URbanSim parameter
			samplingRate = matsim4UrbanSimParameter.getUrbansimParameter().getSamplingRate();
			year = matsim4UrbanSimParameter.getUrbansimParameter().getYear().intValue();
			tempDirectory = matsim4UrbanSimParameter.getUrbansimParameter().getTempDirectory();
			opusHomeDirectory = matsim4UrbanSimParameter.getUrbansimParameter().getOpusHOME();
			isTestRun = matsim4UrbanSimParameter.getUrbansimParameter().isIsTestRun();
			
			// only for debugging and testing reasons
			// sets a new OPUS_HOME directory
			// isModifyingOpusHomeDirectory();
	
			log.info("Network: " + networkFile);
			log.info("Controler FirstIteration: " + firstIteration + " LastIteration: " + lastIteration );
			log.info("PlanCalcScore Activity_Type_0: " + activityType_0 + " Activity_Type_1: " + activityType_1);
			log.info("UrbansimParameter SamplingRate: " + samplingRate + " Year: " + year + " TempDir: " + tempDirectory + " OPUS_HOME: " + opusHomeDirectory + " TestRun: " + isTestRun);
			
			createAndInitializeConfigObject();
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
//	/**
//	 * only for debugging and testing reasons.
//	 * cecks if xml config contains the key word "TEST_DIRECTORY"
//	 * to set OPUS_HOME to that directory.
//	 */
//	private static void isModifyingOpusHomeDirectory(){
//		
//		if(opusHomeDirectory.equalsIgnoreCase("TEST_DIRECTORY")){
//			log.info("Indicated to set OPUS_HOME to a temp directory (for debugging or testing reasons)...");
//			Constants.setOpusHomeDirectory( System.getProperty("java.io.tmpdir") );
//		}
//		// update opus home path if needed
//		else if( Constants.OPUS_HOME == null || !Constants.OPUS_HOME.equalsIgnoreCase(opusHomeDirectory)){
//			log.info("Indicated new OPUS_HOME path. Setting new path...");
//			Constants.setOpusHomeDirectory(opusHomeDirectory);
//		}
//	}
	
	/**
	 * creates a MATSim config object with the parameter from the JaxB data structure
	 */
	private static void createAndInitializeConfigObject(){
		
		scenario = new ScenarioImpl();
		config = scenario.getConfig();

		NetworkConfigGroup networkCG = (NetworkConfigGroup) config.getModule(NetworkConfigGroup.GROUP_NAME);
		ControlerConfigGroup controlerCG = (ControlerConfigGroup) config.getModule(ControlerConfigGroup.GROUP_NAME);
		
		// set values
		networkCG.setInputFile( networkFile );	// network
		log.info("Setting network to config...");
		
		controlerCG.setFirstIteration( firstIteration );	// controller (first, last iteration)
		controlerCG.setLastIteration( lastIteration);
		controlerCG.setOutputDirectory( Constants.OPUS_MATSIM_OUTPUT_DIRECTORY ); 
		log.info("Setting controler to config...");
		
		ActivityParams actType0 = new ActivityParams(activityType_0);
		actType0.setTypicalDuration(12*60*60);
		ActivityParams actType1 = new ActivityParams(activityType_1);
		actType1.setTypicalDuration(8*60*60);
		config.charyparNagelScoring().addActivityParams( actType0 ); // planCalcScore
		config.charyparNagelScoring().addActivityParams( actType1 );
		log.info("Setting planCalcScore to config...");
		
		// configure strategies for replanning
		config.strategy().setMaxAgentPlanMemorySize(4);
		StrategyConfigGroup.StrategySettings selectExp = new StrategyConfigGroup.StrategySettings(IdFactory.get(1));
		selectExp.setModuleName("ReRoute_Dijkstra");
		selectExp.setProbability(1.0);
		config.strategy().addStrategySettings(selectExp);
		log.info("Setting strategy to config...");
		
		// init loader
		ScenarioLoaderImpl loader = new ScenarioLoaderImpl(scenario);
		loader.loadScenario();
		
		// output dir
		config.controler().setOutputDirectory( Constants.OPUS_MATSIM_OUTPUT_DIRECTORY );
	}
}

