/* *********************************************************************** *
 * project: org.matsim.*
 * AllTests.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2007 by the members listed in the COPYING,        *
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

package org.matsim.counts;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {

		TestSuite suite = new TestSuite("Test for org.matsim.counts");
		//$JUnit-BEGIN$

		// functional/integration unit tests:---------------
		suite.addTestSuite(CountsParserWriterTest.class);
		suite.addTestSuite(CountsGraphWriterTest.class);
		suite.addTestSuite(CountsTableWriterTest.class);
		suite.addTestSuite(CountsKMLWriterTest.class);


		// logic unit tests --------------------------------
		suite.addTestSuite(OutputDelegateTest.class);

		suite.addTestSuite(CountsErrorGraphTest.class);
		suite.addTestSuite(CountsSimRealPerHourGraphTest.class);
		suite.addTestSuite(CountsLoadCurveGraphTest.class);

		suite.addTestSuite(CountTest.class);
		suite.addTestSuite(CountsTest.class);

		suite.addTestSuite(CountsParserTest.class);
		suite.addTestSuite(CountsComparisonAlgorithmTest.class);
		suite.addTestSuite(CountsReaderHandlerImplV1Test.class);
		// -------------------------------------------------
		//$JUnit-END$
		return suite;
	}

}

/*
 * Library jFreeChart located in matsimJ/libs/ needs to be included!
 *
 * No TDD, thus writing code for regression tests.
 *
 * JUnit version 4.x and its special features allowed to be used in MatsimJ-Testing??
 */
