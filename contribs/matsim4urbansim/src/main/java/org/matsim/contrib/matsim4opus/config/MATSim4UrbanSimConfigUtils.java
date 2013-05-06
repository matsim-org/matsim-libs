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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.matsim.contrib.matsim4opus.constants.InternalConstants;
import org.matsim.contrib.matsim4opus.matsim4urbansim.jaxbconfig2.ConfigType;
import org.matsim.contrib.matsim4opus.matsim4urbansim.jaxbconfig2.Matsim4UrbansimType;
import org.matsim.contrib.matsim4opus.matsim4urbansim.jaxbconfig2.MatsimConfigType;
import org.matsim.contrib.matsim4opus.utils.ids.IdFactory;
import org.matsim.contrib.matsim4opus.utils.io.Paths;
import org.matsim.core.config.Config;
import org.matsim.core.config.ConfigUtils;
import org.matsim.core.config.ConfigWriter;
import org.matsim.core.config.MatsimConfigReader;
import org.matsim.core.config.Module;
import org.matsim.core.config.groups.ControlerConfigGroup;
import org.matsim.core.config.groups.NetworkConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup;
import org.matsim.core.config.groups.PlansConfigGroup;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspExperimentalConfigKey;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * @author nagel
 *
 */
public class MATSim4UrbanSimConfigUtils {
	
	static final Logger log = Logger.getLogger(MATSim4UrbanSimConfigUtils.class);
	// module and param names for matsim4urbansim settings stored in an external MATSim config file
	public static final String MATSIM4URBANSIM_MODULE_EXTERNAL_CONFIG = "matsim4urbansimParameter";// module
	// parameter names in matsim4urbansimParameter module
	public static final String TIME_OF_DAY = "timeOfDay";
	public static final String URBANSIM_ZONE_SHAPEFILE_LOCATION_DISTRIBUTION = "urbanSimZoneShapefileLocationDistribution";
	public static final String PT_STOPS = "ptStops";
	public static final String PT_STOPS_SWITCH = "usePtStops";
	public static final String PT_TRAVEL_TIMES = "ptTravelTimes";
	public static final String PT_TRAVEL_DISTANCES = "ptTravelDistances";
	public static final String PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH = "useTravelTimesAndDistances";

	/**
	 * Setting 
	 * 
	 * @param matsim4UrbanSimParameter
	 * @param matsim4urbansimModule TODO
	 * @param config TODO
	 */
	static void initMATSim4UrbanSimControler(Matsim4UrbansimType matsim4UrbanSimParameter, 
			Module matsim4urbansimModule, Config config){
		
		boolean computeCellBasedAccessibility	= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isCellBasedAccessibility();
		boolean computeCellBasedAccessibilityNetwork   = false;
		boolean computeCellbasedAccessibilityShapeFile = false;
		
		String shapeFile						= matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getShapeFileCellBasedAccessibility().getInputFile();
	
		// if cell-based accessibility is enabled, check whether a shapefile is given 
		if(computeCellBasedAccessibility){ 
			if(!Paths.pathExsits(shapeFile)){ // since no shape file found, accessibility computation is applied on the area covering the network
				computeCellBasedAccessibilityNetwork   = true;
				log.warn("No shape-file given or shape-file not found:" + shapeFile);
				log.warn("Instead the boundary of the road network is used to determine the area for which the accessibility computation is applied.");
				log.warn("This may be ok of that was your intention.") ;
				// yyyyyy the above is automagic; should be replaced by a flag.  kai, apr'13
			} else {
				computeCellbasedAccessibilityShapeFile = true;
			}
		}
		
		// ===
		
		double timeOfDay						= 8 * 3600.; // default value
		String ptStops							= null;
		String ptTravelTimes					= null;
		String ptTravelDistances				= null;
		if(matsim4urbansimModule != null){
			try{
				double tmp = Double.parseDouble( matsim4urbansimModule.getValue(TIME_OF_DAY) );
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
			
			// check if pseudo pt should be used
			if( matsim4urbansimModule.getValue( PT_STOPS_SWITCH ) != null && 
				matsim4urbansimModule.getValue( PT_STOPS_SWITCH ).equalsIgnoreCase("TRUE")){
				
				// check if pt stops input file is given and existing
				ptStops = matsim4urbansimModule.getValue(PT_STOPS);
				if( ptStops != null && Paths.pathExsits(ptStops))
					log.info("Found pt stop input file: " + ptStops);
				else
					throw new RuntimeException("The parameter 'usePtStop' is set TRUE, but no pt stop input file found!!! Given input file for 'ptStop' = " + ptStops);
				
				
				// check if input files for pt travel times and distances are set
				if( matsim4urbansimModule.getValue( PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH ) != null &&
					matsim4urbansimModule.getValue( PT_TRAVEL_TIMES_AND_DISTANCES_SWITCH ).equalsIgnoreCase( "TRUE" )){
					
					ptTravelTimes = matsim4urbansimModule.getValue(PT_TRAVEL_TIMES);
					ptTravelDistances =  matsim4urbansimModule.getValue(PT_TRAVEL_DISTANCES);
					
					if( ptTravelTimes != null && Paths.pathExsits(ptTravelTimes) && 
						ptTravelDistances != null && Paths.pathExsits(ptTravelDistances)){
						log.info("Found pt travel time input file: " + ptTravelTimes);
						log.info("Found pt travel distance input file: " + ptTravelDistances);
					}
					else{
						log.error("The parameter 'useTravelTimesAndDistances' is set TRUE but either no pt travel time or distance input file found! " +
								"Both files needs to be set to use precomputed pt times and distances.");
						log.error("Given input file name for 'ptTravelTimes' = " + ptTravelTimes);
						log.error("Given input file name for 'ptTravelDistances' = " + ptTravelDistances);
						log.error("At least one of these two files was not found.  Aborting ...") ;
						throw new RuntimeException("Parameter not found!");
					}
				}
			}
		}
		
		// ===
		
		// set parameter in module 
		MATSim4UrbanSimControlerConfigModuleV3 module = getMATSim4UrbaSimControlerConfigAndPossiblyConvert(config);
		module.setAgentPerformance(matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isAgentPerformance());
		module.setZone2ZoneImpedance(matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isZone2ZoneImpedance());
		module.setZoneBasedAccessibility(matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isZoneBasedAccessibility());
		module.setCellBasedAccessibility(computeCellBasedAccessibility);
		module.setCellSizeCellBasedAccessibility(matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getCellSizeCellBasedAccessibility().intValue());
		module.setCellBasedAccessibilityShapeFile(computeCellbasedAccessibilityShapeFile);
		module.setCellBasedAccessibilityNetwork(computeCellBasedAccessibilityNetwork);
		module.setShapeFileCellBasedAccessibility(shapeFile);
		module.setUsingCustomBoundingBox(matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isUseCustomBoundingBox());
		module.setBoundingBoxLeft(matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getBoundingBoxLeft());
		module.setBoundingBoxBottom(matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getBoundingBoxBottom());
		module.setBoundingBoxRight(matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getBoundingBoxRight());
		module.setBoundingBoxTop(matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getBoundingBoxTop());
		module.setTimeOfDay(timeOfDay);
		module.setPtStopsInputFile(ptStops);
		module.setPtTravelTimesInputFile(ptTravelTimes);
		module.setPtTravelDistancesInputFile(ptTravelDistances);
	}

	/**
	 * store UrbanSimParameter
	 * 
	 * @param matsim4UrbanSimParameter
	 * @param matsim4urbansimModule TODO
	 * @param config TODO
	 */
	static void initUrbanSimParameter(Matsim4UrbansimType matsim4UrbanSimParameter, Module matsim4urbansimModule, Config config){
		
		// get every single matsim4urbansim/urbansimParameter
		String projectName 		= ""; // not needed anymore dec'12
		double populationSamplingRate = matsim4UrbanSimParameter.getUrbansimParameter().getPopulationSamplingRate();
		int year 				= matsim4UrbanSimParameter.getUrbansimParameter().getYear().intValue();
		
		boolean useShapefileLocationDistribution = false;
		String urbanSimZoneShapefileLocationDistribution = null;
		double randomLocationDistributionRadiusForUrbanSimZone = matsim4UrbanSimParameter.getUrbansimParameter().getRandomLocationDistributionRadiusForUrbanSimZone();
		
		if(matsim4urbansimModule != null)
			urbanSimZoneShapefileLocationDistribution = matsim4urbansimModule.getValue(URBANSIM_ZONE_SHAPEFILE_LOCATION_DISTRIBUTION);
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
		UrbanSimParameterConfigModuleV3 module = getUrbanSimParameterConfigAndPossiblyConvert(config);
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
		module.setUsingShapefileLocationDistribution(useShapefileLocationDistribution);
		module.setUrbanSimZoneShapefileLocationDistribution(urbanSimZoneShapefileLocationDistribution);
		module.setUrbanSimZoneRadiusLocationDistribution(randomLocationDistributionRadiusForUrbanSimZone);
		module.setBackup(backupRunData);
		module.setTestRun(isTestRun);	
		
		// setting paths into constants structure
		InternalConstants.setOPUS_HOME(module.getOpusHome());
		InternalConstants.OPUS_DATA_PATH = module.getOpusDataPath();
		InternalConstants.MATSIM_4_OPUS = module.getMATSim4Opus();
		InternalConstants.MATSIM_4_OPUS_CONFIG = module.getMATSim4OpusConfig();
		InternalConstants.MATSIM_4_OPUS_OUTPUT = module.getMATSim4OpusOutput();
		InternalConstants.MATSIM_4_OPUS_TEMP = module.getMATSim4OpusTemp();
		InternalConstants.MATSIM_4_OPUS_BACKUP = module.getMATSim4OpusBackup();
	}

	/**
		 * @param matsim4urbansimConfigPart1
		 * @throws UncheckedIOException
		 */
		static Module initMATSim4UrbanSimModule(String externalMATSimConfigFilename) throws UncheckedIOException {
			
			if(externalMATSimConfigFilename != null && Paths.pathExsits(externalMATSimConfigFilename)){
				Config tempConfig = ConfigUtils.loadConfig( externalMATSimConfigFilename.trim() );
				
				// loading additional matsim4urbansim parameter settings from external config file
				Module module = tempConfig.getModule(MATSIM4URBANSIM_MODULE_EXTERNAL_CONFIG);
				if(module == null)
					log.info("No \""+ MATSIM4URBANSIM_MODULE_EXTERNAL_CONFIG + "\" settings found in " + externalMATSimConfigFilename);
				else
					log.info("Found \""+ MATSIM4URBANSIM_MODULE_EXTERNAL_CONFIG + "\" settings in " + externalMATSimConfigFilename);
				return module ;
			}
			return null ;
		}

	/**
	 * setting MATSim network
	 * 
	 * NOTE: If the MATSim4UrbanSim network section contains a road network 
	 * this overwrites a previous network, e.g. from an external MATSim configuration
	 * <p/>
	 * (The above statement is correct.  But irrelevant, since at the end everything is overwritten 
	 * again from an external MATSim configuration. kai, apr'13)
	 * 
	 * @param matsimParameter
	 * @param config TODO
	 */
	static void initNetwork(ConfigType matsimParameter, Config config){
		log.info("Setting NetworkConfigGroup to config...");
		String networkFile = matsimParameter.getNetwork().getInputFile();
		if( !networkFile.isEmpty() )  // the MATSim4UrbanSim config contains a network file
			config.network().setInputFile( networkFile );
		else
			throw new RuntimeException("Missing MATSim network! The network must be specified either directly in the " +
					"MATSim4UrbanSim configuration or in an external MATSim configuration.");

		// yyyyyy ???  Aber es gibt die exception doch auch, wenn es in der external matsim config gesetzt ist? kai, apr'13

		log.info("...done!");
	}

	/**
	 * setting input plans file (for warm/hot start)
	 * 
	 * @param matsimParameter
	 * @param config TODO
	 */
	static void initInputPlansFile(ConfigType matsimParameter, Config config){
		log.info("Checking for warm or hot start...");
		// get plans file for hot start
		String hotStart = matsimParameter.getHotStartPlansFile().getInputFile();
		// get plans file for warm start 
		String warmStart = matsimParameter.getInputPlansFile().getInputFile();
		
		MATSim4UrbanSimControlerConfigModuleV3 module = getMATSim4UrbaSimControlerConfigAndPossiblyConvert(config);
		
		// setting plans file as input
		if( !hotStart.equals("") &&
		  (new File(hotStart)).exists() ){
			log.info("Hot Start detcted!");
			setPlansFile( hotStart, config );
			module.setHotStart(true);
		}
		else if( !warmStart.equals("") ){
			log.info("Warm Start detcted!");
			setPlansFile( warmStart, config );
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
	 * @param config TODO
	 */
	static void setPlansFile(String plansFile, Config config) {
		
		log.info("Setting PlansConfigGroup to config...");
		PlansConfigGroup plansCG = (PlansConfigGroup) config.getModule(PlansConfigGroup.GROUP_NAME);
		// set input plans file
		plansCG.setInputFile( plansFile );

		log.info("...done!");
	}

	/**
	 * setting controler parameter
	 * 
	 * @param matsimParameter
	 * @param config TODO
	 */
	static void initControler(ConfigType matsimParameter, Config config){
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

		log.info("...done!");
	}

	/**
	 * setting planCalcScore parameter
	 * 
	 * @param matsimParameter
	 * @param config TODO
	 */
	static void initPlanCalcScore(ConfigType matsimParameter, Config config){
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

		log.info("...done!");
	}

	/**
	 * @param config TODO
	 * 
	 */
	static StrategyConfigGroup.StrategySettings getChangeLegModeStrategySettings(Config config) {
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
	 * setting qsim
	 * @param matsim4urbansimConfig TODO
	 * @param config TODO
	 */
	static void initQSim(MatsimConfigType matsim4urbansimConfig, Config config){
		log.info("Setting QSimConfigGroup to config...");
		
		QSimConfigGroup qsimCG = config.getQSimConfigGroup();
		if( qsimCG == null){		
			qsimCG = new QSimConfigGroup();
			config.addQSimConfigGroup( qsimCG );
		}
		
		// setting number of threads
//		qsimCG.setNumberOfThreads(Runtime.getRuntime().availableProcessors());
		// log.error("setting qsim number of threads automagically; this is almost certainly not good; fix") ;
		// just changed this, setting it to one:  kai, apr'13
		qsimCG.setNumberOfThreads(1);
		
		double popSampling = matsim4urbansimConfig.getMatsim4Urbansim().getUrbansimParameter().getPopulationSamplingRate();
		log.info("FlowCapFactor and StorageCapFactor are adapted to the population sampling rate (sampling rate = " + popSampling + ").");
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
			// yyyyyy how can this happen???? kai, apr'13
			// yyyyyy if this has happens, it is plausible that the flow cap factor is NOT corrected??? kai, apr'13
			double popSamplingBefore = popSampling ;
			popSampling = 0.01;
			log.warn("Raised popSampling rate from " + popSamplingBefore + 
					" to " + popSampling + " to to avoid errors while calulating the correction factor ...");
		}
		// tnicolai dec'11
		double storageCapCorrectionFactor = Math.pow(popSampling, -0.25);	// same as: / Math.sqrt(Math.sqrt(sample))
		// setting StorageCapFactor
		qsimCG.setStorageCapFactor( popSampling * storageCapCorrectionFactor );	
		
		boolean removeStuckVehicles = false;
		qsimCG.setRemoveStuckVehicles( removeStuckVehicles );
		qsimCG.setStuckTime(10.);
		qsimCG.setEndTime(30.*3600.); // 30h

		log.info("...done!");
	}

	/**
	 * setting walk speed in plancalcroute
	 * @param config TODO
	 */
	static void initPlanCalcRoute(Config config){
		log.info("Setting PlanCalcRouteGroup to config...");
		
		double defaultWalkSpeed = 1.38888889; 	// 1.38888889m/s corresponds to 5km/h -- alternatively: use 0.833333333333333m/s corresponds to 3km/h
		double defaultBicycleSpeed = 4.16666666;// 4.16666666m/s corresponds to 15 km/h
		double defaultPtSpeed 	= 6.94444444;	// 6.94444444m/s corresponds to 25 km/h
		
		//  log.error( "ignoring any external default speeds for walk/bicycle/pt and using internal values.  fix!!" ) ;
		//  this is not a problem since the complete config is overwritten by the external config at the very end.
		
		// setting teleportation speeds in router
		config.plansCalcRoute().setWalkSpeed( defaultWalkSpeed ); 
		config.plansCalcRoute().setBikeSpeed( defaultBicycleSpeed );
		config.plansCalcRoute().setPtSpeed( defaultPtSpeed );

		log.info("...done!");
	}

	/**
	 * setting strategy
	 * @param config TODO
	 */
	static void initStrategy(ConfigType matsim4urbansimConfig, Config config){
		log.info("Setting StrategyConfigGroup to config...");
		
		// some modules are disables after 80% of overall iterations, 
		// last iteration for them determined here tnicolai feb'12
		int disableStrategyAfterIteration = (int) Math.ceil(config.controler().getLastIteration() * 0.8);
		
		// configure strategies for re-planning (should be something like 5)
		config.strategy().setMaxAgentPlanMemorySize( matsim4urbansimConfig.getStrategy().getMaxAgentPlanMemorySize().intValue() );
		
		StrategyConfigGroup.StrategySettings timeAlocationMutator = new StrategyConfigGroup.StrategySettings(IdFactory.get(1));
		timeAlocationMutator.setModuleName("TimeAllocationMutator"); 	// module name given in org.matsim.core.replanning.StrategyManagerConfigLoader
		timeAlocationMutator.setProbability( matsim4urbansimConfig.getStrategy().getTimeAllocationMutatorProbability() ); // should be something like 0.1
		timeAlocationMutator.setDisableAfter(disableStrategyAfterIteration);
		config.strategy().addStrategySettings(timeAlocationMutator);
		// change mutation range to 2h. tnicolai feb'12
		config.setParam("TimeAllocationMutator", "mutationRange", "7200"); 
		
		StrategyConfigGroup.StrategySettings changeExpBeta = new StrategyConfigGroup.StrategySettings(IdFactory.get(2));
		changeExpBeta.setModuleName("ChangeExpBeta");					// module name given in org.matsim.core.replanning.StrategyManagerConfigLoader
		changeExpBeta.setProbability( matsim4urbansimConfig.getStrategy().getChangeExpBetaProbability() ); // should be something like 0.9
		config.strategy().addStrategySettings(changeExpBeta);
		
		StrategyConfigGroup.StrategySettings reroute = new StrategyConfigGroup.StrategySettings(IdFactory.get(3));
		reroute.setModuleName("ReRoute");  // old name "ReRoute_Dijkstra"						// module name given in org.matsim.core.replanning.StrategyManagerConfigLoader
		reroute.setProbability( matsim4urbansimConfig.getStrategy().getReRouteDijkstraProbability() ); 	// should be something like 0.1
		reroute.setDisableAfter(disableStrategyAfterIteration);
		config.strategy().addStrategySettings(reroute);
		
		// check if a 4th module is given in the external MATSim config
		StrategyConfigGroup.StrategySettings changeLegMode = getChangeLegModeStrategySettings(config);
		boolean set4thStrategyModule = ( changeLegMode != null && 
									   ( changeLegMode.getModuleName().equalsIgnoreCase("ChangeLegMode") || changeLegMode.getModuleName().equalsIgnoreCase("ChangeSingleLegMode")) && 
									     changeLegMode.getProbability() > 0.);
		if(set4thStrategyModule){
			// to be consistent, setting the same iteration number as in the strategies above 
			changeLegMode.setDisableAfter(disableStrategyAfterIteration);
			log.warn("setting disableStrategyAfterIteration for ChangeLegMode to " + disableStrategyAfterIteration + "; possibly overriding config settings!");
			// check if other modes are set
			Module changelegMode = config.getModule("changeLegMode");
			if(changelegMode != null && changelegMode.getValue("modes") != null)
				log.info("Following modes are found: " + changelegMode.getValue("modes"));
		}
		log.info("...done!");
	}

	/**
	 * loads the external config into a temporary structure
	 * this is done to initialize MATSim4UrbanSim settings that are defined
	 * in the external MATSim config
	 * 
	 * @param matsimParameter
	 * @param config TODO
	 * @throws UncheckedIOException
	 */
	static void loadExternalConfigAndOverwriteMATSim4UrbanSimSettings(ConfigType matsimParameter, Config config) throws UncheckedIOException {
		// check if external MATsim config is given
		String externalMATSimConfig = matsimParameter.getMatsimConfig().getInputFile();
		if(externalMATSimConfig != null && Paths.pathExsits(externalMATSimConfig)){
			
			log.info("Loading settings from external MATSim config: " + externalMATSimConfig);
			log.warn("NOTE: MATSim4UrbanSim settings will be overwritten by settings in the external config! Make sure that this is what you intended!");
			new MatsimConfigReader(config).parse(externalMATSimConfig);
			log.info("... loading settings done!");
		}
	}

	/**
	 * creates an empty MATSim config to be filled by MATSim4UrbanSim + external MATSim config settings
	 */
	static Config createEmptyConfigWithSomeDefaults() {
		log.info("Creating an empty MATSim scenario.");
		Config config = ConfigUtils.createConfig();
		
		//"materialize" the local config groups:
		config.addModule(UrbanSimParameterConfigModuleV3.GROUP_NAME, 
				new UrbanSimParameterConfigModuleV3(UrbanSimParameterConfigModuleV3.GROUP_NAME) ) ;
		config.addModule(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME,
				new MATSim4UrbanSimControlerConfigModuleV3(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME));
		config.addModule(AccessibilityParameterConfigModule.GROUP_NAME,
				new AccessibilityParameterConfigModule(AccessibilityParameterConfigModule.GROUP_NAME)) ;
		
		// set some defaults:
		VspExperimentalConfigGroup vsp = config.vspExperimental();
		vsp.addParam(VspExperimentalConfigKey.vspDefaultsCheckingLevel, VspExperimentalConfigGroup.ABORT ) ;
		vsp.setActivityDurationInterpretation(VspExperimentalConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration) ;
		vsp.setRemovingUnneccessaryPlanAttributes(true) ;
		
		return config ;
	}

	/**
	 * loading, validating and initializing MATSim config.
	 */
	static MatsimConfigType unmarschal(String matsim4urbansimConfigFilename){
		
		// JAXBUnmaschal reads the UrbanSim generated MATSim config, validates it against
		// the current xsd (checks e.g. the presents and data type of parameter) and generates
		// an Java object representing the config file.
		JAXBUnmaschalV2 unmarschal = new JAXBUnmaschalV2( matsim4urbansimConfigFilename );
		
		MatsimConfigType matsim4urbansimConfig = null;
		
		// binding the parameter from the MATSim Config into the JAXB data structure
		if( (matsim4urbansimConfig = unmarschal.unmaschalMATSimConfig()) == null){
			log.error("Unmarschalling failed. SHUTDOWN MATSim!");
			System.exit(InternalConstants.UNMARSCHALLING_FAILED);
		}
		return matsim4urbansimConfig;
	}

	static void initAccessibilityParameter(Matsim4UrbansimType matsim4UrbanSimParameter, Module matsim4UrbanSimModule, Config config){
			
			// these are all parameter for the accessibility computation
			double logitScaleParameter;	
			double betaCarTT = 0.;		// Car
			double betaCarTTPower = 0.; // Car
			double betaCarLnTT = 0.;	// Car
			double betaCarTD = 0.; 		// Car
			double betaCarTDPower = 0.;	// Car
			double betaCarLnTD = 0.;	// Car
			double betaCarTMC = 0.;		// Car
			double betaCarTMCPower = 0.;// Car 
			double betaCarLnTMC = 0.;	// Car
			
			double betaBikeTT = 0.;		// Bike
			double betaBikeTTPower = 0.; // Bike
			double betaBikeLnTT = 0.;	// Bike
			double betaBikeTD = 0.; 	// Bike
			double betaBikeTDPower = 0.;// Bike
			double betaBikeLnTD = 0.;	// Bike
			double betaBikeTMC = 0.;	// Bike
			double betaBikeTMCPower = 0.;// Bike 
			double betaBikeLnTMC = 0.;	// Bike
	
			double betaWalkTT = 0.;		// Walk
			double betaWalkTTPower = 0.; // Walk
			double betaWalkLnTT = 0.;	// Walk
			double betaWalkTD = 0.; 	// Walk
			double betaWalkTDPower = 0.;// Walk
			double betaWalkLnTD = 0.;	// Walk
			double betaWalkTMC = 0.;	// Walk
			double betaWalkTMCPower = 0.;// Walk 
			double betaWalkLnTMC = 0.;	// Walk
	
			double betaPtTT = 0.;		// Pt
			double betaPtTTPower = 0.; 	// Pt
			double betaPtLnTT = 0.;		// Pt
			double betaPtTD = 0.; 		// Pt
			double betaPtTDPower = 0.;	// Pt
			double betaPtLnTD = 0.;		// Pt
			double betaPtTMC = 0.;		// Pt
			double betaPtTMCPower = 0.;	// Pt 
			double betaPtLnTMC = 0.;	// Pt
			
			PlanCalcScoreConfigGroup planCalcScoreConfigGroup = config.planCalcScore();
			
			double accessibilityDestinationSamplingRate = matsim4UrbanSimParameter.getAccessibilityParameter().getAccessibilityDestinationSamplingRate();
			// these parameter define if the beta or logit_scale parameter are taken from MATSim or the config file
			boolean useMATSimLogitScaleParameter 	= matsim4UrbanSimParameter.getAccessibilityParameter().isUseLogitScaleParameterFromMATSim();
			boolean useMATSimCarParameter			= matsim4UrbanSimParameter.getAccessibilityParameter().isUseCarParameterFromMATSim();
			boolean useMATSimBikeParameter			= !useCustomMarginalUtilitiesBike( matsim4UrbanSimModule ); // true if relevant settings in the external MATSim config are found
			boolean useMATSimWalkParameter			= matsim4UrbanSimParameter.getAccessibilityParameter().isUseWalkParameterFromMATSim();
			boolean useMATSimPtParameter			= !useCustomMarginalUtilitiesPt( matsim4UrbanSimModule );	 // true if relevant settings in the external MATSim config are found
			boolean useRawSum						= matsim4UrbanSimParameter.getAccessibilityParameter().isUseRawSumsWithoutLn();
			
			if(useMATSimLogitScaleParameter) {
				logitScaleParameter = planCalcScoreConfigGroup.getBrainExpBeta();
	//			if ( config.planCalcScore().getBrainExpBeta() != 1. ) {
	//				throw new RuntimeException("the code claims that it uses the matsim logit scale parameter, but in fact it sets it silently to one.  aborting ...") ;
	//			}
				// yyyyyy I don't know any more how to fix the above without increasing the confusion.  kai, apr'13
				//
			} else {
				logitScaleParameter = matsim4UrbanSimParameter.getAccessibilityParameter().getLogitScaleParameter();
			}
			// tnicolai nov'12: decided with Kai that beta_brain (the accessibility scale parameter) should be 1 because of the pre-factor of the logsum term
			if(logitScaleParameter != 1.0){
				log.error("You are using a logit scale parameter != 1! The default is 1.");
				log.error("The accessibility calulation proceeds with a logit scale parameter = " + logitScaleParameter);
				log.error("This is ok for the sustaincity case studies but needs to be changed afterwards.") ;
			}
	
			log.info("The logit scale parameter used for the accessibility computation is now set to " + logitScaleParameter );
			log.info("The logit scale parameter used inside the behavioral model for the traffic simulation is now set to " + config.planCalcScore().getBrainExpBeta() ) ;
			if ( logitScaleParameter != 1.0 || config.planCalcScore().getBrainExpBeta()!= 1.0 ) {
				log.info("It is best to have both of them at 1.0, but changing them means that you have to re-estimate urbansim models.") ;
			}
			
			final String noSeparateBetasMessage = "This MATSim4UrbanSim version does not support custom beta parameters such as \"betaBikeTravelTime\" etc. anymore (both in the UrbanSim GUI (car and walk) and the external MATSim config file (bike and pt)). Please let us know if this causes serious problems." +
					"To avoid the error message please : 1) select \"use_car_parameter_from_MATSim\" and \"use_walk_parameter_from_MATSim\" in the UrbanSim GUI and 2) remove all beta parameters for bike and pt (such as \"<param name=\"betaBikeTravelTime\" value=\"-12.\" />\") from your external MATSim config file.";
			if(useMATSimCarParameter){
				// usually travelling_utils are negative
				betaCarTT 	   	= planCalcScoreConfigGroup.getTraveling_utils_hr() - planCalcScoreConfigGroup.getPerforming_utils_hr(); // [utils/h]
				betaCarTTPower	= 0.;
				betaCarLnTT		= 0.;
				betaCarTD		= planCalcScoreConfigGroup.getMarginalUtilityOfMoney() * planCalcScoreConfigGroup.getMonetaryDistanceCostRateCar(); 	// this is [utils/money * money/meter] = [utils/meter]
				betaCarTDPower	= 0.;																														// useful setting for MonetaryDistanceCostRateCar: 10cent/km (only fuel) or 
				betaCarLnTD		= 0.;																														// 80cent/km (including taxes, insurance ...)
				betaCarTMC		= - planCalcScoreConfigGroup.getMarginalUtilityOfMoney(); // [utils/money]
				betaCarTMCPower	= 0.;
				betaCarLnTMC	= 0.;
			}
			else{
				throw new RuntimeException(noSeparateBetasMessage);
			}
			
			if(useMATSimBikeParameter){
				// usually travelling_utils are negative
				betaBikeTT		= planCalcScoreConfigGroup.getTravelingBike_utils_hr() - planCalcScoreConfigGroup.getPerforming_utils_hr(); // [utils/h]
				betaBikeTTPower	= 0.;
				betaBikeLnTT	= 0.;
				betaBikeTD		= planCalcScoreConfigGroup.getMarginalUtlOfDistanceOther(); // [utils/meter]
				betaBikeTDPower	= 0.;												
				betaBikeLnTD	= 0.;
				betaBikeTMC		= - planCalcScoreConfigGroup.getMarginalUtilityOfMoney(); // [utils/money]
				betaBikeTMCPower= 0.;
				betaBikeLnTMC	= 0.;
			}
			else{
				throw new RuntimeException(noSeparateBetasMessage);
			}
			
			if(useMATSimWalkParameter){
				// usually travelling_utils are negative
				betaWalkTT		= planCalcScoreConfigGroup.getTravelingWalk_utils_hr() - planCalcScoreConfigGroup.getPerforming_utils_hr(); // [utils/h]
				betaWalkTTPower	= 0.;
				betaWalkLnTT	= 0.;
				betaWalkTD		= planCalcScoreConfigGroup.getMarginalUtlOfDistanceWalk(); // [utils/meter]
				betaWalkTDPower	= 0.;												
				betaWalkLnTD	= 0.;
				betaWalkTMC		= - planCalcScoreConfigGroup.getMarginalUtilityOfMoney(); // [utils/money]
				betaWalkTMCPower= 0.;
				betaWalkLnTMC	= 0.;
			}
			else{
				throw new RuntimeException(noSeparateBetasMessage);
			}
			
			if(useMATSimPtParameter){
				// usually travelling_utils are negative
				betaPtTT		= planCalcScoreConfigGroup.getTravelingPt_utils_hr() - planCalcScoreConfigGroup.getPerforming_utils_hr(); // [utils/h]
				betaPtTTPower	= 0.;
				betaPtLnTT		= 0.;
				betaPtTD		= planCalcScoreConfigGroup.getMarginalUtilityOfMoney() * planCalcScoreConfigGroup.getMonetaryDistanceCostRatePt(); // [utils/meter]
				betaPtTDPower	= 0.;												
				betaPtLnTD		= 0.;
				betaPtTMC		= - planCalcScoreConfigGroup.getMarginalUtilityOfMoney(); // [utils/money]
				betaPtTMCPower	= 0.;
				betaPtLnTMC		= 0.;
			}
			else{
				throw new RuntimeException(noSeparateBetasMessage);
			}
			
			// set parameter in module 
			AccessibilityParameterConfigModule module = getAccessibilityParameterConfigAndPossiblyConvert(config);
			module.setAccessibilityDestinationSamplingRate(accessibilityDestinationSamplingRate);
			module.setUsingLogitScaleParameterFromMATSim(useMATSimLogitScaleParameter);
			module.setUsingRawSumsWithoutLn(useRawSum);
			module.setUsingCarParameterFromMATSim(useMATSimCarParameter);
			module.setUsingBikeParameterFromMATSim(useMATSimBikeParameter);
			module.setUsingWalkParameterFromMATSim(useMATSimWalkParameter);
			module.setUsingPtParameterFromMATSim(useMATSimPtParameter);
			module.setLogitScaleParameter(logitScaleParameter);
			module.setBetaCarTravelTime(betaCarTT);
			module.setBetaCarTravelTimePower2(betaCarTTPower);
			module.setBetaCarLnTravelTime(betaCarLnTT);
			module.setBetaCarTravelDistance(betaCarTD);
			module.setBetaCarTravelDistancePower2(betaCarTDPower);
			module.setBetaCarLnTravelDistance(betaCarLnTD);
			module.setBetaCarTravelMonetaryCost(betaCarTMC);
			module.setBetaCarTravelMonetaryCostPower2(betaCarTMCPower);
			module.setBetaCarLnTravelMonetaryCost(betaCarLnTMC);
			module.setBetaBikeTravelTime(betaBikeTT);
			module.setBetaBikeTravelTimePower2(betaBikeTTPower);
			module.setBetaBikeLnTravelTime(betaBikeLnTT);
			module.setBetaBikeTravelDistance(betaBikeTD);
			module.setBetaBikeTravelDistancePower2(betaBikeTDPower);
			module.setBetaBikeLnTravelDistance(betaBikeLnTD);
			module.setBetaBikeTravelMonetaryCost(betaBikeTMC);
			module.setBetaBikeTravelMonetaryCostPower2(betaBikeTMCPower);
			module.setBetaBikeLnTravelMonetaryCost(betaBikeLnTMC);
			module.setBetaWalkTravelTime(betaWalkTT);
			module.setBetaWalkTravelTimePower2(betaWalkTTPower);
			module.setBetaWalkLnTravelTime(betaWalkLnTT);
			module.setBetaWalkTravelDistance(betaWalkTD);
			module.setBetaWalkTravelDistancePower2(betaWalkTDPower);
			module.setBetaWalkLnTravelDistance(betaWalkLnTD);
			module.setBetaWalkTravelMonetaryCost(betaWalkTMC);
			module.setBetaWalkTravelMonetaryCostPower2(betaWalkTMCPower);
			module.setBetaWalkLnTravelMonetaryCost(betaWalkLnTMC);
			module.setBetaPtTravelTime(betaPtTT);
			module.setBetaPtTravelTimePower2(betaPtTTPower);
			module.setBetaPtLnTravelTime(betaPtLnTT);
			module.setBetaPtTravelDistance(betaPtTD);
			module.setBetaPtTravelDistancePower2(betaPtTDPower);
			module.setBetaPtLnTravelDistance(betaPtLnTD);
			module.setBetaPtTravelMonetaryCost(betaPtTMC);
			module.setBetaPtTravelMonetaryCostPower2(betaPtTMCPower);
			module.setBetaPtLnTravelMonetaryCost(betaPtLnTMC);
		}

	static UrbanSimParameterConfigModuleV3 getUrbanSimParameterConfigAndPossiblyConvert(Config config) {
		Module m = config.getModule(UrbanSimParameterConfigModuleV3.GROUP_NAME);
		if (m instanceof UrbanSimParameterConfigModuleV3) {
			return (UrbanSimParameterConfigModuleV3) m;
		}
		UrbanSimParameterConfigModuleV3 upcm = new UrbanSimParameterConfigModuleV3(UrbanSimParameterConfigModuleV3.GROUP_NAME);
		config.getModules().put(UrbanSimParameterConfigModuleV3.GROUP_NAME, upcm);
		return upcm;
	}

	static MATSim4UrbanSimControlerConfigModuleV3 getMATSim4UrbaSimControlerConfigAndPossiblyConvert(Config config) {
		Module m = config.getModule(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME);
		if (m instanceof MATSim4UrbanSimControlerConfigModuleV3) {
			return (MATSim4UrbanSimControlerConfigModuleV3) m;
		}
		MATSim4UrbanSimControlerConfigModuleV3 mccm = new MATSim4UrbanSimControlerConfigModuleV3(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME);
		config.getModules().put(MATSim4UrbanSimControlerConfigModuleV3.GROUP_NAME, mccm);
		return mccm;
	}

	static AccessibilityParameterConfigModule getAccessibilityParameterConfigAndPossiblyConvert(Config config) {
		Module m = config.getModule(AccessibilityParameterConfigModule.GROUP_NAME);
		if (m instanceof AccessibilityParameterConfigModule) {
			return (AccessibilityParameterConfigModule) m;
		}
		AccessibilityParameterConfigModule apcm = new AccessibilityParameterConfigModule(AccessibilityParameterConfigModule.GROUP_NAME);
		config.getModules().put(AccessibilityParameterConfigModule.GROUP_NAME, apcm);
		return apcm;
	}

	/**
	 * returns true if an external MATSim config contains the matsim4UrbanSimModule 
	 * with at least one marginal utility for bicycles.
	 * 
	 * @return
	 */
	static boolean useCustomMarginalUtilitiesBike( Module matsim4UrbanSimModule ){
		if(matsim4UrbanSimModule != null) {
			boolean used = (matsim4UrbanSimModule.getValue("betaBikeTravelTime") != null ||
					matsim4UrbanSimModule.getValue("betaBikeTravelTimePower2") != null ||
					matsim4UrbanSimModule.getValue("betaBikeLnTravelTime") != null ||
					matsim4UrbanSimModule.getValue("betaBikeTravelDistance") != null ||
					matsim4UrbanSimModule.getValue("betaBikeTravelDistancePower2") != null ||
					matsim4UrbanSimModule.getValue("betaBikeLnTravelDistance") != null ||
					matsim4UrbanSimModule.getValue("betaBikeTravelCost") != null ||
					matsim4UrbanSimModule.getValue("betaBikeTravelCostPower2") != null ||
					matsim4UrbanSimModule.getValue("betaBikeLnTravelCost") != null);
			if ( used ) {
				log.warn( "At least one of tyour betaBike parameters (matsim4urbansim section in external matsim config) is different from zero; this will probably fail later.") ;
				log.warn( "The current recommendation is to remove all betaBike parameters.") ;
			}
			return used ;
		}
		return false;
	}

	/**
	 * returns a matsim4urbansim parameter as double or zero in case of conversion errors.
	 * 
	 * @param paramName
	 * @return matsim4urbansim parameter as double
	 */
	static double getValueAsDouble(Module module, String paramName){
		if(module != null){
			try{
				double tmp = Double.parseDouble(module.getValue(paramName));
				return tmp;
			} catch(Exception e){}
			return 0.;
		}
		return 0.;
	}

	/**
	 * returns true if an external MATSim config contains the matsim4UrbanSimModule 
	 * with at least one marginal utility for pt.
	 * 
	 * @return
	 */
	static boolean useCustomMarginalUtilitiesPt( Module matsim4UrbanSimModule ){
		if(matsim4UrbanSimModule != null) {
			boolean used = (matsim4UrbanSimModule.getValue("betaPtTravelTime") != null ||
					matsim4UrbanSimModule.getValue("betaPtTravelTimePower2") != null ||
					matsim4UrbanSimModule.getValue("betaPtLnTravelTime") != null ||
					matsim4UrbanSimModule.getValue("betaPtTravelDistance") != null ||
					matsim4UrbanSimModule.getValue("betaPtTravelDistancePower2") != null ||
					matsim4UrbanSimModule.getValue("betaPtLnTravelDistance") != null ||
					matsim4UrbanSimModule.getValue("betaPtTravelCost") != null ||
					matsim4UrbanSimModule.getValue("betaPtTravelCostPower2") != null ||
					matsim4UrbanSimModule.getValue("betaPtLnTravelCost") != null);
			if ( used ) {
				log.warn( "At least one of tyour betaPt parameters (matsim4urbansim section in external matsim config) is different from zero; this will probably fail later.") ;
				log.warn( "The current recommendation is to remove all betaBike parameters.") ;
			}
			return used ;
		}
		return false;
	}

//	/**
//	 * printing GlobalConfigGroupSettings
//	 * @param config TODO
//	 */
//	static void printGlobalConfigGroupSettings(Config config) {
//		GlobalConfigGroup globalCG = (GlobalConfigGroup) config.getModule(GlobalConfigGroup.GROUP_NAME);
//		
//		log.info("GlobalConfigGroup settings:");
//		log.info("Number of Threads: " + globalCG.getNumberOfThreads() + " ...");
//	}

	/**
		 * printing UrbanSimParameterSettings
		 */
		static void printUrbanSimParameterSettings( UrbanSimParameterConfigModuleV3 module) {
			
	//		UrbanSimParameterConfigModuleV3 module = this.getUrbanSimParameterConfig();
			
			log.info("UrbanSimParameter settings:");
			log.info("ProjectName: " + module.getProjectName() );
			log.info("PopulationSamplingRate: " + module.getPopulationSampleRate() );
			log.info("Year: " + module.getYear() ); 
			log.info("OPUS_HOME: " + InternalConstants.getOPUS_HOME() );
			log.info("OPUS_DATA_PATH: " + InternalConstants.OPUS_DATA_PATH );
			log.info("MATSIM_4_OPUS: " + InternalConstants.MATSIM_4_OPUS );
			log.info("MATSIM_4_OPUS_CONIG: " + InternalConstants.MATSIM_4_OPUS_CONFIG );
			log.info("MATSIM_4_OPUS_OUTPUT: " + InternalConstants.MATSIM_4_OPUS_OUTPUT );
			log.info("MATSIM_4_OPUS_TEMP: " + InternalConstants.MATSIM_4_OPUS_TEMP ); 
			log.info("MATSIM_4_OPUS_BACKUP: " + InternalConstants.MATSIM_4_OPUS_BACKUP );
			log.info("(Custom) Test Parameter: " + module.getTestParameter() );
			log.info("UsingShapefileLocationDistribution:" + module.isUsingShapefileLocationDistribution());
			log.info("UrbanSimZoneShapefileLocationDistribution:" + module.getUrbanSimZoneShapefileLocationDistribution());
			log.info("RandomLocationDistributionRadiusForUrbanSimZone:" + module.getUrbanSimZoneRadiusLocationDistribution());
			log.info("Backing Up Run Data: " + module.isBackup() );
			log.info("Is Test Run: " + module.isTestRun() );
		}

	/**
		 * printing MATSim4UrbanSimControlerSettings
		 */
		static void printMATSim4UrbanSimControlerSettings( MATSim4UrbanSimControlerConfigModuleV3 module ) {
			
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
		static void printAccessibilityParameterSettings(AccessibilityParameterConfigModule module) {
			
	//		AccessibilityParameterConfigModule module = getAccessibilityParameterConfig();
			
			// display results
			log.info("AccessibilityParameter settings (only non-zero values):");
			
			log.info("AccessibilityDestinationSamplingRate: " + module.getAccessibilityDestinationSamplingRate());
			log.info("Compute raw sum (not logsum): " + module.usingRawSumsWithoutLn() );
			log.info("Logit Scale Parameter: " + module.usingLogitScaleParameterFromMATSim() ); 
			
			if ( module.getBetaCarTravelTime()!=0 ) 
				log.info("BETA_CAR_TRAVEL_TIMES: " + module.getBetaCarTravelTime() );
			if ( module.getBetaCarTravelTimePower2()!=0 ) 
				log.info("BETA_CAR_TRAVEL_TIMES_POWER: " + module.getBetaCarTravelTimePower2() );
			if ( module.getBetaCarLnTravelTime()!=0 ) 
				log.info("BETA_CAR_LN_TRAVEL_TIMES: " + module.getBetaCarLnTravelTime());
			if ( module.getBetaCarTravelDistance()!=0 ) 
				log.info("BETA_CAR_TRAVEL_DISTANCE: " + module.getBetaCarTravelDistance() );
			if ( module.getBetaCarTravelDistancePower2()!=0 ) 
			log.info("BETA_CAR_TRAVEL_DISTANCE_POWER: " + module.getBetaCarTravelDistancePower2() );
			if ( module.getBetaCarLnTravelDistance()!=0 ) 
			log.info("BETA_CAR_LN_TRAVEL_DISTANCE: " + module.getBetaCarLnTravelDistance() );
			if ( module.getBetaCarTravelMonetaryCost()!=0 ) 
			log.info("BETA_CAR_TRAVEL_MONETARY_COSTS: " + module.getBetaCarTravelMonetaryCost() );
			if ( module.getBetaCarTravelMonetaryCostPower2()!=0 ) 
			log.info("BETA_CAR_TRAVEL_MONETARY_COSTS_POWER: " + module.getBetaCarTravelMonetaryCostPower2() );
			if ( module.getBetaCarLnTravelMonetaryCost()!=0 ) 
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

//	/**
//	 * printing NetworkConfigGroupSettings
//	 * @param config TODO
//	 */
//	static void printNetworkConfigGroupSettings(Config config) {
//		NetworkConfigGroup networkCG = (NetworkConfigGroup) config.getModule(NetworkConfigGroup.GROUP_NAME);
//
//		log.info("Some NetworkConfigGroup settings:");
//		log.info("Network: " + networkCG.getInputFile());
//	}
//
//	/**
//	 * printing PlansConfigGroupSettings
//	 * @param config TODO
//	 */
//	static void printPlansConfigGroupSettings(Config config) {
//		PlansConfigGroup plansCG = (PlansConfigGroup) config.getModule(PlansConfigGroup.GROUP_NAME);
//		
//		log.info("Some PlansConfigGroup setting:");
//		log.info("Input plans file set to: " + plansCG.getInputFile());
//	}
//
//	/**
//	 * printing StrategyConfigGroupSettings
//	 * @param config TODO
//	 */
//	static void printStrategyConfigGroupSettings(Config config) {
//		
//		StrategyConfigGroup strategyCG = (StrategyConfigGroup) config.getModule(StrategyConfigGroup.GROUP_NAME);
//		
//		log.info("Some StrategyConfigGroup settings:");
//		Iterator<StrategySettings> iteratorStrategyCG = strategyCG.getStrategySettings().iterator();
//		while(iteratorStrategyCG.hasNext()){
//			StrategySettings strategySettings = iteratorStrategyCG.next();
//			log.info("Strategy_"+strategySettings.getId()+ ":" + strategySettings.getModuleName() + " Probability: " + strategySettings.getProbability() + " Disable After Itereation: " + strategySettings.getDisableAfter() ); 
//		}
//		log.info("Max agent plan memory size: " + strategyCG.getMaxAgentPlanMemorySize());
//	}
//
//	/**
//	 * printing ControlerConfigGroupSetting
//	 * @param config TODO
//	 */
//	static void printControlerConfigGroupSetting(Config config) {
//		ControlerConfigGroup controlerCG = (ControlerConfigGroup) config.getModule(ControlerConfigGroup.GROUP_NAME);
//		
//		log.info("Some ControlerConfigGroup settings:");
//		log.info("FirstIteration: " + controlerCG.getFirstIteration());
//		log.info("LastIteration: " + controlerCG.getLastIteration());
//		log.info("MATSim output directory: " +  controlerCG.getOutputDirectory());
//		log.info("Mobsim: " + controlerCG.getMobsim());
//	}
//
//	/**
//	 * printing PlanCalcScoreSettings
//	 * @param config TODO
//	 */
//	static void printPlanCalcScoreSettings(Config config) {
//		
//		ActivityParams homeActivity = config.planCalcScore().getActivityParams("home");
//		ActivityParams workActivity = config.planCalcScore().getActivityParams("work");
//		
//		log.info("Some PlanCalcScore settings:");
//		log.info("Activity_Type_0: " + homeActivity.getType() + " Typical Duration Activity_Type_0: " + homeActivity.getTypicalDuration());
//		log.info("Activity_Type_1: " + workActivity.getType() + " Typical Duration Activity_Type_1: " + workActivity.getTypicalDuration());
//		log.info("Opening Time Activity_Type_1: " + workActivity.getOpeningTime()); 
//		log.info("Latest Start Time Activity_Type_1: " + workActivity.getLatestStartTime());
//	}
//
//	/**
//	 * printing QSimConfigGroupSettings
//	 * @param config TODO
//	 */
//	static void printQSimConfigGroupSettings(Config config) {
//		QSimConfigGroup qsimCG = config.getQSimConfigGroup();
//		
//		log.info("QSimConfigGroup settings:");
//		log.info("Number of Threads: " + qsimCG.getNumberOfThreads());
//		log.info("FlowCapFactor (= population sampling rate): "+ config.getQSimConfigGroup().getFlowCapFactor());
//		log.info("StorageCapFactor: " + config.getQSimConfigGroup().getStorageCapFactor() );//+ " (with correction factor = " + storageCapCorrectionFactor + ")" );
//		// log.info("RemoveStuckVehicles: " + (removeStuckVehicles?"True":"False") );
//		log.info("StuckTime: " + config.getQSimConfigGroup().getStuckTime());
//		log.info("End Time: " + qsimCG.getEndTime());
//	}
//
	static final void checkConfigConsistencyAndWriteToLog(Config config, final String message) {
		String newline = System.getProperty("line.separator");// use native line endings for logfile
		log.info(newline + newline + message + ":");
		StringWriter writer = new StringWriter();
		new ConfigWriter(config).writeStream(new PrintWriter(writer), newline);
		log.info(newline + "Complete config dump:" + newline + writer.getBuffer().toString());
		log.info("Complete config dump done.");
		log.info("Checking consistency of config...");
		config.checkConsistency();
		log.info("Checking consistency of config done.");
		log.info("("+message+")" + newline + newline ) ;
	}
//
//	/**
//	 * printing PlanCalcRouteGroupSettings
//	 * @param config TODO
//	 */
//	static void printPlanCalcRouteGroupSettings(Config config) {
//		log.info("PlanCalcRouteGroup settings:");							 
//		log.info("Walk Speed: " + config.plansCalcRoute().getWalkSpeed() );
//		log.info("Bike Speed: " + config.plansCalcRoute().getBikeSpeed() );
//		log.info("Pt Speed: " + config.plansCalcRoute().getPtSpeed() );
//		log.info("Beeline Distance Factor: " + config.plansCalcRoute().getBeelineDistanceFactor() );
//	}

}
