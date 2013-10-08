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
package playground.tnicolai.matsim4opus.config;

import java.io.File;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.TransportMode;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.GlobalConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;

import playground.tnicolai.matsim4opus.constants.InternalConstants;
import playground.tnicolai.matsim4opus.matsim4urbansim.jaxbconfig2.ConfigType;
import playground.tnicolai.matsim4opus.matsim4urbansim.jaxbconfig2.Matsim4UrbansimType;
import playground.tnicolai.matsim4opus.matsim4urbansim.jaxbconfig2.MatsimConfigType;
import playground.tnicolai.matsim4opus.utils.ids.IdFactory;
import playground.tnicolai.matsim4opus.utils.io.Paths;

/**
 * @author thomas
 * 
 * improvements dec'11:
 * - adjusting flow- and storage capacities to population sample rate. The
 * storage capacity includes a fetch factor to avoid backlogs and network breakdown
 * for small sample rates.
 * 
 * improvements jan'12:
 * - initGlobalSettings sets the number of available processors in the 
 * 	GlobalConfigGroup to speed up MATSim computations. Before that only
 * 	2 processors were used even if there are more.
 * 
 * improvements feb'12:
 * - setting mutationrange = 2h for TimeAllocationMutator (this seems to 
 * shift the depature times ???)
 * 
 * improvements march'12:
 * - extended the MATSim4UrbanSim configuration, e.g. a standard MATSim config can be loaded
 * 
 * improvements aug'12
 * - extended the MATSim4UrbanSim configuration: 
 *   - added a switch to select between between radius or shape-file distribution of locations within a zone
 *   - added a field "project_name" of the UrbanSim scenario as an identifier
 *   - added a time-of-a-day parameter
 *   - added beta parameter for mode bike
 *   
 * improvements/changes oct'12
 * - switched to qsim
 * 
 * changes dec'12
 * - switched matsim4urbansim config v2 parameters from v3 are out sourced into external matsim config
 * - introducing pseudo pt (configurable via external MATSim config)
 * - introducing new strategy module "changeSingeLegMode" (configurable via external MATSim config)
 *
 */
public class MATSim4UrbanSimConfigurationConverterV4 {
	
	// logger
	private static final Logger log = Logger.getLogger(MATSim4UrbanSimConfigurationConverterV4.class);
	
	// MATSim config
	private Config config = null;
	// JAXB representation of matsim4urbansim config
	private MatsimConfigType matsimConfig = null;
	// MATSim4UrbanSim module from external MATSim config
	private Module matsim4UrbanSimModule = null;
	
	
	// module and param names for matsim4urbansim settings stored in an external MATSim config file
	private static final String MATSIM4URBANSIM_MODULE_EXTERNAL_CONFIG = "matsim4urbansimParameter";// module
	// parameter names in matsim4urbansimParameter module
	private static final String TIME_OF_DAY = "timeOfDay";								
	private static final String URBANSIM_ZONE_SHAPEFILE_LOCATION_DISTRIBUTION = "urbanSimZoneShapefileLocationDistribution";
	private static final String BETA_BIKE_TRAVEL_TIME = "betaBikeTravelTime";
	private static final String BETA_BIKE_TRAVEL_TIME_POWER2 = "betaBikeTravelTimePower2";
	private static final String BETA_BIKE_LN_TRAVEL_TIME = "betaBikeLnTravelTime";
	private static final String BETA_BIKE_TRAVEL_DISTANCE = "betaBikeTravelDistance";
	private static final String BETA_BIKE_TRAVEL_DISTANCE_POWER2 = "betaBikeTravelDistancePower2";
	private static final String BETA_BIKE_LN_TRAVEL_DISTANCE = "betaBikeLnTravelDistance";
	private static final String BETA_BIKE_TRAVEL_COST = "betaBikeTravelCost";
	private static final String BETA_BIKE_TRAVEL_COST_POWER2 = "betaBikeTravelCostPower2";
	private static final String BETA_BIKE_LN_TRAVEL_COST = "betaBikeLnTravelCost";
	
	/**
	 * constructor
	 * 
	 * @param config stores MATSim parameters
	 * @param matsimConfig stores all parameters from matsim4urbansim config ( generated by UrbanSim )
	 */
	public MATSim4UrbanSimConfigurationConverterV4(final MatsimConfigType matsimConfig){
		this.config = null;
		this.matsimConfig = matsimConfig;	
	}
	
	/**
	 * constructor
	 * 
	 * @param config stores MATSim parameters
	 * @param matsimConfiFile path to matsim config file
	 */
	public MATSim4UrbanSimConfigurationConverterV4(final String matsimConfiFile){
		this.config = null;
		this.matsimConfig = unmarschal(matsimConfiFile); // loading and initializing MATSim config		
	}
	
	/**
	 * loading, validating and initializing MATSim config.
	 */
	MatsimConfigType unmarschal(String matsimConfigFile){
		
		// JAXBUnmaschal reads the UrbanSim generated MATSim config, validates it against
		// the current xsd (checks e.g. the presents and data type of parameter) and generates
		// an Java object representing the config file.
		JAXBUnmaschalV2 unmarschal = new JAXBUnmaschalV2( matsimConfigFile );
		
		MatsimConfigType matsimConfig = null;
		
		// binding the parameter from the MATSim Config into the JAXB data structure
		if( (matsimConfig = unmarschal.unmaschalMATSimConfig()) == null){
			log.error("Unmarschalling failed. SHUTDOWN MATSim!");
			System.exit(InternalConstants.UNMARSCHALLING_FAILED);
		}
		return matsimConfig;
	}
	
	/**
	 * Transferring all parameter from matsim4urbansim config to internal MATSim config/scenario
	 * @return boolean true if initialization successful
	 */
	public boolean init(){
		
		try{
			// get root elements from JAXB matsim4urbansim config object
			ConfigType matsimParameter = matsimConfig.getConfig();
			Matsim4UrbansimType matsim4UrbanSimParameter = matsimConfig.getMatsim4Urbansim();
			
			// init standard MATSim config first, this may be overwritten from MATSim4UrbanSim config
			initExternalMATSimConfig(matsimParameter);
			
			// MATSim4UrbanSim config initiation
			initGlobalSettings();
			initMATSim4UrbanSimParameter(matsim4UrbanSimParameter);
			initNetwork(matsimParameter);
			initInputPlansFile(matsimParameter);
			initControler(matsimParameter);
			initPlanCalcScore(matsimParameter);
			initStrategy(matsimParameter);
			initPlanCalcRoute();
			initQSim();
			
		}catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return true;
	}
	
	/**
	 * This initializes MATSim from an external, standard config file
	 * @param matsimParameter an external MATSim config file 
	 */
	private void initExternalMATSimConfig(ConfigType matsimParameter){
		String externalMATSimConfig = matsimParameter.getMatsimConfig().getInputFile();
		if(externalMATSimConfig != null && Paths.pathExsits(externalMATSimConfig)){
			log.info("Initializing MATSim settings from external MATSim config: " + externalMATSimConfig);
			config = ConfigUtils.loadConfig( externalMATSimConfig.trim() );
			log.info("NOTE: Some external config settigs can be overwritten by the travel model configuration settings in UrbanSim !");
			
			// loading additional matsim4urbansim parameter settings from external config
			matsim4UrbanSimModule = setModule(MATSIM4URBANSIM_MODULE_EXTERNAL_CONFIG, externalMATSimConfig);
		}
		else{
			log.info("Creating an empty MATSim scenario.");
			config = ConfigUtils.createConfig();
		}
		
		// changing checking level to ABORT for vsp defaults means 
		// that MATSim will stop if any parameter settings differ from that defaults
		Module vsp = config.getModule("vspExperimental");
		vsp.addParam("vspDefaultsCheckingLevel", VspExperimentalConfigGroup.ABORT);
		
		log.info("...done!");
	}

	/**
	 * @param externalMATSimConfig
	 */
	private Module setModule(String moduleName, String externalMATSimConfig) {
		Module module = config.getModule(moduleName);
		if(module == null)
			log.info("No \""+ moduleName + "\" settings found in " + externalMATSimConfig);
		else
			log.info("Found \""+ moduleName + "\" settings in " + externalMATSimConfig);
		return module;
	}
	
	/**
	 * Determines and sets available processors into MATSim config
	 */
	private void initGlobalSettings(){
		log.info("Setting GlobalConfigGroup to config...");
		GlobalConfigGroup globalCG = (GlobalConfigGroup) config.getModule(GlobalConfigGroup.GROUP_NAME);
		globalCG.setNumberOfThreads(Runtime.getRuntime().availableProcessors());
		log.info("GlobalConfigGroup settings:");
		log.info("Number of Threads: " + Runtime.getRuntime().availableProcessors() + " ...");
		log.info("... done!");
	}
	
	/**
	 * Preparing MATSim4UrbanSim to store UrbanSim settings in internal modules.
	 * 
	 * @param matsim4UrbanSimParameter
	 */
	private void initMATSim4UrbanSimParameter(Matsim4UrbansimType matsim4UrbanSimParameter){
		log.info("Setting MATSim4UrbanSim to config...");
		
		initUrbanSimParameter(matsim4UrbanSimParameter);
		initMATSim4UrbanSimControler(matsim4UrbanSimParameter);
		initAccessibilityParameter(matsim4UrbanSimParameter);
		
		log.info("... done!");
	}
	
	/**
	 * store UrbanSimParameter
	 * 
	 * @param matsim4UrbanSimParameter
	 */
	private void initUrbanSimParameter(Matsim4UrbansimType matsim4UrbanSimParameter){
		
		// get every single matsim4urbansim/urbansimParameter
		String projectName 		= ""; // not needed anymore dec'12
		double populationSamplingRate = matsim4UrbanSimParameter.getUrbansimParameter().getPopulationSamplingRate();
		int year 				= matsim4UrbanSimParameter.getUrbansimParameter().getYear().intValue();
		
		boolean useShapefileLocationDistribution = false;
		String urbanSimZoneShapefileLocationDistribution = null;
		double randomLocationDistributionRadiusForUrbanSimZone = matsim4UrbanSimParameter.getUrbansimParameter().getRandomLocationDistributionRadiusForUrbanSimZone();
		
		if(matsim4UrbanSimModule != null)
			urbanSimZoneShapefileLocationDistribution = matsim4UrbanSimModule.getValue(URBANSIM_ZONE_SHAPEFILE_LOCATION_DISTRIBUTION);
		log.info("This message affects UrbanSim ZONE applications only:");
		if(urbanSimZoneShapefileLocationDistribution != null && Paths.pathExsits(urbanSimZoneShapefileLocationDistribution)){
			useShapefileLocationDistribution = true;
			log.info("Found a zone shape file: " + urbanSimZoneShapefileLocationDistribution);
			log.info("This activates the distribution of persons within a zone using the zone boundaries of this shape file."); 
		}
		else{
			log.info("Persons are distributed within a zone using the zone centroid and a radius of " + randomLocationDistributionRadiusForUrbanSimZone + " meter.");
			log.info("In order to use exact zone boundaries for your sceanrio provide a zone shape file and enter the file location in the external MATSim config file as follows:");
			log.info("<module name=\"matsim4urbansimParameter\" >");
			log.info("<param name=\"urbanSimZoneShapefileLocationDistribution\" value=\"/path/to/shapeFile\" />");
			log.info("</module>");
		}

		String opusHome 		= Paths.checkPathEnding( matsim4UrbanSimParameter.getUrbansimParameter().getOpusHome() );
		String opusDataPath 	= Paths.checkPathEnding( matsim4UrbanSimParameter.getUrbansimParameter().getOpusDataPath() );
		String matsim4Opus 		= Paths.checkPathEnding( matsim4UrbanSimParameter.getUrbansimParameter().getMatsim4Opus() );
		String matsim4OpusConfig= Paths.checkPathEnding( matsim4UrbanSimParameter.getUrbansimParameter().getMatsim4OpusConfig() );
		String matsim4OpusOutput= Paths.checkPathEnding( matsim4UrbanSimParameter.getUrbansimParameter().getMatsim4OpusOutput() );
		String matsim4OpusTemp 	= Paths.checkPathEnding( matsim4UrbanSimParameter.getUrbansimParameter().getMatsim4OpusTemp() );
		String matsim4OpusBackup= Paths.checkPathEnding( matsim4UrbanSimParameter.getUrbansimParameter().getMatsim4Opus() ) + Paths.checkPathEnding( "backup" );
		boolean isTestRun 		= matsim4UrbanSimParameter.getUrbansimParameter().isIsTestRun();
		boolean backupRunData 	= matsim4UrbanSimParameter.getUrbansimParameter().isBackupRunData();
		String testParameter 	= matsim4UrbanSimParameter.getUrbansimParameter().getTestParameter();
		
		// // set parameter in module 
		UrbanSimParameterConfigModuleV3 module = this.getUrbanSimParameterConfig();
		module.setProjectName(projectName);
		// module.setSpatialUnitFlag(spatialUnit); // tnicolai not needed anymore dec'12
		module.setPopulationSampleRate(populationSamplingRate);
		module.setYear(year);
		module.setOpusHome(opusHome);
		module.setOpusDataPath(opusDataPath);
		module.setMATSim4Opus(matsim4Opus);
		module.setMATSim4OpusConfig(matsim4OpusConfig);
		module.setMATSim4OpusOutput(matsim4OpusOutput);
		module.setMATSim4OpusTemp(matsim4OpusTemp);
		module.setMATSim4OpusBackup(matsim4OpusBackup);
		module.setTestParameter(testParameter);
		module.setUseShapefileLocationDistribution(useShapefileLocationDistribution);
		module.setUrbanSimZoneShapefileLocationDistribution(urbanSimZoneShapefileLocationDistribution);
		module.setUrbanSimZoneRadiusLocationDistribution(randomLocationDistributionRadiusForUrbanSimZone);
		module.setBackup(backupRunData);
		module.setTestRun(isTestRun);	
		
		// setting paths into constants structure
		InternalConstants.OPUS_HOME = module.getOpusHome();
		InternalConstants.OPUS_DATA_PATH = module.getOpusDataPath();
		InternalConstants.MATSIM_4_OPUS = module.getMATSim4Opus();
		InternalConstants.MATSIM_4_OPUS_CONFIG = module.getMATSim4OpusConfig();
		InternalConstants.MATSIM_4_OPUS_OUTPUT = module.getMATSim4OpusOutput();
		InternalConstants.MATSIM_4_OPUS_TEMP = module.getMATSim4OpusTemp();
		InternalConstants.MATSIM_4_OPUS_BACKUP = module.getMATSim4OpusBackup();
		
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
		log.info("UseShapefileLocationDistribution:" + module.isUseShapefileLocationDistribution());
		log.info("UrbanSimZoneShapefileLocationDistribution:" + module.getUrbanSimZoneShapefileLocationDistribution());
		log.info("RandomLocationDistributionRadiusForUrbanSimZone:" + module.getUrbanSimZoneRadiusLocationDistribution());
		log.info("Backing Up Run Data: " + module.isBackup() );
		log.info("Is Test Run: " + module.isTestRun() );
	}
	
	/**
	 * Setting 
	 * 
	 * @param matsim4UrbanSimParameter
	 */
	private void initMATSim4UrbanSimControler(Matsim4UrbansimType matsim4UrbanSimParameter){
		
		// get every single matsim4urbansim/matsim4urbansimContoler parameter
		boolean computeZone2ZoneImpedance 		= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isZone2ZoneImpedance();
		boolean computeAgentPerformanceFeedback	= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isAgentPerformance();
		boolean computeZoneBasedAccessibility 	= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isZoneBasedAccessibility();
		boolean computeCellBasedAccessibility	= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isCellBasedAccessibility();
		boolean computeCellBasedAccessibilityNetwork   = false;
		boolean computeCellbasedAccessibilityShapeFile = false;
		
		int cellSize 							= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getCellSizeCellBasedAccessibility().intValue();
		boolean useCustomBoundingBox			= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isUseCustomBoundingBox();
		double boundingBoxLeft					= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getBoundingBoxLeft();
		double boundingBoxBottom				= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getBoundingBoxBottom();
		double boundingBoxRight					= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getBoundingBoxRight();
		double boundingBoxTop					= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getBoundingBoxTop();
		String shapeFile						= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getShapeFileCellBasedAccessibility().getInputFile();

		// if cell-based accessibility is enabled, check whether a shapefile is given 
		if(computeCellBasedAccessibility){ 
			if(!Paths.pathExsits(shapeFile)){ // since no shape file found, accessibility computation is applied on the area covering the network
				computeCellBasedAccessibilityNetwork   = true;
				log.warn("No shape-file given or shape-file not found:" + shapeFile);
				log.warn("Instead the boundary of the road network is used to determine the area for which the accessibility computation is applied.");
			}
			else
				computeCellbasedAccessibilityShapeFile = true;
		}
		
		double timeOfDay						= 8 * 3600.;
		if(matsim4UrbanSimModule != null){
			try{
				double tmp = Double.parseDouble( matsim4UrbanSimModule.getValue(TIME_OF_DAY) );
				log.info("Found custom time of day for accessibility calculation: " + tmp );
				timeOfDay = tmp;
			} catch(Exception e){
				log.info("No time of day for accessibility calulation given or given time has wrong format.");
				log.info("By default, MATSim calulates accessibilities for the following time of day: " + timeOfDay );
				log.info("In order to use another time of day enter it in the external MATSim config file as follows:");
				log.info("<module name=\"matsim4urbansimParameter\" >");
				log.info("<param name=\"timeOfDay\" value=\"28800\" />");
				log.info("</module>");
			}
		}

		// set parameter in module 
		MATSim4UrbanSimControlerConfigModuleV3 module = getMATSim4UrbaSimControlerConfig();
		module.setAgentPerformance(computeAgentPerformanceFeedback);
		module.setZone2ZoneImpedance(computeZone2ZoneImpedance);
		module.setZoneBasedAccessibility(computeZoneBasedAccessibility);
		module.setCellBasedAccessibility(computeCellBasedAccessibility);
		module.setCellSizeCellBasedAccessibility(cellSize);
		module.setCellBasedAccessibilityShapeFile(computeCellbasedAccessibilityShapeFile);
		module.setCellBasedAccessibilityNetwork(computeCellBasedAccessibilityNetwork);
		module.setShapeFileCellBasedAccessibility(shapeFile);
		module.setUseCustomBoundingBox(useCustomBoundingBox);
		module.setBoundingBoxLeft(boundingBoxLeft);
		module.setBoundingBoxBottom(boundingBoxBottom);
		module.setBoundingBoxRight(boundingBoxRight);
		module.setBoundingBoxTop(boundingBoxTop);
		module.setTimeOfDay(timeOfDay);
		
		// view results
		log.info("MATSim4UrbanSimControler settings:");
		log.info("Compute Agent-performance: " + module.isAgentPerformance() );
		log.info("Compute Zone2Zone Impedance Matrix: " + module.isZone2ZoneImpedance() ); 
		log.info("Compute Zone-Based Accessibilities: " + module.isZoneBasedAccessibility() );
		log.info("Compute Parcel/Cell-Based Accessibilities (using ShapeFile): " + module.isCellBasedAccessibilityShapeFile() ); 
		log.info("Compute Parcel/Cell-Based Accessibilities (using Network Boundaries): " + module.isCellBasedAccessibilityNetwork() );
		log.info("Cell Size: " + module.getCellSizeCellBasedAccessibility() );
		log.info("Use (Custom) Network Boundaries: " + module.isUseCustomBoundingBox() );
		log.info("Network Boundary (Top): " + module.getBoundingBoxTop() ); 
		log.info("Network Boundary (Left): " + module.getBoundingBoxLeft() ); 
		log.info("Network Boundary (Right): " + module.getBoundingBoxRight() ); 
		log.info("Network Boundary (Bottom): " + module.getBoundingBoxBottom() ); 
		log.info("Shape File: " + module.getShapeFileCellBasedAccessibility() );
		log.info("Time of day: " + module.getTimeOfDay() );
	}
	
	private void initAccessibilityParameter(Matsim4UrbansimType matsim4UrbanSimParameter){
		
		// these are all parameter for the accessibility computation
		double logitScaleParameter,
		betaCarTT, betaCarTTPower, betaCarLnTT,		// car
		betaCarTD, betaCarTDPower, betaCarLnTD,
		betaCarTC, betaCarTCPower, betaCarLnTC,
		betaBikeTT, betaBikeTTPower, betaBikeLnTT,	// bike
		betaBikeTD, betaBikeTDPower, betaBikeLnTD,
		betaBikeTC, betaBikeTCPower, betaBikeLnTC,
		betaWalkTT, betaWalkTTPower, betaWalkLnTT,	// walk
		betaWalkTD, betaWalkTDPower, betaWalkLnTD,
		betaWalkTC, betaWalkTCPower, betaWalkLnTC;
		
		PlanCalcScoreConfigGroup planCalcScoreConfigGroup = config.planCalcScore();
		
		double accessibilityDestinationSamplingRate = matsim4UrbanSimParameter.getAccessibilityParameter().getAccessibilityDestinationSamplingRate();
		// these parameter define if the beta or logit_scale parameter are taken from MATSim or the config file
		boolean useMATSimLogitScaleParameter 	= matsim4UrbanSimParameter.getAccessibilityParameter().isUseLogitScaleParameterFromMATSim();
		boolean useMATSimCarParameter			= matsim4UrbanSimParameter.getAccessibilityParameter().isUseCarParameterFromMATSim();
		boolean useMATSimBikeParameter			= !useCustomMarginalUtilitiesBike(); // true if relevant settings in the external MATSim config is found
		boolean useMATSimWalkParameter			= matsim4UrbanSimParameter.getAccessibilityParameter().isUseWalkParameterFromMATSim();
		boolean useRawSum						= matsim4UrbanSimParameter.getAccessibilityParameter().isUseRawSumsWithoutLn();
		
		if(useMATSimLogitScaleParameter)
			logitScaleParameter = 1.;
		else
			logitScaleParameter = matsim4UrbanSimParameter.getAccessibilityParameter().getLogitScaleParameter();
		// tnicolai nov'12: decided with Kai that beta_brain (the accessibility scale parameter) should be 1 because of the pre-factor of the logsum term
		if(logitScaleParameter != 1.0){
			log.error("You are using a logit scale parameter != 1! The default is 1.");
			log.error("The accessibility calulation proceeds with a logit scale parameter = " + logitScaleParameter);
		}
		log.info("The logit scale parameter is used for accessibility computation only. It does not set scale parameter (beta brain) inside MATSim that is used for the traffic simulation!");
		
		if(useMATSimCarParameter){
			// usually travelling_utils are negative
			betaCarTT 	   	= planCalcScoreConfigGroup.getTraveling_utils_hr() - planCalcScoreConfigGroup.getPerforming_utils_hr(); // [utils/h]
			betaCarTTPower	= 0.;
			betaCarLnTT		= 0.;
			betaCarTD		= 0.;//mixing parameter makes no sense, thus disabled: planCalcScoreConfigGroup.getMarginalUtilityOfMoney() * planCalcScoreConfigGroup.getMonetaryDistanceCostRateCar(); 	// this is [utils/money * money/meter] = [utils/meter]
			betaCarTDPower	= 0.;																														// useful setting for MonetaryDistanceCostRateCar: 10cent/km (only fuel) or 
			betaCarLnTD		= 0.;																														// 80cent/km (including taxes, insurance ...)
			betaCarTC		= 0.;//planCalcScoreConfigGroup.getMarginalUtilityOfMoney(); // [utils/money], (no computation of money in MATSim implemented yet)
			betaCarTCPower	= 0.;
			betaCarLnTC		= 0.;
		}
		else{
			betaCarTT 	   	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaCarTravelTime();
			betaCarTTPower	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaCarTravelTimePower2();
			betaCarLnTT		= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaCarLnTravelTime();
			betaCarTD		= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaCarTravelDistance();
			betaCarTDPower	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaCarTravelDistancePower2();
			betaCarLnTD		= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaCarLnTravelDistance();
			betaCarTC		= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaCarTravelCost();
			betaCarTCPower	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaCarTravelCostPower2();
			betaCarLnTC		= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaCarLnTravelCost();
		}
		
		if(useMATSimBikeParameter){
			// usually travelling_utils are negative
			betaBikeTT		= planCalcScoreConfigGroup.getTravelingBike_utils_hr() - planCalcScoreConfigGroup.getPerforming_utils_hr(); // [utils/h]
			betaBikeTTPower	= 0.;
			betaBikeLnTT	= 0.;
			betaBikeTD		= 0.;//mixing parameter makes no sense, thus disabled: planCalcScoreConfigGroup.getMarginalUtlOfDistanceBike(); // [utils/meter]
			betaBikeTDPower	= 0.;												
			betaBikeLnTD	= 0.;
			betaBikeTC		= 0.;// [utils/money], not available in MATSim
			betaBikeTCPower	= 0.;
			betaBikeLnTC	= 0.;
		}
		else{
			betaBikeTT		= getValueAsDouble(BETA_BIKE_TRAVEL_TIME);
			betaBikeTTPower	= getValueAsDouble(BETA_BIKE_TRAVEL_TIME_POWER2);
			betaBikeLnTT	= getValueAsDouble(BETA_BIKE_LN_TRAVEL_TIME);
			betaBikeTD		= getValueAsDouble(BETA_BIKE_TRAVEL_DISTANCE);
			betaBikeTDPower	= getValueAsDouble(BETA_BIKE_TRAVEL_DISTANCE_POWER2);
			betaBikeLnTD	= getValueAsDouble(BETA_BIKE_LN_TRAVEL_DISTANCE);
			betaBikeTC		= getValueAsDouble(BETA_BIKE_TRAVEL_COST);
			betaBikeTCPower	= getValueAsDouble(BETA_BIKE_TRAVEL_COST_POWER2);
			betaBikeLnTC	= getValueAsDouble(BETA_BIKE_LN_TRAVEL_COST);
		}
		
		if(useMATSimWalkParameter){
			// usually travelling_utils are negative
			betaWalkTT		= planCalcScoreConfigGroup.getTravelingWalk_utils_hr() - planCalcScoreConfigGroup.getPerforming_utils_hr(); // [utils/h]
			betaWalkTTPower	= 0.;
			betaWalkLnTT	= 0.;
			betaWalkTD		= 0.;//mixing parameter makes no sense, thus disabled: planCalcScoreConfigGroup.getMarginalUtlOfDistanceWalk(); // [utils/meter]
			betaWalkTDPower	= 0.;												
			betaWalkLnTD	= 0.;
			betaWalkTC		= 0.;// [utils/money], not available in MATSim
			betaWalkTCPower	= 0.;
			betaWalkLnTC	= 0.;
		}
		else{
			betaWalkTT		= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaWalkTravelTime();
			betaWalkTTPower	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaWalkTravelTimePower2();
			betaWalkLnTT	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaWalkLnTravelTime();
			betaWalkTD		= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaWalkTravelDistance();
			betaWalkTDPower	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaWalkTravelDistancePower2();
			betaWalkLnTD	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaWalkLnTravelDistance();
			betaWalkTC		= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaWalkTravelCost();
			betaWalkTCPower	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaWalkTravelCostPower2();
			betaWalkLnTC	= matsim4UrbanSimParameter.getAccessibilityParameter().getBetaWalkLnTravelCost();
		}
		
		// set parameter in module 
		AccessibilityParameterConfigModule module = getAccessibilityParameterConfig();
		module.setAccessibilityDestinationSamplingRate(accessibilityDestinationSamplingRate);
		module.setUseLogitScaleParameterFromMATSim(useMATSimLogitScaleParameter);
		module.setUseRawSumsWithoutLn(useRawSum);
		module.setUseCarParameterFromMATSim(useMATSimCarParameter);
		module.setUseBikeParameterFromMATSim(useMATSimBikeParameter);
		module.setUseWalkParameterFromMATSim(useMATSimWalkParameter);
		module.setLogitScaleParameter(logitScaleParameter);
		module.setBetaCarTravelTime(betaCarTT);
		module.setBetaCarTravelTimePower2(betaCarTTPower);
		module.setBetaCarLnTravelTime(betaCarLnTT);
		module.setBetaCarTravelDistance(betaCarTD);
		module.setBetaCarTravelDistancePower2(betaCarTDPower);
		module.setBetaCarLnTravelDistance(betaCarLnTD);
		module.setBetaCarTravelCost(betaCarTC);
		module.setBetaCarTravelCostPower2(betaCarTCPower);
		module.setBetaCarLnTravelCost(betaCarLnTC);
		module.setBetaBikeTravelTime(betaBikeTT);
		module.setBetaBikeTravelTimePower2(betaBikeTTPower);
		module.setBetaBikeLnTravelTime(betaBikeLnTT);
		module.setBetaBikeTravelDistance(betaBikeTD);
		module.setBetaBikeTravelDistancePower2(betaBikeTDPower);
		module.setBetaBikeLnTravelDistance(betaBikeLnTD);
		module.setBetaBikeTravelCost(betaBikeTC);
		module.setBetaBikeTravelCostPower2(betaBikeTCPower);
		module.setBetaBikeLnTravelCost(betaBikeLnTC);
		module.setBetaWalkTravelTime(betaWalkTT);
		module.setBetaWalkTravelTimePower2(betaWalkTTPower);
		module.setBetaWalkLnTravelTime(betaWalkLnTT);
		module.setBetaWalkTravelDistance(betaWalkTD);
		module.setBetaWalkTravelDistancePower2(betaWalkTDPower);
		module.setBetaWalkLnTravelDistance(betaWalkLnTD);
		module.setBetaWalkTravelCost(betaWalkTC);
		module.setBetaWalkTravelCostPower2(betaWalkTCPower);
		module.setBetaWalkLnTravelCost(betaWalkLnTC);
		
		
		// display results
		log.info("AccessibilityParameter settings:");
		
		log.info("AccessibilityDestinationSamplingRate: " + module.getAccessibilityDestinationSamplingRate());
		log.info("Compute raw sum (not logsum): " + module.isUseRawSumsWithoutLn() );
		log.info("Logit Scale Parameter: " + module.isUseLogitScaleParameterFromMATSim() ); 
		
		log.info("BETA_CAR_TRAVEL_TIMES: " + module.getBetaCarTravelTime() );
		log.info("BETA_CAR_TRAVEL_TIMES_POWER: " + module.getBetaCarTravelTimePower2() );
		log.info("BETA_CAR_LN_TRAVEL_TIMES: " + module.getBetaCarLnTravelTime());
		log.info("BETA_CAR_TRAVEL_DISTANCE: " + module.getBetaCarTravelDistance() );
		log.info("BETA_CAR_TRAVEL_DISTANCE_POWER: " + module.getBetaCarTravelDistancePower2() );
		log.info("BETA_CAR_LN_TRAVEL_DISTANCE: " + module.getBetaCarLnTravelDistance() );
		log.info("BETA_CAR_TRAVEL_COSTS: " + module.getBetaCarTravelCost() );
		log.info("BETA_CAR_TRAVEL_COSTS_POWER: " + module.getBetaCarTravelCostPower2() );
		log.info("BETA_CAR_LN_TRAVEL_COSTS: " + module.getBetaCarLnTravelCost());
		
		log.info("BETA_BIKE_TRAVEL_TIMES: " + module.getBetaBikeTravelTime()  );
		log.info("BETA_BIKE_TRAVEL_TIMES_POWER: " + module.getBetaBikeTravelTimePower2() );
		log.info("BETA_BIKE_LN_TRAVEL_TIMES: " + module.getBetaBikeLnTravelTime() );
		log.info("BETA_BIKE_TRAVEL_DISTANCE: " + module.getBetaBikeTravelDistance() );
		log.info("BETA_BIKE_TRAVEL_DISTANCE_POWER: " + module.getBetaBikeTravelDistancePower2() );
		log.info("BETA_BIKE_LN_TRAVEL_DISTANCE: " + module.getBetaBikeLnTravelDistance() );
		log.info("BETA_BIKE_TRAVEL_COSTS: " + module.getBetaBikeTravelCost() );
		log.info("BETA_BIKE_TRAVEL_COSTS_POWER: " + module.getBetaBikeTravelCostPower2() );
		log.info("BETA_BIKE_LN_TRAVEL_COSTS: " + module.getBetaBikeLnTravelCost() );
		
		log.info("BETA_WALK_TRAVEL_TIMES: " + module.getBetaWalkTravelTime()  );
		log.info("BETA_WALK_TRAVEL_TIMES_POWER: " + module.getBetaWalkTravelTimePower2() );
		log.info("BETA_WALK_LN_TRAVEL_TIMES: " + module.getBetaWalkLnTravelTime() );
		log.info("BETA_WALK_TRAVEL_DISTANCE: " + module.getBetaWalkTravelDistance() );
		log.info("BETA_WALK_TRAVEL_DISTANCE_POWER: " + module.getBetaWalkTravelDistancePower2() );
		log.info("BETA_WALK_LN_TRAVEL_DISTANCE: " + module.getBetaWalkLnTravelDistance() );
		log.info("BETA_WALK_TRAVEL_COSTS: " + module.getBetaWalkTravelCost() );
		log.info("BETA_WALK_TRAVEL_COSTS_POWER: " + module.getBetaWalkTravelCostPower2() );
		log.info("BETA_WALK_LN_TRAVEL_COSTS: " + module.getBetaWalkLnTravelCost() );
	}
	
	/**
	 * setting MATSim network
	 * 
	 * NOTE: If the MATSim4UrbanSim network section contains a road network 
	 * this overwrites a previous network, e.g. from an external MATSim configuration
	 * 
	 * @param matsimParameter
	 */
	private void initNetwork(ConfigType matsimParameter){
		log.info("Setting NetworkConfigGroup to config...");
		String networkFile = matsimParameter.getNetwork().getInputFile();
		NetworkConfigGroup networkCG = (NetworkConfigGroup) config.getModule(NetworkConfigGroup.GROUP_NAME);
		// the MATSim4UrbanSim config contains a network file
		if(!networkFile.isEmpty()){
			// check if a network was already set by an external matsim config.
			if(networkCG.getInputFile() != null && !networkCG.getInputFile().isEmpty()){
				log.warn("A network file is already set by an external matsim config: " + networkCG.getInputFile());
				log.warn("It will be replaced by the MATSim4UrbanSim network file: " + networkFile);
				log.warn("To avoid this, keep the network parameter in MATSim4UrbanSim empty!");
			}
			// set MATSim4UrbanSim network
			networkCG.setInputFile( networkFile );
		}
		else if (networkCG.getInputFile() == null || networkCG.getInputFile().isEmpty()){
			log.error("Missing MATSim network! The network must be specified either directly in the MATSim4UrbanSim configuration or in an external MATSim configuration.");
			System.exit( InternalConstants.NO_MATSIM_NETWORK );
		}

		log.info("NetworkConfigGroup settings:");
		log.info("Network: " + networkCG.getInputFile());
		log.info("... done!");
	}
	
	/**
	 * setting input plans file (for warm/hot start)
	 * 
	 * @param matsimParameter
	 */
	private void initInputPlansFile(ConfigType matsimParameter){
		log.info("Checking for warm or hot start...");
		// get plans file for hot start
		String hotStart = matsimParameter.getHotStartPlansFile().getInputFile();
		// get plans file for warm start 
		String warmStart = matsimParameter.getInputPlansFile().getInputFile();
		
		MATSim4UrbanSimControlerConfigModuleV3 module = getMATSim4UrbaSimControlerConfig();
		
		// setting plans file as input
		if( !hotStart.equals("") &&
		  (new File(hotStart)).exists() ){
			log.info("Hot Start detcted!");
			setPlansFile( hotStart );
			module.setHotStart(true);
		}
		else if( !warmStart.equals("") ){
			log.info("Warm Start detcted!");
			setPlansFile( warmStart );
			module.setWarmStart(true);
		}
		else{
			log.info("Cold Start (no plans file) detected!");
			module.setColdStart(true);
		}
		
		// setting target location for hot start plans file
		if(!hotStart.equals("")){
			log.info("The resulting plans file after this MATSim run is stored at a specified place to enable hot start for the following MATSim run.");
			log.info("The specified place is : " + hotStart);
			module.setHotStartTargetLocation(hotStart);
		}
		else
			module.setHotStartTargetLocation("");
	}

	/**
	 * sets (either a "warm" or "hot" start) a plans file, see above.
	 */
	private void setPlansFile(String plansFile) {
		log.info("Setting PlansConfigGroup to config...");
		PlansConfigGroup plansCG = (PlansConfigGroup) config.getModule(PlansConfigGroup.GROUP_NAME);
		// set input plans file
		plansCG.setInputFile( plansFile );
		
		log.info("PlansConfigGroup setting:");
		log.info("Input plans file set to: " + plansCG.getInputFile());
		log.info("... done!");
	}
	
	/**
	 * setting controler parameter
	 * 
	 * @param matsimParameter
	 */
	private void initControler(ConfigType matsimParameter){
		log.info("Setting ControlerConfigGroup to config...");
		int firstIteration = matsimParameter.getControler().getFirstIteration().intValue();
		int lastIteration = matsimParameter.getControler().getLastIteration().intValue();
		ControlerConfigGroup controlerCG = (ControlerConfigGroup) config.getModule(ControlerConfigGroup.GROUP_NAME);
		// set values
		controlerCG.setFirstIteration( firstIteration );
		controlerCG.setLastIteration( lastIteration);
		controlerCG.setOutputDirectory( InternalConstants.MATSIM_4_OPUS_OUTPUT );
		
		controlerCG.setSnapshotFormat(Arrays.asList("otfvis"));
		controlerCG.setWriteSnapshotsInterval( 0 ); // disabling snapshots
		
		// set Qsim
		controlerCG.setMobsim(QSimConfigGroup.GROUP_NAME);
		
		log.info("ControlerConfigGroup settings:");
		log.info("FirstIteration: " + controlerCG.getFirstIteration());
		log.info("LastIteration: " + controlerCG.getLastIteration());
		log.info("MATSim output directory: " +  controlerCG.getOutputDirectory());
		log.info("Mobsim: " + controlerCG.getMobsim());
		log.info("... done!");
	}
	
	/**
	 * setting planCalcScore parameter
	 * 
	 * @param matsimParameter
	 */
	private void initPlanCalcScore(ConfigType matsimParameter){
		log.info("Setting PlanCalcScore to config...");
		String activityType_0 = matsimParameter.getPlanCalcScore().getActivityType0();
		String activityType_1 = matsimParameter.getPlanCalcScore().getActivityType1();
		
		ActivityParams homeActivity = new ActivityParams(activityType_0);
		homeActivity.setTypicalDuration( matsimParameter.getPlanCalcScore().getHomeActivityTypicalDuration().intValue() ); 	// should be something like 12*60*60
		
		ActivityParams workActivity = new ActivityParams(activityType_1);
		workActivity.setTypicalDuration( matsimParameter.getPlanCalcScore().getWorkActivityTypicalDuration().intValue() );	// should be something like 8*60*60
		workActivity.setOpeningTime( matsimParameter.getPlanCalcScore().getWorkActivityOpeningTime().intValue() );			// should be something like 7*60*60
		workActivity.setLatestStartTime( matsimParameter.getPlanCalcScore().getWorkActivityLatestStartTime().intValue() );	// should be something like 9*60*60
		config.planCalcScore().addActivityParams( homeActivity );
		config.planCalcScore().addActivityParams( workActivity );

		log.info("PlanCalcScore settings:");
		log.info("Activity_Type_0: " + homeActivity.getType() + " Typical Duration Activity_Type_0: " + homeActivity.getTypicalDuration());
		log.info("Activity_Type_1: " + workActivity.getType() + " Typical Duration Activity_Type_1: " + workActivity.getTypicalDuration());
		log.info("Opening Time Activity_Type_1: " + workActivity.getOpeningTime()); 
		log.info("Latest Start Time Activity_Type_1: " + workActivity.getLatestStartTime());
		log.info("... done!");
	}
	
	/**
	 * setting qsim
	 */
	private void initQSim(){
		log.info("Setting QSimConfigGroup to config...");
		
		QSimConfigGroup qsimCG = config.qsim();

		
		// setting number of threads
		qsimCG.setNumberOfThreads(Runtime.getRuntime().availableProcessors());
		
		double popSampling = this.matsimConfig.getMatsim4Urbansim().getUrbansimParameter().getPopulationSamplingRate();
		log.warn("FlowCapFactor and StorageCapFactor are adapted to the population sampling rate (sampling rate = " + popSampling + ").");
		// setting FlowCapFactor == population sampling rate (no correction factor needed here)
		qsimCG.setFlowCapFactor( popSampling );	
		
		// Adapting the storageCapFactor has the following reason:
		// Too low SorageCapacities especially with small sampling 
		// rates can (eg 1%) lead to strong backlogs on the traffic network. 
		// This leads to an unstable behavior of the simulation (by breakdowns 
		// during the learning progress).
		// The correction fetch factor introduced here raises the 
		// storage capacity at low sampling rates and becomes flatten 
		// with increasing sampling rates (at a 100% sample, the 
		// storage capacity == 1).			tnicolai nov'11
		if(popSampling <= 0.){
			popSampling = 0.01;
			log.warn("Raised popSampling rate to " + popSampling + " to to avoid erros while calulating the correction fetch factor ...");
		}
		// tnicolai dec'11
		double storageCapCorrectionFactor = Math.pow(popSampling, -0.25);	// same as: / Math.sqrt(Math.sqrt(sample))
		double storageCap = popSampling * storageCapCorrectionFactor;
		// setting StorageCapFactor
		qsimCG.setStorageCapFactor( storageCap );	
		
		boolean removeStuckVehicles = false;
		qsimCG.setRemoveStuckVehicles( removeStuckVehicles );
		qsimCG.setStuckTime(10.);
		qsimCG.setEndTime(30.*3600.); // 30h
		
		log.info("QSimConfigGroup settings:");
		log.info("Number of Threads: " + qsimCG.getNumberOfThreads());
		log.info("FlowCapFactor (= population sampling rate): "+ config.qsim().getFlowCapFactor());
		log.warn("StorageCapFactor: " + config.qsim().getStorageCapFactor() + " (with correction factor = " + storageCapCorrectionFactor + ")" );
		log.info("RemoveStuckVehicles: " + (removeStuckVehicles?"True":"False") );
		log.info("StuckTime: " + config.qsim().getStuckTime());
		log.info("End Time: " + qsimCG.getEndTime());
		log.info("... done!");
	}
	
	/**
	 * setting strategy
	 */
	private void initStrategy(ConfigType matsimParameter){
		log.info("Setting StrategyConfigGroup to config...");
		
		// some modules are disables after 80% of overall iterations, 
		// last iteration for them determined here tnicolai feb'12
		int disableStrategyAfterIteration = (int) Math.ceil(config.controler().getLastIteration() * 0.8);
		
		// configure strategies for re-planning (should be something like 5)
		config.strategy().setMaxAgentPlanMemorySize( matsimParameter.getStrategy().getMaxAgentPlanMemorySize().intValue() );
		
		StrategyConfigGroup.StrategySettings timeAlocationMutator = new StrategyConfigGroup.StrategySettings(IdFactory.get(1));
		timeAlocationMutator.setModuleName("TimeAllocationMutator"); 	// module name given in org.matsim.core.replanning.StrategyManagerConfigLoader
		timeAlocationMutator.setProbability( matsimParameter.getStrategy().getTimeAllocationMutatorProbability() ); // should be something like 0.1
		timeAlocationMutator.setDisableAfter(disableStrategyAfterIteration);
		config.strategy().addStrategySettings(timeAlocationMutator);
		// change mutation range to 2h. tnicolai feb'12
		config.setParam("TimeAllocationMutator", "mutationRange", "7200"); 
		
		StrategyConfigGroup.StrategySettings changeExpBeta = new StrategyConfigGroup.StrategySettings(IdFactory.get(2));
		changeExpBeta.setModuleName("ChangeExpBeta");					// module name given in org.matsim.core.replanning.StrategyManagerConfigLoader
		changeExpBeta.setProbability( matsimParameter.getStrategy().getChangeExpBetaProbability() ); // should be something like 0.9
		config.strategy().addStrategySettings(changeExpBeta);
		
		StrategyConfigGroup.StrategySettings reroute = new StrategyConfigGroup.StrategySettings(IdFactory.get(3));
		reroute.setModuleName("ReRoute_Dijkstra");						// module name given in org.matsim.core.replanning.StrategyManagerConfigLoader
		reroute.setProbability( matsimParameter.getStrategy().getReRouteDijkstraProbability() ); 	// should be something like 0.1
		reroute.setDisableAfter(disableStrategyAfterIteration);
		config.strategy().addStrategySettings(reroute);
		
		// check if a 4th module is given in the external MATSim config
		StrategyConfigGroup.StrategySettings changeLegMode = getChangeLegModeStrategySettings();
		boolean set4thStrategyModule = ( changeLegMode != null && 
									   (changeLegMode.getModuleName().equalsIgnoreCase("ChangeLegMode") || changeLegMode.getModuleName().equalsIgnoreCase("ChangeSingleLegMode")) && 
									    changeLegMode.getProbability() > 0.);
		if(set4thStrategyModule){
			// to be consistent, setting the same iteration number as in the strategies above 
			changeLegMode.setDisableAfter(disableStrategyAfterIteration);
			// check if other modes are set
			Module changelegMode = config.getModule("changeLegMode");
			if(changelegMode != null && changelegMode.getValue("modes") != null)
				log.info("Following modes are found: " + changelegMode.getValue("modes"));
		}
		
		// tnicolai old version
		// StrategyConfigGroup.StrategySettings changeLegMode = new StrategyConfigGroup.StrategySettings(IdFactory.get(4));
		// changeLegMode.setModuleName(moduleName);		// module name given in org.matsim.core.replanning.StrategyManagerConfigLoader
		// changeLegMode.setProbability( module4Probability );
		// changeLegMode.setDisableAfter(disableStrategyAfterIteration);
		// scenario.getConfig().strategy().addStrategySettings(changeLegMode);‚
		// this sets some additional modes. by default car and pt is available
		// scenario.getConfig().setParam("changeLegMode", "modes", TransportMode.car +","+ TransportMode.pt +"," + TransportMode.bike + "," + TransportMode.walk);
		
		log.info("StrategyConfigGroup settings:");
		log.info("Strategy_1: " + timeAlocationMutator.getModuleName() + " Probability: " + timeAlocationMutator.getProbability() + " Disable After Itereation: " + timeAlocationMutator.getDisableAfter() ); 
		log.info("Strategy_2: " + changeExpBeta.getModuleName() + " Probability: " + changeExpBeta.getProbability() );
		log.info("Strategy_3: " + reroute.getModuleName() + " Probability: " + reroute.getProbability() + " Disable After Itereation: " + reroute.getDisableAfter() );
		if(set4thStrategyModule)
			log.info("Strategy_4: " + changeLegMode.getModuleName() + " Probability: " + changeLegMode.getProbability() + " Disable After Itereation: " + changeLegMode.getDisableAfter() );
		log.info("... done!");
	}

	/**
	 * 
	 */
	private StrategyConfigGroup.StrategySettings getChangeLegModeStrategySettings() {
		Iterator<StrategyConfigGroup.StrategySettings> iter = config.strategy().getStrategySettings().iterator();
		StrategyConfigGroup.StrategySettings setting = null;
		while(iter.hasNext()){
			setting = iter.next();
			if(setting.getModuleName().equalsIgnoreCase("ChangeLegMode") || setting.getModuleName().equalsIgnoreCase("ChangeSingleLegMode"))
				break;
			setting = null;
		}
		return setting;
	}
	
	/**
	 * setting walk speed in plancalcroute
	 */
	private void initPlanCalcRoute(){
		log.info("Setting PlanCalcRouteGroup to config...");
		
		double defaultWalkSpeed = 1.38888889; 	// 1.38888889m/s corresponds to 5km/h -- alternatively: use 0.833333333333333m/s corresponds to 3km/h
		double defaultBicycleSpeed = 4.16666666;// 4.16666666m/s corresponds to 15 km/h
		double defaultPtSpeed 	= 6.94444444;	// 6.94444444m/s corresponds to 25 km/h
		
		// setting teleportation speeds in router
		config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.walk, defaultWalkSpeed); 
		config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.bike, defaultBicycleSpeed);
		config.plansCalcRoute().setTeleportedModeSpeed(TransportMode.pt, defaultPtSpeed);

		log.info("PlanCalcRouteGroup settings:");							 
		log.info("Walk Speed: " + config.plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.walk) );
		log.info("Bike Speed: " + config.plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.bike) );
		log.info("Pt Speed: " + config.plansCalcRoute().getTeleportedModeSpeeds().get(TransportMode.pt) );
		log.info("Beeline Distance Factor: " + config.plansCalcRoute().getBeelineDistanceFactor() );
		
		log.info("...done!");
	}
	
	/**
	 * returns true if an external MATSim config contains the matsim4UrbanSimModule 
	 * with at least one marginal utility for bicycles.
	 * 
	 * @return
	 */
	private boolean useCustomMarginalUtilitiesBike(){
		if(matsim4UrbanSimModule != null)
			return (matsim4UrbanSimModule.getValue(BETA_BIKE_TRAVEL_TIME) != null ||
					matsim4UrbanSimModule.getValue(BETA_BIKE_TRAVEL_TIME_POWER2) != null ||
					matsim4UrbanSimModule.getValue(BETA_BIKE_LN_TRAVEL_TIME) != null ||
					matsim4UrbanSimModule.getValue(BETA_BIKE_TRAVEL_DISTANCE) != null ||
					matsim4UrbanSimModule.getValue(BETA_BIKE_TRAVEL_DISTANCE_POWER2) != null ||
					matsim4UrbanSimModule.getValue(BETA_BIKE_LN_TRAVEL_DISTANCE) != null ||
					matsim4UrbanSimModule.getValue(BETA_BIKE_TRAVEL_COST) != null ||
					matsim4UrbanSimModule.getValue(BETA_BIKE_TRAVEL_COST_POWER2) != null ||
					matsim4UrbanSimModule.getValue(BETA_BIKE_LN_TRAVEL_COST) != null);
		return false;
	}
	
	/**
	 * returns a matsim4urbansim parameter as double or zero in case of conversion errors.
	 * 
	 * @param paramName
	 * @return matsim4urbansim parameter as double
	 */
	private double getValueAsDouble(String paramName){
		if(matsim4UrbanSimModule != null){
			try{
				double tmp = Double.parseDouble(matsim4UrbanSimModule.getValue(paramName));
				return tmp;
			} catch(Exception e){}
			return 0.;
		}
		return 0.;
	}
	
	public Config getConfig(){
			return this.config;
	}
	
	public AccessibilityParameterConfigModule getAccessibilityParameterConfig() {
		Module m = this.config.getModule(AccessibilityParameterConfigModule.GROUP_NAME);
		if (m instanceof AccessibilityParameterConfigModule) {
			return (AccessibilityParameterConfigModule) m;
		}
		AccessibilityParameterConfigModule apcm = new AccessibilityParameterConfigModule(AccessibilityParameterConfigModule.GROUP_NAME);
		this.config.getModules().put(AccessibilityParameterConfigModule.GROUP_NAME, apcm);
		return apcm;
	}
	
	public MATSim4UrbanSimControlerConfigModuleV3 getMATSim4UrbaSimControlerConfig() {
		Module m = this.config.getModule(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME);
		if (m instanceof MATSim4UrbanSimControlerConfigModuleV3) {
			return (MATSim4UrbanSimControlerConfigModuleV3) m;
		}
		MATSim4UrbanSimControlerConfigModuleV3 mccm = new MATSim4UrbanSimControlerConfigModuleV3(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME);
		this.config.getModules().put(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME, mccm);
		return mccm;
	}
	
	public UrbanSimParameterConfigModuleV3 getUrbanSimParameterConfig() {
		Module m = this.config.getModule(UrbanSimParameterConfigModuleV3.GROUP_NAME);
		if (m instanceof UrbanSimParameterConfigModuleV3) {
			return (UrbanSimParameterConfigModuleV3) m;
		}
		UrbanSimParameterConfigModuleV3 upcm = new UrbanSimParameterConfigModuleV3(UrbanSimParameterConfigModuleV3.GROUP_NAME);
		this.config.getModules().put(UrbanSimParameterConfigModuleV3.GROUP_NAME, upcm);
		return upcm;
	}
	
	// Testing fetch  factor calculation for storageCap 
	public static void main(String[] args) {
		// testing calculation of storage capacity fetch factor
		for(double sample = 0.01; sample <=1.; sample += 0.01){
			
			double factor = Math.pow(sample, -0.25); // same as: 1. / Math.sqrt(Math.sqrt(sample))
			double storageCap = sample * factor;
			
			System.out.println("Sample rate " + sample + " leads to a fetch fector of: " + factor + " and a StroraceCapacity of: " + storageCap );
		}
		
		for(int i = 0; i <= 100; i++){
			System.out.println("i = " + i + " disable int = " + (int) Math.ceil(i * 0.8)+ " disable double = " + i * 0.8);			
		}
	}
}

