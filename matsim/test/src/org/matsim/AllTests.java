/* *********************************************************************** *
 * project: org.matsim.*
 * AllTests.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007, 2008 by the members listed in the COPYING,  *
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

package org.matsim;

import java.io.File;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.matsim.core.utils.io.IOUtils;

public class AllTests {

	public static Test suite() {

		File outputDir = new File("test/output");
		if (outputDir.exists()) {
			IOUtils.deleteDirectory(outputDir);
		}
		if (!outputDir.mkdir()) {
			System.err.println("Could not create output directory " + outputDir + ". Trying to continue anyway.");
		}

		TestSuite suite = new TestSuite("All tests for org.matsim");
		//$JUnit-BEGIN$

		// first run api tests
		suite.addTest(org.matsim.api.basic.v01.AllTests.suite());
		suite.addTest(org.matsim.api.core.v01.AllTests.suite());
		
		// second run core test
		suite.addTest(org.matsim.core.AllTests.suite());

		// now run other unit tests
		suite.addTest(org.matsim.analysis.AllTests.suite());
		suite.addTest(org.matsim.counts.AllTests.suite());
		suite.addTest(org.matsim.demandmodeling.AllTests.suite());
		suite.addTest(org.matsim.evacuation.AllTests.suite());
		suite.addTest(org.matsim.facilities.AllTests.suite());
		suite.addTest(org.matsim.households.AllTests.suite());
		suite.addTest(org.matsim.knowledges.AllTests.suite());
		suite.addTest(org.matsim.locationchoice.AllTests.suite());
		suite.addTest(org.matsim.planomat.AllTests.suite());
		suite.addTest(org.matsim.population.AllTests.suite());
		suite.addTest(org.matsim.pt.AllTests.suite());
		suite.addTest(org.matsim.ptproject.AllTests.suite());
		suite.addTest(org.matsim.roadpricing.AllTests.suite());
		suite.addTest(org.matsim.signalsystems.AllTests.suite());
		suite.addTest(org.matsim.transitSchedule.AllTests.suite());
		suite.addTest(org.matsim.utils.AllTests.suite());
		suite.addTest(org.matsim.vehicles.AllTests.suite());
		suite.addTest(org.matsim.vis.AllTests.suite());
		suite.addTest(org.matsim.withinday.AllTests.suite());
		suite.addTest(org.matsim.world.AllTests.suite());

		// run integration tests
		suite.addTest(org.matsim.integration.AllTests.suite());

		// run applications and examples (=extensive integration tests)
		suite.addTest(org.matsim.run.AllTests.suite());
		suite.addTest(org.matsim.examples.AllTests.suite());
		
		//$JUnit-END$
		return suite;
	}

}
