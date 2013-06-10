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

import org.apache.log4j.Logger;
import org.matsim.contrib.matsim4opus.config.modules.AccessibilityConfigGroup;
import org.matsim.contrib.matsim4opus.config.modules.M4UControlerConfigModuleV3;
import org.matsim.contrib.matsim4opus.config.modules.UrbanSimParameterConfigModuleV3;
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
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.QSimConfigGroup;
import org.matsim.core.config.groups.StrategyConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspExperimentalConfigKey;
import org.matsim.core.controler.PlanStrategyRegistrar.Names;
import org.matsim.core.controler.PlanStrategyRegistrar.Selector;
import org.matsim.core.utils.io.UncheckedIOException;

/**
 * @author nagel
 *
 */
public class M4UConfigUtils {

	private static final Logger log = Logger.getLogger(M4UConfigUtils.class);
	// module and param names for matsim4urbansim settings stored in an external MATSim config file
	public static final String MATSIM4URBANSIM_MODULE_EXTERNAL_CONFIG = "matsim4urbansimParameter";// module
	public static final String URBANSIM_ZONE_SHAPEFILE_LOCATION_DISTRIBUTION = "urbanSimZoneShapefileLocationDistribution";

	/**
	 * Setting 
	 * 
	 * @param matsim4UrbanSimParameter
	 * @param config TODO
	 */
	static void initMATSim4UrbanSimControler(Matsim4UrbansimType matsim4UrbanSimParameter, Config config){

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

		// set parameter in module 
		M4UControlerConfigModuleV3 module = getMATSim4UrbaSimControlerConfigAndPossiblyConvert(config);
		module.setAgentPerformance(matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isAgentPerformance());
		module.setZone2ZoneImpedance(matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isZone2ZoneImpedance());
		module.setZoneBasedAccessibility(matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isZoneBasedAccessibility());
		module.setCellBasedAccessibility(computeCellBasedAccessibility);
		
		AccessibilityConfigGroup acm = M4UAccessibilityConfigUtils.getConfigModuleAndPossiblyConvert(config) ;
		acm.setUsingCustomBoundingBox(matsim4UrbanSimParameter.getMatsim4UrbansimContoler().isUseCustomBoundingBox());
		acm.setBoundingBoxLeft(matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getBoundingBoxLeft());
		acm.setBoundingBoxBottom(matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getBoundingBoxBottom());
		acm.setBoundingBoxRight(matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getBoundingBoxRight());
		acm.setBoundingBoxTop(matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getBoundingBoxTop());
		acm.setCellSizeCellBasedAccessibility(matsim4UrbanSimParameter.getMatsim4UrbansimContoler().getCellSizeCellBasedAccessibility().intValue());
		acm.setCellBasedAccessibilityShapeFile(computeCellbasedAccessibilityShapeFile);
		acm.setCellBasedAccessibilityNetwork(computeCellBasedAccessibilityNetwork);
		acm.setShapeFileCellBasedAccessibility(shapeFile);
	}

	/**
	 * store UrbanSimParameter
	 * 
	 * @param matsim4UrbanSimParameter
	 * @param matsim4urbansimModule TODO
	 * @param config TODO
	 */
	static void initUrbanSimParameters(Matsim4UrbansimType matsim4UrbanSimParameter, Module matsim4urbansimModule, Config config){

		// get every single matsim4urbansim/urbansimParameter
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
		// module.setProjectName(""); // not needed anymore dec'12
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
	 static Module getM4UModuleFromExternalConfig(String externalMATSimConfigFilename) throws UncheckedIOException {

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
		config.network().setInputFile( matsimParameter.getNetwork().getInputFile() );
		log.info("...done!");
	}

	/**
	 * setting input plans file (for warm/hot start)
	 * 
	 * @param matsimParameter
	 * @param config TODO
	 */
	static void insertPlansParamsAndConfigureWarmOrHotStart(ConfigType matsimParameter, Config config){
		log.info("Checking for warm or hot start...");
		// get plans file for hot start
		String hotStartFileName = matsimParameter.getHotStartPlansFile().getInputFile();
		// get plans file for warm start 
		String warmStartFileName = matsimParameter.getInputPlansFile().getInputFile();

		M4UControlerConfigModuleV3 module = getMATSim4UrbaSimControlerConfigAndPossiblyConvert(config);
		
		if ( !hotStartFileName.equals("") ) {
			log.info("Hot start detected.  Will use plans file from last matsim run, or warm start plans file if this is the first matsim call within this urbansim run.") ;
			module.setHotStart(true);
		} else if ( !warmStartFileName.equals("") ) {
			log.info("Warm start detected.  Will use warm start plans file with name " + warmStartFileName ) ;
			module.setWarmStart(true);
		} else{
			// else it is a cold start:
			log.info("Cold Start (no plans file) detected!");
			module.setColdStart(true);
		}

		if( !hotStartFileName.equals("")  && (new File(hotStartFileName)).exists() ) {
			// if the hot start file name is given, and the file exists, then it is used:
			log.info("Using hot start plans file from last matsim run: " + hotStartFileName );
			config.plans().setInputFile( hotStartFileName ) ;
		}
		else if( !warmStartFileName.equals("") ){
			// else if the warm start file name is given, then it is used:
			config.plans().setInputFile( warmStartFileName ) ;
		}

		if(!hotStartFileName.equals("")){
			log.info("Hot start: The resulting plans file after this MATSim run is stored at a specified place for the following MATSim run.");
			log.info("The specified place is : " + hotStartFileName);
			module.setHotStartTargetLocation(hotStartFileName);
		}
		else { 
			module.setHotStartTargetLocation("");
		}

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
		ControlerConfigGroup controlerCG = config.controler() ;
		// set values
		controlerCG.setFirstIteration( firstIteration );
		controlerCG.setLastIteration( lastIteration);
		controlerCG.setOutputDirectory( InternalConstants.MATSIM_4_OPUS_OUTPUT );
		// yyyy don't use static variables (this is a variable albeit it claims to be a constant).  kai, may'13

//		controlerCG.setSnapshotFormat(Arrays.asList("otfvis")); // I don't think that this is necessary.  kai, may'13
		controlerCG.setWriteSnapshotsInterval( 0 ); // disabling snapshots

		// set Qsim
		controlerCG.setMobsim(QSimConfigGroup.GROUP_NAME);
		// yyyy if we do this, do we not get a warning that we should also put in a corresponding config group?  Maybe done later ...

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
//		qsimCG.setNumberOfThreads(1);
		// should leave it to matsim defaults

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

		qsimCG.setRemoveStuckVehicles( false );
		qsimCG.setStuckTime(10.);
		qsimCG.setEndTime(30.*3600.); // 30h

		log.info("...done!");
	}


	/**
	 * setting strategy
	 * @param config TODO
	 */
	static void initStrategy(ConfigType matsim4urbansimConfig, Config config){
		log.info("Setting StrategyConfigGroup to config...");

		config.strategy().setMaxAgentPlanMemorySize( matsim4urbansimConfig.getStrategy().getMaxAgentPlanMemorySize().intValue() );

		StrategyConfigGroup.StrategySettings changeExpBeta = new StrategyConfigGroup.StrategySettings(IdFactory.get(1));
		changeExpBeta.setModuleName(Selector.ChangeExpBeta.toString());
		changeExpBeta.setProbability( matsim4urbansimConfig.getStrategy().getChangeExpBetaProbability() ); // should be something like 0.9
		config.strategy().addStrategySettings(changeExpBeta);

		StrategyConfigGroup.StrategySettings timeAlocationMutator = new StrategyConfigGroup.StrategySettings(IdFactory.get(2));
		timeAlocationMutator.setModuleName(Names.TimeAllocationMutator.toString()); 
		timeAlocationMutator.setProbability( matsim4urbansimConfig.getStrategy().getTimeAllocationMutatorProbability() ); // should be something like 0.1
		timeAlocationMutator.setDisableAfter(disableStrategyAfterIteration(config)); // just to be sure
		config.strategy().addStrategySettings(timeAlocationMutator);
		config.timeAllocationMutator().setMutationRange(7200.) ;

		StrategyConfigGroup.StrategySettings reroute = new StrategyConfigGroup.StrategySettings(IdFactory.get(3));
		reroute.setModuleName(Names.ReRoute.toString());  
		reroute.setProbability( matsim4urbansimConfig.getStrategy().getReRouteDijkstraProbability() ); 	// should be something like 0.1
		reroute.setDisableAfter(disableStrategyAfterIteration(config));
		config.strategy().addStrategySettings(reroute);

		log.info("...done!");
	}

	private static int disableStrategyAfterIteration(Config config) {
		return (int) Math.ceil(config.controler().getLastIteration() * 0.8);
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
		config.addModule(	new UrbanSimParameterConfigModuleV3() ) ;
		config.addModule( new M4UControlerConfigModuleV3() );
		config.addModule(	new AccessibilityConfigGroup() ) ;

		// set some defaults:
		VspExperimentalConfigGroup vsp = config.vspExperimental();
		vsp.addParam(VspExperimentalConfigKey.vspDefaultsCheckingLevel, VspExperimentalConfigGroup.ABORT ) ;
		vsp.setActivityDurationInterpretation(VspExperimentalConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration) ;
		vsp.setRemovingUnneccessaryPlanAttributes(true) ;
		
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8) ;

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

	public static UrbanSimParameterConfigModuleV3 getUrbanSimParameterConfigAndPossiblyConvert(Config config) {
		Module m = config.getModule(UrbanSimParameterConfigModuleV3.GROUP_NAME);
		if (m instanceof UrbanSimParameterConfigModuleV3) {
			return (UrbanSimParameterConfigModuleV3) m;
		}
		UrbanSimParameterConfigModuleV3 upcm = new UrbanSimParameterConfigModuleV3();
		config.addModule( UrbanSimParameterConfigModuleV3.GROUP_NAME, upcm ) ;
		return upcm;
	}

	public static M4UControlerConfigModuleV3 getMATSim4UrbaSimControlerConfigAndPossiblyConvert(Config config) {
		Module m = config.getModule(M4UControlerConfigModuleV3.GROUP_NAME);
		if (m instanceof M4UControlerConfigModuleV3) {
			return (M4UControlerConfigModuleV3) m;
		}
		M4UControlerConfigModuleV3 mccm = new M4UControlerConfigModuleV3();
		config.addModule(M4UControlerConfigModuleV3.GROUP_NAME, mccm ) ;
		return mccm;
	}

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
	static void printMATSim4UrbanSimControlerSettings( M4UControlerConfigModuleV3 module ) {

		// view results
		log.info("MATSim4UrbanSimControler settings:");
		log.info("Compute Agent-performance: " + module.isAgentPerformance() );
		log.info("Compute Zone2Zone Impedance Matrix: " + module.isZone2ZoneImpedance() ); 
		log.info("Compute Zone-Based Accessibilities: " + module.isZoneBasedAccessibility() );
	}

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

}
