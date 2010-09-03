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

import org.apache.log4j.Logger;
import org.junit.Ignore;
import org.junit.Test;
import org.matsim.testcases.MatsimTestCase;

import playground.tnicolai.urbansim.UpdateXMLBindingClasses;
import playground.tnicolai.urbansim.utils.CommonUtilities;
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

