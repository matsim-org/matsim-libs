/* *********************************************************************** *
 * project: org.matsim.*
 * CreateXSDBindingClassTest.java
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
package playground.tnicolai.urbansim.jaxbTest;

import java.io.File;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.testcases.MatsimTestCase;

import playground.tnicolai.urbansim.MATSim4Urbansim;
import playground.tnicolai.urbansim.UpdateXMLBindingClasses;
import playground.tnicolai.urbansim.com.matsim.config.MatsimConfigType;
import playground.tnicolai.urbansim.constants.Constants;
import playground.tnicolai.urbansim.utils.CommonUtilities;
import playground.tnicolai.urbansim.utils.MATSimConfigObject;
import playground.tnicolai.urbansim.utils.io.LoadFile;
import playground.tnicolai.urbansim.utils.io.TempDirectoryUtil;



/**
 * @author thomas
 *
 */
public class CreateXSDBindingClassTest extends MatsimTestCase {
	
	private static final Logger log = Logger.getLogger(CreateXSDBindingClassTest.class);
	
	@Ignore
	//@Rule public MatsimTestUtils utils = new MatsimTestUtils();
	@Test
	public void testCreateBindingClass(){
		
		prepareTest();
		
	}
	
	/**
	 * preparing creation of binding classes test run
	 */
	private void prepareTest(){
		
		String jaxbLocation = findJAXBLibLocation();
		String destination = createDestinationDirectory();
		String packageName = "org.test.bindingClasses";
		
		String[] args = {"--jaxbLocation="+jaxbLocation, 
						 "--destination="+destination, 
						 "--package="+packageName};
		log.info("Strating UpdateXMLBindingClasses with following arguments: ");
		log.info("JAXB location : " + jaxbLocation);
		log.info("Destination : " + destination);
		log.info("Package name : " + packageName);
		
		UpdateXMLBindingClasses.main(args);
		
		// TODO test Binding classes with 
		
		// remove all created temp directories
		TempDirectoryUtil.deleteDirectory(destination);
	}
	
	
	private void testBindingClassesViaJAXB(){
		try{ // TODO adopt path to generated binding classes
			JAXBContext jaxbContext = JAXBContext.newInstance(playground.tnicolai.urbansim.com.matsim.config.ObjectFactory.class);
			// create an unmaschaller (write xml file)
			Unmarshaller unmarschaller = jaxbContext.createUnmarshaller();
	
			// crate a schema factory ...
			SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
			// ... and initialize it with an xsd (xsd lies in the urbansim project)
			
			LoadFile loadFile = new LoadFile(Constants.MATSim_4_UrbanSim_XSD, CommonUtilities.getCurrentPath(MATSim4Urbansim.class) + "tmp/", "MATSim4UrbanSimConfigSchema.xsd");
			File file2XSD = loadFile.loadMATSim4UrbanSimXSD();
			
			// for debugging
			// File file2XSD = new File( "/Users/thomas/Development/workspace/urbansim_trunk/opus_matsim/sustain_city/models/pyxb_xml_parser/MATSim4UrbanSimConfigSchema.xsd" ); 
			if(file2XSD == null || !file2XSD.exists()){
				
				log.warn(file2XSD.getCanonicalPath() + " is not available. Loading compensatory xsd instead (this may be is an older version).");
				log.warn("Compensatory xsd file: " + CommonUtilities.getCurrentPath(MATSim4Urbansim.class) + "tmp/MATSim4UrbanSimConfigSchema.xsd");
				
				file2XSD = new File(CommonUtilities.getCurrentPath(MATSim4Urbansim.class) + "tmp/MATSim4UrbanSimConfigSchema.xsd");
				if(!file2XSD.exists())
					log.error(file2XSD.getCanonicalPath() + " not found!!!");

			}
			log.info("Using following xsd schema: " + file2XSD.getCanonicalPath());
			// create a schema object via the given xsd to validate the MATSim xml config.
			Schema schema = schemaFactory.newSchema(file2XSD);
			// set the schema for validation while reading/importing the MATSim xml config.
			unmarschaller.setSchema(schema);
			
			File inputFile = new File( "" );// TODO MATSim config location
			if(!inputFile.exists())
				log.error(inputFile.getCanonicalPath() + " not found!!!");
			// contains the content of the MATSim config.
			Object object = unmarschaller.unmarshal(inputFile);
			
			// Java representation of the schema file.
			MatsimConfigType matsimConfig;
			
			// The structure of both objects must match.
			if(object.getClass() == MatsimConfigType.class)
				matsimConfig = (MatsimConfigType) object;
			else
				matsimConfig = (( JAXBElement<MatsimConfigType>) object).getValue();
			
			// creatin MASim config object that contains the values from the xml config file.
			if(matsimConfig != null){
				log.info("Creating new MATSim config object to store the values from the xml configuration.");
				MATSimConfigObject.initMATSimConfigObject(matsimConfig);
			}
		}
		catch(Exception e){
			e.printStackTrace();
		}
	}
	
	/**
	 * gets the location of JAXB library
	 * @return path of JAXB library
	 */
	private String findJAXBLibLocation(){
		
		String path = CommonUtilities.getCurrentPath(CreateXSDBindingClassTest.class);
		String subPath = "lib/jaxb-2.1.7/lib/jaxb-xjc.jar";
		
		return CommonUtilities.replaceSubPath(1, path, subPath);
	}
	
	/**
	 * returns the path to the temp destination directory for generated 
	 * binding classes
	 * @return path to the temp destination directory for generated 
	 * binding classes
	 */
	private String createDestinationDirectory(){
		
		String path = CommonUtilities.getCurrentPath(CreateXSDBindingClassTest.class);
		path = CommonUtilities.replaceSubPath(1, path, "tmp/jaxbBindingClasses");
		
		if (TempDirectoryUtil.createDirectory(path))
			return path;
		return null;
	}

}

