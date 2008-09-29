/* *********************************************************************** *
 * project: org.matsim.*
 * AllTests.java
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

package playground;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.matsim.utils.io.IOUtils;

public class AllTests {

	public static Test suite() {

		File outputDir = new File("test/output");
		if (outputDir.exists()) {
			IOUtils.deleteDirectory(outputDir);
		}
		if (!outputDir.mkdir()) {
			System.err.println("Could not create output directory " + outputDir + ". Trying to continue anyway.");
		}

		TestSuite suite = new TestSuite("All tests for MATSim-playground");
		//$JUnit-BEGIN$

		// run unit tests
		suite.addTest(playground.marcel.AllTests.suite());

		//$JUnit-END$
		return suite;
	}

}
