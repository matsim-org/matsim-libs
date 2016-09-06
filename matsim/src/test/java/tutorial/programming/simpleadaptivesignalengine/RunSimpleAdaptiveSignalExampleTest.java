/*
 *  *********************************************************************** *
 *  * project: org.matsim.*
 *  * DefaultControlerModules.java
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  * copyright       : (C) 2014 by the members listed in the COPYING, *
 *  *                   LICENSE and WARRANTY file.                            *
 *  * email           : info at matsim dot org                                *
 *  *                                                                         *
 *  * *********************************************************************** *
 *  *                                                                         *
 *  *   This program is free software; you can redistribute it and/or modify  *
 *  *   it under the terms of the GNU General Public License as published by  *
 *  *   the Free Software Foundation; either version 2 of the License, or     *
 *  *   (at your option) any later version.                                   *
 *  *   See also COPYING, LICENSE and WARRANTY file                           *
 *  *                                                                         *
 *  * ***********************************************************************
 */
package tutorial.programming.simpleadaptivesignalengine;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

import tutorial.programming.simpleAdaptiveSignalEngine.RunSimpleAdaptiveSignalExample;

/**
 * @author tthunig
 *
 */
public class RunSimpleAdaptiveSignalExampleTest {
	
	@Rule public MatsimTestUtils testUtils = new MatsimTestUtils();

	private static final String OUTPUT_DIR = "./output/simpleAdaptiveSignalEngineExample/";
	
	@Test
	public void testRunSimpleAdaptiveSignalExample(){
		RunSimpleAdaptiveSignalExample.main(null);
		
		// compare event files
		Assert.assertEquals("different event files", 
				CRCChecksum.getCRCFromFile(OUTPUT_DIR + "output_events.xml.gz"), 
				CRCChecksum.getCRCFromFile(testUtils.getClassInputDirectory() + "output_events.xml.gz"));
	}
	
}
