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
package playground.tnicolai.matsim4opus.jaxbTest;

import java.io.File;
import java.io.IOException;

import javax.xml.XMLConstants;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.validation.Schema;
import javax.xml.validation.SchemaFactory;

import org.apache.log4j.Logger;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.matsim.testcases.MatsimTestCase;

import playground.tnicolai.matsim4opus.constants.Constants;
import playground.tnicolai.matsim4opus.jaxbTest.test.MatsimConfigType;
import playground.tnicolai.matsim4opus.jaxbTest.test.ObjectFactory;
import playground.tnicolai.matsim4opus.matsimTestData.GenerateOPUSTestEnvironment;
import playground.tnicolai.matsim4opus.utils.UpdateXMLBindingClasses;
import playground.tnicolai.matsim4opus.utils.io.LoadFile;
import playground.tnicolai.matsim4opus.utils.io.TempDirectoryUtil;
import playground.tnicolai.matsim4opus.utils.securityManager.NoExitSecurityManager;



/**
 * @author thomas
 *
 */
public class CreateXSDBindingClassTest extends MatsimTestCase {
	
	private static final Logger log = Logger.getLogger(CreateXSDBindingClassTest.class);
	private SecurityManager securityManagerSwap = null;
	private GenerateOPUSTestEnvironment gote = null;
	
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
//		String matsimConfigPath = prepareTest();
//		boolean result = testBindingClassesViaJAXB(matsimConfigPath);
//		
//		// test creation of binding classes
//		 Assert.assertTrue( result );
	}
	
	@After
	/**
	 * restores the security manager again.
	 */
	public void tearDown() {
	    System.setSecurityManager(securityManagerSwap);
		// remove all created temp directories
		TempDirectoryUtil.cleaningUpCustomTempDirectories();
	}

	
	/**
	 * preparing creation of binding classes test run
	 */
	private String prepareTest(){
		
		gote = new GenerateOPUSTestEnvironment( Boolean.TRUE );
		String matsimConfigPath = gote.createOPUSTestEnvironment();
		
		// define were to store the jaxb binding classes ...
		String destination = Constants.MATSIM_WORKING_DIRECTORY + "/src/test/java/";
		String packageName = new CreateXSDBindingClassTest().getClass().getPackage().getName() + ".test";
		String[] args = {"--destination="+destination,
						 "--package="+packageName};
		log.info("Strating UpdateXMLBindingClasses with following arguments: ");
		log.info("");
		log.info("Destination : " + destination);
		log.info("Package name : " + packageName);
		log.info("");
		
		// generating new jaxb binding classes
		UpdateXMLBindingClasses.main(args);
		
		return matsimConfigPath;
	}
	
	
	private boolean testBindingClassesViaJAXB(String matsimConfigPath){
		
		// Java representation of the schema file.
		MatsimConfigType matsimConfig = null;
		
		log.info("Staring unmaschalling MATSim configuration from: " + matsimConfigPath );
		log.info("...");
		try{
			JAXBContext jaxbContext = JAXBContext.newInstance(ObjectFactory.class);
			// create an unmaschaller (write xml file)
			Unmarshaller unmarschaller = jaxbContext.createUnmarshaller();

			// crate a schema factory ...
			SchemaFactory schemaFactory = SchemaFactory.newInstance( XMLConstants.W3C_XML_SCHEMA_NS_URI );
			// ... and initialize it with an xsd (xsd lies in the urbansim project)
			
			String tempDir = TempDirectoryUtil.createCustomTempDirectory("tmp");

			// init loadFile object: it downloads a xsd from matsim.org into a temp directory
			LoadFile loadFile = new LoadFile(Constants.MATSIM_4_URBANSIM_XSD, tempDir , Constants.XSD_FILE_NAME);
			File file2XSD = loadFile.loadMATSim4UrbanSimXSD(); // trigger loadFile
			
			if(file2XSD == null || !file2XSD.exists()){
				log.error(file2XSD.getCanonicalPath() + " not found!!!");
				return Boolean.FALSE;
			}
			
			log.info("Using following xsd schema: " + file2XSD.getCanonicalPath());
			
			// create a schema object via the given xsd to validate the MATSim xml config.
			Schema schema = schemaFactory.newSchema(file2XSD);
			// set the schema for validation while reading/importing the MATSim xml config.
			unmarschaller.setSchema(schema);
			
			File inputFile = new File( matsimConfigPath );
			if(!inputFile.exists())
				log.error(inputFile.getCanonicalPath() + " not found!!!");
			// contains the content of the MATSim config.
			Object object = unmarschaller.unmarshal(inputFile);
			
			// The structure of both objects must match.
			if(object.getClass() == MatsimConfigType.class)
				matsimConfig = (MatsimConfigType) object;
			else
				matsimConfig = (( JAXBElement<MatsimConfigType>) object).getValue();
			
		} catch(JAXBException je){
			je.printStackTrace();
			return Boolean.FALSE;
		} catch(IOException ioe){
			ioe.printStackTrace();
			return Boolean.FALSE;
		} catch(Exception e){
			e.printStackTrace();
			return Boolean.FALSE;
		}

		log.info("... finished unmarschallig");
		// return initialized object representation of matsim4urbansim config file
		return (matsimConfig != null);
	}

}

