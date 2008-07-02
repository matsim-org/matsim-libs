/* *********************************************************************** *
 * project: org.matsim.*
 * KmlNetworkWriter.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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
package org.matsim.demandmodeling;

import java.io.IOException;

import org.matsim.plans.Plans;
import org.matsim.testcases.MatsimTestCase;

import junit.framework.TestCase;


/**
 * @author dgrether
 *
 */
public class PopulationAsciiFileReaderTest extends MatsimTestCase {

	private static final String filename = "asciipopulation.txt";
	
	/**
	 * @see junit.framework.TestCase#setUp()
	 */
	protected void setUp() throws Exception {
		super.setUp();
	}
	
	public void testReadFile() throws IOException {
		String f = this.getClassInputDirectory() + filename;
		PopulationAsciiFileReader p = new PopulationAsciiFileReader();
		Plans plans = p.readFile(f);
		
		
		
		
		
	}
	
	
	
	

}
