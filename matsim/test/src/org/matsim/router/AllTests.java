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

package org.matsim.router;

import junit.framework.Test;
import junit.framework.TestSuite;

import org.matsim.testcases.TestDepth;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for org.matsim.router");
		//$JUnit-BEGIN$
		suite.addTestSuite(DijkstraTest.class);
		suite.addTestSuite(AStarEuclideanTest.class);
		suite.addTestSuite(AStarLandmarksTest.class);
		if (TestDepth.getDepth() == TestDepth.extended) {
			suite.addTestSuite(RoutingTest.class);
		}
		//$JUnit-END$
		return suite;
	}

}
