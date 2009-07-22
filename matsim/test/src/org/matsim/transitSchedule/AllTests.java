/* *********************************************************************** *
 * project: org.matsim.*
 * AllTests.java
 *                                                                         *
 * *********************************************************************** *
 *                                                                         *
 * copyright       : (C) 2009 by the members listed in the COPYING,        *
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

package org.matsim.transitSchedule;

import junit.framework.Test;
import junit.framework.TestSuite;

public abstract class AllTests {
	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for " + AllTests.class.getPackage().getName());

		suite.addTestSuite(DepartureTest.class);
		suite.addTestSuite(TransitLineTest.class);
		suite.addTestSuite(TransitRouteTest.class);
		suite.addTestSuite(TransitRouteStopTest.class);
		suite.addTestSuite(TransitStopFacilityTest.class);
		suite.addTestSuite(TransitScheduleTest.class);
		suite.addTestSuite(TransitScheduleBuilderTest.class);
		suite.addTestSuite(TransitScheduleReaderTest.class);
		suite.addTestSuite(TransitScheduleReaderV1Test.class);
		suite.addTestSuite(TransitScheduleFormatV1Test.class);

		return suite;
	}

}
