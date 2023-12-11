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
public class CreateIntergreensExampleTest {

	private static final String DIR_TO_COMPARE_WITH = "./examples/tutorial/example90TrafficLights/useSignalInput/";

	@RegisterExtension private MatsimTestUtils testUtils = new MatsimTestUtils();

	@Test
	void testIntergreenExample(){
		try {
			String[] args = {testUtils.getOutputDirectory()};
			CreateIntergreensExample.main(args);
		} catch (IOException e) {
			e.printStackTrace();
			Assertions.fail("something went wrong") ;
		}
		// compare intergreen output
		Assertions.assertEquals(CRCChecksum.getCRCFromFile(testUtils.getOutputDirectory() + "intergreens.xml"),
				CRCChecksum.getCRCFromFile(DIR_TO_COMPARE_WITH + "intergreens.xml"),
				"different intergreen files");
	}

}
