/* *********************************************************************** *
 * project: org.matsim.*
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2013 by the members listed in the COPYING,        *
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

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.contrib.matsim4opus.constants.InternalConstants;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;

/**
 * @author nagel
 *
 */
public class Utils {

	/**
	 * printing GlobalConfigGroupSettings
	 * @param log TODO
	 * @param config TODO
	 */
	static void printGlobalConfigGroupSettings(Logger log, Config config) {
		if ( MATSim4UrbanSimConfigurationConverterV4.AFTER_END_OF_CASE_STUDIES ) {
			throw new RuntimeException("remove this method; regular config dump should be sufficient" ) ;
		}
		
		GlobalConfigGroup globalCG = (GlobalConfigGroup) config.getModule(GlobalConfigGroup.GROUP_NAME);
		
		log.info("GlobalConfigGroup settings:");
		log.info("Number of Threads: " + globalCG.getNumberOfThreads() + " ...");
	}

	/**
		 * printing UrbanSimParameterSettings
		 */
		static void printUrbanSimParameterSettings( Logger log, UrbanSimParameterConfigModuleV3 module) {
			
	//		UrbanSimParameterConfigModuleV3 module = this.getUrbanSimParameterConfig();
			
			log.info("UrbanSimParameter settings:");
			log.info("ProjectName: " + module.getProjectName() );
			log.info("PopulationSamplingRate: " + module.getPopulationSampleRate() );
			log.info("Year: " + module.getYear() ); 
			log.info("OPUS_HOME: " + InternalConstants.OPUS_HOME );
			log.info("OPUS_DATA_PATH: " + InternalConstants.OPUS_DATA_PATH );
			log.info("MATSIM_4_OPUS: " + InternalConstants.MATSIM_4_OPUS );
			log.info("MATSIM_4_OPUS_CONIG: " + InternalConstants.MATSIM_4_OPUS_CONFIG );
			log.info("MATSIM_4_OPUS_OUTPUT: " + InternalConstants.MATSIM_4_OPUS_OUTPUT );
			log.info("MATSIM_4_OPUS_TEMP: " + InternalConstants.MATSIM_4_OPUS_TEMP ); 
			log.info("MATSIM_4_OPUS_BACKUP: " + InternalConstants.MATSIM_4_OPUS_BACKUP );
			log.info("(Custom) Test Parameter: " + module.getTestParameter() );
			log.info("UsingShapefileLocationDistribution:" + module.usingShapefileLocationDistribution());
			log.info("UrbanSimZoneShapefileLocationDistribution:" + module.getUrbanSimZoneShapefileLocationDistribution());
			log.info("RandomLocationDistributionRadiusForUrbanSimZone:" + module.getUrbanSimZoneRadiusLocationDistribution());
			log.info("Backing Up Run Data: " + module.isBackup() );
			log.info("Is Test Run: " + module.isTestRun() );
		}

	/**
		 * printing MATSim4UrbanSimControlerSettings
		 */
		static void printMATSim4UrbanSimControlerSettings( Logger log, MATSim4UrbanSimControlerConfigModuleV3 module ) {
			
	//		MATSim4UrbanSimControlerConfigModuleV3 module = getMATSim4UrbaSimControlerConfig();
			
			// view results
			log.info("MATSim4UrbanSimControler settings:");
			log.info("Compute Agent-performance: " + module.isAgentPerformance() );
			log.info("Compute Zone2Zone Impedance Matrix: " + module.isZone2ZoneImpedance() ); 
			log.info("Compute Zone-Based Accessibilities: " + module.isZoneBasedAccessibility() );
			log.info("Compute Parcel/Cell-Based Accessibilities (using ShapeFile): " + module.isCellBasedAccessibilityShapeFile() ); 
			log.info("Compute Parcel/Cell-Based Accessibilities (using Network Boundaries): " + module.isCellBasedAccessibilityNetwork() );
			log.info("Cell Size: " + module.getCellSizeCellBasedAccessibility() );
			log.info("Using (Custom) Network Boundaries: " + module.usingCustomBoundingBox() );
			log.info("Network Boundary (Top): " + module.getBoundingBoxTop() ); 
			log.info("Network Boundary (Left): " + module.getBoundingBoxLeft() ); 
			log.info("Network Boundary (Right): " + module.getBoundingBoxRight() ); 
			log.info("Network Boundary (Bottom): " + module.getBoundingBoxBottom() ); 
			log.info("Shape File: " + module.getShapeFileCellBasedAccessibility() );
			log.info("Time of day: " + module.getTimeOfDay() );
			log.info("Pt Stops Input File: " + module.getPtStopsInputFile());
			log.info("Pt Travel Times Input File: " + module.getPtTravelTimesInputFile());
			log.info("Pt travel Distances Input File: " + module.getPtTravelDistancesInputFile());
		}

	/**
		 * printing AccessibilityParameterSettings
		 */
		static void printAccessibilityParameterSettings(Logger log, AccessibilityParameterConfigModule module) {
			
	//		AccessibilityParameterConfigModule module = getAccessibilityParameterConfig();
			
			// display results
			log.info("AccessibilityParameter settings:");
			
			log.info("AccessibilityDestinationSamplingRate: " + module.getAccessibilityDestinationSamplingRate());
			log.info("Compute raw sum (not logsum): " + module.usingRawSumsWithoutLn() );
			log.info("Logit Scale Parameter: " + module.usingLogitScaleParameterFromMATSim() ); 
			
			log.info("BETA_CAR_TRAVEL_TIMES: " + module.getBetaCarTravelTime() );
			log.info("BETA_CAR_TRAVEL_TIMES_POWER: " + module.getBetaCarTravelTimePower2() );
			log.info("BETA_CAR_LN_TRAVEL_TIMES: " + module.getBetaCarLnTravelTime());
			log.info("BETA_CAR_TRAVEL_DISTANCE: " + module.getBetaCarTravelDistance() );
			log.info("BETA_CAR_TRAVEL_DISTANCE_POWER: " + module.getBetaCarTravelDistancePower2() );
			log.info("BETA_CAR_LN_TRAVEL_DISTANCE: " + module.getBetaCarLnTravelDistance() );
			log.info("BETA_CAR_TRAVEL_MONETARY_COSTS: " + module.getBetaCarTravelMonetaryCost() );
			log.info("BETA_CAR_TRAVEL_MONETARY_COSTS_POWER: " + module.getBetaCarTravelMonetaryCostPower2() );
			log.info("BETA_CAR_LN_TRAVEL_MONETARY_COSTS: " + module.getBetaCarLnTravelMonetaryCost());
			
			log.info("BETA_PT_TRAVEL_TIMES: " + module.getBetaPtTravelTime()  );
			log.info("BETA_PT_TRAVEL_TIMES_POWER: " + module.getBetaPtTravelTimePower2() );
			log.info("BETA_PT_LN_TRAVEL_TIMES: " + module.getBetaPtLnTravelTime() );
			log.info("BETA_PT_TRAVEL_DISTANCE: " + module.getBetaPtTravelDistance() );
			log.info("BETA_PT_TRAVEL_DISTANCE_POWER: " + module.getBetaPtTravelDistancePower2() );
			log.info("BETA_PT_LN_TRAVEL_DISTANCE: " + module.getBetaPtLnTravelDistance() );
			log.info("BETA_PT_TRAVEL_MONETARY_COSTS: " + module.getBetaPtTravelMonetaryCost() );
			log.info("BETA_PT_TRAVEL_MONETARY_COSTS_POWER: " + module.getBetaPtTravelMonetaryCostPower2() );
			log.info("BETA_PT_LN_TRAVEL_MONETARY_COSTS: " + module.getBetaPtLnTravelMonetaryCost() );
			
			log.info("BETA_BIKE_TRAVEL_TIMES: " + module.getBetaBikeTravelTime()  );
			log.info("BETA_BIKE_TRAVEL_TIMES_POWER: " + module.getBetaBikeTravelTimePower2() );
			log.info("BETA_BIKE_LN_TRAVEL_TIMES: " + module.getBetaBikeLnTravelTime() );
			log.info("BETA_BIKE_TRAVEL_DISTANCE: " + module.getBetaBikeTravelDistance() );
			log.info("BETA_BIKE_TRAVEL_DISTANCE_POWER: " + module.getBetaBikeTravelDistancePower2() );
			log.info("BETA_BIKE_LN_TRAVEL_DISTANCE: " + module.getBetaBikeLnTravelDistance() );
			log.info("BETA_BIKE_TRAVEL_MONETARY_COSTS: " + module.getBetaBikeTravelMonetaryCost() );
			log.info("BETA_BIKE_TRAVEL_MONETARY_COSTS_POWER: " + module.getBetaBikeTravelMonetaryCostPower2() );
			log.info("BETA_BIKE_LN_TRAVEL_MONETARY_COSTS: " + module.getBetaBikeLnTravelMonetaryCost() );
			
			log.info("BETA_WALK_TRAVEL_TIMES: " + module.getBetaWalkTravelTime()  );
			log.info("BETA_WALK_TRAVEL_TIMES_POWER: " + module.getBetaWalkTravelTimePower2() );
			log.info("BETA_WALK_LN_TRAVEL_TIMES: " + module.getBetaWalkLnTravelTime() );
			log.info("BETA_WALK_TRAVEL_DISTANCE: " + module.getBetaWalkTravelDistance() );
			log.info("BETA_WALK_TRAVEL_DISTANCE_POWER: " + module.getBetaWalkTravelDistancePower2() );
			log.info("BETA_WALK_LN_TRAVEL_DISTANCE: " + module.getBetaWalkLnTravelDistance() );
			log.info("BETA_WALK_TRAVEL_MONETARY_COSTS: " + module.getBetaWalkTravelMonetaryCost() );
			log.info("BETA_WALK_TRAVEL_MONETARY_COSTS_POWER: " + module.getBetaWalkTravelMonetaryCostPower2() );
			log.info("BETA_WALK_LN_TRAVEL_MONETARY_COSTS: " + module.getBetaWalkLnTravelMonetaryCost() );
		}

	/**
	 * printing NetworkConfigGroupSettings
	 * @param log TODO
	 * @param config TODO
	 */
	static void printNetworkConfigGroupSettings(Logger log, Config config) {
		if ( MATSim4UrbanSimConfigurationConverterV4.AFTER_END_OF_CASE_STUDIES ) {
			throw new RuntimeException("remove this method; regular config dump should be sufficient" ) ;
		}
		
		NetworkConfigGroup networkCG = (NetworkConfigGroup) config.getModule(NetworkConfigGroup.GROUP_NAME);
		
		log.info("NetworkConfigGroup settings:");
		log.info("Network: " + networkCG.getInputFile());
	}

	/**
	 * printing PlansConfigGroupSettings
	 * @param log TODO
	 * @param config TODO
	 */
	static void printPlansConfigGroupSettings(Logger log, Config config) {
		if ( MATSim4UrbanSimConfigurationConverterV4.AFTER_END_OF_CASE_STUDIES ) {
			throw new RuntimeException("remove this method; regular config dump should be sufficient" ) ;
		}
		
		PlansConfigGroup plansCG = (PlansConfigGroup) config.getModule(PlansConfigGroup.GROUP_NAME);
		
		log.info("PlansConfigGroup setting:");
		log.info("Input plans file set to: " + plansCG.getInputFile());
	}

	/**
	 * printing StrategyConfigGroupSettings
	 * @param log TODO
	 * @param config TODO
	 */
	static void printStrategyConfigGroupSettings(Logger log, Config config) {
		
		StrategyConfigGroup strategyCG = (StrategyConfigGroup) config.getModule(StrategyConfigGroup.GROUP_NAME);
		
		log.info("StrategyConfigGroup settings:");
		Iterator<StrategySettings> iteratorStrategyCG = strategyCG.getStrategySettings().iterator();
		while(iteratorStrategyCG.hasNext()){
			StrategySettings strategySettings = iteratorStrategyCG.next();
			log.info("Strategy_"+strategySettings.getId()+ ":" + strategySettings.getModuleName() + " Probability: " + strategySettings.getProbability() + " Disable After Itereation: " + strategySettings.getDisableAfter() ); 
		}
		log.info("Max agent plan memory size: " + strategyCG.getMaxAgentPlanMemorySize());
	}

	/**
	 * printing ControlerConfigGroupSetting
	 * @param log TODO
	 * @param config TODO
	 */
	static void printControlerConfigGroupSetting(Logger log, Config config) {
		if ( MATSim4UrbanSimConfigurationConverterV4.AFTER_END_OF_CASE_STUDIES ) {
			throw new RuntimeException("remove this method; regular config dump should be sufficient" ) ;
		}
		
		ControlerConfigGroup controlerCG = (ControlerConfigGroup) config.getModule(ControlerConfigGroup.GROUP_NAME);
		
		log.info("ControlerConfigGroup settings:");
		log.info("FirstIteration: " + controlerCG.getFirstIteration());
		log.info("LastIteration: " + controlerCG.getLastIteration());
		log.info("MATSim output directory: " +  controlerCG.getOutputDirectory());
		log.info("Mobsim: " + controlerCG.getMobsim());
	}

	/**
	 * printing PlanCalcScoreSettings
	 * @param log TODO
	 * @param config TODO
	 */
	static void printPlanCalcScoreSettings(Logger log, Config config) {
		
		ActivityParams homeActivity = config.planCalcScore().getActivityParams("home");
		ActivityParams workActivity = config.planCalcScore().getActivityParams("work");
		
		log.info("PlanCalcScore settings:");
		log.info("Activity_Type_0: " + homeActivity.getType() + " Typical Duration Activity_Type_0: " + homeActivity.getTypicalDuration());
		log.info("Activity_Type_1: " + workActivity.getType() + " Typical Duration Activity_Type_1: " + workActivity.getTypicalDuration());
		log.info("Opening Time Activity_Type_1: " + workActivity.getOpeningTime()); 
		log.info("Latest Start Time Activity_Type_1: " + workActivity.getLatestStartTime());
	}

	/**
	 * printing QSimConfigGroupSettings
	 * @param log TODO
	 * @param config TODO
	 */
	static void printQSimConfigGroupSettings(Logger log, Config config) {
		QSimConfigGroup qsimCG = config.getQSimConfigGroup();
		
		log.info("QSimConfigGroup settings:");
		log.info("Number of Threads: " + qsimCG.getNumberOfThreads());
		log.info("FlowCapFactor (= population sampling rate): "+ config.getQSimConfigGroup().getFlowCapFactor());
		log.info("StorageCapFactor: " + config.getQSimConfigGroup().getStorageCapFactor() );//+ " (with correction factor = " + storageCapCorrectionFactor + ")" );
		// log.info("RemoveStuckVehicles: " + (removeStuckVehicles?"True":"False") );
		log.info("StuckTime: " + config.getQSimConfigGroup().getStuckTime());
		log.info("End Time: " + qsimCG.getEndTime());
	}

	static final void checkConfigConsistencyAndWriteToLog(Config config, final String message) {
		MATSim4UrbanSimConfigurationConverterV4.log.info(message);
		String newline = System.getProperty("line.separator");// use native line endings for logfile
		StringWriter writer = new StringWriter();
		new ConfigWriter(config).writeStream(new PrintWriter(writer), newline);
		MATSim4UrbanSimConfigurationConverterV4.log.info(newline + newline + writer.getBuffer().toString());
		MATSim4UrbanSimConfigurationConverterV4.log.info("Complete config dump done.");
		MATSim4UrbanSimConfigurationConverterV4.log.info("Checking consistency of config...");
		config.checkConsistency();
		MATSim4UrbanSimConfigurationConverterV4.log.info("Checking consistency of config done.");
	}

	/**
	 * printing PlanCalcRouteGroupSettings
	 * @param log TODO
	 * @param config TODO
	 */
	static void printPlanCalcRouteGroupSettings(Logger log, Config config) {
		log.info("PlanCalcRouteGroup settings:");							 
		log.info("Walk Speed: " + config.plansCalcRoute().getWalkSpeed() );
		log.info("Bike Speed: " + config.plansCalcRoute().getBikeSpeed() );
		log.info("Pt Speed: " + config.plansCalcRoute().getPtSpeed() );
		log.info("Beeline Distance Factor: " + config.plansCalcRoute().getBeelineDistanceFactor() );
	}

}
