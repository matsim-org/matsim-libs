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
package org.matsim.contrib.matsim4opus.utils;

import java.math.BigInteger;

import org.matsim.contrib.matsim4opus.config.AccessibilityConfigModule;
import org.matsim.contrib.matsim4opus.config.M4UConfigUtils;
import org.matsim.contrib.matsim4opus.config.MATSim4UrbanSimControlerConfigModuleV3;
import org.matsim.contrib.matsim4opus.config.UrbanSimParameterConfigModuleV3;
import org.matsim.contrib.matsim4opus.utils.io.TempDirectoryUtil;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansCalcRouteConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.controler.PlanStrategyRegistrar;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * @author thomas
 *
 */
public class CreateTestExternalMATSimConfig extends CreateTestMATSimConfig{
	
	public static final String DUMMY_FILE_2 = "/dummy2.xml";
	
	public final int timeOfDay 						= 10000;
//	public final double betaBikeTravelTime 			= -12.;
//	public final double betaBikeTravelTimePower2 	= 0.;
//	public final double betaBikeLnTravelTime		= 0.;
//	public final double betaBikeTravelDistance		= 0.;
//	public final double betaBikeTravelDistancePower2= 0.;
//	public final double betaBikeLnTravelDistance	= 0.;
//	public final double betaBikeTravelCost			= 0.;
//	public final double betaBikeTravelCostPower2	= 0.;
//	public final double betaBikeLnTravelCost		= 0.;
//	public final double betaPtTravelTime			= -12.;
//	public final double betaPtTravelTimePower2		= 0.;
//	public final double betaPtLnTravelTime			= 0.;
//	public final double betaPtTravelDistance		= 0.;
//	public final double betaPtTravelDistancePower2	= 0.;
//	public final double betaPtLnTravelDistance		= 0.;
//	public final double betaPtTravelCost			= 0.;
//	public final double betaPtTravelCostPower2		= 0.;
//	public final double betaPtLnTravelCost			= 0.;
	public final String urbanSimZoneShapefileLocationDistribution;
	public final String ptStops;
	public final String usePtStops;
	public final String ptTravelTimes;
	public final String ptTravelDistances;
	public final String useTravelTimesAndDistances;
	public final String changeLegModeModuleName 	= "changeLegMode";
	public final String changeLegModeParamName		= "modes";
	public final String changeLegModeValue			= "car,pt";
	public final String strategyModuleName 			= "strategy";
	public final String startegyModule4ProbabilityPramName= "ModuleProbability_4";
	public final double startegyModuleProbabilityValue= 0.1;
	public final String startegyModule4ParamName	= "Module_4";
//	private final String startegyModule4Value		= "ChangeLegMode"; // use from PlanStrategyRegistrar instead.  kai, may'13
	public final String networkParamName			= "inputNetworkFile";
	public final String controlerFirstIterationPramName = "firstIteration";
	public final String controlerLastIterationPramName = "lastIteration";
	public final String activityType0ParamName		= "activityType_0"; 
	public final String activityTypicalDuration0ParamName= "activityTypicalDuration_0";
	public final String activityTypicalDuration0Value= "10:00:00";
	public final String activityType1ParamName		= "activityType_1";
	public final String activityTypicalDuration1ParamName= "activityTypicalDuration_1";
	public final String activityTypicalDuration1Value = "10:00:00";
	public final String activityOpeningTime1ParamName = "activityOpeningTime_1";
	public final String activityOpeningTime1Value 	= "10:00:00";
	public final String activityLatestStartTime1ParamName = "activityLatestStartTime_1";
	public final String activityLatestStartTime1Value = "10:00:00";
	public final String plansPramName 				= "inputPlansFile";
	public final String qsimNumberOfThreadsPramName	= "numberOfThreads";
	public final int  qsimNumberOfThreadsValue		= 3;
	public final String flowCapacityFactorParamName	= "flowCapacityFactor";
	public final double flowCapacityFactorValue		= 0.15;
	public final String storageCapacityFactorParamName= "storageCapacityFactor";
	public final double storageCapacityFactorValue	= 0.2;
	public final String stuckTimeParamName			= "stuckTime";
	public final double stuckTimeValue				= 9.;
	public final String endTimeParamName			= "endTime";
	public final String endTimeValue				= "30:00:00";
	public final String beelineDistanceFactorParamName= "beelineDistanceFactor";
	public final double beelineDistanceFactorValue	= 1.5;
	public final String teleportedModeSpeedWalkParamName = "teleportedModeSpeed_walk";
	public final double teleportedModeSpeedWalkValue= 1.4;
	public final String teleportedModeSpeedBikeParamName = "teleportedModeSpeed_bike";
	public final double teleportedModeSpeedBikeValue= 4.0;
	public final String teleportedModeSpeedPtParamName= "teleportedModeSpeed_pt";
	public final double teleportedModeSpeedPtValue	= 7.0;
	
	/**
	 * constructor
	 * 
	 * @param startMode distinguishes between cold, warm and hot start
	 * @param path gives the path, were the generated config (and other files) should be stored
	 */
	public CreateTestExternalMATSimConfig(final int startMode, String path){
		super(startMode, path);
		
		this.urbanSimZoneShapefileLocationDistribution = path + DUMMY_FILE_2;
		this.ptStops 			= path + DUMMY_FILE_2;
		this.usePtStops 		= "FALSE";
		this.ptTravelTimes 		= path + DUMMY_FILE_2;
		this.ptTravelDistances 	= path + DUMMY_FILE_2;
		this.useTravelTimesAndDistances = "FALSE";
		this.networkInputFile	= path + DUMMY_FILE_2;
		this.lastIteration     	= new BigInteger("100");
		this.inputPlansFile		= path + DUMMY_FILE_2;
	}

	/**
	 * generates the external MATSim config file with the specified parameter settings
	 */
	@Override
	public String generate(){
		
		// creating an empty MASim config
		Config config = new Config();
		
		// matsim4urbansimParameter module
		Module matsim4UrbanSimModule = config.createModule( M4UConfigUtils.MATSIM4URBANSIM_MODULE_EXTERNAL_CONFIG);
		matsim4UrbanSimModule.addParam(AccessibilityConfigModule.TIME_OF_DAY, this.timeOfDay + "");
		matsim4UrbanSimModule.addParam(M4UConfigUtils.URBANSIM_ZONE_SHAPEFILE_LOCATION_DISTRIBUTION, this.urbanSimZoneShapefileLocationDistribution);
		matsim4UrbanSimModule.addParam(M4UConfigUtils.PT_STOPS, this.ptStops);
		matsim4UrbanSimModule.addParam(M4UConfigUtils.PT_STOPS_SWITCH, this.usePtStops);
		matsim4UrbanSimModule.addParam(M4UConfigUtils.PT_TRAVEL_TIMES, this.ptTravelTimes);
		matsim4UrbanSimModule.addParam(M4UConfigUtils.PT_TRAVEL_DISTANCES, this.ptTravelDistances);
		matsim4UrbanSimModule.addParam(M4UConfigUtils.PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH, this.useTravelTimesAndDistances);
		// tnicolai: this are no default parameters anymore.
//		matsim4UrbanSimModule.addParam(MATSim4UrbanSimConfigurationConverterV4.BETA_BIKE_TRAVEL_TIME, this.betaBikeTravelTime + "");
//		matsim4UrbanSimModule.addParam(MATSim4UrbanSimConfigurationConverterV4.BETA_BIKE_TRAVEL_TIME_POWER2, this.betaBikeTravelTimePower2 + "");
//		matsim4UrbanSimModule.addParam(MATSim4UrbanSimConfigurationConverterV4.BETA_BIKE_LN_TRAVEL_TIME, this.betaBikeLnTravelTime + "");
//		matsim4UrbanSimModule.addParam(MATSim4UrbanSimConfigurationConverterV4.BETA_BIKE_TRAVEL_DISTANCE, this.betaBikeTravelDistance + "");
//		matsim4UrbanSimModule.addParam(MATSim4UrbanSimConfigurationConverterV4.BETA_BIKE_TRAVEL_DISTANCE_POWER2, this.betaBikeTravelDistancePower2 + "");
//		matsim4UrbanSimModule.addParam(MATSim4UrbanSimConfigurationConverterV4.BETA_BIKE_LN_TRAVEL_DISTANCE, this.betaBikeLnTravelDistance + "");
//		matsim4UrbanSimModule.addParam(MATSim4UrbanSimConfigurationConverterV4.BETA_BIKE_TRAVEL_MONETARY_COST, this.betaBikeTravelCost + "");
//		matsim4UrbanSimModule.addParam(MATSim4UrbanSimConfigurationConverterV4.BETA_BIKE_TRAVEL_MONETARY_COST_POWER2, this.betaBikeTravelCostPower2+ "");
//		matsim4UrbanSimModule.addParam(MATSim4UrbanSimConfigurationConverterV4.BETA_BIKE_LN_TRAVEL_MONETARY_COST, this.betaBikeLnTravelCost + "");
//		matsim4UrbanSimModule.addParam(MATSim4UrbanSimConfigurationConverterV4.BETA_PT_TRAVEL_TIME, this.betaPtTravelTime + "");
//		matsim4UrbanSimModule.addParam(MATSim4UrbanSimConfigurationConverterV4.BETA_PT_TRAVEL_TIME_POWER2, this.betaPtTravelTimePower2 + "");
//		matsim4UrbanSimModule.addParam(MATSim4UrbanSimConfigurationConverterV4.BETA_PT_LN_TRAVEL_TIME, this.betaPtLnTravelTime + "");
//		matsim4UrbanSimModule.addParam(MATSim4UrbanSimConfigurationConverterV4.BETA_PT_TRAVEL_DISTANCE, this.betaPtTravelDistance + "");
//		matsim4UrbanSimModule.addParam(MATSim4UrbanSimConfigurationConverterV4.BETA_PT_TRAVEL_DISTANCE_POWER2, this.betaPtTravelDistancePower2 + "");
//		matsim4UrbanSimModule.addParam(MATSim4UrbanSimConfigurationConverterV4.BETA_PT_LN_TRAVEL_DISTANCE, this.betaPtLnTravelDistance + "");
//		matsim4UrbanSimModule.addParam(MATSim4UrbanSimConfigurationConverterV4.BETA_PT_TRAVEL_MONETARY_COST, this.betaPtTravelCost + "");
//		matsim4UrbanSimModule.addParam(MATSim4UrbanSimConfigurationConverterV4.BETA_PT_TRAVEL_MONETARY_COST_POWER2, this.betaPtTravelCostPower2+ "");
//		matsim4UrbanSimModule.addParam(MATSim4UrbanSimConfigurationConverterV4.BETA_PT_LN_TRAVEL_MONETARY_COST, this.betaPtLnTravelCost + "");
		
		// changeLegMode module
		Module changeLegModeModule = config.createModule(changeLegModeModuleName);
		changeLegModeModule.addParam(changeLegModeParamName, changeLegModeValue);
		
		// strategy module
		Module strategyModule = config.createModule(strategyModuleName);
		strategyModule.addParam(startegyModule4ProbabilityPramName, startegyModuleProbabilityValue + "");
		strategyModule.addParam(startegyModule4ParamName, PlanStrategyRegistrar.Names.ChangeLegMode.toString() );
		
		// generating already existing MATSim4UrbanSim entries in external MATsim config
		
		// network module
		Module networkModule = config.createModule(NetworkConfigGroup.GROUP_NAME);
		networkModule.addParam(networkParamName, networkInputFile);
		
		// controler module 
		Module contolerModule = config.createModule(ControlerConfigGroup.GROUP_NAME);
		contolerModule.addParam(controlerFirstIterationPramName, firstIteration.toString());
		contolerModule.addParam(controlerLastIterationPramName, lastIteration.toString());
		
		// plan calc score module 
		Module planCalcScoreModule = config.createModule(PlanCalcScoreConfigGroup.GROUP_NAME);
		planCalcScoreModule.addParam(activityType0ParamName, activityType_0);
		planCalcScoreModule.addParam(activityTypicalDuration0ParamName, activityTypicalDuration0Value);
		planCalcScoreModule.addParam(activityType1ParamName, activityType_1);
		planCalcScoreModule.addParam(activityTypicalDuration1ParamName, activityTypicalDuration1Value);
		planCalcScoreModule.addParam(activityOpeningTime1ParamName, activityTypicalDuration1Value);
		planCalcScoreModule.addParam(activityLatestStartTime1ParamName, activityLatestStartTime1Value);
		
		// plans module
		Module plansModule = config.createModule(PlansConfigGroup.GROUP_NAME);
		plansModule.addParam(plansPramName, inputPlansFile);
		
		// qsim module
		Module qsimModule = config.createModule(QSimConfigGroup.GROUP_NAME);
		qsimModule.addParam(qsimNumberOfThreadsPramName, qsimNumberOfThreadsValue + "");
		qsimModule.addParam(flowCapacityFactorParamName, flowCapacityFactorValue + "");
		qsimModule.addParam(storageCapacityFactorParamName, storageCapacityFactorValue + "");
		qsimModule.addParam(stuckTimeParamName, stuckTimeValue + "");
		qsimModule.addParam(endTimeParamName, endTimeValue);
		
		// plan calc route module
		Module planCalcRouteModule = config.createModule(PlansCalcRouteConfigGroup.GROUP_NAME);
		planCalcRouteModule.addParam(beelineDistanceFactorParamName, beelineDistanceFactorValue + "");
		planCalcRouteModule.addParam(teleportedModeSpeedWalkParamName, teleportedModeSpeedWalkValue + "");
		planCalcRouteModule.addParam(teleportedModeSpeedBikeParamName, teleportedModeSpeedBikeValue + "");
		planCalcRouteModule.addParam(teleportedModeSpeedPtParamName, teleportedModeSpeedPtValue + "");

		return writeConfigFile(config);
	}
	
	/**
	 * generates the external MATSim config file with the specified parameter settings
	 */
	@Override
	public String generateMinimalConfig(){
		
		// creating an empty MASim config
		Config config = new Config();
		
		// matsim4urbansimParameter module
		Module matsim4UrbanSimModule = config.createModule( M4UConfigUtils.MATSIM4URBANSIM_MODULE_EXTERNAL_CONFIG);
		matsim4UrbanSimModule.addParam(M4UConfigUtils.PT_STOPS_SWITCH, "True");
		matsim4UrbanSimModule.addParam(M4UConfigUtils.PT_STOPS, this.ptStops);
		matsim4UrbanSimModule.addParam(M4UConfigUtils.PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH, this.useTravelTimesAndDistances);
		
		// changeLegMode module
		Module changeLegModeModule = config.createModule(changeLegModeModuleName);
		changeLegModeModule.addParam(changeLegModeParamName, changeLegModeValue);
		
//		// strategy module
//		Module strategyModule = config.createModule(strategyModuleName);
//		strategyModule.addParam(startegyModule4ProbabilityPramName, startegyModuleProbabilityValue + "");
//		strategyModule.addParam(startegyModule4ParamName, startegyModule4Value);
		// I don't think this is part of the "minimal" external config. kai, may'13
		
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
	
	
	public AccessibilityConfigModule getAccessibilityParameterConfig(Config config) {
		Module m = config.getModule(AccessibilityConfigModule.GROUP_NAME);
		if (m instanceof AccessibilityConfigModule) {
			return (AccessibilityConfigModule) m;
		}
		AccessibilityConfigModule apcm = new AccessibilityConfigModule();
		config.getModules().put(AccessibilityConfigModule.GROUP_NAME, apcm);
		return apcm;
	}
	
	public MATSim4UrbanSimControlerConfigModuleV3 getMATSim4UrbaSimControlerConfig(Config config) {
		Module m = config.getModule(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME);
		if (m instanceof MATSim4UrbanSimControlerConfigModuleV3) {
			return (MATSim4UrbanSimControlerConfigModuleV3) m;
		}
		MATSim4UrbanSimControlerConfigModuleV3 mccm = new MATSim4UrbanSimControlerConfigModuleV3(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME);
		config.getModules().put(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME, mccm);
		return mccm;
	}
	
	public UrbanSimParameterConfigModuleV3 getUrbanSimParameterConfig(Config config) {
		Module m = config.getModule(UrbanSimParameterConfigModuleV3.GROUP_NAME);
		if (m instanceof UrbanSimParameterConfigModuleV3) {
			return (UrbanSimParameterConfigModuleV3) m;
		}
		UrbanSimParameterConfigModuleV3 upcm = new UrbanSimParameterConfigModuleV3(UrbanSimParameterConfigModuleV3.GROUP_NAME);
		config.getModules().put(UrbanSimParameterConfigModuleV3.GROUP_NAME, upcm);
		return upcm;
	}
	
	
	/**
	 * for quick tests
	 * @param args
	 */
	public static void main(String args[]){
		
		String tmpPath = TempDirectoryUtil.createCustomTempDirectory("tmp");
		
		CreateTestExternalMATSimConfig configGenerator = new CreateTestExternalMATSimConfig(CreateTestExternalMATSimConfig.COLD_START, tmpPath);
		System.out.println("Config stored at: " + configGenerator.generateMinimalConfig() );
		
		TempDirectoryUtil.cleaningUpCustomTempDirectories();
	}

}
