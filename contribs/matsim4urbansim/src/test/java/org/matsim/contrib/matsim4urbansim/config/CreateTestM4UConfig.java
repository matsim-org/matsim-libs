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
 *   it under the terms of the GNU General License as published by  *
 *   the Free Software Foundation; either version 2 of the License, or     *
 *   (at your option) any later version.                                   *
 *   See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                         *
 * *********************************************************************** */

/**
 * 
 */
package org.matsim.contrib.matsim4urbansim.config;

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
import org.matsim.core.utils.io.MatsimJaxbXmlWriter;
import org.matsim.core.utils.io.UncheckedIOException;
import org.xml.sax.SAXException;


/**
 * @author thomas
 *
 */
public class CreateTestM4UConfig {
	private static final Logger log = Logger.getLogger(CreateTestM4UConfig.class) ;
	
	 static final int COLD_START = 0;
	 static final int WARRM_START = 1;
	 static final int HOT_START = 2;
	
	 static final String DUMMY_FILE = "/dummy.xml";
	
	private int startMode;
	String dummyPath;
	
	// yy why is all of this public?  could you please write a comment why that design decision was made?  thx.  kai, apr'13
	
	private String matsimExternalConfigFileName 				= "";
	 private String networkInputFileName 	 					= "";
	 private String inputPlansFileName 						= "";
	 String hotstartPlansFileName						= "";
	 BigInteger firstIteration					= new BigInteger("0");
	 BigInteger lastIteration						= new BigInteger("1");
	 String activityType_0						= "home";
	 String activityType_1						= "work";
	 BigInteger homeActivityTypicalDuration		= new BigInteger("43200");	
	 BigInteger workActivityTypicalDuration		= new BigInteger("28800");	
	BigInteger workActivityOpeningTime			= new BigInteger("25200");
	BigInteger workActivityLatestStartTime		= new BigInteger("32400");
	BigInteger maxAgentPlanMemorySize			= new BigInteger("5");
	Double timeAllocationMutatorProbability		= 0.1;
	Double changeExpBetaProbability				= 0.9;
	Double reRouteDijkstraProbability			= 0.1;
	Double populationSamplingRate				= 1.0;
	BigInteger year								= new BigInteger("2000");
	String opusHome								= "";
	String opusDataPath							= "";
	String matsim4opus							= "";
	String matsim4opusConfig						= "";
	String matsim4opusOutput 					= "";
	String matsim4opusTemp						= "";
	boolean isTestRun							= false;
	Double randomLocationDistributionRadiusForUrbanSimZone	= 0.;
	String customParameter							= "";
	boolean backupRunData						= false;
	boolean zone2zoneImpedance					= true;
	boolean agentPerformance						= true;
	boolean zoneBasedAccessibility				= true;
	boolean cellBasedAccessibility 				= true;
	BigInteger cellSizeCellBasedAccessibility	= new BigInteger("100");
	String shapeFileCellBasedAccessibilityInputFile	= "";
	boolean useCustomBoundingBox 				= false;
	Double boundingBoxTop						= 0.;
	Double boundingBoxLeft						= 0.;
	Double boundingBoxRight 						= 0.;
	Double boundingBoxBottom						= 0.;
	Double accessibilityDestinationSamplingRate	= 1.0;
	boolean useLogitScaleParameterFromMATSim		= true;
	boolean useCarParameterFromMATSim			= true;
	boolean useWalkParameterFromMATSim			= true;
	boolean useRawSumsWithoutLn					= false;
	Double logitScaleParameter					= 1.0;
	Double betaCarTravelTime						= 0.;
	Double betaCarTravelTimePower2				= 0.;
	Double betaCarLnTravelTime					= 0.;
	Double betaCarTravelDistance					= 0.;
	Double betaCarTravelDistancePower2			= 0.;
	Double betaCarLnTravelDistance				= 0.;
	Double betaCarTravelCost						= 0.;
	Double betaCarTravelCostPower2				= 0.;
	Double betaCarLnTravelCost					= 0.;
	Double betaWalkTravelTime					= 0.;
	Double betaWalkTravelTimePower2				= 0.;
	Double betaWalkLnTravelTime					= 0.;
	Double betaWalkTravelDistance				= 0.;
	Double betaWalkTravelDistancePower2			= 0.;
	Double betaWalkLnTravelDistance				= 0.;
	Double betaWalkTravelCost					= 0.;
	Double betaWalkTravelCostPower2				= 0.;
	Double betaWalkLnTravelCost					= 0.;
	
	/**
	 * constructor
	 * 
	 * this is makes parameter settings for ConfigLoadTest, 
	 * create another constructor for another test
	 * 
	 * @param startMode distinguishes between cold, warm and hot start
	 * @param path gives the path, were the generated config (and other files) should be stored
	 */
	CreateTestM4UConfig(final int startMode, String path){
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
	CreateTestM4UConfig(final int startMode, String path, boolean testrun){
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
	CreateTestM4UConfig(final int startMode, String path, String externalConfig){
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
	public CreateTestM4UConfig(String path, String inputNetworkFile){
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
	public final String generateConfigV3(){
		
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
			
//			String xsdPath = TempDirectoryUtil.createCustomTempDirectory("xsd");
//			// init loadFile object: it downloads a xsd from matsim.org into a temp directory
//			LoadFile loadFile = new LoadFile(InternalConstants.CURRENT_MATSIM_4_URBANSIM_XSD_MATSIMORG, xsdPath, InternalConstants.CURRENT_XSD_FILE_NAME);
//			File file2XSD = loadFile.loadMATSim4UrbanSimXSD(); // trigger loadFile
//			if(file2XSD == null || !file2XSD.exists()){
//				throw new RuntimeException("Did not find xml schema!");
//			}
//			log.info("Using following xsd schema: " + file2XSD.getCanonicalPath());
			
//			// crate a schema factory ...
//			SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
//			// create a schema object via the given xsd to validate the MATSim xml config.
//			Schema schema = schemaFactory.newSchema(file2XSD);
			
			JAXBContext jaxbContext = JAXBContext.newInstance(org.matsim.contrib.matsim4urbansim.matsim4urbansim.jaxbconfigv3.ObjectFactory.class);
			Marshaller m = jaxbContext.createMarshaller();
//			m.setSchema(schema);
//			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			MatsimJaxbXmlWriter.setMarshallerProperties(InternalConstants.CURRENT_MATSIM_4_URBANSIM_XSD_MATSIMORG, m);
			
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
		} 
//		catch (SAXException e) {
//			e.printStackTrace();
//			Assert.assertFalse(true) ; // otherwise the test neither returns "good" nor "bad" when there is an exception.  kai, apr'13
//		} catch (IOException e) {
//			e.printStackTrace();
//			Assert.assertFalse(true) ; // otherwise the test neither returns "good" nor "bad" when there is an exception.  kai, apr'13
//		}
		return null;
	}
	
	int getStartMode(){
		return this.startMode;
	}
	
	String getNetworkInputFileName() {
		return networkInputFileName;
	}

	void setNetworkInputFileName(String networkInputFileName) {
		this.networkInputFileName = networkInputFileName;
	}

	String getInputPlansFileName() {
		return inputPlansFileName;
	}

	void setInputPlansFileName(String inputPlansFileName) {
		this.inputPlansFileName = inputPlansFileName;
	}
	
}
