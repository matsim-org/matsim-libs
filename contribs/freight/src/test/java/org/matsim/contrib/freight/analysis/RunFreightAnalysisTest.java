/*
 *   *********************************************************************** *
 *   project: org.matsim.*
 *   *********************************************************************** *
 *                                                                           *
 *   copyright       : (C) 2021 by the members listed in the COPYING,        *
 *                     LICENSE and WARRANTY file.                            *
 *   email           : info at matsim dot org                                *
 *                                                                           *
 *   *********************************************************************** *
 *                                                                           *
 *     This program is free software; you can redistribute it and/or modify  *
 *     it under the terms of the GNU General Public License as published by  *
 *     the Free Software Foundation; either version 2 of the License, or     *
 *     (at your option) any later version.                                   *
 *     See also COPYING, LICENSE and WARRANTY file                           *
 *                                                                           *
 *   ***********************************************************************
 *
 */

package org.matsim.contrib.freight.analysis;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.matsim.testcases.MatsimTestUtils;

public class RunFreightAnalysisTest {

	@Rule
	public static MatsimTestUtils testUtils = new MatsimTestUtils();

	@Before
	public void analysisRun(){
		RunFreightAnalysis freightAnalysis = new RunFreightAnalysis(testUtils.getPackageInputDirectory(), testUtils.getOutputDirectory());
	}

	@Test
	public void compareOutput1() {
		String filename = "";
		testUtils.compareFilesLineByLine(testUtils.getOutputDirectory() + filename , testUtils.getInputDirectory() + filename);
	}
}