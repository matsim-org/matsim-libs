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
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.testcases.MatsimTestCase;

import playground.tnicolai.urbansim.constants.Constants;
import playground.tnicolai.urbansim.matsim4urbansim.MATSim4Urbansim;
import playground.tnicolai.urbansim.utils.CommonUtilities;
import playground.tnicolai.urbansim.utils.UpdateXMLBindingClasses;
import playground.tnicolai.urbansim.utils.io.LoadFile;
import playground.tnicolai.urbansim.utils.io.TempDirectoryUtil;
import playground.tnicolai.urbansim.utils.securityManager.NoExitSecurityManager;


/**
 * @author thomas
 *
 */
public class CreateXSDBindingClassTest extends MatsimTestCase {
	
	private static final Logger log = Logger.getLogger(CreateXSDBindingClassTest.class);
	private SecurityManager securityManagerSwap = null;
	
	@Before
	/**
	 * replaces the security manager in order to catch a System.exit() call from 
	 * external XJCFacade package used in "UpdateXMLBindingClasses".
	 */
	public void setUp() {
	    securityManagerSwap = System.getSecurityManager();
	    System.setSecurityManager(new NoExitSecurityManager());
	}
 
	
	@Test
	public void testCreateBindingClass(){
		prepareTest();
	}
	
	@After
	/**
	 * restores the security manager again.
	 */
	public void tearDown() {
	    System.setSecurityManager(securityManagerSwap);
	}

	
	/**
	 * preparing creation of binding classes test run
	 */
	private void prepareTest(){
		
		String destination = Constants.MATSIM_WORKING_DIRECTORY + "/tnicolai/src/test/java/";
		String packageName = "playground.tnicolai.urbansim.jaxbTest.org.bindingClassesTest";
		
		String[] args = {"--destination="+destination, 
						 "--package="+packageName};
		log.info("Strating UpdateXMLBindingClasses with following arguments: ");
		log.info("");
		log.info("Destination : " + destination);
		log.info("Package name : " + packageName);
		log.info("");
		
		 UpdateXMLBindingClasses.main(args);
		
		// test creation of binding classes
		 Assert.assertTrue(testBindingClassesViaJAXB());
		
		// remove all created temp directories
		TempDirectoryUtil.cleaningUpCustomTempDirectories();
	}
	
	
	private boolean testBindingClassesViaJAXB(){
		try{
			JAXBContext jaxbContext = JAXBContext.newInstance(playground.tnicolai.urbansim.jaxbTest.org.bindingClassesTest.ObjectFactory.class);
			// create an unmaschaller (write xml file)
			Unmarshaller unmarschaller = jaxbContext.createUnmarshaller();
	
			// crate a schema factory ...
			SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
			// ... and initialize it with an xsd (xsd lies in the urbansim project)
			
			LoadFile loadFile = new LoadFile(Constants.MATSim_4_UrbanSim_XSD, TempDirectoryUtil.createCustomTempDirectory("xsd"), "MATSim4UrbanSimConfigSchema.xsd");
			File file2XSD = loadFile.loadMATSim4UrbanSimXSD();
			
			// for debugging
			// File file2XSD = new File( "/Users/thomas/Development/workspace/urbansim_trunk/opus_matsim/sustain_city/models/pyxb_xml_parser/MATSim4UrbanSimConfigSchema.xsd" ); 
			if(file2XSD == null || !file2XSD.exists()){
				
				log.warn(file2XSD.getCanonicalPath() + " is not available. Loading compensatory xsd instead (this may be is an older version).");
				log.warn("Compensatory xsd file: " + CommonUtilities.getCurrentPath(MATSim4Urbansim.class) + "tmp/MATSim4UrbanSimConfigSchema.xsd");
				
				return false;
			}
			log.info("Using following xsd schema: " + file2XSD.getCanonicalPath());
			// create a schema object via the given xsd to validate the MATSim xml config.
			Schema schema = schemaFactory.newSchema(file2XSD);
			// set the schema for validation while reading/importing the MATSim xml config.
			unmarschaller.setSchema(schema);
			
			File inputFile = new File( CommonUtilities.getTestMATSimConfigDir( CreateXSDBindingClassTest.class ) + "matsim_config_test_run.xml" );
			if(!inputFile.exists())
				log.error(inputFile.getCanonicalPath() + " not found!!!");
			// contains the content of the MATSim config.
			Object object = unmarschaller.unmarshal(inputFile);
			
			// Java representation of the schema file.
			playground.tnicolai.urbansim.jaxbTest.org.bindingClassesTest.MatsimConfigType matsimConfig;
			
			// The structure of both objects must match.
			if(object.getClass() == playground.tnicolai.urbansim.jaxbTest.org.bindingClassesTest.MatsimConfigType.class)
				matsimConfig = (playground.tnicolai.urbansim.jaxbTest.org.bindingClassesTest.MatsimConfigType) object;
			else
				matsimConfig = (( JAXBElement<playground.tnicolai.urbansim.jaxbTest.org.bindingClassesTest.MatsimConfigType>) object).getValue();
			
			if(matsimConfig != null)
				return true;
		}
		catch(Exception e){
			e.printStackTrace();
			return false;
		}
		return false;
	}

}

