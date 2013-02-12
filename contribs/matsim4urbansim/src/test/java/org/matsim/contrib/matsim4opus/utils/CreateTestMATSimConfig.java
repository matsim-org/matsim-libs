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

import org.matsim.contrib.matsim4opus.config.MATSim4UrbanSimConfigurationConverterV4;
import org.matsim.contrib.matsim4opus.constants.InternalConstants;
import org.matsim.contrib.matsim4opus.matsim4urbansim.jaxbconfig2.AccessibilityParameterType;
import org.matsim.contrib.matsim4opus.matsim4urbansim.jaxbconfig2.ConfigType;
import org.matsim.contrib.matsim4opus.matsim4urbansim.jaxbconfig2.ControlerType;
import org.matsim.contrib.matsim4opus.matsim4urbansim.jaxbconfig2.FileType;
import org.matsim.contrib.matsim4opus.matsim4urbansim.jaxbconfig2.InputPlansFileType;
import org.matsim.contrib.matsim4opus.matsim4urbansim.jaxbconfig2.Matsim4UrbansimContolerType;
import org.matsim.contrib.matsim4opus.matsim4urbansim.jaxbconfig2.Matsim4UrbansimType;
import org.matsim.contrib.matsim4opus.matsim4urbansim.jaxbconfig2.MatsimConfigType;
import org.matsim.contrib.matsim4opus.matsim4urbansim.jaxbconfig2.ObjectFactory;
import org.matsim.contrib.matsim4opus.matsim4urbansim.jaxbconfig2.PlanCalcScoreType;
import org.matsim.contrib.matsim4opus.matsim4urbansim.jaxbconfig2.StrategyType;
import org.matsim.contrib.matsim4opus.matsim4urbansim.jaxbconfig2.UrbansimParameterType;
import org.matsim.contrib.matsim4opus.utils.io.LoadFile;
import org.matsim.contrib.matsim4opus.utils.io.TempDirectoryUtil;
import org.matsim.core.utils.io.IOUtils;
import org.xml.sax.SAXException;


/**
 * @author thomas
 *
 */
public class CreateTestMATSimConfig {
	
	public static final int COLD_START = 0;
	public static final int WARRM_START = 1;
	public static final int HOT_START = 2;
	
	private int startMode;
	private String destination;
	private String dummyPath;
	private String dummyFile;
	
	public CreateTestMATSimConfig(final int startMode, String path){
		this.startMode = startMode;
		this.destination = path + "/test_config.xml";	
		this.dummyPath = path;
		this.dummyFile = path + "/dummy.xml";
	}

	
	public String generate(){
		
		BufferedWriter bw = IOUtils.getBufferedWriter( this.destination );
		
		ObjectFactory of = new ObjectFactory();	
		
		// create xml hierarchy
		
		// Config Type
		FileType matsim_config = of.createFileType();
		matsim_config.setInputFile("");
		FileType network = of.createFileType();
		network.setInputFile( this.dummyFile );
		InputPlansFileType inputPlansFileType = of.createInputPlansFileType();
		if(this.startMode == this.COLD_START)
			inputPlansFileType.setInputFile( "" );
		else if(this.startMode == this.WARRM_START)
			inputPlansFileType.setInputFile( this.dummyFile );
		InputPlansFileType hotStratPlansFile = of.createInputPlansFileType();
		if(this.startMode == this.HOT_START)
			hotStratPlansFile.setInputFile( this.dummyFile );
		ControlerType controler = of.createControlerType();
		controler.setFirstIteration(new BigInteger("0"));
		controler.setLastIteration(new BigInteger("0"));
		PlanCalcScoreType planCalcScore = of.createPlanCalcScoreType();
		planCalcScore.setActivityType0("home");
		planCalcScore.setActivityType1("work");
		planCalcScore.setHomeActivityTypicalDuration(new BigInteger("43200"));
		planCalcScore.setWorkActivityTypicalDuration(new BigInteger("28800"));
		planCalcScore.setWorkActivityOpeningTime(new BigInteger("25200"));
		planCalcScore.setWorkActivityLatestStartTime(new BigInteger("32400"));
		StrategyType strategy = of.createStrategyType();
		strategy.setMaxAgentPlanMemorySize( new BigInteger("5") );
		strategy.setTimeAllocationMutatorProbability( 0.1 );
		strategy.setChangeExpBetaProbability( 0.9 );
		strategy.setReRouteDijkstraProbability( 0.1 );
		
		ConfigType configType = of.createConfigType();
		configType.setMatsimConfig(matsim_config);
		configType.setNetwork(network);
		configType.setInputPlansFile(inputPlansFileType);
		configType.setHotStartPlansFile(hotStratPlansFile);
		configType.setControler(controler);
		configType.setPlanCalcScore(planCalcScore);
		configType.setStrategy(strategy);
		
		// UrbanSimParameterType
		UrbansimParameterType ubansimParameterType = of.createUrbansimParameterType();
		ubansimParameterType.setPopulationSamplingRate( 1. );
		ubansimParameterType.setYear( new BigInteger("2000") );
		ubansimParameterType.setOpusHome( this.dummyPath );
		ubansimParameterType.setOpusDataPath( this.dummyPath );
		ubansimParameterType.setMatsim4Opus( this.dummyPath );
		ubansimParameterType.setMatsim4OpusConfig( this.dummyPath );
		ubansimParameterType.setMatsim4OpusOutput( this.dummyPath );
		ubansimParameterType.setMatsim4OpusTemp( this.dummyPath );
		ubansimParameterType.setIsTestRun(true);
		ubansimParameterType.setRandomLocationDistributionRadiusForUrbanSimZone( 500. );
		ubansimParameterType.setTestParameter( "" );
		ubansimParameterType.setBackupRunData(false);
		
		// matsim4UrbanSimControlerType
		Matsim4UrbansimContolerType matsim4UrbanSimControlerType = of.createMatsim4UrbansimContolerType();
		matsim4UrbanSimControlerType.setZone2ZoneImpedance(true);
		matsim4UrbanSimControlerType.setAgentPerformance(true);
		matsim4UrbanSimControlerType.setZoneBasedAccessibility(true);
		matsim4UrbanSimControlerType.setCellBasedAccessibility(true);
		FileType shapeFile = of.createFileType();
		shapeFile.setInputFile("");
		matsim4UrbanSimControlerType.setShapeFileCellBasedAccessibility( shapeFile );
		matsim4UrbanSimControlerType.setUseCustomBoundingBox(false);
		matsim4UrbanSimControlerType.setBoundingBoxBottom( 0. );
		matsim4UrbanSimControlerType.setBoundingBoxLeft( 0. );
		matsim4UrbanSimControlerType.setBoundingBoxRight( 0. );
		matsim4UrbanSimControlerType.setBoundingBoxTop( 0. );
		
		// accessibilityParameterType
		AccessibilityParameterType accessibilityParameterType = of.createAccessibilityParameterType();
		accessibilityParameterType.setAccessibilityDestinationSamplingRate( 1. );
		accessibilityParameterType.setUseLogitScaleParameterFromMATSim(true);
		accessibilityParameterType.setUseCarParameterFromMATSim(true);
		accessibilityParameterType.setUseWalkParameterFromMATSim(true);
		accessibilityParameterType.setUseRawSumsWithoutLn(false);
		accessibilityParameterType.setBetaCarTravelTime( 0. );
		accessibilityParameterType.setBetaCarTravelTimePower2( 0. );
		accessibilityParameterType.setBetaCarLnTravelTime( 0. );
		accessibilityParameterType.setBetaCarTravelDistance( 0. );
		accessibilityParameterType.setBetaCarTravelDistancePower2( 0. );
		accessibilityParameterType.setBetaCarLnTravelDistance( 0. );
		accessibilityParameterType.setBetaCarTravelCost( 0. );
		accessibilityParameterType.setBetaCarTravelCostPower2( 0. );
		accessibilityParameterType.setBetaCarLnTravelCost( 0. );
		accessibilityParameterType.setBetaWalkTravelTime( 0. );
		accessibilityParameterType.setBetaWalkTravelTimePower2( 0. );
		accessibilityParameterType.setBetaWalkLnTravelTime( 0. );
		accessibilityParameterType.setBetaWalkTravelDistance( 0. );
		accessibilityParameterType.setBetaWalkTravelDistancePower2( 0. );
		accessibilityParameterType.setBetaWalkLnTravelDistance( 0. );
		accessibilityParameterType.setBetaWalkTravelCost( 0. );
		accessibilityParameterType.setBetaWalkTravelCostPower2( 0. );
		accessibilityParameterType.setBetaWalkLnTravelCost( 0. );
		
		// matsim4urbansimtype
		Matsim4UrbansimType matsim4UrbanSimType = of.createMatsim4UrbansimType();
		matsim4UrbanSimType.setUrbansimParameter(ubansimParameterType);
		matsim4UrbanSimType.setMatsim4UrbansimContoler(matsim4UrbanSimControlerType);
		matsim4UrbanSimType.setAccessibilityParameter(accessibilityParameterType);
		
		// MatsimConfigType
		MatsimConfigType matsimConfigType = of.createMatsimConfigType();
		matsimConfigType.setConfig(configType);
		matsimConfigType.setMatsim4Urbansim(matsim4UrbanSimType);
		
		try {
			String xsdPath = TempDirectoryUtil.createCustomTempDirectory("xsd");
			// init loadFile object: it downloads a xsd from matsim.org into a temp directory
			LoadFile loadFile = new LoadFile(InternalConstants.V2_MATSIM_4_URBANSIM_XSD_MATSIMORG, xsdPath, InternalConstants.V2_XSD_FILE_NAME);
			File file2XSD = loadFile.loadMATSim4UrbanSimXSD(); // trigger loadFile
			if(file2XSD == null || !file2XSD.exists()){
				System.err.println("Did not find xml schema!");
				System.exit(1);
			}
			System.out.println("Using following xsd schema: " + file2XSD.getCanonicalPath());
			
			// crate a schema factory ...
			SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
			// create a schema object via the given xsd to validate the MATSim xml config.
			Schema schema = schemaFactory.newSchema(file2XSD);
			
			JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
			Marshaller m = jaxbContext.createMarshaller();
			m.setSchema(schema);
			m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, Boolean.TRUE);
			JAXBElement elem = new JAXBElement( new QName("","matsim_config"), MatsimConfigType.class, matsimConfigType); // this is because there is no XMLRootElemet annotation
			m.marshal(elem, bw );
			
			return this.destination;
			
		} catch (JAXBException e) {
			e.printStackTrace();
		} catch (SAXException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}
	
	public static void main(String args[]){
		
		String path = TempDirectoryUtil.createCustomTempDirectory("tmp");
		CreateTestMATSimConfig config = new CreateTestMATSimConfig(COLD_START, path);
		String matsimConfiFile = config.generate();
		
		MATSim4UrbanSimConfigurationConverterV4 connector = new MATSim4UrbanSimConfigurationConverterV4( matsimConfiFile );
		connector.init();
		
		TempDirectoryUtil.cleaningUpCustomTempDirectories();
	}
}
