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
package org.matsim.codeexamples.fixedTimeSignals;

import java.io.IOException;

import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author tthunig
 *
 */
public class CreateSignalInputExampleTest {
	private static final Logger log = Logger.getLogger( CreateSignalInputExampleTest.class ) ;

	private static final String DIR_TO_COMPARE_WITH = "./examples/tutorial/example90TrafficLights/useSignalInput/woLanes/";

	@Rule public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	public void testCreateSignalInputExample(){
		try {
			(new CreateSignalInputExample()).run(testUtils.getOutputDirectory());
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("something went wrong") ;
		}
		// compare signal output
		{
			final String outputFilename = testUtils.getOutputDirectory() + "signal_systems.xml";
			final String referenceFilename = DIR_TO_COMPARE_WITH + "signal_systems.xml";
			log.info( "outputFilename=" + outputFilename ) ;
			log.info( "referenceFilename=" + referenceFilename ) ;
			Assert.assertEquals("different signal system files", 
					CRCChecksum.getCRCFromFile(outputFilename), 
					CRCChecksum.getCRCFromFile(referenceFilename));
		}
		Assert.assertEquals("different signal group files", 
				CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "signal_groups.xml"),
				CRCChecksum.getCRCFromFile(DIR_TO_COMPARE_WITH + "signal_groups.xml"));
		Assert.assertEquals("different signal control files", 
				CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "signal_groups.xml"),
				CRCChecksum.getCRCFromFile(DIR_TO_COMPARE_WITH + "signal_groups.xml"));
	}

}
