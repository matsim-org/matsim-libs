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

package playground.meisterk;

import junit.framework.Test;
import junit.framework.TestSuite;
import playground.meisterk.org.matsim.analysis.CalcLegTimesKTITest;

public class AllTests {

	public static Test suite() {
		TestSuite suite = new TestSuite("Tests for playground.meisterk");
		//$JUnit-BEGIN$
		suite.addTest(playground.meisterk.org.matsim.config.groups.AllTests.suite());
		suite.addTest(playground.meisterk.org.matsim.population.algorithms.AllTests.suite());
		suite.addTest(playground.meisterk.kti.AllTests.suite());
		suite.addTest(playground.meisterk.phd.AllTests.suite());
		suite.addTestSuite(CalcLegTimesKTITest.class);
		//$JUnit-END$
		return suite;
	}


}
