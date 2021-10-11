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

import org.junit.*;
import org.matsim.testcases.MatsimTestUtils;

import java.io.File;

import static org.junit.Assert.assertTrue;

public class RunFreightAnalysisTest {

	@Rule
	public MatsimTestUtils testUtils = new MatsimTestUtils();

	@Before
	public void runAnalysis(){
		final String packageInputDirectory = testUtils.getClassInputDirectory();
		final String outputDirectory = testUtils.getOutputDirectory();
		RunFreightAnalysis freightAnalysis = new RunFreightAnalysis(packageInputDirectory, outputDirectory);
		freightAnalysis.runAnalysis();
	}

	@Test
	public void compareResults() {
		String filename = "carrierStats.tsv";

		final String inputFilename = testUtils.getInputDirectory() + filename;
		final String outputFilename = testUtils.getOutputDirectory() + filename;
		MatsimTestUtils.compareFilesLineByLine(inputFilename, outputFilename);
		
		}
}