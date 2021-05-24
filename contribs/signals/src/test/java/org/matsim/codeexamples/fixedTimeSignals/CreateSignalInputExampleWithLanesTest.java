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

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author tthunig
 *
 */
public class CreateSignalInputExampleWithLanesTest {

	private static final String DIR_TO_COMPARE_WITH = "./examples/tutorial/example90TrafficLights/useSignalInput/withLanes/";
	
	@Rule public MatsimTestUtils testUtils = new MatsimTestUtils();
	
	@Test
	public void testCreateSignalInputExampleWithLanes(){
		try {
			(new CreateSignalInputWithLanesExample()).run(testUtils.getOutputDirectory());
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("something went wrong") ;
		}
		// compare signal output
		Assert.assertEquals("different signal system files", 
				CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "signal_systems.xml"), 
				CRCChecksum.getCRCFromFile(DIR_TO_COMPARE_WITH + "signal_systems.xml"));
		Assert.assertEquals("different signal group files", 
				CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "signal_groups.xml"),
				CRCChecksum.getCRCFromFile(DIR_TO_COMPARE_WITH + "signal_groups.xml"));
		Assert.assertEquals("different signal control files", 
				CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "signal_groups.xml"),
				CRCChecksum.getCRCFromFile(DIR_TO_COMPARE_WITH + "signal_groups.xml"));
		Assert.assertEquals("different lane files", 
				CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "lane_definitions_v2.0.xml"),
				CRCChecksum.getCRCFromFile(DIR_TO_COMPARE_WITH + "lane_definitions_v2.0.xml"));
	}
	
}
