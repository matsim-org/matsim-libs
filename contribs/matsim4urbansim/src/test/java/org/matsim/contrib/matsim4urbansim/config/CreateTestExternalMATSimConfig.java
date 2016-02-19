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

import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matsim4urbansim.config.modules.M4UControlerConfigModuleV3;
import org.matsim.contrib.matsim4urbansim.config.modules.UrbanSimParameterConfigModuleV3;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigGroup;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * @author thomas
 *
 */
public class CreateTestExternalMATSimConfig {
	
	 static final String DUMMY_FILE_2 = "/dummy2.xml";
	
	 final int timeOfDay 						= 10000;
	 final String urbanSimZoneShapefileLocationDistribution;
	 final String ptStops;
	 final String usePtStops;
	 final String ptTravelTimes;
	 final String ptTravelDistances;
	 final String useTravelTimesAndDistances;
	 final String changeLegModeModuleName 	= "changeLegMode";
	 final String changeLegModeParamName		= "modes";
	 final String changeLegModeValue			= "car,pt";
	 final String strategyModuleName 			= "strategy";
	 final String startegyModule4ProbabilityPramName= "ModuleProbability_4";
	 final double startegyModuleProbabilityValue= 0.1;
	 final String startegyModule4ParamName	= "Module_4";
//	private final String startegyModule4Value		= "ChangeLegMode"; // use from PlanStrategyRegistrar instead.  kai, may'13
	 final String networkParamName			= "inputNetworkFile";
	 final String controlerFirstIterationPramName = "firstIteration";
	 final String controlerLastIterationPramName = "lastIteration";
	 final String activityType0ParamName		= "activityType_0"; 
	 final String activityTypicalDuration0ParamName= "activityTypicalDuration_0";
	 final String activityTypicalDuration0Value= "10:00:00";
	 final String activityType1ParamName		= "activityType_1";
	 final String activityTypicalDuration1ParamName= "activityTypicalDuration_1";
	 final String activityTypicalDuration1Value = "10:00:00";
	 final String activityOpeningTime1ParamName = "activityOpeningTime_1";
	 final String activityOpeningTime1Value 	= "10:00:00";
	 final String activityLatestStartTime1ParamName = "activityLatestStartTime_1";
	 final String activityLatestStartTime1Value = "10:00:00";
	 final String plansPramName 				= "inputPlansFile";
	 final String qsimNumberOfThreadsPramName	= "numberOfThreads";
	 final int  qsimNumberOfThreadsValue		= 3;
	 final String flowCapacityFactorParamName	= "flowCapacityFactor";
	 final double flowCapacityFactorValue		= 0.15;
	 final String storageCapacityFactorParamName= "storageCapacityFactor";
	 final double storageCapacityFactorValue	= 0.2;
	 final String stuckTimeParamName			= "stuckTime";
	 final double stuckTimeValue				= 9.;
	 final String endTimeParamName			= "endTime";
	 final String endTimeValue				= "30:00:00";
	 final String beelineDistanceFactorParamName= "beelineDistanceFactor";
	 final double beelineDistanceFactorValue	= 1.5;
	 final String teleportedModeSpeedWalkParamName = "teleportedModeSpeed_walk";
	 final double teleportedModeSpeedWalkValue= 1.4;
	 final String teleportedModeSpeedBikeParamName = "teleportedModeSpeed_bike";
	 final double teleportedModeSpeedBikeValue= 4.0;
	 final String teleportedModeSpeedPtParamName= "teleportedModeSpeed_pt";
	 final double teleportedModeSpeedPtValue	= 7.0;

	private String networkInputFileName;

	private long lastIteration;

	private String inputPlansFileName;

	private long firstIteration = 0 ;

	private String dummyPath;

	private String matsim4opusOutput;
	
	/**
	 * constructor
	 * 
	 * @param startMode distinguishes between cold, warm and hot start
	 * @param path gives the path, were the generated config (and other files) should be stored
	 */
	public CreateTestExternalMATSimConfig(final int startMode, String path){
		this.dummyPath = path ;
		this.matsim4opusOutput = path ;
		this.urbanSimZoneShapefileLocationDistribution = path + DUMMY_FILE_2;
		this.ptStops 			= path + DUMMY_FILE_2;
		this.usePtStops 		= "FALSE";
		this.ptTravelTimes 		= path + DUMMY_FILE_2;
		this.ptTravelDistances 	= path + DUMMY_FILE_2;
		this.useTravelTimesAndDistances = "FALSE";
		this.networkInputFileName = path + DUMMY_FILE_2 ;
		this.lastIteration     	= 100 ;
		this.inputPlansFileName		= path + DUMMY_FILE_2;
	}

	/**
	 * generates the external MATSim config file with the specified parameter settings
	 */
	public String generateMATSimConfig(){
		
		// creating an empty MASim config
		Config config = new Config();
		
		// !! note that the modules are deliberately anonymous (i.e. not typed) since we test the effect of incomplete entries
		// (i.e. config groups that are not fully specified in the file).  [[Not sure why this is needed??]]
		
		// matsim4urbansimParameter module

		ConfigGroup ippcm = config.createModule( MatrixBasedPtRouterConfigGroup.GROUP_NAME ) ;
		ippcm.addParam(MatrixBasedPtRouterConfigGroup.PT_STOPS, this.ptStops);
		ippcm.addParam(MatrixBasedPtRouterConfigGroup.USING_PT_STOPS, this.usePtStops);
		ippcm.addParam(MatrixBasedPtRouterConfigGroup.PT_TRAVEL_TIMES, this.ptTravelTimes);
		ippcm.addParam(MatrixBasedPtRouterConfigGroup.PT_TRAVEL_DISTANCES, this.ptTravelDistances);
		ippcm.addParam(MatrixBasedPtRouterConfigGroup.PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH, this.useTravelTimesAndDistances);

		ConfigGroup acm = config.createModule( AccessibilityConfigGroup.GROUP_NAME ) ;
		acm.addParam(AccessibilityConfigGroup.TIME_OF_DAY, this.timeOfDay + "");
		
		// changeLegMode module
		ConfigGroup changeLegModeModule = config.createModule(changeLegModeModuleName);
		changeLegModeModule.addParam(changeLegModeParamName, changeLegModeValue);
		
		// strategy module
		ConfigGroup strategyModule = config.createModule(strategyModuleName);
		strategyModule.addParam(startegyModule4ProbabilityPramName, startegyModuleProbabilityValue + "");
		strategyModule.addParam(startegyModule4ParamName, DefaultPlanStrategiesModule.DefaultStrategy.ChangeLegMode.toString() );
		
		// generating already existing MATSim4UrbanSim entries in external MATsim config
		
		// network module
		ConfigGroup networkModule = config.createModule(NetworkConfigGroup.GROUP_NAME);
		networkModule.addParam(networkParamName, this.getNetworkInputFileName() );
		
		// controler module 
		ConfigGroup contolerModule = config.createModule(ControlerConfigGroup.GROUP_NAME);
		contolerModule.addParam(controlerFirstIterationPramName, Long.toString(getFirstIteration()) );
		contolerModule.addParam(controlerLastIterationPramName, Long.toString(lastIteration) );
		
		// plan calc score module 
		ConfigGroup planCalcScoreModule = config.createModule(PlanCalcScoreConfigGroup.GROUP_NAME);
		planCalcScoreModule.addParam(activityType0ParamName, "home");
		planCalcScoreModule.addParam(activityTypicalDuration0ParamName, activityTypicalDuration0Value);
		planCalcScoreModule.addParam(activityType1ParamName, "work");
		planCalcScoreModule.addParam(activityTypicalDuration1ParamName, activityTypicalDuration1Value);
		planCalcScoreModule.addParam(activityOpeningTime1ParamName, activityTypicalDuration1Value);
		planCalcScoreModule.addParam(activityLatestStartTime1ParamName, activityLatestStartTime1Value);
		
		// plans module
		ConfigGroup plansModule = config.createModule(PlansConfigGroup.GROUP_NAME);
		plansModule.addParam(plansPramName, inputPlansFileName);
		
		// qsim module
		ConfigGroup qsimModule = config.createModule(QSimConfigGroup.GROUP_NAME);
		qsimModule.addParam(qsimNumberOfThreadsPramName, qsimNumberOfThreadsValue + "");
		qsimModule.addParam(flowCapacityFactorParamName, flowCapacityFactorValue + "");
		qsimModule.addParam(storageCapacityFactorParamName, storageCapacityFactorValue + "");
		qsimModule.addParam(stuckTimeParamName, stuckTimeValue + "");
		qsimModule.addParam(endTimeParamName, endTimeValue);
		
		// plan calc route module
		ConfigGroup planCalcRouteModule = config.createModule(PlansCalcRouteConfigGroup.GROUP_NAME);
		planCalcRouteModule.addParam(beelineDistanceFactorParamName, beelineDistanceFactorValue + "");
		planCalcRouteModule.addParam(teleportedModeSpeedWalkParamName, teleportedModeSpeedWalkValue + "");
		planCalcRouteModule.addParam(teleportedModeSpeedBikeParamName, teleportedModeSpeedBikeValue + "");
		planCalcRouteModule.addParam(teleportedModeSpeedPtParamName, teleportedModeSpeedPtValue + "");

		return writeConfigFile(config);
	}
	
	/**
	 * generates the external MATSim config file with the specified parameter settings
	 */
	public String generateMinimalMATSimConfig(){
		
		// creating an empty MASim config
		Config config = new Config();
		
		// !! note that the modules are deliberately anonymous (i.e. not typed) since we test the effect of incomplete entries !!
		
		// improved pseudo pt:
		ConfigGroup ippcm = config.createModule( MatrixBasedPtRouterConfigGroup.GROUP_NAME ) ;
		ippcm.addParam(MatrixBasedPtRouterConfigGroup.PT_STOPS, this.ptStops);
		ippcm.addParam(MatrixBasedPtRouterConfigGroup.USING_PT_STOPS, "tRue" );
		ippcm.addParam(MatrixBasedPtRouterConfigGroup.PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH, this.useTravelTimesAndDistances);

		// changeLegMode module
		ConfigGroup changeLegModeModule = config.createModule(changeLegModeModuleName);
		changeLegModeModule.addParam(changeLegModeParamName, changeLegModeValue);
		
		return writeConfigFile(config);
	}
	
	/**
	 * writes the external MATSim confing at the specified place
	 * 
	 * @param config in MATSim format
	 * @return location of the written external config file
	 * @throws UncheckedIOException
	 */
	String writeConfigFile(Config config) throws UncheckedIOException {
		try {
			String destination = this.dummyPath + "/test_external_config.xml";	
			(new ConfigWriter(config)).write(destination);
			
			return destination;
			
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}
	
	
	public static AccessibilityConfigGroup getAccessibilityParameterConfig(Config config) {
		return ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class ) ;
	}
	
	public static M4UControlerConfigModuleV3 getMATSim4UrbaSimControlerConfig(Config config) {
		return ConfigUtils.addOrGetModule(config, M4UControlerConfigModuleV3.GROUP_NAME, M4UControlerConfigModuleV3.class );
	}
	
	public static UrbanSimParameterConfigModuleV3 getUrbanSimParameterConfig(Config config) {
		return ConfigUtils.addOrGetModule(config, UrbanSimParameterConfigModuleV3.GROUP_NAME, UrbanSimParameterConfigModuleV3.class );
	}

	String getNetworkInputFileName() {
		return networkInputFileName;
	}

	long getLastIteration() {
		return lastIteration;
	}

	long getFirstIteration() {
		return firstIteration;
	}

	String getInputPlansFileName() {
		return inputPlansFileName;
	}

	void setInputPlansFileName(String inputPlansFileName) {
		this.inputPlansFileName = inputPlansFileName;
	}

	String getMatsim4opusOutput() {
		return matsim4opusOutput;
	}

}
