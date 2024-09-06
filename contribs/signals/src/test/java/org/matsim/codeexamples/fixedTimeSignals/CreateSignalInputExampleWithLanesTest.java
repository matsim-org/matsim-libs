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

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.matsim.core.utils.misc.CRCChecksum;
import org.matsim.testcases.MatsimTestUtils;

/**
 * @author tthunig
 *
 */
public class CreateSignalInputExampleWithLanesTest {

	private static final String DIR_TO_COMPARE_WITH = "./examples/tutorial/example90TrafficLights/useSignalInput/withLanes/";

	@RegisterExtension private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void testCreateSignalInputExampleWithLanes(){
		try {
			(new CreateSignalInputWithLanesExample()).run(testUtils.getOutputDirectory());
		} catch (IOException e) {
			e.printStackTrace();
			Assertions.fail("something went wrong") ;
		}
		// compare signal output
		Assertions.assertEquals(CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "signal_systems.xml"),
				CRCChecksum.getCRCFromFile(DIR_TO_COMPARE_WITH + "signal_systems.xml"),
				"different signal system files");
		Assertions.assertEquals(CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "signal_groups.xml"),
				CRCChecksum.getCRCFromFile(DIR_TO_COMPARE_WITH + "signal_groups.xml"),
				"different signal group files");
		Assertions.assertEquals(CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "signal_groups.xml"),
				CRCChecksum.getCRCFromFile(DIR_TO_COMPARE_WITH + "signal_groups.xml"),
				"different signal control files");
		Assertions.assertEquals(CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "lane_definitions_v2.0.xml"),
				CRCChecksum.getCRCFromFile(DIR_TO_COMPARE_WITH + "lane_definitions_v2.0.xml"),
				"different lane files");
	}

}
