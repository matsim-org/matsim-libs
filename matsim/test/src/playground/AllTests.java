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

import junit.framework.Test;
import junit.framework.TestSuite;

public class AllTests {

	public static Test suite() {

	

		TestSuite suite = new TestSuite("All tests for MATSim-playground");
		//$JUnit-BEGIN$

		// run unit tests
		suite.addTest(playground.benjamin.AllTests.suite());
		suite.addTest(playground.gregor.withindayevac.AllTests.suite());
		suite.addTest(playground.johannes.AllTests.suite());
		suite.addTest(playground.marcel.AllTests.suite());
		suite.addTest(playground.meisterk.AllTests.suite());
		suite.addTest(playground.wrashid.AllTests.suite());
		
		//$JUnit-END$
		return suite;
	}

}
