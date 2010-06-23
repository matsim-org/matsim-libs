/* *********************************************************************** *
 * project: org.matsim.*
 * MyRunsTest.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2008 by the members listed in the COPYING,        *
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

package playground.meisterk;

import java.io.File;

import org.junit.Test;
import org.matsim.testcases.MatsimTestCase;

public class MyRunsTest extends MatsimTestCase {

	@Test
	public void testMoveInitDemandToDifferentNetwork() {
		fail("Not yet implemented");
	}

	@Test
	public void testDoSUEStudySensitivityAnalysis() {

		String[] args = new String[]{this.getInputDirectory() + "config.xml"};

		MyRuns myRuns = new MyRuns();
		myRuns.doSUEStudySensitivityAnalysis(args, this.getOutputDirectory());

		File aRandomResultsFile = new File(this.getOutputDirectory() + "timingModule_Planomat/brainExpBeta_1.0/learningRate_0.1/personTreatment.txt");
		assertTrue(aRandomResultsFile.exists());
		aRandomResultsFile = new File(this.getOutputDirectory() + "timingModule_Planomat/brainExpBeta_1.0/learningRate_0.1/ITERS/it.2");

	}

}
