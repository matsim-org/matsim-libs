/* *********************************************************************** *
 * project: org.matsim.*
 * MyXmlConverterTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package playground.jjoubert.Utilities;

import org.matsim.testcases.MatsimTestCase;

import playground.jjoubert.Utilities.MyXmlConverter;

public class MyXmlConverterTest extends MatsimTestCase{

	/**
	 * This test creates a dummy XML object, and tests the XML converter with no
	 * argument about the log messages being silent in the constructor.
	 */
	public void testMyXmlConverterNoMessage(){
		String theFile = getOutputDirectory() + "TestFile.txt";
		
		MyTestXmlObject testObject = new MyTestXmlObject("Type", 123, true);
		
		MyXmlConverter xmlConverter = new MyXmlConverter();
		xmlConverter.writeObjectToFile(testObject, theFile);
		
		Object newObject = xmlConverter.readObjectFromFile(theFile);
		assertTrue("The read file should be of type MyTestXmlObject.", (newObject instanceof MyTestXmlObject));
		if(newObject instanceof MyTestXmlObject){
			newObject = (MyTestXmlObject) newObject;
			assertTrue("Attribute 'Type' converted incorrectly.", ((MyTestXmlObject) newObject).getType().equalsIgnoreCase("Type"));
			assertTrue("Attribute 'number' converted incorrectly.", ((MyTestXmlObject) newObject).getNumber() == 123);
			assertTrue("Attribute 'test' converted incorrectly.", ((MyTestXmlObject) newObject).isTest() == true);
		}
	}

	/**
	 * This test creates a dummy XML object, and tests the XML converter with the
	 * argument about the log messages being silent set to <code>true</code> in the
	 * constructor.
	 */
	public void testMyXmlConverterTrue(){
		String theFile = getOutputDirectory() + "TestFile.txt";
		
		MyTestXmlObject testObject = new MyTestXmlObject("Type", 123, true);
		
		MyXmlConverter xmlConverter = new MyXmlConverter(true);
		xmlConverter.writeObjectToFile(testObject, theFile);
		
		Object newObject = xmlConverter.readObjectFromFile(theFile);
		assertTrue("The read file should be of type MyTestXmlObject.", (newObject instanceof MyTestXmlObject));
		if(newObject instanceof MyTestXmlObject){
			newObject = (MyTestXmlObject) newObject;
			assertTrue("Attribute 'Type' converted incorrectly.", ((MyTestXmlObject) newObject).getType().equalsIgnoreCase("Type"));
			assertTrue("Attribute 'number' converted incorrectly.", ((MyTestXmlObject) newObject).getNumber() == 123);
			assertTrue("Attribute 'test' converted incorrectly.", ((MyTestXmlObject) newObject).isTest() == true);
		}
	}
	
	/**
	 * This test creates a dummy XML object, and tests the XML converter with the
	 * argument about the log messages being silent set to <code>false</code> in the
	 * constructor.
	 */
	public void testMyXmlConverterFalse(){
		String theFile = getOutputDirectory() + "TestFile.txt";
		
		MyTestXmlObject testObject = new MyTestXmlObject("Type", 123, true);
		
		MyXmlConverter xmlConverter = new MyXmlConverter(false);
		xmlConverter.writeObjectToFile(testObject, theFile);
		
		Object newObject = xmlConverter.readObjectFromFile(theFile);
		assertTrue("The read file should be of type MyTestXmlObject.", (newObject instanceof MyTestXmlObject));
		if(newObject instanceof MyTestXmlObject){
			newObject = (MyTestXmlObject) newObject;
			assertTrue("Attribute 'Type' converted incorrectly.", ((MyTestXmlObject) newObject).getType().equalsIgnoreCase("Type"));
			assertTrue("Attribute 'number' converted incorrectly.", ((MyTestXmlObject) newObject).getNumber() == 123);
			assertTrue("Attribute 'test' converted incorrectly.", ((MyTestXmlObject) newObject).isTest() == true);
		}
	}

}
