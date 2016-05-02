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
package org.matsim.contrib.matsim4urbansim.config;

import org.apache.log4j.Logger;
import org.matsim.api.core.v01.Id;
import org.matsim.contrib.accessibility.AccessibilityConfigGroup;
import org.matsim.contrib.matrixbasedptrouter.MatrixBasedPtRouterConfigGroup;
import org.matsim.contrib.matsim4urbansim.config.modules.M4UControlerConfigModuleV3;
import org.matsim.contrib.matsim4urbansim.config.modules.UrbanSimParameterConfigModuleV3;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.Matsim4UrbansimConfigType;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.Matsim4UrbansimType;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.MatsimConfigType;
import org.matsim.contrib.matsim4urbansim.utils.io.Paths;
import org.matsim.core.config.*;
import org.matsim.core.config.groups.*;
import org.matsim.core.config.groups.PlanCalcScoreConfigGroup.ActivityParams;
import org.matsim.core.config.groups.StrategyConfigGroup.StrategySettings;
import org.matsim.core.config.groups.VspExperimentalConfigGroup.VspDefaultsCheckingLevel;
import org.matsim.core.replanning.strategies.DefaultPlanStrategiesModule;
import org.matsim.core.utils.io.UncheckedIOException;

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;

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
	 * store UrbanSimParameter
	 * 
	 * @param matsim4UrbanSimParameter
	 * @param config TODO
	 */
	static void initAccessibilityConfigGroupParameters(MatsimConfigType matsim4urbansimConfigPart1, Config config){

		// get every single matsim4urbansim/urbansimParameter
		
		// add accessibility parameter
		AccessibilityConfigGroup acm = ConfigUtils.addOrGetModule(config, AccessibilityConfigGroup.GROUP_NAME, AccessibilityConfigGroup.class);
		
		acm.setCellSizeCellBasedAccessibility( matsim4urbansimConfigPart1.getCellSize().intValue() );
		
		// by shape file
		if(matsim4urbansimConfigPart1.isAccessibilityComputationAreaFromShapeFile()){
			
			acm.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromShapeFile.toString());

			if(matsim4urbansimConfigPart1.getStudyAreaBoundaryShapeFile()  != null &&
			  (new File(matsim4urbansimConfigPart1.getStudyAreaBoundaryShapeFile().getInputFile())).exists())
				acm.setShapeFileCellBasedAccessibility( matsim4urbansimConfigPart1.getStudyAreaBoundaryShapeFile().getInputFile() );
			else
				throw new RuntimeException("Study area boundary shape file not found! Given shape file location:" 
			+ matsim4urbansimConfigPart1.getStudyAreaBoundaryShapeFile());
		}
		// by bounding box
		if(matsim4urbansimConfigPart1.isAccessibilityComputationAreaFromBoundingBox()){
			
			acm.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromBoundingBox.toString());
			
			acm.setBoundingBoxBottom(matsim4urbansimConfigPart1.getBoundingBoxBottom()) ;
			acm.setBoundingBoxTop(matsim4urbansimConfigPart1.getBoundingBoxTop()) ;
			acm.setBoundingBoxLeft(matsim4urbansimConfigPart1.getBoundingBoxLeft()) ;
			acm.setBoundingBoxRight(matsim4urbansimConfigPart1.getBoundingBoxRight()) ;
		}
		// by network
		if(matsim4urbansimConfigPart1.isAccessibilityComputationAreaFromNetwork()){
			
			acm.setAreaOfAccessibilityComputation(AccessibilityConfigGroup.AreaOfAccesssibilityComputation.fromNetwork.toString());
		}
	}
	
	/**
	 * 
	 * @param matsim4urbansimConfigPart1
	 * @param config
	 */
	static void initM4UControlerConfigModuleV3Parameters(MatsimConfigType matsim4urbansimConfigPart1, Config config){
		
		M4UControlerConfigModuleV3 module = getMATSim4UrbaSimControlerConfigAndPossiblyConvert(config);
		
		module.setUrbansimZoneRandomLocationDistributionRadius(matsim4urbansimConfigPart1.getUrbansimZoneRandomLocationDistributionByRadius());
		module.setUrbansimZoneRandomLocationDistributionShapeFile(matsim4urbansimConfigPart1.getUrbansimZoneRandomLocationDistributionByShapeFile());
		module.setUsingShapefileLocationDistribution( (new File(matsim4urbansimConfigPart1.getUrbansimZoneRandomLocationDistributionByShapeFile())).exists() );
		log.info("This message affects UrbanSim ZONE applications only:");
		if(module.usingShapefileLocationDistribution()){
			log.info("Found a zone shape file: " + module.getUrbansimZoneRandomLocationDistributionShapeFile());
			log.info("This activates the distribution of persons within a zone using the zone boundaries of this shape file."); 
		}
		else{
			log.info("Persons are distributed within a zone using the zone centroid and a radius of " + module.getUrbanSimZoneRadiusLocationDistribution() + " meter.");
			log.info("In order to use exact zone boundaries for your sceanrio provide a zone shape file and enter the file location in the external MATSim config file as follows:");
			log.info("<module name=\"matsim4urbansimParameter\" >");
			log.info("<param name=\"urbanSimZoneShapefileLocationDistribution\" value=\"/path/to/shapeFile\" />");
			log.info("</module>");
		}
		if ( matsim4urbansimConfigPart1.getWarmStartPlansFile()==null ) {
			module.setWarmStart(false) ;
		} else if ( matsim4urbansimConfigPart1.getWarmStartPlansFile() == null ) {
			module.setWarmStart(false) ;
		} else {
			module.setWarmStart( (new File(matsim4urbansimConfigPart1.getWarmStartPlansFile().getInputFile())).exists() );
		}
		module.setWarmStartPlansLocation(matsim4urbansimConfigPart1.getWarmStartPlansFile().getInputFile());
		module.setHotStart(matsim4urbansimConfigPart1.isUseHotStart());
		module.setHotStartPlansFileLocation(matsim4urbansimConfigPart1.getHotStartPlansFile().getInputFile());
		
		// setting right plans file
		if(module.usingWarmStart()){
			PlansConfigGroup pcg = config.plans();
			boolean setHotSart = false;
			
			// check if hot start is switched on and if the hot start plans file file is available
			if(module.usingHotStart()){
				File f = new File(module.getHotStartPlansFileLocation());
				if(f.exists()){
					log.info("Hot start flag is set to true. Hot start plans file found at : " + module.getHotStartPlansFileLocation());
					pcg.setInputFile(module.getHotStartPlansFileLocation());
					log.info("Setting hot start plans file done!");
					setHotSart = true;
				}
				else{
					log.warn("Hot start flag is set to true. However, the hot start plans file is not found at given location : " + module.getHotStartPlansFileLocation());
					log.warn("Warm start will be used instead!");
					log.warn("This is be ok for the first time when MATSim is called by UrbanSim. Next time MATSim should start with hot start.");
				}
			}
			
			if(!setHotSart){
				File f = new File(module.getWarmStartPlansFileLocation());
				if(f.exists()){
					log.info("Warm start plans file found at : " + module.getWarmStartPlansFileLocation());
					pcg.setInputFile(module.getWarmStartPlansFileLocation());
					log.info("Setting warm start plans file done!");
				}
				else{
					throw new RuntimeException("Given warm start plans file not found : " + module.getWarmStartPlansFileLocation() + " Please check the path in your config file!");
				}
			}
		}
	}

	/**
	 * 
	 * @param matsim4urbansimConfigPart2
	 * @param config
	 */
	static void initUrbanSimParameterConfigModuleV3Parameters(Matsim4UrbansimType matsim4urbansimConfigPart2, Config config){
		
		UrbanSimParameterConfigModuleV3 module = getUrbanSimParameterConfigAndPossiblyConvert(config);
		
		module.setPopulationSampleRate(matsim4urbansimConfigPart2.getPopulationSamplingRate());
		module.setYear(matsim4urbansimConfigPart2.getYear().intValue());
		module.setOpusHome(Paths.checkPathEnding(matsim4urbansimConfigPart2.getOpusHome()));
		module.setOpusDataPath(Paths.checkPathEnding(matsim4urbansimConfigPart2.getOpusDataPath()));
		module.setMATSim4Opus(Paths.checkPathEnding(matsim4urbansimConfigPart2.getMatsim4Opus()));
		module.setMATSim4OpusConfig(Paths.checkPathEnding(matsim4urbansimConfigPart2.getMatsim4OpusConfig()));
		module.setMATSim4OpusOutput(Paths.checkPathEnding(matsim4urbansimConfigPart2.getMatsim4OpusOutput()));
		module.setMATSim4OpusTemp(Paths.checkPathEnding(matsim4urbansimConfigPart2.getMatsim4OpusTemp()));
		module.setMATSim4OpusBackup(Paths.checkPathEnding(matsim4urbansimConfigPart2.getMatsim4Opus() + "/backup"));
		module.setCustomParameter(matsim4urbansimConfigPart2.getCustomParameter());
		module.setUsingZone2ZoneImpedance(matsim4urbansimConfigPart2.isZone2ZoneImpedance());
		module.setUsingAgentPerformance(matsim4urbansimConfigPart2.isAgentPerfomance());
		module.setUsingZoneBasedAccessibility(matsim4urbansimConfigPart2.isZoneBasedAccessibility());
		module.setUsingGridBasedAccessibility(matsim4urbansimConfigPart2.isParcelBasedAccessibility());
		module.setBackup(matsim4urbansimConfigPart2.isBackupRunData());
		
		// setting paths into constants structure
//		InternalConstants.setOPUS_HOME( Paths.checkPathEnding(module.getOpusHome()) );
//		InternalConstants.OPUS_DATA_PATH = Paths.checkPathEnding(module.getOpusDataPath());
//		InternalConstants.MATSIM_4_OPUS = Paths.checkPathEnding(module.getMATSim4Opus());
//		InternalConstants.MATSIM_4_OPUS_CONFIG = Paths.checkPathEnding(module.getMATSim4OpusConfig());
//		InternalConstants.MATSIM_4_OPUS_OUTPUT = Paths.checkPathEnding(module.getMATSim4OpusOutput());
//		InternalConstants.MATSIM_4_OPUS_TEMP = Paths.checkPathEnding(module.getMATSim4OpusTemp());
//		InternalConstants.MATSIM_4_OPUS_BACKUP = Paths.checkPathEnding(module.getMATSim4OpusBackup());
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
	static void initNetwork(MatsimConfigType matsim4urbansimConfigPart1, Config config){
		log.info("Setting NetworkConfigGroup to config...");
		config.network().setInputFile( matsim4urbansimConfigPart1.getNetwork().getInputFile() );
		log.info("...done!");
	}

	/**
	 * setting controler parameter
	 * 
	 * @param matsim4urbansimConfigPart1
	 * @param config TODO
	 */
	static void initControler(MatsimConfigType matsim4urbansimConfigPart1, Config config){
		log.info("Setting ControlerConfigGroup to config...");
		
		int firstIteration = matsim4urbansimConfigPart1.getFirstIteration().intValue();
		int lastIteration =matsim4urbansimConfigPart1.getLastIteration().intValue();
		ControlerConfigGroup controlerCG = config.controler() ;
		// set values
		controlerCG.setFirstIteration( firstIteration );
		controlerCG.setLastIteration( lastIteration);
		UrbanSimParameterConfigModuleV3 module = getUrbanSimParameterConfigAndPossiblyConvert(config);
		controlerCG.setOutputDirectory( module.getMATSim4OpusOutput() );
		controlerCG.setWriteSnapshotsInterval( 0 ); // disabling snapshots

		// set Qsim
		controlerCG.setMobsim(QSimConfigGroup.GROUP_NAME);
		// yyyy if we do this, do we not get a warning that we should also put in a corresponding config group?  Maybe done later ...
		
		log.info("...done!");
	}

	/**
	 * setting planCalcScore parameter
	 * 
	 * @param matsim4urbansimConfigPart1
	 * @param config TODO
	 */
	static void initPlanCalcScore(MatsimConfigType matsim4urbansimConfigPart1, Config config){
		log.info("Setting PlanCalcScore to config...");
		String activityType_0 = matsim4urbansimConfigPart1.getActivityType0();
		String activityType_1 = matsim4urbansimConfigPart1.getActivityType1();

		ActivityParams homeActivity = new ActivityParams(activityType_0);
		homeActivity.setTypicalDuration( matsim4urbansimConfigPart1.getHomeActivityTypicalDuration().intValue() ); 	// should be something like 12*60*60

		ActivityParams workActivity = new ActivityParams(activityType_1);
		workActivity.setTypicalDuration( matsim4urbansimConfigPart1.getWorkActivityTypicalDuration().intValue() );	// should be something like 8*60*60
		workActivity.setOpeningTime( matsim4urbansimConfigPart1.getWorkActivityOpeningTime().intValue() );			// should be something like 7*60*60
		workActivity.setLatestStartTime( matsim4urbansimConfigPart1.getWorkActivityLatestStartTime().intValue() );	// should be something like 9*60*60
		config.planCalcScore().addActivityParams( homeActivity );
		config.planCalcScore().addActivityParams( workActivity );
		
		config.planCalcScore().setBrainExpBeta(1.) ;

		log.info("...done!");
	}

	/**
	 * setting qsim
	 * @param matsim4urbansimConfigPart2 TODO
	 * @param config TODO
	 */
	static void initQSim(Matsim4UrbansimType matsim4urbansimConfigPart2 , Config config){
		log.info("Setting QSimConfigGroup to config...");

		QSimConfigGroup qsimCG = config.qsim();

		double popSampling = matsim4urbansimConfigPart2.getPopulationSamplingRate();
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
			double popSamplingBefore = popSampling ;
			popSampling = 0.01;
			log.warn("Raised popSampling rate from " + popSamplingBefore + 
					" to " + popSampling + " to to avoid errors while calulating the correction factor ...");
		}
		
		log.info("FlowCapFactor and StorageCapFactor are adapted to the population sampling rate (sampling rate = " + popSampling + ").");
		// setting FlowCapFactor == population sampling rate (no correction factor needed here)
		qsimCG.setFlowCapFactor( popSampling );	
		
			
		qsimCG.setStorageCapFactor( popSampling * Math.pow(popSampling, -0.25) ) ;   // same as: sample / Math.sqrt(Math.sqrt(sample))
		// yy and same as Math.pow( popSampling, 0.75 ), isn't it? 

		qsimCG.setRemoveStuckVehicles( false );
		qsimCG.setStuckTime(10.);
		qsimCG.setEndTime(30.*3600.); // 30h

		log.info("...done!");
	}

	static void initStrategy(Config config){
		// It is no longer possible to configure any of this from the urbansim side.  However, something still needs to be set for
		// matsim, which is defined here.  
		// Unfortunately, this makes it a bit tricky to use your own matsim config, since depending on the strategy settings id one
		// either adds to these strategy settings, or overrides them, or modifies them.  It is, however, also unclear what else to do; this
		// is just the standard matsim behavior.  kai, aug'15
		
		log.info("Setting StrategyConfigGroup to config...");

		config.strategy().setMaxAgentPlanMemorySize( 5 );

		StrategyConfigGroup.StrategySettings changeExpBeta = new StrategyConfigGroup.StrategySettings(Id.create(1, StrategySettings.class));
		changeExpBeta.setStrategyName(DefaultPlanStrategiesModule.DefaultSelector.ChangeExpBeta.toString());
		changeExpBeta.setWeight( 0.8 ) ;
		config.strategy().addStrategySettings(changeExpBeta);

		StrategyConfigGroup.StrategySettings timeAlocationMutator = new StrategyConfigGroup.StrategySettings(Id.create(2, StrategySettings.class));
		timeAlocationMutator.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.TimeAllocationMutator.toString());
		timeAlocationMutator.setWeight( 0.1 ); 
//		timeAlocationMutator.setDisableAfter(disableStrategyAfterIteration(config)); // now matsim default
		config.strategy().addStrategySettings(timeAlocationMutator);
		config.timeAllocationMutator().setMutationRange(7200.) ;

		StrategyConfigGroup.StrategySettings reroute = new StrategyConfigGroup.StrategySettings(Id.create(3, StrategySettings.class));
		reroute.setStrategyName(DefaultPlanStrategiesModule.DefaultStrategy.ReRoute.toString());
		reroute.setWeight( 0.1 );
//		reroute.setDisableAfter(disableStrategyAfterIteration(config)); // now matsim default
		config.strategy().addStrategySettings(reroute);

		log.info("...done!");
	}

//	private static int disableStrategyAfterIteration(Config config) {
//		return (int) Math.ceil(config.controler().getLastIteration() * 0.8);
//	}
	// no longer needed. kai, aug'15
	
	/**
	 * loads the external config into a temporary structure
	 * this is done to initialize MATSim4UrbanSim settings that are defined
	 * in the external MATSim config
	 * 
	 * @param matsimParameter
	 * @param config TODO
	 * @throws UncheckedIOException
	 */
	static void loadExternalConfigAndOverwriteMATSim4UrbanSimSettings(String externalMATSimConfigFileName, Config config) throws UncheckedIOException {
		// check if external MATsim config is given
		if(externalMATSimConfigFileName != null && Paths.pathExsits(externalMATSimConfigFileName)){

			log.info("Loading settings from external MATSim config: " + externalMATSimConfigFileName);
			log.warn("NOTE: MATSim4UrbanSim settings will be overwritten by settings in the external config! Make sure that this is what you intended!");
			new ConfigReader(config).parse(externalMATSimConfigFileName);
			log.info("... loading settings done!");
		}
	}

	/**
	 * creates an empty MATSim config to be filled by MATSim4UrbanSim + external MATSim config settings
	 */
	public static Config createEmptyConfigWithSomeDefaults() {
		log.info("Creating an empty MATSim scenario.");
		Config config = ConfigUtils.createConfig();

		//"materialize" the local config groups:
		config.addModule(	new UrbanSimParameterConfigModuleV3() ) ;
		config.addModule( new M4UControlerConfigModuleV3() );
		config.addModule(	new AccessibilityConfigGroup() ) ;
		config.addModule( new MatrixBasedPtRouterConfigGroup() ) ;
		
		// set some defaults:
		config.vspExperimental().setVspDefaultsCheckingLevel( VspDefaultsCheckingLevel.abort ) ;
		config.plans().setActivityDurationInterpretation(PlansConfigGroup.ActivityDurationInterpretation.tryEndTimeThenDuration ) ;
		config.plans().setRemovingUnneccessaryPlanAttributes(true) ;
		
		config.strategy().setFractionOfIterationsToDisableInnovation(0.8) ;

		return config ;
	}

	/**
	 * loading, validating and initializing MATSim config.
	 */
	static Matsim4UrbansimConfigType unmarschal(String matsim4urbansimConfigFilename){

		// JAXBUnmarschal reads the UrbanSim generated MATSim config, validates it against
		// the current xsd (checks e.g. the presents and data type of parameter) and generates
		// a Java object representing the config file.
		JAXBUnmarshalV3 um = new JAXBUnmarshalV3();
		
		Matsim4UrbansimConfigType m4uConfigType = null;
		m4uConfigType = um.unmarshal(matsim4urbansimConfigFilename);
		
		if(m4uConfigType == null) {
			throw new RuntimeException("Unmarschalling failed. SHUTDOWN MATSim!");
		}

		return m4uConfigType;
		
	}

	public static UrbanSimParameterConfigModuleV3 getUrbanSimParameterConfigAndPossiblyConvert(Config config) {
		ConfigGroup m = config.getModule(UrbanSimParameterConfigModuleV3.GROUP_NAME);
		if (m instanceof UrbanSimParameterConfigModuleV3) {
			return (UrbanSimParameterConfigModuleV3) m;
		}
		UrbanSimParameterConfigModuleV3 upcm = new UrbanSimParameterConfigModuleV3();
		config.addModule( upcm ) ;
		return upcm;
	}

	public static M4UControlerConfigModuleV3 getMATSim4UrbaSimControlerConfigAndPossiblyConvert(Config config) {
		ConfigGroup m = config.getModule(M4UControlerConfigModuleV3.GROUP_NAME);
		if (m instanceof M4UControlerConfigModuleV3) {
			return (M4UControlerConfigModuleV3) m;
		}
		M4UControlerConfigModuleV3 mccm = new M4UControlerConfigModuleV3();
		config.addModule(mccm ) ;
		return mccm;
	}

	/**
	 * printing UrbanSimParameterSettings
	 */
	static void printUrbanSimParameterSettings( UrbanSimParameterConfigModuleV3 module) {

		//		UrbanSimParameterConfigModuleV3 module = this.getUrbanSimParameterConfig();

		log.info("UrbanSimParameter settings:");
		log.info("PopulationSamplingRate: " + module.getPopulationSampleRate() );
		log.info("Year: " + module.getYear() ); 
		log.info("OPUS_HOME: " + module.getOpusHome() );
		log.info("OPUS_DATA_PATH: " + module.getOpusDataPath() );
		log.info("MATSIM_4_OPUS: " + module.getMATSim4Opus() );
		log.info("MATSIM_4_OPUS_CONIG: " + module.getMATSim4OpusConfig() );
		log.info("MATSIM_4_OPUS_OUTPUT: " + module.getMATSim4OpusOutput() );
		log.info("MATSIM_4_OPUS_TEMP: " + module.getMATSim4OpusTemp() ); 
		log.info("MATSIM_4_OPUS_BACKUP: " + module.getMATSim4OpusBackup());
		log.info("Compute Agent-performance: " + module.usingAgentPerformance() );
		log.info("Compute Zone2Zone Impedance Matrix: " + module.usingZone2ZoneImpedance() ); 
		log.info("Compute Zone-Based Accessibilities: " + module.usingZoneBasedAccessibility() );
		log.info("Compute Grid-Based Accessibilities: " + module.usingGridBasedAccessibility() );
		log.info("(Custom) Test Parameter: " + module.getCustomParameter() );
		log.info("Backing Up Run Data: " + module.isBackup() );
	}

	/**
	 * printing MATSim4UrbanSimControlerSettings
	 */
	static void printMATSim4UrbanSimControlerSettings( M4UControlerConfigModuleV3 module ) {

		// view results
		log.info("MATSim4UrbanSimControler settings:");
		log.info("UsingShapefileLocationDistribution:" + module.usingShapefileLocationDistribution());
		log.info("UrbanSimZoneShapefileLocationDistribution:" + module.getUrbansimZoneRandomLocationDistributionShapeFile());
		log.info("RandomLocationDistributionRadiusForUrbanSimZone:" + module.getUrbanSimZoneRadiusLocationDistribution());
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
