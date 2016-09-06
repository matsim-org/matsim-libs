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
package tutorial.fixedTimeSignals;

import java.io.IOException;

import org.junit.Assert;
import org.junit.Test;
import org.matsim.core.utils.misc.CRCChecksum;

/**
 * @author tthunig
 *
 */
public class CreateSignalInputExampleWithLanesTest {

	private static final String DIR_TO_COMPARE_WITH = "./examples/tutorial/example90TrafficLights/useSignalInput/withLanes/";
	private static final String TEST_OUTPUT_DIR = "./output/example90TrafficLights/";
	
	@Test
	public void testCreateSignalInputExampleWithLanes(){
		try {
			(new CreateSignalInputWithLanesExample()).run();
		} catch (IOException e) {
			e.printStackTrace();
			Assert.fail("something went wrong") ;
		}
		// compare signal output
		Assert.assertEquals("different signal system files", 
				CRCChecksum.getCRCFromFile(TEST_OUTPUT_DIR + "signal_systems.xml"), 
				CRCChecksum.getCRCFromFile(DIR_TO_COMPARE_WITH + "signal_systems.xml"));
		Assert.assertEquals("different signal group files", 
				CRCChecksum.getCRCFromFile(TEST_OUTPUT_DIR + "signal_groups.xml"),
				CRCChecksum.getCRCFromFile(DIR_TO_COMPARE_WITH + "signal_groups.xml"));
		Assert.assertEquals("different signal control files", 
				CRCChecksum.getCRCFromFile(TEST_OUTPUT_DIR + "signal_groups.xml"),
				CRCChecksum.getCRCFromFile(DIR_TO_COMPARE_WITH + "signal_groups.xml"));
		Assert.assertEquals("different lane files", 
				CRCChecksum.getCRCFromFile(TEST_OUTPUT_DIR + "lane_definitions_v2.0.xml"),
				CRCChecksum.getCRCFromFile(DIR_TO_COMPARE_WITH + "lane_definitions_v2.0.xml"));
	}
	
}
