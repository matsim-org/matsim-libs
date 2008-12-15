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

package org.matsim.facilities;

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {

		TestSuite suite = new TestSuite("Tests for org.matsim.facilities");
		//$JUnit-BEGIN$
		suite.addTest(org.matsim.facilities.algorithms.AllTests.suite());
		suite.addTestSuite(FacilitiesTest.class);
		suite.addTestSuite(FacilitiesParserWriterTest.class);
		suite.addTestSuite(FacilityMoveTest.class);
		//$JUnit-END$
		return suite;
	}

}
