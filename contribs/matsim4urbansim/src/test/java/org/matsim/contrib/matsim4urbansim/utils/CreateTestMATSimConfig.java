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
package org.matsim.contrib.matsim4urbansim.utils;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.math.BigInteger;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.matsim.contrib.matrixbasedptrouter.utils.TempDirectoryUtil;
import org.matsim.contrib.matsim4urbansim.constants.InternalConstants;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfig2.AccessibilityParameterType;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfig2.ConfigType;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfig2.ControlerType;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfig2.FileType;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfig2.InputPlansFileType;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfig2.Matsim4UrbansimContolerType;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfig2.Matsim4UrbansimType;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfig2.MatsimConfigType;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfig2.ObjectFactory;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfig2.PlanCalcScoreType;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfig2.UrbansimParameterType;
import org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.Matsim4UrbansimConfigType;
import org.matsim.contrib.matsim4urbansim.utils.io.LoadFile;
import org.matsim.core.utils.io.IOUtils;
import org.matsim.core.utils.io.UncheckedIOException;
import org.xml.sax.SAXException;


/**
 * @author thomas
 *
 */
public class CreateTestMATSimConfig {
	private static final Logger log = Logger.getLogger(CreateTestMATSimConfig.class) ;
	
	public static final int COLD_START = 0;
	public static final int WARRM_START = 1;
	public static final int HOT_START = 2;
	
	public static final String DUMMY_FILE = "/dummy.xml";
	
	private int startMode;
	protected String dummyPath;
	
	// yy why is all of this public?  could you please write a comment why that design decision was made?  thx.  kai, apr'13
	
	private String matsimExternalConfigFileName 				= "";
	public String networkInputFileName 	 					= "";
	public String inputPlansFileName 						= "";
	public String hotstartPlansFileName						= "";
	public BigInteger firstIteration					= new BigInteger("0");
	public BigInteger lastIteration						= new BigInteger("1");
	public String activityType_0						= "home";
	public String activityType_1						= "work";
	public BigInteger homeActivityTypicalDuration		= new BigInteger("43200");	
	public BigInteger workActivityTypicalDuration		= new BigInteger("28800");	
	public BigInteger workActivityOpeningTime			= new BigInteger("25200");
	public BigInteger workActivityLatestStartTime		= new BigInteger("32400");
	public BigInteger maxAgentPlanMemorySize			= new BigInteger("5");
	public Double timeAllocationMutatorProbability		= 0.1;
	public Double changeExpBetaProbability				= 0.9;
	public Double reRouteDijkstraProbability			= 0.1;
	public Double populationSamplingRate				= 1.0;
	public BigInteger year								= new BigInteger("2000");
	public String opusHome								= "";
	public String opusDataPath							= "";
	public String matsim4opus							= "";
	public String matsim4opusConfig						= "";
	public String matsim4opusOutput 					= "";
	public String matsim4opusTemp						= "";
	public boolean isTestRun							= false;
	public Double randomLocationDistributionRadiusForUrbanSimZone	= 0.;
	public String customParameter							= "";
	public boolean backupRunData						= false;
	public boolean zone2zoneImpedance					= true;
	public boolean agentPerformance						= true;
	public boolean zoneBasedAccessibility				= true;
	public boolean cellBasedAccessibility 				= true;
	public BigInteger cellSizeCellBasedAccessibility	= new BigInteger("100");
	public String shapeFileCellBasedAccessibilityInputFile	= "";
	public boolean useCustomBoundingBox 				= false;
	public Double boundingBoxTop						= 0.;
	public Double boundingBoxLeft						= 0.;
	public Double boundingBoxRight 						= 0.;
	public Double boundingBoxBottom						= 0.;
	public Double accessibilityDestinationSamplingRate	= 1.0;
	public boolean useLogitScaleParameterFromMATSim		= true;
	public boolean useCarParameterFromMATSim			= true;
	public boolean useWalkParameterFromMATSim			= true;
	public boolean useRawSumsWithoutLn					= false;
	public Double logitScaleParameter					= 1.0;
	public Double betaCarTravelTime						= 0.;
	public Double betaCarTravelTimePower2				= 0.;
	public Double betaCarLnTravelTime					= 0.;
	public Double betaCarTravelDistance					= 0.;
	public Double betaCarTravelDistancePower2			= 0.;
	public Double betaCarLnTravelDistance				= 0.;
	public Double betaCarTravelCost						= 0.;
	public Double betaCarTravelCostPower2				= 0.;
	public Double betaCarLnTravelCost					= 0.;
	public Double betaWalkTravelTime					= 0.;
	public Double betaWalkTravelTimePower2				= 0.;
	public Double betaWalkLnTravelTime					= 0.;
	public Double betaWalkTravelDistance				= 0.;
	public Double betaWalkTravelDistancePower2			= 0.;
	public Double betaWalkLnTravelDistance				= 0.;
	public Double betaWalkTravelCost					= 0.;
	public Double betaWalkTravelCostPower2				= 0.;
	public Double betaWalkLnTravelCost					= 0.;
	
	/**
	 * constructor
	 * 
	 * this is makes parameter settings for ConfigLoadTest, 
	 * create another constructor for another test
	 * 
	 * @param startMode distinguishes between cold, warm and hot start
	 * @param path gives the path, were the generated config (and other files) should be stored
	 */
	public CreateTestMATSimConfig(final int startMode, String path){
		this.startMode 			= startMode;
		this.dummyPath 			= path;
		this.networkInputFileName 	= path + DUMMY_FILE;
		this.inputPlansFileName		= path + DUMMY_FILE;
		this.hotstartPlansFileName	= path + DUMMY_FILE;
		this.opusHome			= path;
		this.opusDataPath		= path;
		this.matsim4opus		= path;
		this.matsim4opusConfig	= path;
		this.matsim4opusOutput	= path;
		this.matsim4opusTemp	= path;
		this.matsimExternalConfigFileName = "";
	}
	
	/**
	 * constructor
	 * 
	 * this is makes parameter settings for MATSim4UrbanSimTestRun. Here, the test run parameter is set
	 * 
	 * @param startMode distinguishes between cold, warm and hot start
	 * @param path gives the path, were the generated config (and other files) should be stored
	 */
	public CreateTestMATSimConfig(final int startMode, String path, boolean testrun){
		this.startMode 			= startMode;
		this.dummyPath 			= path;
		this.networkInputFileName 	= path + DUMMY_FILE;
		this.inputPlansFileName		= path + DUMMY_FILE;
		this.hotstartPlansFileName	= path + DUMMY_FILE;
		this.opusHome			= path;
		this.opusDataPath		= path;
		this.matsim4opus		= path;
		this.matsim4opusConfig	= path;
		this.matsim4opusOutput	= path;
		this.matsim4opusTemp	= path;
		this.matsimExternalConfigFileName = "";
		this.isTestRun			= testrun;
	}
	
	/**
	 * constructor
	 * 
	 * this determines the parameter settings for ConfigLoadTest, 
	 * create another constructor for another test
	 * 
	 * @param startMode distinguishes between cold, warm and hot start
	 * @param path gives the path, were the generated config (and other files) should be stored
	 * @param externalConfig gives the path, were the external MATSim config is stored
	 */
	public CreateTestMATSimConfig(final int startMode, String path, String externalConfig){
		this.startMode = startMode;
		this.dummyPath = path;
		this.networkInputFileName 	= path + DUMMY_FILE;
		this.inputPlansFileName		= path + DUMMY_FILE;
		this.hotstartPlansFileName	= path + DUMMY_FILE;
		this.opusHome			= path;
		this.opusDataPath		= path;
		this.matsim4opus		= path;
		this.matsim4opusConfig	= path;
		this.matsim4opusOutput	= path;
		this.matsim4opusTemp	= path;
		this.matsimExternalConfigFileName = externalConfig;
	}
	
	/**
	 * constructor
	 * 
	 * this determines the parameter settings for Accessibility tests.
	 * 
	 * @param path
	 * @param inputNetworkFile
	 */
	public CreateTestMATSimConfig(String path, String inputNetworkFile){
		this.dummyPath = path;
		this.networkInputFileName = inputNetworkFile;
		this.inputPlansFileName		= "";
		this.hotstartPlansFileName	= "";
		this.opusHome			= path;
		this.opusDataPath		= path;
		this.matsim4opus		= path;
		this.matsim4opusConfig	= path;
		this.matsim4opusOutput	= path;
		this.matsim4opusTemp	= path;
		this.matsimExternalConfigFileName = "";
	}

	/**
	 * generates the external MATSim config file with the specified parameter settings
	 */
	@Deprecated // this generates a config in old format, use generateConfigV3 instead
	public String generateConfigV2(){
		
		ObjectFactory of = new ObjectFactory();	
		
		// create MATSim4UrbanSim xml hierarchy
		
		// Config Type
		FileType matsim_config = of.createFileType();
		matsim_config.setInputFile( this.matsimExternalConfigFileName );
		FileType network = of.createFileType();
		network.setInputFile( this.networkInputFileName );
		InputPlansFileType inputPlansFileType = of.createInputPlansFileType();
		if(this.startMode == CreateTestMATSimConfig.COLD_START)
			inputPlansFileType.setInputFile( "" );
		else if(this.startMode == CreateTestMATSimConfig.WARRM_START)
			inputPlansFileType.setInputFile( this.inputPlansFileName );
		InputPlansFileType hotStratPlansFile = of.createInputPlansFileType();
		if(this.startMode == CreateTestMATSimConfig.HOT_START)
			hotStratPlansFile.setInputFile( this.hotstartPlansFileName );
		else
			hotStratPlansFile.setInputFile( "" );
		ControlerType controler = of.createControlerType();
		controler.setFirstIteration( this.firstIteration );
		controler.setLastIteration(  this.lastIteration );
		PlanCalcScoreType planCalcScore = of.createPlanCalcScoreType();
		planCalcScore.setActivityType0( this.activityType_0 );
		planCalcScore.setActivityType1( this.activityType_1 );
		planCalcScore.setHomeActivityTypicalDuration( this.homeActivityTypicalDuration );
		planCalcScore.setWorkActivityTypicalDuration( this.workActivityTypicalDuration );
		planCalcScore.setWorkActivityOpeningTime( this.workActivityOpeningTime );
		planCalcScore.setWorkActivityLatestStartTime( this.workActivityLatestStartTime );
//		StrategyType strategy = of.createStrategyType();
//		strategy.setMaxAgentPlanMemorySize( this.maxAgentPlanMemorySize );
//		strategy.setTimeAllocationMutatorProbability( this.timeAllocationMutatorProbability );
//		strategy.setChangeExpBetaProbability( this.changeExpBetaProbability );
//		strategy.setReRouteDijkstraProbability( this.reRouteDijkstraProbability );
		
		ConfigType configType = of.createConfigType();
		configType.setMatsimConfig(matsim_config);
		configType.setNetwork(network);
		configType.setInputPlansFile(inputPlansFileType);
		configType.setHotStartPlansFile(hotStratPlansFile);
		configType.setControler(controler);
		configType.setPlanCalcScore(planCalcScore);
//		configType.setStrategy(strategy);
		
		// UrbanSimParameterType
		UrbansimParameterType ubansimParameterType = of.createUrbansimParameterType();
		ubansimParameterType.setPopulationSamplingRate( this.populationSamplingRate );
		ubansimParameterType.setYear( this.year );
		ubansimParameterType.setOpusHome( this.opusHome );
		ubansimParameterType.setOpusDataPath( this.opusDataPath );
		ubansimParameterType.setMatsim4Opus( this.matsim4opus );
		ubansimParameterType.setMatsim4OpusConfig( this.matsim4opusConfig );
		ubansimParameterType.setMatsim4OpusOutput( this.matsim4opusOutput );
		ubansimParameterType.setMatsim4OpusTemp( this.matsim4opusTemp );
		ubansimParameterType.setIsTestRun( this.isTestRun );
		ubansimParameterType.setRandomLocationDistributionRadiusForUrbanSimZone( this.randomLocationDistributionRadiusForUrbanSimZone );
		ubansimParameterType.setTestParameter( this.customParameter );
		ubansimParameterType.setBackupRunData( this.backupRunData );
		
		// matsim4UrbanSimControlerType
		Matsim4UrbansimContolerType matsim4UrbanSimControlerType = of.createMatsim4UrbansimContolerType();
		matsim4UrbanSimControlerType.setZone2ZoneImpedance( this.zone2zoneImpedance );
		matsim4UrbanSimControlerType.setAgentPerformance( this.agentPerformance );
		matsim4UrbanSimControlerType.setZoneBasedAccessibility( this.zoneBasedAccessibility );
		matsim4UrbanSimControlerType.setCellBasedAccessibility( this.cellBasedAccessibility );
		matsim4UrbanSimControlerType.setCellSizeCellBasedAccessibility( this.cellSizeCellBasedAccessibility );
		FileType shapeFile = of.createFileType();
		shapeFile.setInputFile( this.shapeFileCellBasedAccessibilityInputFile );
		matsim4UrbanSimControlerType.setShapeFileCellBasedAccessibility( shapeFile );
		matsim4UrbanSimControlerType.setUseCustomBoundingBox( this.useCustomBoundingBox );
		matsim4UrbanSimControlerType.setBoundingBoxBottom( this.boundingBoxTop );
		matsim4UrbanSimControlerType.setBoundingBoxLeft( this.boundingBoxLeft );
		matsim4UrbanSimControlerType.setBoundingBoxRight( this.boundingBoxRight );
		matsim4UrbanSimControlerType.setBoundingBoxTop( this.boundingBoxBottom );
		
		// accessibilityParameterType
		AccessibilityParameterType accessibilityParameterType = of.createAccessibilityParameterType();
		accessibilityParameterType.setAccessibilityDestinationSamplingRate( this.accessibilityDestinationSamplingRate );
		accessibilityParameterType.setUseLogitScaleParameterFromMATSim( this.useLogitScaleParameterFromMATSim );
		accessibilityParameterType.setUseCarParameterFromMATSim( this.useCarParameterFromMATSim );
		accessibilityParameterType.setUseWalkParameterFromMATSim( this.useWalkParameterFromMATSim );
		accessibilityParameterType.setUseRawSumsWithoutLn( this.useRawSumsWithoutLn );
		accessibilityParameterType.setBetaCarTravelTime( this.betaCarTravelTime );
		accessibilityParameterType.setBetaCarTravelTimePower2( this.betaCarTravelTimePower2 );
		accessibilityParameterType.setBetaCarLnTravelTime( this.betaCarLnTravelTime );
		accessibilityParameterType.setBetaCarTravelDistance( this.betaCarTravelDistance );
		accessibilityParameterType.setBetaCarTravelDistancePower2( this.betaCarTravelDistancePower2 );
		accessibilityParameterType.setBetaCarLnTravelDistance( this.betaCarLnTravelDistance );
		accessibilityParameterType.setBetaCarTravelCost( this.betaCarTravelCost );
		accessibilityParameterType.setBetaCarTravelCostPower2( this.betaCarTravelCostPower2 );
		accessibilityParameterType.setBetaCarLnTravelCost( this.betaCarLnTravelCost );
		accessibilityParameterType.setBetaWalkTravelTime( this.betaWalkTravelTime );
		accessibilityParameterType.setBetaWalkTravelTimePower2( this.betaWalkTravelTimePower2 );
		accessibilityParameterType.setBetaWalkLnTravelTime( this.betaWalkLnTravelTime );
		accessibilityParameterType.setBetaWalkTravelDistance( this.betaWalkTravelDistance );
		accessibilityParameterType.setBetaWalkTravelDistancePower2( this.betaWalkTravelDistancePower2 );
		accessibilityParameterType.setBetaWalkLnTravelDistance( this.betaWalkLnTravelDistance );
		accessibilityParameterType.setBetaWalkTravelCost( this.betaWalkTravelCost );
		accessibilityParameterType.setBetaWalkTravelCostPower2( this.betaWalkTravelCostPower2 );
		accessibilityParameterType.setBetaWalkLnTravelCost( this.betaWalkLnTravelCost );
		
		// matsim4urbansimtype
		Matsim4UrbansimType matsim4UrbanSimType = of.createMatsim4UrbansimType();
		matsim4UrbanSimType.setUrbansimParameter(ubansimParameterType);
		matsim4UrbanSimType.setMatsim4UrbansimContoler(matsim4UrbanSimControlerType);
		matsim4UrbanSimType.setAccessibilityParameter(accessibilityParameterType);
		
		// MatsimConfigType
		MatsimConfigType matsimConfigType = of.createMatsimConfigType();
		matsimConfigType.setConfig(configType);
		matsimConfigType.setMatsim4Urbansim(matsim4UrbanSimType);
		
		return writeConfigFileV2(matsimConfigType);
	}
	
	/**
	 * generates the external MATSim config file with the specified parameter settings
	 */
	public String generateConfigV3(){
		
		org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.ObjectFactory of 
			= new org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.ObjectFactory();	
		
		// create MATSim4UrbanSim xml hierarchy
		
		org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.FileType externalMatsimConfig = of.createFileType();
		externalMatsimConfig.setInputFile(this.matsimExternalConfigFileName);
		org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.FileType network = of.createFileType();
		network.setInputFile(this.networkInputFileName);
		org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.FileType emptyShapeFile = of.createFileType();
		emptyShapeFile.setInputFile("");
		org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.FileType warmStartPlansFile = of.createFileType();
		warmStartPlansFile.setInputFile(this.hotstartPlansFileName);
		org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.FileType hotStartPlansFile = of.createFileType();
		hotStartPlansFile.setInputFile(this.hotstartPlansFileName);
		
		// matsimConfigType
		org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.MatsimConfigType matsimConfigType = of.createMatsimConfigType();
		matsimConfigType.setCellSize(this.cellSizeCellBasedAccessibility);
		matsimConfigType.setAccessibilityComputationAreaFromShapeFile(false);
		matsimConfigType.setAccessibilityComputationAreaFromBoundingBox(false);
		matsimConfigType.setAccessibilityComputationAreaFromNetwork(true);
		matsimConfigType.setStudyAreaBoundaryShapeFile(emptyShapeFile);
		matsimConfigType.setUrbansimZoneRandomLocationDistributionByRadius(this.randomLocationDistributionRadiusForUrbanSimZone);
		matsimConfigType.setUrbansimZoneRandomLocationDistributionByShapeFile("");
		matsimConfigType.setExternalMatsimConfig(externalMatsimConfig);
		matsimConfigType.setNetwork(network);
		matsimConfigType.setWarmStartPlansFile(warmStartPlansFile);
		matsimConfigType.setHotStartPlansFile(hotStartPlansFile);		
		matsimConfigType.setUseHotStart(true);
		matsimConfigType.setActivityType0(this.activityType_0);
		matsimConfigType.setActivityType1(this.activityType_1);
		matsimConfigType.setHomeActivityTypicalDuration(this.homeActivityTypicalDuration);
		matsimConfigType.setWorkActivityTypicalDuration(this.workActivityTypicalDuration);
		matsimConfigType.setWorkActivityOpeningTime(this.workActivityOpeningTime);
		matsimConfigType.setWorkActivityLatestStartTime(this.workActivityLatestStartTime);
		matsimConfigType.setFirstIteration(this.firstIteration);
		matsimConfigType.setLastIteration(this.lastIteration);
		
		// 
		org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.Matsim4UrbansimType matsim4UrbanSimType 
			= of.createMatsim4UrbansimType();
		matsim4UrbanSimType.setPopulationSamplingRate(this.populationSamplingRate);
		matsim4UrbanSimType.setYear(this.year);
		matsim4UrbanSimType.setOpusHome(this.opusHome);
		matsim4UrbanSimType.setOpusDataPath(this.opusDataPath);
		matsim4UrbanSimType.setMatsim4Opus(this.matsim4opus);
		matsim4UrbanSimType.setMatsim4OpusConfig(this.matsim4opusConfig);
		matsim4UrbanSimType.setMatsim4OpusOutput(this.matsim4opusOutput);
		matsim4UrbanSimType.setMatsim4OpusTemp(this.matsim4opusTemp);
		matsim4UrbanSimType.setCustomParameter(this.customParameter);
		matsim4UrbanSimType.setZone2ZoneImpedance(this.zone2zoneImpedance);
		matsim4UrbanSimType.setAgentPerfomance(this.agentPerformance);
		matsim4UrbanSimType.setZoneBasedAccessibility(this.zoneBasedAccessibility);
		matsim4UrbanSimType.setParcelBasedAccessibility(this.cellBasedAccessibility);
		matsim4UrbanSimType.setBackupRunData(this.backupRunData);

		// MatsimConfigType
		Matsim4UrbansimConfigType m4uConfigType = of.createMatsim4UrbansimConfigType();
		m4uConfigType.setMatsim4Urbansim(matsim4UrbanSimType);
		m4uConfigType.setMatsimConfig(matsimConfigType);
		
		return writeConfigFileV3(m4uConfigType);
	}
	
	/**
	 * generates a minimal matsim4urbansim config
	 * 
	 * tnicolai: the resulting config has the same structure, i.e. the number of entries,
	 * as the outcome of the above mehtod "generate()". The difference is that some parameters
	 * are set zero instead of using the above defined parameters , e.g.
	 * timeAllocationMutatorProbability, changeExpBetaProbability, reRouteDijkstraProbability, 
	 * populationSamplingRate, etc..
	 */
	@Deprecated // this generates a config in old format, use generateConfigV3 instead
	public String generateMinimalConfig(){
		
		ObjectFactory of = new ObjectFactory();	
		
		// create MATSim4UrbanSim xml hierarchy
		
		// Config Type
		FileType matsim_config = of.createFileType();
		matsim_config.setInputFile( this.matsimExternalConfigFileName );
		FileType network = of.createFileType();
		network.setInputFile( this.networkInputFileName );
		InputPlansFileType inputPlansFileType = of.createInputPlansFileType();
		if(this.startMode == CreateTestMATSimConfig.COLD_START)
			inputPlansFileType.setInputFile( "" );
		else if(this.startMode == CreateTestMATSimConfig.WARRM_START)
			inputPlansFileType.setInputFile( this.inputPlansFileName );
		InputPlansFileType hotStratPlansFile = of.createInputPlansFileType();
		if(this.startMode == CreateTestMATSimConfig.HOT_START)
			hotStratPlansFile.setInputFile( this.hotstartPlansFileName );
		else
			hotStratPlansFile.setInputFile( "" );
		ControlerType controler = of.createControlerType();
		controler.setFirstIteration( this.firstIteration );
		controler.setLastIteration(  this.lastIteration );
		PlanCalcScoreType planCalcScore = of.createPlanCalcScoreType();
		planCalcScore.setActivityType0( this.activityType_0 );
		planCalcScore.setActivityType1( this.activityType_1 );
		planCalcScore.setHomeActivityTypicalDuration( this.homeActivityTypicalDuration );
		planCalcScore.setWorkActivityTypicalDuration( this.workActivityTypicalDuration );
		planCalcScore.setWorkActivityOpeningTime( this.workActivityOpeningTime );
		planCalcScore.setWorkActivityLatestStartTime( this.workActivityLatestStartTime );
//		StrategyType strategy = of.createStrategyType();
//		strategy.setMaxAgentPlanMemorySize( this.maxAgentPlanMemorySize );
		
		// UrbanSimParameterType
		UrbansimParameterType ubansimParameterType = of.createUrbansimParameterType();
		ubansimParameterType.setYear( this.year );
		ubansimParameterType.setOpusHome( this.opusHome );
		ubansimParameterType.setOpusDataPath( this.opusDataPath );
		ubansimParameterType.setMatsim4Opus( this.matsim4opus );
		ubansimParameterType.setMatsim4OpusConfig( this.matsim4opusConfig );
		ubansimParameterType.setMatsim4OpusOutput( this.matsim4opusOutput );
		ubansimParameterType.setMatsim4OpusTemp( this.matsim4opusTemp );
		ubansimParameterType.setTestParameter( this.customParameter );
		
		// matsim4UrbanSimControlerType
		Matsim4UrbansimContolerType matsim4UrbanSimControlerType = of.createMatsim4UrbansimContolerType();
		matsim4UrbanSimControlerType.setCellSizeCellBasedAccessibility( this.cellSizeCellBasedAccessibility );
		FileType shapeFile = of.createFileType();
		shapeFile.setInputFile( this.shapeFileCellBasedAccessibilityInputFile );
		matsim4UrbanSimControlerType.setShapeFileCellBasedAccessibility( shapeFile );

		// accessibilityParameterType
		AccessibilityParameterType accessibilityParameterType = of.createAccessibilityParameterType();
		// for the following, ``false'' is currently not supported, thus I prefer setting it to true here. kai, apr'13
		accessibilityParameterType.setUseLogitScaleParameterFromMATSim( true );
		accessibilityParameterType.setUseCarParameterFromMATSim( true );
		accessibilityParameterType.setUseWalkParameterFromMATSim( true );
		
		// === below here are no parameters, but just "plugging together"
		
		ConfigType configType = of.createConfigType();
		configType.setMatsimConfig(matsim_config);
		configType.setNetwork(network);
		configType.setInputPlansFile(inputPlansFileType);
		configType.setHotStartPlansFile(hotStratPlansFile);
		configType.setControler(controler);
		configType.setPlanCalcScore(planCalcScore);
//		configType.setStrategy(strategy);
		
		// matsim4urbansimtype
		Matsim4UrbansimType matsim4UrbanSimType = of.createMatsim4UrbansimType();
		matsim4UrbanSimType.setUrbansimParameter(ubansimParameterType);
		matsim4UrbanSimType.setMatsim4UrbansimContoler(matsim4UrbanSimControlerType);
		matsim4UrbanSimType.setAccessibilityParameter(accessibilityParameterType);
		
		// MatsimConfigType
		MatsimConfigType matsimConfigType = of.createMatsimConfigType();
		matsimConfigType.setConfig(configType);
		matsimConfigType.setMatsim4Urbansim(matsim4UrbanSimType);
		
		return writeConfigFileV2(matsimConfigType);
	}

	/**
	 * writes the MATSim4UrbanSim confing at the specified place using JAXB
	 * 
	 * @param matsimConfigType config in MATSim4UrbanSim format 
	 * @throws UncheckedIOException
	 */
	String writeConfigFileV2(MatsimConfigType matsimConfigType) throws UncheckedIOException {
		try {
			String destination = this.dummyPath + "/test_config.xml";	
			BufferedWriter bw = IOUtils.getBufferedWriter( destination );
			
			String xsdPath = TempDirectoryUtil.createCustomTempDirectory("xsd");
			// init loadFile object: it downloads a xsd from matsim.org into a temp directory
			LoadFile loadFile = new LoadFile(InternalConstants.V2_MATSIM_4_URBANSIM_XSD_MATSIMORG, xsdPath, InternalConstants.V2_XSD_FILE_NAME);
			File file2XSD = loadFile.loadMATSim4UrbanSimXSD(); // trigger loadFile
			if(file2XSD == null || !file2XSD.exists()){
				log.error("Did not find xml schema!");
				System.exit(1);
			}
			log.info("Using following xsd schema: " + file2XSD.getCanonicalPath());
			
			// crate a schema factory ...
			SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
			// create a schema object via the given xsd to validate the MATSim xml config.
			Schema schema = schemaFactory.newSchema(file2XSD);
			
			JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
			Marshaller m = jaxbContext.createMarshaller();
			m.setSchema(schema);
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			JAXBElement elem = new JAXBElement( new QName("","matsim_config"), MatsimConfigType.class, matsimConfigType); 
			// (this is because there is no XMLRootElemet annotation)
			m.marshal(elem, bw );
			
			return destination;
			
		} catch (JAXBException e) {
			e.printStackTrace();
			Assert.assertFalse(true) ; // otherwise the test neither returns "good" nor "bad" when there is an exception.  kai, apr'13
		} catch (SAXException e) {
			e.printStackTrace();
			Assert.assertFalse(true) ; // otherwise the test neither returns "good" nor "bad" when there is an exception.  kai, apr'13
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertFalse(true) ; // otherwise the test neither returns "good" nor "bad" when there is an exception.  kai, apr'13
		}
		return null;
	}
	
	/**
	 * writes the Matsim4UrbansimConfigType confing at the specified place using JAXB
	 * 
	 * @param m4uConfigType config in MATSim4UrbanSim format 
	 * @throws UncheckedIOException
	 */
	String writeConfigFileV3(Matsim4UrbansimConfigType m4uConfigType) throws UncheckedIOException {
		try {
			String destination = this.dummyPath + "/test_config.xml";	
			log.info("writing test config into: " + destination ) ;
			BufferedWriter bw = IOUtils.getBufferedWriter( destination );
			
			String xsdPath = TempDirectoryUtil.createCustomTempDirectory("xsd");
			// init loadFile object: it downloads a xsd from matsim.org into a temp directory
			LoadFile loadFile = new LoadFile(InternalConstants.CURRENT_MATSIM_4_URBANSIM_XSD_MATSIMORG, xsdPath, InternalConstants.CURRENT_XSD_FILE_NAME);
			File file2XSD = loadFile.loadMATSim4UrbanSimXSD(); // trigger loadFile
			if(file2XSD == null || !file2XSD.exists()){
				log.error("Did not find xml schema!");
				System.exit(1);
			}
			log.info("Using following xsd schema: " + file2XSD.getCanonicalPath());
			
			// crate a schema factory ...
			SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
			// create a schema object via the given xsd to validate the MATSim xml config.
			Schema schema = schemaFactory.newSchema(file2XSD);
			
			JAXBContext jaxbContext = JAXBContext.newInstance(org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.ObjectFactory.class);
			Marshaller m = jaxbContext.createMarshaller();
			m.setSchema(schema);
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			
			org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.ObjectFactory of 
				= new org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.ObjectFactory();	
//			JAXBElement jaxbElement = new JAXBElement( new QName("","matsim4urbansim_config"), 
//			Matsim4UrbansimConfigRoot.class, m4uConfigType); 
			JAXBElement<Matsim4UrbansimConfigType> jaxbElement = of.createMatsim4UrbansimConfig(m4uConfigType);
			// (this is because there is no XMLRootElemet annotation)
			m.marshal(jaxbElement, bw );
			
			return destination;
			
		} catch (JAXBException e) {
			e.printStackTrace();
			Assert.assertFalse(true) ; // otherwise the test neither returns "good" nor "bad" when there is an exception.  kai, apr'13
		} catch (SAXException e) {
			e.printStackTrace();
			Assert.assertFalse(true) ; // otherwise the test neither returns "good" nor "bad" when there is an exception.  kai, apr'13
		} catch (IOException e) {
			e.printStackTrace();
			Assert.assertFalse(true) ; // otherwise the test neither returns "good" nor "bad" when there is an exception.  kai, apr'13
		}
		return null;
	}
	
	public int getStartMode(){
		return this.startMode;
	}
	
	/**
	 * for quick test
	 * @param args
	 */
	public static void main(String args[]){
		
		String path = TempDirectoryUtil.createCustomTempDirectory("tmp");
		CreateTestMATSimConfig testConfig = new CreateTestMATSimConfig(COLD_START, path);

		String matsimConfiFile = testConfig.generateConfigV3();

//		M4UConfigurationConverterV4 connector = new M4UConfigurationConverterV4( matsimConfiFile );
//		connector.init();
		
//		TempDirectoryUtil.cleaningUpCustomTempDirectories();
	}
	
}
